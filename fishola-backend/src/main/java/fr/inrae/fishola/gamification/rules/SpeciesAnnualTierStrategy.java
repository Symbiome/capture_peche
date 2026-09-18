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

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Badge par espèce (#146), réinitialisé chaque année civile : {@code annual_reset = true}
 * sur la ligne {@code gamification_badge}, le millésime est géré par
 * {@link fr.inrae.fishola.gamification.GamificationEngine}. {@code rule_params.speciesNames}
 * porte la ou les espèces couvertes (plusieurs pour les groupes, ex. "Cyprins"/"Alose").
 */
@Singleton
public class SpeciesAnnualTierStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "species_annual_tier";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        List<String> speciesNames = RuleParams.stringListParam(badge.ruleParams(), "speciesNames");
        int threshold = RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
        if (speciesNames.isEmpty()) {
            return Optional.empty();
        }
        int count = dao.countCatchesForSpeciesInYear(userId, speciesNames, Year.now().getValue());
        return count >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("catchCount", count)))
                : Optional.empty();
    }
}
