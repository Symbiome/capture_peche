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
 * A candidate water entity for a fishing position, with the distance, the
 * projected point on its geometry and — for watercourses — the closest river
 * section. Item of {@code GET /api/v1/waterEntities/attribution} (#9): the first
 * one is the proposal, the following ones are alternatives the user can pick
 * instead of re-searching.
 */
@ImmutableObject
@JsonSerialize(as = ImmutableWaterEntityAttribution.class)
public interface WaterEntityAttribution {

    UUID waterEntityId();

    String name();

    /** STILL or FLOWING (the {@code water_entity_kind} enum, as text). */
    String kind();

    /** Metric distance from the queried point to the closest geometry. */
    double distanceM();

    /** Point of the entity geometry closest to the queried point (projected). */
    GeoPoint closestPoint();

    /**
     * Id of the closest river section, when the closest geometry is a river
     * section. Empty for still waters / surfaces (no section to record).
     */
    Optional<UUID> riverSectionId();

    /**
     * Whether the closest river section is permanent. Empty for still waters and
     * surfaces. {@code false} flags a non-permanent watercourse (non-blocking
     * warning for the trip validation).
     */
    Optional<Boolean> persistent();
}
