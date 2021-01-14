package fr.inrae.fishola.rest.dashboard;

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
import org.immutables.value.Value;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableGlobalDashboard.class)
public interface GlobalDashboard {

    Map<UUID, Integer> caughtSpeciesCount();

    Map<UUID, Double> caughtSpeciesDistribution();

    Map<UUID, Double> caughtAndReleasedSpeciesDistribution();

    Map<UUID, Set<String>> speciesAliases();

    List<Month> orderedMonths();

    Map<UUID, Map<Month, Double>> monthlySizes();

    LocalDateTime computedOn();

}
