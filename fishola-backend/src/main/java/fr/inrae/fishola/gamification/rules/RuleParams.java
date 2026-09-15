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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Petits accesseurs pour lire {@code gamification_badge.rule_params} (jsonb) sans bruit. */
final class RuleParams {

    private RuleParams() {}

    static int intParam(JsonNode node, String key, int defaultValue) {
        return node != null && node.hasNonNull(key) ? node.get(key).asInt() : defaultValue;
    }

    static double doubleParam(JsonNode node, String key, double defaultValue) {
        return node != null && node.hasNonNull(key) ? node.get(key).asDouble() : defaultValue;
    }

    static String stringParam(JsonNode node, String key, String defaultValue) {
        return node != null && node.hasNonNull(key) ? node.get(key).asText() : defaultValue;
    }

    static List<String> stringListParam(JsonNode node, String key) {
        List<String> result = new ArrayList<>();
        if (node == null || !node.has(key)) {
            return result;
        }
        node.get(key).forEach(v -> result.add(v.asText()));
        return result;
    }
}
