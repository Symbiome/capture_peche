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

import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consultation des badges par le pêcheur lui-même (#146) : le catalogue actif, avec son
 * propre état de déblocage. Pendant de {@link GamificationAdminResource} côté admin.
 */
@Path("/api/v1/gamification")
@Produces(MediaType.APPLICATION_JSON)
public class GamificationResource extends AbstractFisholaResource {

    @Inject
    protected GamificationDao gamificationDao;

    @GET
    @Path("/me/badges")
    public Response myBadges() {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        // #90 : un badge peut désormais avoir plusieurs déblocages pour un même pêcheur
        // (CONCOURS, un par concours) -- on groupe donc par badge plutôt que de ne garder
        // que le plus récent, pour ensuite émettre une entrée par déblocage.
        Map<UUID, List<GamificationDao.UnlockRow>> unlocksByBadgeId = gamificationDao.listUnlocksForUser(userId)
                .stream().collect(java.util.stream.Collectors.groupingBy(GamificationDao.UnlockRow::badgeId));

        List<BadgeBean> badges = gamificationDao.listActiveBadges().stream().flatMap(badge -> {
            List<GamificationDao.UnlockRow> unlocks = unlocksByBadgeId.get(badge.id());
            if (unlocks == null || unlocks.isEmpty()) {
                return java.util.stream.Stream.of(toBean(badge));
            }
            return unlocks.stream().map(unlock -> {
                BadgeBean bean = toBean(badge);
                bean.unlocked = true;
                bean.unlockedAt = unlock.unlockedAt();
                bean.context = unlock.context();
                bean.competitionId = unlock.competitionId();
                bean.competitionName = unlock.competitionName();
                bean.competitionDate = unlock.competitionDate();
                return bean;
            });
        }).toList();

        return wrapEntity(badges, userIdAndRenewal);
    }

    static BadgeBean toBean(GamificationDao.BadgeRow badge) {
        BadgeBean bean = new BadgeBean();
        bean.id = badge.id();
        bean.code = badge.code();
        bean.category = badge.category();
        bean.name = badge.name();
        bean.description = badge.description();
        bean.icon = badge.icon();
        bean.ruleType = badge.ruleType();
        bean.tier = badge.tier();
        bean.annualReset = badge.annualReset();
        return bean;
    }
}
