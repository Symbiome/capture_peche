package fr.inra.fishola.rest;

import fr.inra.fishola.database.ReferentialDao;
import fr.inra.fishola.entities.tables.pojos.Lake;
import fr.inra.fishola.entities.tables.pojos.Method;
import fr.inra.fishola.entities.tables.pojos.Species;
import fr.inra.fishola.entities.tables.pojos.Weather;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/v1/referential")
@Produces(MediaType.APPLICATION_JSON)
public class ReferentialResource {

    @Inject
    protected ReferentialDao referentialDao;

    @GET
    @Path("/lakes")
    public List<Lake> getLakes() {
        List<Lake> result = referentialDao.listLakes();
        return result;
    }

    @GET
    @Path("/methods")
    public List<Method> getMethods() {
        List<Method> result = referentialDao.listBuiltInMethods();
        return result;
    }

    @GET
    @Path("/species")
    public List<Species> getSpecies() {
        List<Species> result = referentialDao.listBuiltInSpecies();
        return result;
    }

    @GET
    @Path("/weathers")
    public List<Weather> getWeathers() {
        List<Weather> result = referentialDao.listWeathers();
        return result;
    }

}
