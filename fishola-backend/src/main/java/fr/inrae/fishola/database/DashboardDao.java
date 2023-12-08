package fr.inrae.fishola.database;

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

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.dashboard.Dashboard;
import fr.inrae.fishola.rest.dashboard.DashboardLastTrip;
import fr.inrae.fishola.rest.dashboard.GlobalDashboard;
import fr.inrae.fishola.rest.dashboard.ImmutableDashboard;
import fr.inrae.fishola.rest.dashboard.ImmutableDashboardLastTrip;
import fr.inrae.fishola.rest.dashboard.ImmutableGlobalDashboard;
import fr.inrae.fishola.rest.trips.CatchBean;
import fr.inrae.fishola.rest.trips.PicturePerTripBean;
import fr.inrae.fishola.rest.trips.TripResource;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jboss.logging.Logger;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

@Singleton
public class DashboardDao  extends AbstractFisholaDao {

    @Inject
    protected CatchsDao catchsDao;
    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;


    public Dashboard getPersonalDashboard(UUID userId, Optional<Integer> yearFilter, Optional<List<UUID>> lakesFilter) {
        ImmutableDashboard.Builder builder = ImmutableDashboard.builder();
        Multimap<Month, Catch> monthlyCatchs = catchsDao.findMonthlyByUserId(userId, yearFilter, lakesFilter);

        Collection<Catch> allCatches = monthlyCatchs.values();
        int allCatchsCount = allCatches.size();

        Map<UUID, Integer> caughtSpeciesCount = computeDistribution(false, allCatches, aCatch -> true);
        builder.caughtSpeciesCount(caughtSpeciesCount);

        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);

        Map<UUID, Integer> caughtAndReleasedSpeciesCount = computeDistribution(false, allCatches, aCatch -> !aCatch.getKept());
        Map<UUID, Double> caughtAndReleasedSpeciesDistribution = Maps.transformValues(caughtAndReleasedSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtAndReleasedSpeciesDistribution(caughtAndReleasedSpeciesDistribution);

        PaginationResult<DashboardLastTrip> latestTrips = computeLatestTrips(userId, allCatches, yearFilter, lakesFilter);
        builder.latestTripsCatchs(latestTrips.getElements());
        int allTripsCount = (int) latestTrips.getCount();
        if (allTripsCount > 0) {
            double averageCatchsPerTrip = 1d * allCatchsCount / allTripsCount;
            builder.averageCatchsPerTrip(averageCatchsPerTrip);
        }

        Set<UUID> catchIds = allCatches.stream()
                .map(Catch::getId)
                .collect(Collectors.toSet());
        ListMultimap<UUID, Integer> catchsWithPictures = catchsDao.getPictureIndexes(catchIds);
        Set<UUID> measurementPictures = catchsDao.getMeasurementPictures(catchIds);
        Map<UUID, List<CatchBean>> topBySize = computeTopCatchs(allCatches, catchsWithPictures, measurementPictures, Catch::getSize);
        builder.topBySize(topBySize);

        Map<UUID, List<CatchBean>> topByWeight = computeTopCatchs(allCatches, catchsWithPictures, measurementPictures, Catch::getWeight);
        builder.topByWeight(topByWeight);

        List<SpeciesByLake> speciesByLakes = referentialDao.listSpeciesWithAliases();
        Multimap<UUID, SpeciesByLake> speciesWithAliasesIndex = Multimaps.index(speciesByLakes, SpeciesByLake::getSpeciesId);
        speciesWithAliasesIndex.asMap().forEach((speciesId, speciesWithAlias) -> {
            Set<String> aliases = speciesWithAlias.stream()
                    .map(SpeciesByLake::getAlias)
                    .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
            builder.putSpeciesAliases(speciesId, aliases);
        });

        builder.addOrderedMonths(Month.values());

        List<UUID> mostCaughtSpecies = get5MostCaughtSpecies(caughtSpeciesCount);

        Map<UUID, Map<Month, Map<Maillage, Double>>> monthlySizes = computeMonthlySizes(false, lakesFilter, mostCaughtSpecies, monthlyCatchs);
        builder.monthlySizesPerMaillage(monthlySizes);
        // Legacy field for old applications that do not know maillage
        // This legacy field may be deleted in a few versions.
        builder.monthlySizes(getLegacyMonthlySizesWithoutMaillage(monthlySizes));


        Integer year = LocalDateTime.now().getYear();
        if (yearFilter.isPresent()) {
            year = yearFilter.get();
        }
        List<PicturePerTripBean> picturePerTrip = tripsDao.getPicturesPerTripForYearAndLakes(userId, year, lakesFilter);
        builder.picturesPerTrip(picturePerTrip);

        Dashboard result = builder.build();
        return result;
    }

