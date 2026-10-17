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
import fr.inrae.fishola.gamification.Basins;
import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.gamification.UnlockCandidate;
import fr.inrae.fishola.gamification.UnlockProof;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * « Explorateur » (#146) : pêche sur au moins {@code threshold} bassins hydrographiques
 * différents. Preuve (#275) : la première sortie dans chaque bassin (carte des milieux pêchés).
 */
@Singleton
public class BasinDiversityStrategy implements BadgeRuleStrategy {

    @Inject
    BadgeProofDao proofDao;

    @Override
    public String ruleType() {
        return "basin_diversity";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int threshold = RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
        Map<String, UUID> firstTripPerBasin = firstTripPerBasin(userId);
        return firstTripPerBasin.size() >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("basinCount", firstTripPerBasin.size()),
                        UnlockProof.ofTrips(List.copyOf(firstTripPerBasin.values()))))
                : Optional.empty();
    }

    @Override
    public Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                           UUID userId) {
        return UnlockProof.ofTrips(List.copyOf(firstTripPerBasin(userId).values())).nonEmpty();
    }

    /** Première sortie dans chaque bassin, par ordre chronologique ; départements hors bassin connu ignorés. */
    private Map<String, UUID> firstTripPerBasin(UUID userId) {
        Map<String, UUID> result = new LinkedHashMap<>();
        proofDao.findFirstTripPerDepartment(userId).forEach((department, tripId) -> {
            String basin = Basins.basinOf(department);
            if (basin != null) {
                result.putIfAbsent(basin, tripId);
            }
        });
        return result;
    }
}
