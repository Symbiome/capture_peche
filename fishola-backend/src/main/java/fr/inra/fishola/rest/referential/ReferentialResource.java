package fr.inra.fishola.rest.referential;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import fr.inra.fishola.database.ReferentialDao;
import fr.inra.fishola.entities.tables.pojos.Lake;
import fr.inra.fishola.entities.tables.pojos.Species;
import fr.inra.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inra.fishola.entities.tables.pojos.Technique;
import fr.inra.fishola.entities.tables.pojos.Weather;
import fr.inra.fishola.rest.AbstractFisholaResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/referential")
@Produces(MediaType.APPLICATION_JSON)
public class ReferentialResource extends AbstractFisholaResource {

    @Inject
    protected ReferentialDao referentialDao;

    @GET
    @Path("/lakes")
    public List<Lake> getLakes() {
        List<Lake> result = referentialDao.listLakes();
        return result;
    }

    @GET
    @Path("/techniques")
    public List<Technique> getMethods() {
        List<Technique> result = referentialDao.listBuiltInMethods();
        return result;
    }

    @GET
    @Path("/species")
    public List<Species> getSpecies() {
        List<Species> result = referentialDao.listBuiltInSpecies();
        return result;
    }

    @GET
    @Path("/species-per-lake")
    public Map<UUID, Collection<Species>> getSpeciesPerLake() {
        Map<UUID, Species> rawSpeciesIndex = referentialDao.speciesIndex();

        List<SpeciesByLake> entities = referentialDao.listSpeciesByLake();
        Multimap<UUID, SpeciesByLake> entitiesByLakeId = Multimaps.index(entities, SpeciesByLake::getLakeId);

        Multimap<UUID, Species> result = Multimaps.transformValues(entitiesByLakeId, input -> {
            Species rawSpecies = rawSpeciesIndex.get(input.getSpeciesId());
            String nameOrAlias = Optional.ofNullable(input.getAlias()).orElse(rawSpecies.getName());
            Species speciesWithAlias = new Species(rawSpecies.getId(), nameOrAlias, rawSpecies.getBuiltIn());
            return speciesWithAlias;
        });

        return result.asMap();
    }

    @GET
    @Path("/weathers")
    public List<Weather> getWeathers() {
        List<Weather> result = referentialDao.listWeathers();
        return result;
    }

}
