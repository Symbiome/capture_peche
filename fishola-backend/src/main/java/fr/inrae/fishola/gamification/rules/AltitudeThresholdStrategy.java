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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * « Grimpeur » (#146) : session sur un plan d'eau d'altitude moyenne supérieure au seuil.
 * Preuve (#275) : la première de ces sessions (le lac et son altitude sur la carte souvenir).
 */
@Singleton
public class AltitudeThresholdStrategy implements BadgeRuleStrategy {

    @Inject
    BadgeProofDao proofDao;

    @Override
    public String ruleType() {
        return "altitude_threshold";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        return proof(badge, userId).map(proof -> UnlockCandidate.unlocked(Map.of(), proof));
    }

    @Override
    public Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                           UUID userId) {
        return proof(badge, userId);
    }

    private Optional<UnlockProof> proof(GamificationDao.BadgeRow badge, UUID userId) {
        double minAltitude = RuleParams.doubleParam(badge.ruleParams(), "minAltitude", Double.MAX_VALUE);
        return proofDao.findFirstTripAboveAltitude(userId, minAltitude);
    }
}
