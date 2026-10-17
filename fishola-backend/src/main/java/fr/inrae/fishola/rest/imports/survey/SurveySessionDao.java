package fr.inrae.fishola.rest.imports.survey;

/*-
 * #%L
 * Carnet de pêche :: Backend
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

import fr.inrae.fishola.database.AbstractCarnetDao;
import fr.inrae.fishola.entities.enums.SurveySessionStatus;
import jakarta.inject.Singleton;
import org.jooq.Record;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.inrae.fishola.entities.Tables.SURVEY_SESSION;

/**
 * Sessions d'enquête de la saisie pyramidale (#236) : ouverture, lecture, sessions
 * ouvertes d'un opérateur et clôture. Les sorties d'une session sont persistées par
 * {@link fr.inrae.fishola.rest.imports.ImportDao#saveSurveySortie}.
 */
@Singleton
public class SurveySessionDao extends AbstractCarnetDao {

    // Une sortie enquêtée produit une trip par pêcheur, d'external_ref « code sortie/pêcheur ».
    private static final String VIEW_SQL = "SELECT ss.*, we.name AS water_entity_name, "
            + "(SELECT count(DISTINCT split_part(t.external_ref, '/', 1)) FROM trip t "
            + "  WHERE t.survey_session_id = ss.id AND t.collection_method = 'enquete') AS sorties_count, "
            + "(SELECT count(*) FROM trip t "
            + "  WHERE t.survey_session_id = ss.id AND t.collection_method = 'enquete') AS anglers_count "
            + "FROM survey_session ss LEFT JOIN water_entity we ON we.id = ss.water_entity_id ";

    /**
     * Ouvre une session d'enquête.
     *
     * @param code code session généré
     * @param waterEntityId secteur
     * @param day date de la session
     * @param startTime heure de début
     * @param operatorName enquêteur
     * @param createdBy compte staff qui ouvre la session
     * @return identifiant de la session créée
     */
    public UUID create(String code, UUID waterEntityId, LocalDate day, LocalTime startTime, String operatorName,
                       UUID createdBy) {
        return withContext(context -> context.insertInto(SURVEY_SESSION,
                        SURVEY_SESSION.CODE, SURVEY_SESSION.WATER_ENTITY_ID, SURVEY_SESSION.DAY,
                        SURVEY_SESSION.START_TIME, SURVEY_SESSION.OPERATOR_NAME, SURVEY_SESSION.CREATED_BY,
                        SURVEY_SESSION.STATUS)
                .values(code, waterEntityId, day, startTime, operatorName, createdBy, SurveySessionStatus.ouverte)
                .returning(SURVEY_SESSION.ID)
                .fetchOne()
                .getId());
    }

    /**
     * Session d'enquête par identifiant.
     *
     * @param id identifiant de la session
     * @return la session, vide si inconnue
     */
    public Optional<SurveySessionView> find(UUID id) {
        return withContext(context -> context.fetchOptional(VIEW_SQL + "WHERE ss.id = ?", id)
                .map(SurveySessionDao::toView));
    }

    /**
     * Sessions encore ouvertes d'un compte staff, les plus récentes d'abord.
     *
     * @param createdBy compte staff qui les a ouvertes
     * @return sessions ouvertes à reprendre
     */
    public List<SurveySessionView> listOpen(UUID createdBy) {
        return withContext(context -> context.fetch(VIEW_SQL
                        + "WHERE ss.created_by = ? AND ss.status = 'ouverte' ORDER BY ss.day DESC, ss.start_time DESC",
                        createdBy)
                .map(SurveySessionDao::toView));
    }

    /**
     * Clôture une session : heure de fin et pêcheurs observés non enquêtés.
     *
     * @param id identifiant de la session
     * @param endTime heure de fin
     * @param unsurveyedShoreAnglers pêcheurs du bord non enquêtés
     * @param unsurveyedBoatAnglers pêcheurs en bateau non enquêtés
     */
    public void close(UUID id, LocalTime endTime, Short unsurveyedShoreAnglers, Short unsurveyedBoatAnglers) {
        withContextNoResult(context -> context.update(SURVEY_SESSION)
                .set(SURVEY_SESSION.END_TIME, endTime)
                .set(SURVEY_SESSION.UNSURVEYED_SHORE_ANGLERS, unsurveyedShoreAnglers)
                .set(SURVEY_SESSION.UNSURVEYED_BOAT_ANGLERS, unsurveyedBoatAnglers)
                .set(SURVEY_SESSION.STATUS, SurveySessionStatus.cloturee)
                .where(SURVEY_SESSION.ID.eq(id))
                .execute());
    }

    private static SurveySessionView toView(Record rec) {
        SurveySessionView view = new SurveySessionView();
        view.id = rec.get("id", UUID.class);
        view.code = rec.get("code", String.class);
        view.day = rec.get("day", LocalDate.class);
        view.startTime = rec.get("start_time", LocalTime.class);
        view.endTime = rec.get("end_time", LocalTime.class);
        view.waterEntityId = rec.get("water_entity_id", UUID.class);
        view.waterEntityName = rec.get("water_entity_name", String.class);
        view.operatorName = rec.get("operator_name", String.class);
        view.status = rec.get("status", String.class);
        view.unsurveyedShoreAnglers = rec.get("unsurveyed_shore_anglers", Short.class);
        view.unsurveyedBoatAnglers = rec.get("unsurveyed_boat_anglers", Short.class);
        view.createdBy = rec.get("created_by", UUID.class);
        view.sortiesCount = rec.get("sorties_count", Integer.class);
        view.anglersCount = rec.get("anglers_count", Integer.class);
        return view;
    }
}
