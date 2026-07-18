package fr.inrae.fishola.rest.hydro;

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;

import java.util.Optional;
import java.util.UUID;

/**
 * A water entity matched by textual search. Result item of
 * {@code GET /api/v1/waterEntities/search}. Carries the centroid so the mobile
 * can centre the map on selection without a second request.
 */
@ImmutableObject
@JsonSerialize(as = ImmutableWaterEntitySearchResult.class)
public interface WaterEntitySearchResult {

    UUID waterEntityId();

    String name();

    /** STILL or FLOWING. */
    String kind();

    /** Centroid of the entity geometry. */
    GeoPoint centroid();

    /**
     * Name of the commune containing the entity centroid (#6), to disambiguate
     * homonyms in the autocomplete. Empty if no commune matches (referential
     * partiel).
     */
    Optional<String> commune();

    /** Code postal principal de la commune (#15). Empty si non couvert. */
    Optional<String> codePostal();
}
