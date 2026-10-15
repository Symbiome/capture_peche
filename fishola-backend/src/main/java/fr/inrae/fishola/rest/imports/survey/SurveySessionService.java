package fr.inrae.fishola.rest.imports.survey;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import fr.inrae.fishola.rest.imports.ImportDao;
import fr.inrae.fishola.rest.imports.ManualError;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sessions d'enquête de la saisie pyramidale (#236) : une session est ouverte une fois,
 * reçoit successivement ses sorties ({@link SurveyManualEntryService}), puis est clôturée
 * avec son heure de fin et les pêcheurs observés non enquêtés. Seul le compte qui a
 * ouvert une session peut la compléter ou la clôturer.
 */
@Singleton
public class SurveySessionService {

    static final String STATUS_OPEN = "ouverte";
    static final String SESSION_CLOSED = "la session d'enquête est clôturée";

    @Inject
    protected SurveySessionDao surveySessionDao;

    @Inject
    protected ImportDao importDao;

    @Inject
    protected SurveyManualEntryService manualEntryService;

    /**
     * Valide puis ouvre une session d'enquête.
     *
     * @param bean session saisie
     * @param adminId compte staff qui ouvre la session
     * @param allowedWaterEntities secteurs du périmètre, null pour un national
     * @param today date du jour (refus des dates futures)
     * @return la session ouverte, ou les erreurs sans rien persister
     */
    public SurveySessionResultBean open(SurveySessionBean bean, UUID adminId, Set<UUID> allowedWaterEntities,
                                        LocalDate today) {
        List<ManualError> errors = new ArrayList<>();
        if (bean == null) {
            errors.add(new ManualError(null, null, "corps de requête manquant"));
            return new SurveySessionResultBean(null, errors);
        }
        if (bean.day == null) {
            errors.add(new ManualError(null, "day", "date obligatoire"));
        } else if (bean.day.isAfter(today)) {
            errors.add(new ManualError(null, "day", "la date ne peut pas être dans le futur"));
        }
        if (bean.startTime == null) {
            errors.add(new ManualError(null, "startTime", "heure de début de session obligatoire"));
        }
        if (StringUtils.isBlank(bean.operatorName)) {
            errors.add(new ManualError(null, "operatorName", "nom de l'opérateur obligatoire"));
        }
        UUID waterEntityId = bean.waterEntityId != null && importDao.existsWaterEntity(bean.waterEntityId)
                ? bean.waterEntityId : importDao.resolveWaterEntity(bean.secteur, null);
        manualEntryService.validateWaterEntity(waterEntityId, allowedWaterEntities, null, "waterEntityId", "secteur",
                errors);
        if (!errors.isEmpty()) {
            return new SurveySessionResultBean(null, errors);
        }
        UUID id = surveySessionDao.create("MANUEL-" + UUID.randomUUID(), waterEntityId, bean.day, bean.startTime,
                bean.operatorName.strip(), adminId);
        return new SurveySessionResultBean(surveySessionDao.find(id).orElseThrow(), List.of());
    }

    /**
     * Session d'enquête du compte, quel que soit son état.
     *
     * @param id identifiant de la session
     * @param adminId compte staff connecté
     * @return la session
     * @throws NotFoundException si la session est inconnue ou ouverte par un autre compte
     */
    public SurveySessionView get(UUID id, UUID adminId) {
        return surveySessionDao.find(id)
                .filter(session -> adminId.equals(session.createdBy))
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Ajoute une sortie enquêtée à une session ouverte du compte.
     *
     * @param id identifiant de la session
     * @param bean sortie saisie
     * @param adminId compte staff connecté
     * @param allowedWaterEntities secteurs du périmètre, null pour un national
     * @param allowedDepartments départements du périmètre, vide pour un national
     * @param today date du jour
     * @return trips créées, ou erreurs (dont session clôturée)
     */
    public SurveyManualResultBean addSortie(UUID id, SurveySortieBean bean, UUID adminId,
                                            Set<UUID> allowedWaterEntities, Set<String> allowedDepartments,
                                            LocalDate today) {
        SurveySessionView session = get(id, adminId);
        if (!STATUS_OPEN.equals(session.status)) {
            return new SurveyManualResultBean(List.of(), 0, List.of(new ManualError(null, null, SESSION_CLOSED)));
        }
        return manualEntryService.submit(session, bean, allowedWaterEntities, allowedDepartments, today);
    }

    /**
     * Clôture une session ouverte du compte : heure de fin et pêcheurs non enquêtés.
     *
     * @param id identifiant de la session
     * @param bean clôture saisie
     * @param adminId compte staff connecté
     * @return la session clôturée, ou les erreurs sans rien modifier
     */
    public SurveySessionResultBean close(UUID id, SurveySessionCloseBean bean, UUID adminId) {
        SurveySessionView session = get(id, adminId);
        List<ManualError> errors = new ArrayList<>();
        if (!STATUS_OPEN.equals(session.status)) {
            errors.add(new ManualError(null, null, SESSION_CLOSED));
            return new SurveySessionResultBean(session, errors);
        }
        if (bean == null || bean.endTime == null) {
            errors.add(new ManualError(null, "endTime", "heure de fin de session obligatoire"));
        } else if (session.startTime != null && !bean.endTime.isAfter(session.startTime)) {
            errors.add(new ManualError(null, "endTime",
                    "l'heure de fin de session doit être postérieure à son heure de début"));
        }
        Integer shore = bean == null ? null : bean.unsurveyedShoreAnglers;
        Integer boat = bean == null ? null : bean.unsurveyedBoatAnglers;
        validateCount(shore, "unsurveyedShoreAnglers", errors);
        validateCount(boat, "unsurveyedBoatAnglers", errors);
        if (!errors.isEmpty()) {
            return new SurveySessionResultBean(session, errors);
        }
        surveySessionDao.close(id, bean.endTime, shore == null ? null : shore.shortValue(),
                boat == null ? null : boat.shortValue());
        return new SurveySessionResultBean(surveySessionDao.find(id).orElseThrow(), List.of());
    }

    private static void validateCount(Integer count, String field, List<ManualError> errors) {
        if (count != null && count < 0) {
            errors.add(new ManualError(null, field, "doit être ≥ 0"));
        }
    }
}