    private Map<UUID, Map<Month, Double>> getLegacyMonthlySizesWithoutMaillage(Map<UUID, Map<Month, Map<Maillage, Double>>> monthlySizes) {
        Map<UUID, Map<Month, Double>> monthlySizesWithoutMaillage = new LinkedHashMap<>();
        for (UUID uuid : monthlySizes.keySet()) {
            Map<Month, Double> sizesForSpecie = new LinkedHashMap<>();
            for (Month month: monthlySizes.get(uuid).keySet()) {
                Double size = monthlySizes.get(uuid).get(month).get(Maillage.NON_DEFINI);
                sizesForSpecie.put(month, size);
            }
            monthlySizesWithoutMaillage.put(uuid, sizesForSpecie);
        }
        return monthlySizesWithoutMaillage;
    }

    public GlobalDashboard computeGlobalDashboard(Optional<Integer> yearFilter, Optional<List<UUID>> lakesFilter, Logger log) {

        ImmutableGlobalDashboard.Builder builder = ImmutableGlobalDashboard.builder();

        Multimap<Month, Catch> monthlyCatchs = catchsDao.findAll(yearFilter, lakesFilter);

        Collection<Catch> allCatches = monthlyCatchs.values();
        int allCatchsCount = allCatches.size();

        Map<UUID, Integer> caughtSpeciesCount = computeDistribution(true, allCatches, aCatch -> true);
        builder.caughtSpeciesCount(caughtSpeciesCount);

        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);

        Map<UUID, Integer> caughtAndReleasedSpeciesCount = computeDistribution(true, allCatches, aCatch -> !aCatch.getKept());
        Map<UUID, Double> caughtAndReleasedSpeciesDistribution = Maps.transformValues(caughtAndReleasedSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtAndReleasedSpeciesDistribution(caughtAndReleasedSpeciesDistribution);

        List<SpeciesByLake> speciesByLakes = referentialDao.listSpeciesWithAliases();
        Multimap<UUID, SpeciesByLake> speciesWithAliasesIndex = Multimaps.index(speciesByLakes, SpeciesByLake::getSpeciesId);
        speciesWithAliasesIndex.asMap().forEach((speciesId, speciesWithAlias) -> {
            Set<String> aliases = speciesWithAlias.stream()
                    .map(SpeciesByLake::getAlias)
                    .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
            builder.putSpeciesAliases(speciesId, aliases);
        });

        builder.addOrderedMonths(Month.values());

        List<UUID> mostCaughtSpecies = get5MostCaughtSpecies(caughtSpeciesCount);

        Map<UUID, Map<Month, Map<Maillage, Double>>> monthlySizes = computeMonthlySizes(true, lakesFilter, mostCaughtSpecies, monthlyCatchs);
        // Legacy field for old applications that do not know maillage
        // This legacy field may be deleted in a few versions.
        builder.monthlySizes(getLegacyMonthlySizesWithoutMaillage(monthlySizes));
        builder.monthlySizesPerMaillage(monthlySizes);

        builder.computedOn(LocalDateTime.now());

        GlobalDashboard result = builder.build();

        if (log.isDebugEnabled()) {
            log.debugf("Nouvelle instance: %s", result);
        }
        return result;
    }


