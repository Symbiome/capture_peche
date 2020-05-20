package fr.inrae.fishola.rest.referential;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.pojos.AuthorizedSample;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.ReleasedFishState;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public List<SpeciesWithAlias> getSpecies() {
        List<Species> species = referentialDao.listBuiltInSpecies();
        List<SpeciesWithAlias> result = species.stream()
                .map(SpeciesWithAlias::of)
                .collect(Collectors.toList());
        return result;
    }

    @GET
    @Path("/species-custom")
    public List<SpeciesWithAlias> getCustomSpecies() {
        List<Species> species = referentialDao.listCustomSpecies();
        List<SpeciesWithAlias> result = species.stream()
                .map(SpeciesWithAlias::of)
                .collect(Collectors.toList());
        return result;
    }

    @GET
    @Path("/species-per-lake")
    public Map<UUID, Collection<SpeciesWithAlias>> getSpeciesPerLake() {
        Map<UUID, Species> rawSpeciesIndex = referentialDao.speciesIndex();

        List<SpeciesByLake> entities = referentialDao.listSpeciesByLake();
        Multimap<UUID, SpeciesByLake> entitiesByLakeId = Multimaps.index(entities, SpeciesByLake::getLakeId);

        List<AuthorizedSample> authorizedSamples = getAuthorizedSamples();
        Set<Pair<UUID, UUID>> authorizedSamplesSet = authorizedSamples.stream()
                .map(authorizedSample -> Pair.of(authorizedSample.getLakeId(), authorizedSample.getSpeciesId()))
                .collect(Collectors.toSet());

        Multimap<UUID, SpeciesWithAlias> result = Multimaps.transformEntries(entitiesByLakeId, (lakeId, input) -> {
            Species rawSpecies = rawSpeciesIndex.get(input.getSpeciesId());
            String alias = input.getAlias();
            boolean authorizedSample = authorizedSamplesSet.contains(Pair.of(lakeId, rawSpecies.getId()));
            SpeciesWithAlias speciesWithAlias = SpeciesWithAlias.of(rawSpecies, alias, authorizedSample);
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

    @GET
    @Path("/authorized-samples")
    public List<AuthorizedSample> getAuthorizedSamples() {
        List<AuthorizedSample> result = referentialDao.listAuthorizedSamples();
        return result;
    }

}
