package fr.inrae.fishola.rest.dashboard;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.trips.CatchBean;
import fr.inrae.fishola.rest.trips.TripResource;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Inject;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource extends AbstractFisholaResource {

    @Inject
    protected CatchsDao catchsDao;

    @Inject
    protected TripsDao tripsDao;

    @GET
    @Path("/dashboard")
    public Dashboard getDashboard(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {

        UUID userId = getUserId(cookie);

        ImmutableDashboard.Builder builder = ImmutableDashboard.builder();

        List<Catch> allCatches = catchsDao.findAllByUserId(userId);
        int allCatchsCount = allCatches.size();

        Map<UUID, Double> caughtSpeciesDistribution = computeDistribution(allCatches);
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

        Dashboard result = builder.build();
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

    protected Map<UUID, List<CatchBean>> computeTopCatchs(List<Catch> allCatches,
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

    protected Map<UUID, Double> computeDistribution(List<Catch> allCatches) {
        int allCatchsCount = allCatches.size();
        ImmutableMultiset<UUID> caughtSpeciesDistributionSet = allCatches.stream()
                .map(Catch::getSpeciesId)
                .collect(ImmutableMultiset.toImmutableMultiset());
        Map<UUID, Integer> caughtSpeciesDistributionCount = caughtSpeciesDistributionSet.entrySet()
                .stream()
                .collect(Collectors.toMap(Multiset.Entry::getElement, Multiset.Entry::getCount));
        Map<UUID, Double> result = Maps.transformValues(caughtSpeciesDistributionCount, count -> count * 100d / allCatchsCount);
        return result;
    }

    protected PaginationResult<DashboardLastTrip> computeLatestTrips(UUID userId, List<Catch> allCatches) {
        ImmutableListMultimap<UUID, Catch> allCatchsIndex = Multimaps.index(allCatches, Catch::getTripId);
        PaginationParameter page = PaginationParameter.builder(0, 9)
                .addOrder("date", true)
                .build();
        PaginationResult<Trip> latestTripsEntities = tripsDao.listMyTrips(userId, page, Optional.empty());
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

}
