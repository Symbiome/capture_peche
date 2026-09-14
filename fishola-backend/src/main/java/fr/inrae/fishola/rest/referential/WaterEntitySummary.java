package fr.inrae.fishola.rest.referential;

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
 * Lightweight water entity listing, without geometry. Result item of
 * {@code GET /api/v1/referential/waterEntities/summary}: the full referential
 * ({@link fr.inrae.fishola.entities.tables.pojos.WaterEntity}, geometry
 * included) now weighs over a gigabyte for the France-wide hydro network,
 * which blows past the mobile client's request timeout. Carries only what the
 * trip/catch entry forms need (name, kind, centroid) — fine geometry stays
 * available via the hydro MVT tiles and the dedicated hydro endpoints.
 */
@ImmutableObject
@JsonSerialize(as = ImmutableWaterEntitySummary.class)
public interface WaterEntitySummary {

    UUID id();

    String name();

    String exportAs();

    /** STILL or FLOWING (the {@code water_entity_kind} enum, as text). */
    String kind();

    /** Centroid, generated from the geometry. Empty if the entity has no geometry. */
    Optional<Double> latitude();

    Optional<Double> longitude();
}
