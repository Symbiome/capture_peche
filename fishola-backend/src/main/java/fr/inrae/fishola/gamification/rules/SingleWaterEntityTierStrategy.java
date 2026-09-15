package fr.inrae.fishola.gamification.rules;

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

import fr.inrae.fishola.gamification.BadgeRuleStrategy;
import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.gamification.UnlockCandidate;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** « Lacustre » (#146) : au moins {@code threshold} sessions concentrées sur un même plan d'eau. */
@Singleton
public class SingleWaterEntityTierStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "single_water_entity_tier";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int threshold = RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
        String kind = RuleParams.stringParam(badge.ruleParams(), "kind", "STILL");
        int max = dao.maxTripsOnSingleWaterEntity(userId, kind);
        return max >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("sessionCount", max)))
                : Optional.empty();
    }
}
