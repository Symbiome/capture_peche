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

import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Saisie manuelle opérateur, format « enquête terrain » (#144), organisée en pyramide
 * (#236) : une session d'enquête est ouverte, reçoit successivement ses sorties (pêcheurs
 * interrogés + captures + session souvenir facultative), puis est clôturée. Mêmes règles
 * que l'import dédié ; pendant de {@link
 * fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireManualEntryResource} (#143).
 */
@Path("/api/v1/admin/manual-entries/survey/sessions")
@Produces(MediaType.APPLICATION_JSON)
public class SurveyManualEntryResource extends AbstractFisholaResource {

    @Inject
    protected SurveySessionService sessionService;

    @Inject
    protected SurveySessionDao surveySessionDao;

    /**
     * Sessions ouvertes du compte connecté, à reprendre.
     *
     * @return sessions ouvertes, les plus récentes d'abord
     */
    @GET
    public List<SurveySessionView> listOpen() {
        FisholaAdmin admin = checkIsStaff();
        return surveySessionDao.listOpen(admin.getId());
    }

    /**
     * Ouvre une session d'enquête.
     *
     * @param bean date, heure de début, secteur et opérateur
     * @return 201 et la session, 400 et les erreurs
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "manual_entry.create.survey_session", entityType = "survey_session")
    public Response open(SurveySessionBean bean) {
        FisholaAdmin admin = checkIsStaff();
        SurveySessionResultBean result = sessionService.open(bean, admin.getId(), allowedWaterEntities(admin),
                LocalDate.now());
        return Response.status(result.errors.isEmpty() ? Response.Status.CREATED : Response.Status.BAD_REQUEST)
                .entity(result).build();
    }

    /**
     * Session d'enquête du compte connecté.
     *
     * @param id identifiant de la session
     * @return la session, 404 si inconnue ou ouverte par un autre compte
     */
    @GET
    @Path("/{id}")
    public SurveySessionView get(@PathParam("id") UUID id) {
        FisholaAdmin admin = checkIsStaff();
        return sessionService.get(id, admin.getId());
    }

    /**
     * Ajoute une sortie enquêtée à une session ouverte.
     *
     * @param id identifiant de la session
     * @param bean sortie, pêcheurs interrogés et captures
     * @return 201 et les trips créées, 400 et les erreurs (dont session clôturée)
     */
    @POST
    @Path("/{id}/sorties")
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "manual_entry.create.survey", entityType = "trip")
    public Response addSortie(@PathParam("id") UUID id, SurveySortieBean bean) {
        FisholaAdmin admin = checkIsStaff();
        SurveyManualResultBean result = sessionService.addSortie(id, bean, admin.getId(),
                allowedWaterEntities(admin), getAllowedAdminDepartments(), LocalDate.now());
        return Response.status(result.errors.isEmpty() ? Response.Status.CREATED : Response.Status.BAD_REQUEST)
                .entity(result).build();
    }

    /**
     * Clôture une session ouverte : heure de fin et pêcheurs observés non enquêtés.
     *
     * @param id identifiant de la session
     * @param bean heure de fin et compteurs
     * @return 200 et la session clôturée, 400 et les erreurs
     */
    @POST
    @Path("/{id}/close")
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "manual_entry.close.survey_session", entityType = "survey_session")
    public Response close(@PathParam("id") UUID id, SurveySessionCloseBean bean) {
        FisholaAdmin admin = checkIsStaff();
        SurveySessionResultBean result = sessionService.close(id, bean, admin.getId());
        return Response.status(result.errors.isEmpty() ? Response.Status.OK : Response.Status.BAD_REQUEST)
                .entity(result).build();
    }

    private Set<UUID> allowedWaterEntities(FisholaAdmin admin) {
        return admin.getIsNationalAdmin() ? null : adminDao.getAllowedWaterEntityIds(admin.getId());
    }
}
