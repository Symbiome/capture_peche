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

/**
 * « Persévérant » (#146) : au moins {@code threshold} sorties sans capture. Le MO souhaite
 * qu'on rappelle au pêcheur l'intérêt de renseigner ses bredouilles (cf. issue) -- message
 * à porter côté UI mobile, pas dans cette stratégie.
 */
@Singleton
public class BredouilleCountStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "bredouille_count";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int threshold = RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
        int count = dao.countBredouilleTrips(userId);
        return count >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("bredouilleCount", count)))
                : Optional.empty();
    }
}
