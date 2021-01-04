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

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import fr.inrae.fishola.rest.trips.CatchBean;
import fr.inrae.fishola.rest.trips.TripResource;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource extends AbstractFisholaResource {

    @Inject
    protected ReferentialDao referentialDao;

    @Inject
    protected CatchsDao catchsDao;

    @Inject
    protected TripsDao tripsDao;

    @GET
    @Path("/dashboard")
    public Response getDashboard() {

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        ImmutableDashboard.Builder builder = ImmutableDashboard.builder();

        Optional<Integer> yearFilter = getYearFilter();
        Multimap<Month, Catch> monthlyCatchs = catchsDao.findMonthlyByUserId(userId, yearFilter);

        Collection<Catch> allCatches = monthlyCatchs.values();
        int allCatchsCount = allCatches.size();

        Map<UUID, Integer> caughtSpeciesCount = computeDistribution(allCatches);
        builder.caughtSpeciesCount(caughtSpeciesCount);

        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);

        PaginationResult<DashboardLastTrip> latestTrips = computeLatestTrips(userId, allCatches);
        builder.latestTripsCatchs(latestTrips.getElements());
        int allTripsCount = (int) latestTrips.getCount();
        if (allTripsCount > 0) {
            double averageCatchsPerTrip = 1d * allCatchsCount / allTripsCount;
            builder.averageCatchsPerTrip(averageCatchsPerTrip);
        }

        Set<UUID> catchIds = allCatches.stream()
                .map(Catch::getId)
                .collect(Collectors.toSet());
        Set<UUID> catchsWithPictures = catchsDao.checkForPictures(catchIds);
        Map<UUID, List<CatchBean>> topBySize = computeTopCatchs(allCatches, catchsWithPictures, Catch::getSize);
        builder.topBySize(topBySize);

        Map<UUID, List<CatchBean>> topByWeight = computeTopCatchs(allCatches, catchsWithPictures, Catch::getWeight);
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

        Map<UUID, Map<Month, Double>> monthlySizes = computeMonthlySizes(mostCaughtSpecies, monthlyCatchs);
        builder.monthlySizes(monthlySizes);

        Dashboard result = builder.build();
        Response response = wrapEntity(result, userIdAndRenewal);
        return response;
    }

    protected Optional<Integer> getYearFilter() {
        Optional<Integer> result = config.isDashboardOnlyCurrentYear()
                ? Optional.of(Year.now().getValue())
                : Optional.empty();
        return result;
    }

    /**
     * Calcule la moyenne mensuelle des tailles de poissons pour les espèces spécifiées
     */
    protected Map<UUID, Map<Month, Double>> computeMonthlySizes(List<UUID> mostCaughtSpecies, Multimap<Month, Catch> monthlyCatches) {
        Map<UUID, Month> catchesMonths = monthlyCatches.entries().stream().collect(Collectors.toMap(e -> e.getValue().getId(), Map.Entry::getKey));
        Map<UUID, Map<Month, Double>> result = computeMonthlySizes(catchesMonths, mostCaughtSpecies, monthlyCatches.values());
        return result;
    }

    protected Map<UUID, Map<Month, Double>> computeMonthlySizes(Map<UUID, Month> catchesMonths, List<UUID> mostCaughtSpecies, Collection<Catch> allCatches) {
        // On garde uniquement les captures avec une taille
        List<Catch> catchsWithSize = allCatches.stream()
                .filter(c -> c.getSize() != null)
                .collect(Collectors.toList());
        ImmutableMap.Builder<UUID, Map<Month, Double>> builder = ImmutableMap.builder();
        for (UUID speciesId : mostCaughtSpecies) {
            // On prend les captures de la bonne espèce
            List<Catch> catchs = catchsWithSize.stream()
                    .filter(c -> c.getSpeciesId().equals(speciesId))
                    .collect(Collectors.toList());
            Map<Month, Double> speciesMonthlySizes = new HashMap<>();
            for (Month month : Month.values()) {
                OptionalDouble average = catchs.stream()
                        .filter(c -> month.equals(catchesMonths.get(c.getId())))
                        .mapToInt(Catch::getSize)
                        .average();
                average.ifPresent(val -> speciesMonthlySizes.put(month, val));
            }
            if (!speciesMonthlySizes.isEmpty()) {
                builder.put(speciesId, speciesMonthlySizes);
            }
        }
        Map<UUID, Map<Month, Double>> result = builder.build();
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
                                                      Set<UUID> catchsWithPictures) {
        List<CatchBean> result = ordering.immutableSortedCopy(catches)
                .stream()
                .limit(5)
                .map(aCatch -> TripResource.toCatchBean(aCatch, catchsWithPictures))
                .collect(Collectors.toList());
        return result;
    }

    protected Map<UUID, List<CatchBean>> computeTopCatchs(Collection<Catch> allCatches,
                                                        Set<UUID> catchsWithPictures,
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
                    List<CatchBean> topCatchs = toDashboardTopCatchs(value, ordering, catchsWithPictures);
                    result.put(key, topCatchs);
                });
        return result;
    }

    protected Map<UUID, Integer> computeDistribution(Collection<Catch> allCatches) {
        ImmutableMultiset<UUID> caughtSpeciesDistributionSet = allCatches.stream()
                .map(Catch::getSpeciesId)
                .collect(ImmutableMultiset.toImmutableMultiset());
        Map<UUID, Integer> result = caughtSpeciesDistributionSet.entrySet()
                .stream()
                .collect(Collectors.toMap(Multiset.Entry::getElement, Multiset.Entry::getCount));
        return result;
    }

    protected PaginationResult<DashboardLastTrip> computeLatestTrips(UUID userId, Collection<Catch> allCatches) {
        ImmutableListMultimap<UUID, Catch> allCatchsIndex = Multimaps.index(allCatches, Catch::getTripId);
        PaginationParameter page = PaginationParameter.builder(0, 9)
                .addOrder("date", true)
                .build();
        Optional<Integer> yearFilter = getYearFilter();
        PaginationResult<Trip> latestTripsEntities = tripsDao.listMyTrips(userId, page, Optional.empty(), yearFilter);
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


    @GET
    @Path("/global-dashboard")
    public GlobalDashboard getGlobalDashboard() {

        // TODO AThimel 30/12/2020 À mettre en cache

        ImmutableGlobalDashboard.Builder builder = ImmutableGlobalDashboard.builder();

        Optional<Integer> yearFilter = getYearFilter();
        Multimap<Month, Catch> monthlyCatchs = catchsDao.findAll(yearFilter);

        Collection<Catch> allCatches = monthlyCatchs.values();
        int allCatchsCount = allCatches.size();

        Map<UUID, Integer> caughtSpeciesCount = computeDistribution(allCatches);
        builder.caughtSpeciesCount(caughtSpeciesCount);

        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);

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

        Map<UUID, Map<Month, Double>> monthlySizes = computeMonthlySizes(mostCaughtSpecies, monthlyCatchs);
        builder.monthlySizes(monthlySizes);

        GlobalDashboard result = builder.build();
        return result;
    }

    @GET
    @Path("/dashboard/export")
    @Produces("text/csv")
    public Response exportAsCSV() {

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        String csv = tripsDao.getPersonalTripsCSV(userId);
        String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String disposition = String.format("filename=\"Fishola_Export_%s.csv\"", dateFormatted);

        Response.ResponseBuilder responseBuilder = Response.ok(csv)
                .header("Content-Disposition", disposition);
        Response response = buildResponse(responseBuilder, userIdAndRenewal);
        return response;
    }

}