    protected Map<UUID, Integer> computeDistribution(boolean useEditedInBoInformation, Collection<Catch> allCatches, Predicate<Catch> predicate) {
        Stream<Catch> catchStream = allCatches.stream()
                .filter(predicate);
        Stream<UUID> catchSpeciesStream = null;
        if (useEditedInBoInformation) {
            catchSpeciesStream = catchStream.map(Catch::getEditedSpeciesId);
        } else {
            catchSpeciesStream = catchStream.map(Catch::getSpeciesId);
        }
        ImmutableMultiset<UUID> caughtSpeciesDistributionSet =
                catchSpeciesStream.collect(ImmutableMultiset.toImmutableMultiset());
        Map<UUID, Integer> result = caughtSpeciesDistributionSet.entrySet()
                .stream()
                .collect(Collectors.toMap(Multiset.Entry::getElement, Multiset.Entry::getCount));
        return result;
    }

    protected PaginationResult<DashboardLastTrip> computeLatestTrips(UUID userId, Collection<Catch> allCatches, Optional<Integer> yearFilter, Optional<List<UUID>> lakesFilter) {
        ImmutableListMultimap<UUID, Catch> allCatchsIndex = Multimaps.index(allCatches, Catch::getTripId);
        PaginationParameter page = PaginationParameter.builder(0, 9)
                .addOrder("date", true)
                .build();
        PaginationResult<Trip> latestTripsEntities = tripsDao.listMyTrips(userId, page, Optional.empty(), yearFilter, lakesFilter);
        PaginationResult<DashboardLastTrip> result = latestTripsEntities.transform(trip -> toDashboardLastTrip(trip, allCatchsIndex));
        return result;
    }

    protected DashboardLastTrip toDashboardLastTrip(Trip trip, ImmutableListMultimap<UUID, Catch> allCatchsIndex) {
        DashboardLastTrip result = ImmutableDashboardLastTrip.builder()
                .tripId(trip.getId())
                .catchsCount(allCatchsIndex.get(trip.getId()).size())
                .day(trip.getDay())
                .build();
        return result;
    }

    /**
     * Calcule la moyenne mensuelle des tailles de poissons pour les espèces spécifiées
     */
    protected  Map<UUID, Map<Month, Map<Maillage, Double>>> computeMonthlySizes(boolean useEditedInBoInformation,Optional<List<UUID>> lakesFilter, List<UUID> mostCaughtSpecies, Multimap<Month, Catch> monthlyCatches) {
        Map<UUID, Month> catchesMonths = monthlyCatches.entries().stream().collect(Collectors.toMap(e -> e.getValue().getId(), Map.Entry::getKey));
        Map<UUID, Map<Month, Map<Maillage, Double>>> result = computeMonthlySizes(useEditedInBoInformation, lakesFilter, catchesMonths, mostCaughtSpecies, monthlyCatches.values());
        return result;
    }

