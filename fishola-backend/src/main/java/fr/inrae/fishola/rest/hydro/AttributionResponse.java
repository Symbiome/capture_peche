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

import java.util.List;
import java.util.Optional;

/**
 * Response of {@code GET /api/v1/waterEntities/attribution} (#9): the closest
 * water entity to a fishing point ({@code proposal}) plus a few next-closest
 * ones ({@code alternatives}) so the user can validate or correct without a new
 * search. {@code proposal} is empty when no entity is within range.
 */
@ImmutableObject
@JsonSerialize(as = ImmutableAttributionResponse.class)
public interface AttributionResponse {

    Optional<WaterEntityAttribution> proposal();

    List<WaterEntityAttribution> alternatives();
}
