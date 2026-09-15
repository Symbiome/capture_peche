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

import java.util.Optional;
import java.util.UUID;

/**
 * « Grand contributeur » (#146) : ne réimplémente pas de logique de percentile ad hoc --
 * le calcul {@code PERCENT_RANK()} + garde de cohorte minimale (RGPD) vit dans
 * {@link GamificationDao#contributorPercentile}, un utilitaire générique réutilisable par
 * un futur classement (#6) plutôt qu'un calcul propre à ce seul badge.
 */
@Singleton
public class ContributorPercentileStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "contributor_percentile";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int topPercent = RuleParams.intParam(badge.ruleParams(), "topPercent", 10);
        int minCohortSize = RuleParams.intParam(badge.ruleParams(), "minCohortSize", 20);
        return dao.contributorPercentile(userId, minCohortSize)
                .filter(percentRank -> percentRank >= (1.0 - topPercent / 100.0))
                .map(percentRank -> UnlockCandidate.unlocked(java.util.Map.of("percentRank", percentRank)));
    }
}
