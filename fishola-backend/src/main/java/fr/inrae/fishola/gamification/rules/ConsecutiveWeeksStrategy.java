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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * « Discipliné » (#146) : au moins {@code threshold} semaines consécutives avec >= 1
 * session. Preuve (#275) : la première sortie de la semaine qui a complété la série.
 */
@Singleton
public class ConsecutiveWeeksStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Inject
    BadgeProofDao proofDao;

    @Override
    public String ruleType() {
        return "consecutive_weeks";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int threshold = threshold(badge);
        List<LocalDate> weekStarts = dao.distinctWeekStarts(userId);
        int longestRun = longestConsecutiveRun(weekStarts);
        return longestRun >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("consecutiveWeeks", longestRun),
                        proof(weekStarts, threshold, userId).orElse(UnlockProof.NONE)))
                : Optional.empty();
    }

    @Override
    public Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                           UUID userId) {
        return proof(dao.distinctWeekStarts(userId), threshold(badge), userId);
    }

    private int threshold(GamificationDao.BadgeRow badge) {
        return RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
    }

    private Optional<UnlockProof> proof(List<LocalDate> sortedWeekStarts, int threshold, UUID userId) {
        return weekCompletingRun(sortedWeekStarts, threshold)
                .flatMap(weekStart -> proofDao.findFirstTripOfWeek(userId, weekStart));
    }

    private int longestConsecutiveRun(List<LocalDate> sortedWeekStarts) {
        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate weekStart : sortedWeekStarts) {
            current = (previous != null && weekStart.equals(previous.plusDays(7))) ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = weekStart;
        }
        return longest;
    }

    /** Première semaine où une série de semaines consécutives atteint {@code threshold}. */
    private Optional<LocalDate> weekCompletingRun(List<LocalDate> sortedWeekStarts, int threshold) {
        int current = 0;
        LocalDate previous = null;
        for (LocalDate weekStart : sortedWeekStarts) {
            current = (previous != null && weekStart.equals(previous.plusDays(7))) ? current + 1 : 1;
            if (current >= threshold) {
                return Optional.of(weekStart);
            }
            previous = weekStart;
        }
        return Optional.empty();
    }
}
