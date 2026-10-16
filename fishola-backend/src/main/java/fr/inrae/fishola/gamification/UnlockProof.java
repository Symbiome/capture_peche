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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Prises « preuves » d'un déblocage (#275) : les sorties et captures du pêcheur qui lui
 * ont valu le badge, mémorisées dans {@code gamification_badge_unlock.context} sous les
 * clés {@link #TRIP_IDS_KEY}/{@link #CATCH_IDS_KEY} pour la carte souvenir. L'ordre
 * compte : la première capture est la prise mise en avant sur la carte.
 */
public record UnlockProof(List<UUID> tripIds, List<UUID> catchIds) {

    public static final String TRIP_IDS_KEY = "tripIds";
    public static final String CATCH_IDS_KEY = "catchIds";

    /** Plafond des mosaïques et cartes multi-sorties, pour garder un contexte léger. */
    public static final int MAX_IDS = 20;

    /** Aucune prise à mettre en avant : la carte souvenir se replie sur la carte générique. */
    public static final UnlockProof NONE = new UnlockProof(List.of(), List.of());

    public static UnlockProof ofTrips(List<UUID> tripIds) {
        return new UnlockProof(tripIds.stream().limit(MAX_IDS).toList(), List.of());
    }

    public boolean isEmpty() {
        return tripIds.isEmpty() && catchIds.isEmpty();
    }

    /** Vide si aucune prise : un déblocage sans preuve retrouvée n'a rien à persister. */
    public Optional<UnlockProof> nonEmpty() {
        return isEmpty() ? Optional.empty() : Optional.of(this);
    }

    /**
     * Concaténation sans doublon, en conservant l'ordre : {@code this} reste en tête
     * (ex. capture qui franchit le palier, puis plus grande capture de l'année).
     */
    public UnlockProof plus(UnlockProof other) {
        return new UnlockProof(distinctConcat(tripIds, other.tripIds), distinctConcat(catchIds, other.catchIds));
    }

    /** Copie de {@code context} enrichie des identifiants de cette preuve. */
    public Map<String, Object> addTo(Map<String, Object> context) {
        Map<String, Object> result = new HashMap<>(context);
        result.put(TRIP_IDS_KEY, tripIds);
        result.put(CATCH_IDS_KEY, catchIds);
        return result;
    }

    /** Vrai si le contexte d'un déblocage porte déjà ses preuves (déblocage postérieur à #275). */
    public static boolean isRecordedIn(JsonNode context) {
        return context != null && (context.has(TRIP_IDS_KEY) || context.has(CATCH_IDS_KEY));
    }

    /** Relit les identifiants mémorisés ; listes vides si absents ou illisibles. */
    public static UnlockProof readFrom(JsonNode context) {
        return new UnlockProof(readUuids(context, TRIP_IDS_KEY), readUuids(context, CATCH_IDS_KEY));
    }

    private static List<UUID> readUuids(JsonNode context, String key) {
        List<UUID> result = new ArrayList<>();
        if (context == null || !context.has(key)) {
            return result;
        }
        for (JsonNode value : context.get(key)) {
            try {
                result.add(UUID.fromString(value.asText()));
            } catch (IllegalArgumentException e) {
                // Identifiant illisible : ignoré, comme une capture supprimée depuis.
            }
        }
        return result;
    }

    private static List<UUID> distinctConcat(List<UUID> first, List<UUID> second) {
        LinkedHashSet<UUID> result = new LinkedHashSet<>(first);
        result.addAll(second);
        return List.copyOf(result);
    }
}
