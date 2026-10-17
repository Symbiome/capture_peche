package fr.inrae.fishola.gamification;

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

import java.util.Optional;
import java.util.UUID;

/**
 * Une stratégie d'évaluation par {@code gamification_badge.rule_type} (#146). Chaque
 * implémentation est un bean CDI (une par valeur de l'enum {@code gamification_rule_type},
 * sauf {@code manual}) découvert par {@link GamificationEngine} via {@code Instance<BadgeRuleStrategy>}.
 */
public interface BadgeRuleStrategy {

    /** Doit correspondre exactement à une valeur de l'enum SQL {@code gamification_rule_type}. */
    String ruleType();

    /**
     * Évalue ce badge pour ce pêcheur. {@code badge.ruleParams()} porte les seuils/listes
     * propres à ce badge (ex. {@code threshold}, {@code speciesNames}...). Vide si le badge
     * n'est pas (encore) débloqué.
     */
    Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID fishowaUserId);

    /**
     * Recalcule les prises « preuves » (#275) d'un déblocage mémorisé avant qu'elles ne
     * soient enregistrées dans son contexte : même requête que {@link #evaluate}, rejouée
     * sur le millésime du déblocage ({@code unlock.periodYear()}) et, pour les records,
     * sur l'espèce mémorisée. Vide pour les règles sans prise associée (percentile,
     * attribution manuelle) ou si ces données ont disparu depuis.
     */
    default Optional<UnlockProof> findProof(GamificationDao.BadgeRow badge, GamificationDao.UnlockRow unlock,
                                            UUID fishowaUserId) {
        return Optional.empty();
    }
}