    protected  Map<UUID, Map<Month, Map<Maillage, Double>>> computeMonthlySizes(boolean useEditedInBoInformation, Optional<List<UUID>> lakesFilter, Map<UUID, Month> catchesMonths, List<UUID> mostCaughtSpecies, Collection<Catch> allCatches) {
        // On garde uniquement les captures avec une taille
        List<Catch> catchsWithSize = allCatches.stream()
                .filter(c -> c.getSize() != null)
                .toList();
        ImmutableMap.Builder<UUID, Map<Month, Map<Maillage, Double>>> builder = ImmutableMap.builder();
        for (UUID speciesId : mostCaughtSpecies) {
            // On prend les captures de la bonne espèce
            List<Catch> catchs = catchsWithSize.stream()
                    .filter(c -> {
                        if (useEditedInBoInformation) {
                           return Objects.equals(c.getEditedSpeciesId(),speciesId);
                        } else {
                            return  Objects.equals(c.getSpeciesId(), speciesId);
                        }
                     })
                    .toList();
            Map<Month, Map<Maillage, Double>> speciesMonthlySizes = new HashMap<>();
            for (Month month : Month.values()) {
                Map<Maillage, Double> averagePerMaillage = new HashMap<>();
                for(Maillage maillageType: Maillage.values()) {
                    Stream<Catch> filteredCatch = catchs.stream()
                            .filter(c -> {
                                        if (month.equals(catchesMonths.get(c.getId()))) {
                                            // If more than one lake selected, cannot make distrinction
                                            // as maille size can be different from one lake to another
                                            if (!lakesFilter.isPresent() || lakesFilter.get().size() > 1) {
                                                return maillageType == Maillage.NON_DEFINI;
                                            } else {
                                                return c.getMaillee() == maillageType;
                                            }
                                        }
                                        return false;
                                    }
                            );
                    OptionalDouble average;
                    if (useEditedInBoInformation) {
                        average = filteredCatch.mapToInt(Catch::getEditedSize).average();
                    } else {
                        average = filteredCatch.mapToInt(Catch::getSize).average();
                    }
                    average.ifPresent(val -> averagePerMaillage.put(maillageType, val));
                }
                if (!averagePerMaillage.isEmpty()) {
                    speciesMonthlySizes.put(month, averagePerMaillage);
                }
            }
            if (!speciesMonthlySizes.isEmpty()) {
                builder.put(speciesId, speciesMonthlySizes);
            }
        }
        Map<UUID, Map<Month, Map<Maillage, Double>>> result = builder.build();
        return result;
    }

    /**
     * Calcule les 5 espèces les plus capturées à partir de la distribution fournie
     */
    protected List<UUID> get5MostCaughtSpecies(Map<UUID, Integer> caughtSpeciesCount) {
        Map<UUID, Integer> copy = new HashMap<>(caughtSpeciesCount);
        List<UUID> result = new LinkedList<>();
        while (result.size() < 5 && !copy.isEmpty()) {
            List<UUID> subList = getMostCaughtSpecies(copy);
            result.addAll(subList);
            subList.forEach(copy::remove);
        }
        return result;
    }

    /**
     * Calcule la (ou les si ex-aequo) espèce(s) la(es) plus capturée(s)
     */
    protected List<UUID> getMostCaughtSpecies(Map<UUID, Integer> caughtSpeciesCount) {
        List<UUID> result = new LinkedList<>();
        int max = 0;
        for (Map.Entry<UUID, Integer> entry : caughtSpeciesCount.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                result.clear();
                result.add(entry.getKey());
            } else if (entry.getValue() == max) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    protected List<CatchBean> toDashboardTopCatchs(Collection<Catch> catches,
                                                   Ordering<Catch> ordering,
                                                   ListMultimap<UUID, Integer> catchsWithPictures,
                                                   Set<UUID> measurementPictures) {
        List<CatchBean> result = ordering.immutableSortedCopy(catches)
                .stream()
                .limit(5)
                .map(aCatch -> TripResource.toCatchBean(aCatch, catchsWithPictures, measurementPictures))
                .toList();
        return result;
    }

    protected Map<UUID, List<CatchBean>> computeTopCatchs(Collection<Catch> allCatches,
                                                          ListMultimap<UUID, Integer> catchsWithPictures,
                                                          Set<UUID> measurementPictures,
                                                          Function<Catch, Integer> getter) {
        Multimap<UUID, Catch> catchsBySpecies = Multimaps.index(allCatches, Catch::getSpeciesId);
        // On commence par retirer les captures dont la valeur est nulle
        catchsBySpecies = Multimaps.filterValues(catchsBySpecies, aCatch -> getter.apply(aCatch) != null);

        Ordering<Catch> ordering = Ordering.natural()
                .onResultOf(getter::apply)
                .reverse();

        Map<UUID, List<CatchBean>> result = new HashMap<>();
        catchsBySpecies.asMap()
                .forEach((key, value) -> {
                    List<CatchBean> topCatchs = toDashboardTopCatchs(value, ordering, catchsWithPictures, measurementPictures);
                    result.put(key, topCatchs);
                });
        return result;
    }
}
