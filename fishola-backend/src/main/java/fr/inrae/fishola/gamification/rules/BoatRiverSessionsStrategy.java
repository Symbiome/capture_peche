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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * « Navigateur » (#146) : sorties en bateau (trip.type = 'Craft') sur l'un des grands
 * cours d'eau listés. Liste explicitement « à cadrer avec le MO » -- livré {@code active
 * = false} avec {@code rule_params.waterEntityNames} vide (cf. migration V2.2.0) ; cette
 * stratégie reste fonctionnelle, prête pour l'activation.
 */
@Singleton
public class BoatRiverSessionsStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "boat_river_sessions";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        List<String> waterEntityNames = RuleParams.stringListParam(badge.ruleParams(), "waterEntityNames");
        int threshold = RuleParams.intParam(badge.ruleParams(), "threshold", Integer.MAX_VALUE);
        if (waterEntityNames.isEmpty()) {
            return Optional.empty();
        }
        int count = dao.countBoatTripsOnWaterEntities(userId, waterEntityNames);
        return count >= threshold
                ? Optional.of(UnlockCandidate.unlocked(Map.of("sessionCount", count)))
                : Optional.empty();
    }
}
