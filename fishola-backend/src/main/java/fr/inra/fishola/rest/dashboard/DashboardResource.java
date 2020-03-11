package fr.inra.fishola.rest.dashboard;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import fr.inra.fishola.database.CatchsDao;
import fr.inra.fishola.entities.tables.pojos.Catch;
import fr.inra.fishola.rest.AbstractFisholaResource;

import javax.inject.Inject;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource extends AbstractFisholaResource {

    @Inject
    protected CatchsDao catchsDao;

    @GET
    @Path("/dashboard")
    public Dashboard getDashboard(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {

        UUID userId = getUserId(cookie);

        List<Catch> allCatches = catchsDao.findAllByUserId(userId);

        ImmutableMultiset<UUID> caughtSpeciesDistributionSet = allCatches.stream()
                .map(Catch::getSpeciesId)
                .collect(ImmutableMultiset.toImmutableMultiset());
        Map<UUID, Integer> caughtSpeciesDistributionCount = caughtSpeciesDistributionSet.entrySet()
                .stream()
                .collect(Collectors.toMap(Multiset.Entry::getElement, Multiset.Entry::getCount));
        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesDistributionCount, count -> count * 100d / allCatches.size());

        ImmutableDashboard.Builder builder = ImmutableDashboard.builder();
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);
        Dashboard result = builder.build();
        return result;
    }

}
