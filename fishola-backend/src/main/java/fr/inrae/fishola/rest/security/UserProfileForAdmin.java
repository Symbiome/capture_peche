package fr.inrae.fishola.rest.security;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.entities.enums.Gender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ImmutableObject
@JsonSerialize(as = ImmutableUserProfileForAdmin.class)
public interface UserProfileForAdmin {

    UUID id();

    String firstName();

    Optional<String> lastName();

    String email();

    Optional<Gender> gender();

    Optional<Integer> birthYear();

    boolean excludeFromExports();

    LocalDateTime createdOn();

    boolean acceptsEmailNotifications();

    boolean acceptsShareTrips();

}
