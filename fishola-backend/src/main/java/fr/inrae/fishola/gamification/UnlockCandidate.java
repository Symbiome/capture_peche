package fr.inrae.fishola.gamification;

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

import java.util.Map;

/**
 * Résultat d'une stratégie de badge (#146) : le badge est débloqué, avec le détail à
 * conserver pour l'affichage/partage. {@code updateIfExists} distingue les badges
 * "record" (dont le contexte doit être rafraîchi à chaque nouveau record, cf.
 * {@code metric_record}) des autres (déblocage une seule fois, jamais réécrit).
 */
public record UnlockCandidate(Map<String, Object> context, boolean updateIfExists) {

    public static UnlockCandidate unlocked() {
        return new UnlockCandidate(Map.of(), false);
    }

    public static UnlockCandidate unlocked(Map<String, Object> context) {
        return new UnlockCandidate(context, false);
    }

    public static UnlockCandidate recordUpdate(Map<String, Object> context) {
        return new UnlockCandidate(context, true);
    }
}
