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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import fr.inrae.fishola.ImmutableObject;
import fr.inrae.fishola.entities.enums.Gender;
import org.immutables.value.Value;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ImmutableObject
@JsonSerialize(as = ImmutableUserProfile.class)
@JsonDeserialize(as = ImmutableUserProfile.class)
public interface UserProfile {

    UUID id();

    String firstName();

    Optional<String> lastName();

    String email();

    Optional<Gender> gender();

    Optional<Integer> birthYear();

    String sampleBaseId();

    Boolean acceptsMailNotifications();

    Boolean acceptsShareTrips();

    LocalDateTime lastNewsSeenDate();

    @Value.Derived
    default String initials() {
        List<Character> initials = new LinkedList<>(splitToInitials(firstName()));
        lastName().map(this::splitToInitials)
                .ifPresent(initials::addAll);
        String joined = Joiner.on("").join(initials).toUpperCase();
        return joined;
    }

    default List<Character> splitToInitials(String input) {
        List<String> parts = Splitter.on(Pattern.compile("[ -]"))
                .trimResults()
                .omitEmptyStrings()
                .splitToList(input.trim());
        List<Character> result = parts.stream()
                .map(part -> part.charAt(0))
                .toList();
        return result;
    }

}
