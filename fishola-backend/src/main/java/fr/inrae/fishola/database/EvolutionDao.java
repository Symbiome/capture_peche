package fr.inrae.fishola.database;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.tables.daos.SpeciesDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.rest.dashboard.EvolutionMetricsForLake;
import fr.inrae.fishola.rest.dashboard.ImmutableEvolutionMetricsForLake;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
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

        for (int year = 2018; year < LocalDate.now().getYear(); year++) {

            // Get all catches made this month on this lake (with user filter or not)
            Multimap<Month, Catch> monthlyCatches = catchsDao.findMonthly0(userId, Optional.of(year), lakeFilter);
            Map<Month, Map<UUID, Map<Boolean, Long>>> catchCountPerMonthAndSpecies = Maps.newLinkedHashMap();
            Map<Month, Map<UUID, Long>> tripCountPerMonthAndSpecies = Maps.newLinkedHashMap();

            for (Month month : Month.values()) {

                for (UUID speciesId : species) {
                    List<Catch> catchesOfSpeciesForMonth = monthlyCatches.get(month).stream()
                            .filter(c -> {
                                if (useEditedInBackOfficeInformation) {
                                    return Objects.equals(c.getEditedSpeciesId(),speciesId);
                                } else {
                                    return  Objects.equals(c.getSpeciesId(), speciesId);
                                }
                            }) .toList();
                    if (!catchesOfSpeciesForMonth.isEmpty()) {
                        catchCountPerMonthAndSpecies.putIfAbsent(month, Maps.newLinkedHashMap());
                        tripCountPerMonthAndSpecies.putIfAbsent(month, Maps.newLinkedHashMap());

                        // Count kept and relase catches for this specie
                        Map<Boolean, Long> keptAndUnkeptCount = Maps.newLinkedHashMap();
                        keptAndUnkeptCount.put(true, catchesOfSpeciesForMonth.stream().filter(Catch::getKept).count());
                        keptAndUnkeptCount.put(false, catchesOfSpeciesForMonth.stream().filter(c -> !c.getKept()).count());
                        catchCountPerMonthAndSpecies.get(month).put(speciesId, keptAndUnkeptCount);

                        // Count distinct trips for this species
                        Long tripCount = catchesOfSpeciesForMonth.stream().map(Catch::getTripId).distinct().count();
                        tripCountPerMonthAndSpecies.get(month).put(speciesId, tripCount);
                    }
                }
            }
            if (!catchCountPerMonthAndSpecies.isEmpty()) {
                builder.putCatchCountPerMonthAndSpecies(year, catchCountPerMonthAndSpecies);
                builder.putTripCountPerMonthAndSpecies(year, tripCountPerMonthAndSpecies);
            }
        }
        return builder.build();
    }
}
