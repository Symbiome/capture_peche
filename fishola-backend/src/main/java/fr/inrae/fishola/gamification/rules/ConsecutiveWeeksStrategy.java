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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** « Discipliné » (#146) : au moins {@code threshold} semaines consécutives avec >= 1 session. */
@Singleton
public class ConsecutiveWeeksStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "consecutive_weeks";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        int threshold = RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
        List<LocalDate> weekStarts = dao.distinctWeekStarts(userId);
        int longestRun = longestConsecutiveRun(weekStarts);
        return longestRun >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("consecutiveWeeks", longestRun)))
                : Optional.empty();
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
}
