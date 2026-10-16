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
 * « Martin pêcheur » / « Pilier de berge » / « Vétéran des eaux » (#146) : palier du
 * nombre total de sorties. Preuve (#275) : la sortie qui a franchi le palier.
 */
@Singleton
public class SessionCountTierStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Inject
    BadgeProofDao proofDao;

    @Override
    public String ruleType() {
        return "session_count_tier";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int threshold = threshold(badge);
        int count = dao.countTotalTrips(userId);
        return count >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("sessionCount", count),
                        proofDao.findNthTrip(userId, threshold).orElse(UnlockProof.NONE)))
                : Optional.empty();
    }

    @Override
    public Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                           UUID userId) {
        return proofDao.findNthTrip(userId, threshold(badge));
    }

    private int threshold(GamificationDao.BadgeRow badge) {
        return RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
    }
}
