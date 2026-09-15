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

import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Gestion admin de la gamification (#146) : consultation du catalogue complet (y compris
 * badges inactifs/manuels, pour le sélecteur d'attribution) et attribution manuelle des
 * badges {@code rule_type = 'manual'} (« Testeur émérite »). Réservé aux admins nationaux
 * -- le badge est global, pas borné à un périmètre départemental.
 */
@Path("/api/v1/admin/gamification")
@Produces(MediaType.APPLICATION_JSON)
public class GamificationAdminResource extends AbstractFisholaResource {

    @Inject
    protected GamificationDao gamificationDao;

    @GET
    @Path("/badges")
    public List<BadgeBean> listBadges() {
        checkIsNationalAdmin();
        return gamificationDao.listAllBadges().stream().map(GamificationResource::toBean).toList();
    }

    @POST
    @Path("/badges/{badgeId}/attribute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "gamification.badge.attribute", entityType = "gamification_badge_unlock", entityIdParam = "badgeId")
    public Response attribute(@PathParam("badgeId") UUID badgeId, AttributeBadgeBean bean) {
        FisholaAdmin admin = checkIsNationalAdmin();

        if (bean == null || bean.userId == null) {
            throw new BadRequestException("Pêcheur manquant");
        }
        GamificationDao.BadgeRow badge = gamificationDao.findBadge(badgeId)
                .orElseThrow(() -> new NotFoundException("Badge inconnu"));
        if (!"manual".equals(badge.ruleType())) {
            throw new BadRequestException("Ce badge n'est pas à attribution manuelle");
        }
        if (!gamificationDao.existsUser(bean.userId)) {
            throw new NotFoundException("Pêcheur inconnu");
        }

        gamificationDao.upsertUnlock(badgeId, bean.userId, (short) 0, java.util.Map.of(), false, admin.getId());
        return Response.noContent().build();
    }
}
