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
import java.util.Optional;
import java.util.UUID;

/**
 * « Espèce remarquable » (#146) : capture d'une espèce de la liste {@code
 * rule_params.speciesNames}. Liste explicitement « à cadrer avec le MO » -- le badge est
 * livré {@code active = false} (cf. migration V2.2.0) tant qu'elle n'est pas validée ;
 * cette stratégie reste fonctionnelle, prête pour l'activation.
 */
@Singleton
public class RemarkableSpeciesCatchStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "remarkable_species_catch";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        List<String> speciesNames = RuleParams.stringListParam(badge.ruleParams(), "speciesNames");
        if (speciesNames.isEmpty()) {
            return Optional.empty();
        }
        return dao.hasCatchOfSpecies(userId, speciesNames) ? Optional.of(UnlockCandidate.unlocked()) : Optional.empty();
    }
}
