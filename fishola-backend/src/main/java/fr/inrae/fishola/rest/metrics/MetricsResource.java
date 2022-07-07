package fr.inrae.fishola.rest.metrics;


import fr.inrae.fishola.database.MetricsDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/v1/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource extends AbstractFisholaResource {

    @Inject
    MetricsDao metricsDao;

    @GET
    @Path("")
    public MetricBean getMetrics() {
        checkIsAdmin();
        return metricsDao.getMetrics();
    }
}
