package fr.inrae.fishola.database;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import fr.inrae.fishola.entities.tables.daos.SpeciesDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.rest.dashboard.EvolutionMetricForSpecieAndMonth;
import fr.inrae.fishola.rest.dashboard.EvolutionMetricsForLake;
import fr.inrae.fishola.rest.dashboard.ImmutableEvolutionMetricForSpecieAndMonth;
import fr.inrae.fishola.rest.dashboard.ImmutableEvolutionMetricsForLake;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class EvolutionDao  extends AbstractFisholaDao {

    @Inject
    protected CatchsDao catchsDao;

    public EvolutionMetricsForLake getEvolutionStatsForLake(UUID lakeId, Optional<UUID> userId) {
        ImmutableEvolutionMetricsForLake.Builder builder = ImmutableEvolutionMetricsForLake.builder();
        Optional<List<UUID>> lakeFilter = Optional.of(Lists.newArrayList(lakeId));
        List<UUID> species = withDao(SpeciesDao.class, SpeciesDao::findAll).stream().map(Species::getId).toList();
        boolean useEditedInBackOfficeInformation = userId.isEmpty();

        for (UUID specieId : species) {
            Map<String, EvolutionMetricForSpecieAndMonth> evolutionMetricsForSpecie = Maps.newLinkedHashMap();

            for (int year = 2018; year < LocalDate.now().getYear(); year++) {
                Multimap<Month, Catch> monthlyCatches = catchsDao.findMonthly0(userId, Optional.of(year), lakeFilter);

                for (Month month : Month.values()) {
                    EvolutionMetricForSpecieAndMonth evolutionMetricsForSpecieAndMonth = getEvolutionMetricsForSpecieAndMonth(month, specieId, monthlyCatches, useEditedInBackOfficeInformation);
                    String yearAndMonthString = month.getDisplayName(TextStyle.SHORT, Locale.FRANCE) + " "  + year;
                    evolutionMetricsForSpecie.put(yearAndMonthString, evolutionMetricsForSpecieAndMonth);
                }
            }

            builder.putEvolutionPerMonthAndSpecie(specieId, evolutionMetricsForSpecie);
        }

        return builder.build();
    }

    private static EvolutionMetricForSpecieAndMonth getEvolutionMetricsForSpecieAndMonth(Month month, UUID specieId, Multimap<Month, Catch> monthlyCatches, boolean useEditedInBackOfficeInformation) {
        ImmutableEvolutionMetricForSpecieAndMonth.Builder builder = ImmutableEvolutionMetricForSpecieAndMonth.builder();
        Long keptCatchCount = 0L;
        Long totalCatchCount = 0L;
        Long tripCount = 0L;

        if (monthlyCatches.containsKey(month)) {
            List<Catch> catchesOfSpeciesForMonth = monthlyCatches.get(month).stream()
                    .filter(c -> {
                        if (useEditedInBackOfficeInformation) {
                            return Objects.equals(c.getEditedSpeciesId(), specieId);
                        } else {
                            return Objects.equals(c.getSpeciesId(), specieId);
                        }
                    }).toList();
            if (!catchesOfSpeciesForMonth.isEmpty()) {
                keptCatchCount = catchesOfSpeciesForMonth.stream().filter(Catch::getKept).count();
                totalCatchCount = (long) catchesOfSpeciesForMonth.size();
                tripCount = catchesOfSpeciesForMonth.stream().map(Catch::getTripId).distinct().count();
            }
        }
        builder.totalCatchesCount(totalCatchCount);
        builder.tripsCount(tripCount);
        builder.keptCatchesCount(keptCatchCount);
        return builder.build();
    }
}
