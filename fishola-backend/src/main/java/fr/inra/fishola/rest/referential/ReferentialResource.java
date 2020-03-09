package fr.inra.fishola.rest.referential;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import fr.inra.fishola.database.ReferentialDao;
import fr.inra.fishola.entities.tables.pojos.Lake;
import fr.inra.fishola.entities.tables.pojos.ReleasedFishState;
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
import java.util.UUID;
import java.util.stream.Collectors;

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
    public List<Technique> getTechniques() {
        List<Technique> result = referentialDao.listBuiltInTechniques();
        return result;
    }

    @GET
    @Path("/species")
    public List<Species> getSpecies() {
        List<Species> result = referentialDao.listBuiltInSpecies();
        return result;
    }

    @GET
    @Path("/species-custom")
    public List<SpeciesWithAlias> getCustomSpecies() {
        List<Species> species = referentialDao.listCustomSpecies();
        List<SpeciesWithAlias> result = species.stream()
                .map(s -> SpeciesWithAlias.of(s, null))
                .collect(Collectors.toList());
        return result;
    }

    @GET
    @Path("/species-per-lake")
    public Map<UUID, Collection<SpeciesWithAlias>> getSpeciesPerLake() {
        Map<UUID, Species> rawSpeciesIndex = referentialDao.speciesIndex();

        List<SpeciesByLake> entities = referentialDao.listSpeciesByLake();
        Multimap<UUID, SpeciesByLake> entitiesByLakeId = Multimaps.index(entities, SpeciesByLake::getLakeId);

        Multimap<UUID, SpeciesWithAlias> result = Multimaps.transformValues(entitiesByLakeId, input -> {
            Species rawSpecies = rawSpeciesIndex.get(input.getSpeciesId());
            String alias = input.getAlias();
            SpeciesWithAlias speciesWithAlias = SpeciesWithAlias.of(rawSpecies, alias);
            return speciesWithAlias;
        });

        return result.asMap();
    }

    @GET
    @Path("/released-fish-states")
    public List<ReleasedFishState> getReleasedFishState() {
        List<ReleasedFishState> result = referentialDao.listReleasedFishStates();
        return result;
    }

    @GET
    @Path("/weathers")
    public List<Weather> getWeathers() {
        List<Weather> result = referentialDao.listWeathers();
        return result;
    }

}
