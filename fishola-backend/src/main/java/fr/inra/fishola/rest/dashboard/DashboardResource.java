package fr.inra.fishola.rest.dashboard;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import fr.inra.fishola.database.CatchsDao;
import fr.inra.fishola.database.TripsDao;
import fr.inra.fishola.entities.tables.pojos.Catch;
import fr.inra.fishola.entities.tables.pojos.Trip;
import fr.inra.fishola.rest.AbstractFisholaResource;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Inject;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

        ImmutableListMultimap<UUID, Catch> allCatchsIndex = Multimaps.index(allCatches, Catch::getTripId);

        ImmutableMultiset<UUID> caughtSpeciesDistributionSet = allCatches.stream()
                .map(Catch::getSpeciesId)
                .collect(ImmutableMultiset.toImmutableMultiset());
        Map<UUID, Integer> caughtSpeciesDistributionCount = caughtSpeciesDistributionSet.entrySet()
                .stream()
                .collect(Collectors.toMap(Multiset.Entry::getElement, Multiset.Entry::getCount));
        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesDistributionCount, count -> count * 100d / allCatchsCount);
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);

        PaginationParameter page = PaginationParameter.builder(0, 9)
                .addOrder("date", true)
                .build();
        PaginationResult<Trip> latestTrips = tripsDao.listMyTrips(userId, page, Optional.empty());
        int allTripsCount = (int) latestTrips.getCount();

        if (allTripsCount > 0) {
            double averageCatchsPerTrip = 1d * allCatchsCount / allTripsCount;
            builder.averageCatchsPerTrip(averageCatchsPerTrip);
        }

        List<DashboardLastTrip> latestTripsCatchs = latestTrips.getElements()
                .stream()
                .map(trip -> toDashboardLastTrip(trip, allCatchsIndex))
                .collect(Collectors.toList());
        builder.latestTripsCatchs(latestTripsCatchs);

        Dashboard result = builder.build();
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
