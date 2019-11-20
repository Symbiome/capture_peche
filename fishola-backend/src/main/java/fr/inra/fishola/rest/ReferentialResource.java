package fr.inra.fishola.rest;

import fr.inra.fishola.database.ReferentialDao;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/referential")
@Produces(MediaType.APPLICATION_JSON)
public class ReferentialResource {

    @Inject
    protected ReferentialDao referentialDao;

    @GET
    @Path("/lakes")
    public Map<UUID, String> getLakes() {
        Map<UUID, String> result = referentialDao.listLakes();
        return result;
    }

    @GET
    @Path("/methods")
    public Map<UUID, String> getMethods() {
        Map<UUID, String> result = referentialDao.listBuiltInMethods();
        return result;
    }

    @GET
    @Path("/species")
    public Map<UUID, String> getSpecies() {
        Map<UUID, String> result = referentialDao.listBuiltInSpecies();
        return result;
    }

    @GET
    @Path("/weathers")
    public Map<UUID, String> getWeathers() {
        Map<UUID, String> result = referentialDao.listWeathers();
        return result;
    }

}
