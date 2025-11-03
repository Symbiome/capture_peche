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
            List<EvolutionMetricForSpecieAndMonth> evolutionMetricsForSpecie = Lists.newArrayList();

            for (int year = 2018; year <= LocalDate.now().getYear(); year++) {
                Multimap<Month, Catch> monthlyCatches = catchsDao.findMonthly0(userId, Optional.of(year), lakeFilter);

                for (Month month : monthlyCatches.keySet()) {
                    Optional<EvolutionMetricForSpecieAndMonth> evolutionMetricsForSpecieAndMonth = getEvolutionMetricsForSpecieAndMonth(year, month, specieId, monthlyCatches, useEditedInBackOfficeInformation);
                    evolutionMetricsForSpecieAndMonth.ifPresent(evolutionMetricsForSpecie::add);
                }
            }

            builder.putEvolutionPerMonthAndSpecie(specieId, evolutionMetricsForSpecie);
        }

        return builder.build();
    }

    private static Optional<EvolutionMetricForSpecieAndMonth> getEvolutionMetricsForSpecieAndMonth(Integer year, Month month, UUID specieId, Multimap<Month, Catch> monthlyCatches, boolean useEditedInBackOfficeInformation) {
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
                Long keptCatchCount = catchesOfSpeciesForMonth.stream().filter(Catch::getKept).count();
                Long totalCatchCount = (long) catchesOfSpeciesForMonth.size();
                Long tripCount = catchesOfSpeciesForMonth.stream().map(Catch::getTripId).distinct().count();
                String monthYear = (month.getValue() < 10 ? "0" : "") + month.getValue()  + "/"  + year;
                return Optional.of(
                        ImmutableEvolutionMetricForSpecieAndMonth.builder()
                        .monthYear(monthYear)
                        .totalCatchesCount(totalCatchCount)
                        .tripsCount(tripCount)
                        .keptCatchesCount(keptCatchCount)
                        .build()
                );
            }
        }
        return Optional.empty();
    }
}
