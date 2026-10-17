package fr.inrae.fishola.gamification.rules;

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

import fr.inrae.fishola.gamification.BadgeProofDao;
import fr.inrae.fishola.gamification.BadgeRuleStrategy;
import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.gamification.UnlockCandidate;
import fr.inrae.fishola.gamification.UnlockProof;
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
 * Preuve (#275) : la capture qui a franchi le palier, puis la plus grande de l'année.
 */
@Singleton
public class SpeciesAnnualTierStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Inject
    BadgeProofDao proofDao;

    @Override
    public String ruleType() {
        return "species_annual_tier";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        List<String> speciesNames = RuleParams.stringListParam(badge.ruleParams(), "speciesNames");
        int threshold = threshold(badge);
        if (speciesNames.isEmpty()) {
            return Optional.empty();
        }
        int year = Year.now().getValue();
        int count = dao.countCatchesForSpeciesInYear(userId, speciesNames, year);
        return count >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("catchCount", count),
                        proofDao.findSpeciesTierCatches(userId, speciesNames, year, threshold).orElse(UnlockProof.NONE)))
                : Optional.empty();
    }

    @Override
    public Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                           UUID userId) {
        List<String> speciesNames = RuleParams.stringListParam(badge.ruleParams(), "speciesNames");
        if (speciesNames.isEmpty()) {
            return Optional.empty();
        }
        int year = unlock.periodYear() == 0 ? Year.now().getValue() : unlock.periodYear();
        return proofDao.findSpeciesTierCatches(userId, speciesNames, year, threshold(badge));
    }

    private int threshold(GamificationDao.BadgeRow badge) {
        return RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
    }
}
