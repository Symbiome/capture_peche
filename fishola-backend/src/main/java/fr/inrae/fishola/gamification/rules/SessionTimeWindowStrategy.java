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
 * « Prise éclair » / « Juste à temps ! » / « Lève-tôt » (#146) : trois badges distincts
 * partageant une même mécanique de fenêtre temporelle, distinguée par
 * {@code rule_params.mode}.
 */
@Singleton
public class SessionTimeWindowStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "session_time_window";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        String mode = RuleParams.stringParam(badge.ruleParams(), "mode", "");
        boolean matched = switch (mode) {
            case "first_catch_after_start" ->
                    dao.hasFirstCatchWithinStart(userId, RuleParams.intParam(badge.ruleParams(), "windowMinutes", 10));
            case "first_catch_before_end" ->
                    dao.hasFirstCatchWithinEnd(userId, RuleParams.intParam(badge.ruleParams(), "windowMinutes", 10));
            case "before_hour" -> dao.hasCatchBeforeHour(userId, RuleParams.intParam(badge.ruleParams(), "hour", 7));
            default -> false;
        };
        return matched ? Optional.of(UnlockCandidate.unlocked()) : Optional.empty();
    }
}
