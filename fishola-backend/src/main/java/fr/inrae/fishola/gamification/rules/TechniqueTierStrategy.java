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
 * Un palier de la famille « Techniques » (#146) : N sorties utilisant une technique
 * donnée. Preuve (#275) : la sortie qui a franchi le palier et sa plus belle prise.
 */
@Singleton
public class TechniqueTierStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Inject
    BadgeProofDao proofDao;

    @Override
    public String ruleType() {
        return "technique_tier";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        String techniqueName = techniqueName(badge);
        int threshold = threshold(badge);
        if (techniqueName == null) {
            return Optional.empty();
        }
        int count = dao.countDistinctTripsWithTechnique(userId, techniqueName);
        return count >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("sessions", count),
                        proofDao.findNthTripWithTechnique(userId, techniqueName, threshold).orElse(UnlockProof.NONE)))
                : Optional.empty();
    }

    @Override
    public Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                           UUID userId) {
        String techniqueName = techniqueName(badge);
        if (techniqueName == null) {
            return Optional.empty();
        }
        return proofDao.findNthTripWithTechnique(userId, techniqueName, threshold(badge));
    }

    private String techniqueName(GamificationDao.BadgeRow badge) {
        return RuleParams.stringParam(badge.ruleParams(), "techniqueName", null);
    }

    private int threshold(GamificationDao.BadgeRow badge) {
        return RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
    }
}
