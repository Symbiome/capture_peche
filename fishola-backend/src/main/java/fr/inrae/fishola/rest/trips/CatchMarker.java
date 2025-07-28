package fr.inrae.fishola.rest.trips;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.entities.enums.Maillage;

import java.time.LocalDate;
import java.util.UUID;

@ImmutableObject
@JsonSerialize(as = ImmutableCatchMarker.class)
public interface CatchMarker {

    UUID id();
    LocalDate date();
    UUID tripId();
    String tripName();
    String specieName();
    String lakeName();
    Double latitude();
    Double longitude();
    Double size();
    Double weight();
    Maillage maillage();
    boolean hasValidCoordinates();
}
