package fr.inrae.fishola.rest;

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

import fr.inrae.fishola.ImmutableObject;

import java.util.Optional;
import java.util.UUID;

@ImmutableObject
public interface UserIdAndRenewal {

    UUID userId();

    Optional<String> renewalToken();

    static UserIdAndRenewal of(UUID userId) {
        return ImmutableUserIdAndRenewal.builder().userId(userId).build();
    }

    static UserIdAndRenewal of(UUID userId, String token) {
        return ImmutableUserIdAndRenewal.builder().userId(userId).renewalToken(token).build();
    }

}
