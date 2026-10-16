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

import fr.inrae.fishola.gamification.BadgeProofDao;
import fr.inrae.fishola.gamification.BadgeRuleStrategy;
import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.gamification.UnlockCandidate;
import fr.inrae.fishola.gamification.UnlockProof;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * « Baroudeur » (#146) : sorties sur au moins {@code threshold} départements différents.
 * Utilise {@code trip.department} déjà estampillé (#159), aucune jointure spatiale nécessaire.
 * Preuve (#275) : la première sortie dans chaque département (carte des milieux pêchés).
 */
@Singleton
public class MultiDepartmentStrategy implements BadgeRuleStrategy {

    @Inject
    BadgeProofDao proofDao;

    @Override
    public String ruleType() {
        return "multi_department";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int threshold = RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
        Map<String, UUID> firstTripPerDepartment = proofDao.findFirstTripPerDepartment(userId);
        int count = firstTripPerDepartment.size();
        return count >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("departmentCount", count),
                        UnlockProof.ofTrips(List.copyOf(firstTripPerDepartment.values()))))
                : Optional.empty();
    }

    @Override
    public Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                           UUID userId) {
        return UnlockProof.ofTrips(List.copyOf(proofDao.findFirstTripPerDepartment(userId).values())).nonEmpty();
    }
}
