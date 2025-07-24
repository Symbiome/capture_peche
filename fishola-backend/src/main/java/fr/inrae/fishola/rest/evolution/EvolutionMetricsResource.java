package fr.inrae.fishola.rest.evolution;
import fr.inrae.fishola.database.DashboardDao;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.dashboard.EvolutionMetricsForLake;
import fr.inrae.fishola.rest.dashboard.ImmutableEvolutionMetricsForLake;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Month;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class EvolutionMetricsResource extends AbstractFisholaResource {

    @Inject
    private DashboardDao dashboardDao;

    /**
     * Returns for the given lake a map having :
     * - One entry per year
     * - For each year, on entry per specie id
     * - For each specie, one entry per month
     * - For each month a pair representing count and avg size of the given specie
     */
    @GET
    @Path("/evolution/catches/{lakeId}")
    public EvolutionMetricsForLake getCatchStats(
            @PathParam("lakeId") UUID lakeId
    ) {
        // TODO
         /*final GlobalDashboard result = GLOBAL_DASHBOARD_HOLDER.get(
                this::computeNewGlobalDashboard,
                GlobalDashboard::computedOn,
                Duration.ofMinutes(config.globalDashboardTimeoutMinutes()),
                false
        );*/
        Map<Integer, Map<UUID, Map<Month, Map<Maillage, Pair<Long, Double>>>>> catchStatsForLake = dashboardDao.getCatchStatsForLake(lakeId, Optional.empty());
        ImmutableEvolutionMetricsForLake statsForLake = ImmutableEvolutionMetricsForLake.builder().monthlySizesPerMaillageAndYear(catchStatsForLake).build();
        return statsForLake;
    }
}
