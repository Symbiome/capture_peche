package fr.inrae.fishola.rest.dashboard;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.rest.trips.CatchBean;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDashboard.class)
public interface Dashboard {

    Map<UUID, Integer> caughtSpeciesCount();

    Map<UUID, Double> caughtSpeciesDistribution();

    List<DashboardLastTrip> latestTripsCatchs();

    Optional<Double> averageCatchsPerTrip();

    Map<UUID, List<CatchBean>> topBySize();

    Map<UUID, List<CatchBean>> topByWeight();

    Map<UUID, Set<String>> speciesAliases();

}
