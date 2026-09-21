package fr.inrae.fishola.rest.gamification;

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

import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.gamification.CompetitionDao;
import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gestion des concours de pêche (#90) : création par un opérateur (technicien de
 * fédération) et attribution manuelle du badge CONCOURS aux pêcheurs participants.
 * Contrairement à {@link GamificationAdminResource} (réservé au national), ces écrans sont
 * ouverts à l'opérateur via {@link #checkIsStaff()} et cloisonnés par périmètre
 * départemental (#159, comme {@code NewsResource}/{@code TripResource}) : un compte
 * régional/opérateur ne voit et ne modifie que les concours de ses départements.
 */
@Path("/api/v1/admin/competitions")
@Produces(MediaType.APPLICATION_JSON)
public class CompetitionResource extends AbstractFisholaResource {

    private static final String CONCOURS_BADGE_CODE = "CONCOURS";

    @Inject
    protected CompetitionDao competitionDao;

    @Inject
    protected GamificationDao gamificationDao;

    @Inject
    protected ReferentialDao referentialDao;

    @GET
    public List<CompetitionBean> listCompetitions() {
        checkIsStaff();
        return competitionDao.listCompetitions(getAllowedAdminDepartments()).stream().map(this::toBean).toList();
    }

    @GET
    @Path("/{competitionId}")
    public CompetitionBean getCompetition(@PathParam("competitionId") UUID competitionId) {
        checkIsStaff();
        CompetitionDao.CompetitionRow row = findCompetitionOrThrow(competitionId);
        assertInPerimeter(row.department());
        return toBean(row);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "competition.create", entityType = "competition")
    public CompetitionBean createCompetition(CreateCompetitionBean bean) {
        FisholaAdmin admin = checkIsStaff();
        if (bean == null || bean.name == null || bean.name.isBlank() || bean.date == null || bean.waterEntityId == null
                || bean.federationName == null || bean.federationName.isBlank()) {
            throw new BadRequestException("Nom, date, plan d'eau/cours d'eau et fédération organisatrice sont obligatoires");
        }
        String department = referentialDao.departmentByWaterEntityId(List.of(bean.waterEntityId)).get(bean.waterEntityId);
        if (department == null) {
            throw new NotFoundException("Plan d'eau ou cours d'eau inconnu");
        }
        assertInPerimeter(department);

        UUID id = competitionDao.createCompetition(bean.name, bean.date, bean.waterEntityId, bean.federationName, admin.getId());
        return toBean(competitionDao.findCompetition(id).orElseThrow());
    }

    @GET
    @Path("/{competitionId}/participants")
    public List<CompetitionParticipantBean> listParticipants(@PathParam("competitionId") UUID competitionId) {
        checkIsStaff();
        CompetitionDao.CompetitionRow row = findCompetitionOrThrow(competitionId);
        assertInPerimeter(row.department());
        return competitionDao.listParticipants(competitionId).stream().map(p -> {
            CompetitionParticipantBean participant = new CompetitionParticipantBean();
            participant.userId = p.userId();
            participant.pseudo = p.pseudo();
            participant.firstName = p.firstName();
            participant.lastName = p.lastName();
            participant.email = p.email();
            participant.unlockedAt = p.unlockedAt();
            return participant;
        }).toList();
    }

    @POST
    @Path("/{competitionId}/attribute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "competition.attribute", entityType = "competition", entityIdParam = "competitionId")
    public Response attribute(@PathParam("competitionId") UUID competitionId, AttributeCompetitionBean bean) {
        FisholaAdmin admin = checkIsStaff();
        if (bean == null || bean.userId == null) {
            throw new BadRequestException("Pêcheur manquant");
        }
        CompetitionDao.CompetitionRow competition = findCompetitionOrThrow(competitionId);
        assertInPerimeter(competition.department());
        if (!gamificationDao.existsUser(bean.userId)) {
            throw new NotFoundException("Pêcheur inconnu");
        }
        GamificationDao.BadgeRow concoursBadge = gamificationDao.findBadgeByCode(CONCOURS_BADGE_CODE)
                .orElseThrow(() -> new IllegalStateException("Badge CONCOURS manquant du catalogue"));

        gamificationDao.attributeCompetitionBadge(concoursBadge.id(), bean.userId, competitionId, admin.getId());
        return Response.noContent().build();
    }

    /** Recherche libre de pêcheur pour le sélecteur d'attribution (#90), ouverte à l'opérateur. */
    @GET
    @Path("/search-users")
    public List<UserSearchResultBean> searchUsers(@QueryParam("q") String q) {
        checkIsStaff();
        Preconditions.checkArgument(q != null && q.trim().length() >= 2, "Le paramètre q doit contenir au moins 2 caractères.");
        return competitionDao.searchUsers(q.trim(), 20).stream().map(u -> {
            UserSearchResultBean result = new UserSearchResultBean();
            result.id = u.id();
            result.pseudo = u.pseudo();
            result.firstName = u.firstName();
            result.lastName = u.lastName();
            result.email = u.email();
            return result;
        }).toList();
    }

    private CompetitionDao.CompetitionRow findCompetitionOrThrow(UUID competitionId) {
        return competitionDao.findCompetition(competitionId).orElseThrow(() -> new NotFoundException("Concours inconnu"));
    }

    // Un opérateur/admin régional ne peut créer/consulter qu'un concours dont le plan
    // d'eau relève de son périmètre départemental (#159). Un national (périmètre vide) passe.
    private void assertInPerimeter(String waterEntityDepartment) {
        Set<String> allowedDepartments = getAllowedAdminDepartments();
        if (!allowedDepartments.isEmpty() && !allowedDepartments.contains(waterEntityDepartment)) {
            throw new ForbiddenException("Ce concours est hors de votre périmètre départemental");
        }
    }

    private CompetitionBean toBean(CompetitionDao.CompetitionRow row) {
        CompetitionBean bean = new CompetitionBean();
        bean.id = row.id();
        bean.name = row.name();
        bean.date = row.competitionDate();
        bean.waterEntityId = row.waterEntityId();
        bean.waterEntityName = row.waterEntityName();
        bean.federationName = row.federationName();
        bean.createdBy = row.createdBy();
        bean.createdOn = row.createdOn();
        return bean;
    }
}
