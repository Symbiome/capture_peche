package fr.inrae.fishola.rest.referential;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.pojos.AuthorizedSample;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.ReleasedFishState;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.exceptions.AccessDeniedException;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Path("/api/v1/referential")
@Produces(MediaType.APPLICATION_JSON)
public class ReferentialResource extends AbstractFisholaResource {

    @Inject
    protected ReferentialDao referentialDao;

    @Inject
    protected CatchsDao catchsDao;

    @GET
    @Path("/lakes")
    public List<Lake> getLakes() {
        Set<UUID> allowedAdminLakes = getAllowedAdminLakes();
        if (!allowedAdminLakes.isEmpty()) {
            return referentialDao.fetchLakesById(allowedAdminLakes);
        } else {
            return referentialDao.listLakes();
        }
    }

    @PUT
    @Path("/lakes/{lakeId}")
    public Response updateLake(@PathParam("lakeId") UUID lakeId, Lake lake) {
        Preconditions.checkArgument(lakeId != null, "Identifiant de lac obligatoire");
        Preconditions.checkArgument(lakeId.equals(lake.getId()), "L'identifiant ne correspond pas");
        checkIsAdmin();
        referentialDao.updateLake(lake);
        return Response.noContent().build();
    }

    @POST
    @Path("/lakes")
    public Response createLake(Lake lake) {
        checkIsAdmin();
        referentialDao.createLake(lake);
        return Response.noContent().build();
    }

    @GET
    @Path("/techniques")
    public List<Technique> getTechniques() {
        List<Technique> result = referentialDao.listBuiltInTechniques();
        return result;
    }

    @PUT
    @Path("/techniques/{techniqueId}")
    public Response updateTechnique(@PathParam("techniqueId") UUID techniqueId, Technique technique) {
        Preconditions.checkArgument(techniqueId != null, "Identifiant de technique obligatoire");
        Preconditions.checkArgument(techniqueId.equals(technique.getId()), "L'identifiant ne correspond pas");
        checkIsAdmin();
        referentialDao.updateTechnique(technique);
        return Response.noContent().build();
    }

    @POST
    @Path("/techniques")
    public Response createTechnique(Technique technique) {
        checkIsAdmin();
        referentialDao.createTechnique(technique);
        return Response.noContent().build();
    }

    @GET
    @Path("/techniques/can-delete/{techniqueId}")
    public Response canDeleteTechnique(@PathParam("techniqueId") UUID techniqueId) {
        boolean canDelete = referentialDao.canDeleteTechnique(techniqueId);
        return Response.ok(canDelete).build();
    }

    @DELETE
    @Path("/techniques/{techniqueId}")
    public Response deleteTechnique(@PathParam("techniqueId") UUID techniqueId) {
        checkIsAdmin();
        referentialDao.deleteTechnique(techniqueId);
        return Response.noContent().build();
    }

    @GET
    @Path("/raw-species")
    public List<Species> getRawSpecies() {
        List<Species> species = referentialDao.listAllSpecies();
        return species;
    }

    @PUT
    @Path("/raw-species/{speciesId}")
    public Response updateSpecie(@PathParam("speciesId") UUID speciesId, Species species) {
        Preconditions.checkArgument(speciesId != null, "Identifiant d'espèce obligatoire");
        Preconditions.checkArgument(speciesId.equals(species.getId()), "L'identifiant ne correspond pas");
        checkIsAdmin();
        referentialDao.updateSpecies(species);
        return Response.noContent().build();
    }

    @POST
    @Path("/raw-species")
    public Response createSpecie( Species species) {
        checkIsAdmin();
        referentialDao.createSpecie(species);
        return Response.noContent().build();
    }

    @GET
    @Path("/raw-species/can-delete/{speciesId}")
    public Response canDeleteSpecie(@PathParam("speciesId") UUID speciesId) {
        boolean canDelete = referentialDao.canDeleteSpecie(speciesId);
        return Response.ok(canDelete).build();
    }

    @DELETE
    @Path("/raw-species/{speciesId}")
    public Response deleteSpecie(@PathParam("speciesId") UUID speciesId) {
        checkIsAdmin();
        referentialDao.deleteSpecie(speciesId);
        return Response.noContent().build();
    }

    @GET
    @Path("/species")
    public List<SpeciesWithAlias> getSpecies() {
        List<Species> species = referentialDao.listBuiltInSpecies();
        List<SpeciesWithAlias> result = species.stream()
                .map(SpeciesWithAlias::of)
                .toList();
        return result;
    }

    @GET
    @Path("/species-custom")
    public List<SpeciesWithAlias> getCustomSpecies() {
        List<Species> species = referentialDao.listCustomSpecies();
        List<SpeciesWithAlias> result = species.stream()
                .map(SpeciesWithAlias::of)
                .toList();
        return result;
    }

    @GET
    @Path("/species-per-lake")
    public Map<UUID, Collection<SpeciesWithAlias>> getSpeciesPerLake() {

        // On récupère la liste des toutes les espèces builtIn et des lacs
        List<Species> builtInSpecies = referentialDao.listBuiltInSpecies();
        Set<UUID> lakeIds = Sets.newLinkedHashSet();
        // If logged as local admin
        // We filter the species ton only show relevant ones
        Set<UUID> allowedAdminLakes = getAllowedAdminLakes();
        if (!allowedAdminLakes.isEmpty()) {
            lakeIds = allowedAdminLakes;
        } else {
            lakeIds = referentialDao.listLakes()
                    .stream()
                    .map(Lake::getId).collect(Collectors.toSet());
        }

        // On charge les alias par lac+espèce et on en fait un index
        List<SpeciesByLake> speciesByLake = referentialDao.listSpeciesByLake();
        Map<Pair<UUID, UUID>, SpeciesByLake> speciesByLakeIndex = Maps.uniqueIndex(speciesByLake, sbl -> Pair.of(sbl.getLakeId(), sbl.getSpeciesId()));

        // On charge les autorisations de prélèvement par lac+espèce et on en fait un index
        List<AuthorizedSample> authorizedSamples = getAuthorizedSamples();
        Set<Pair<UUID, UUID>> authorizedSamplesSet = authorizedSamples.stream()
                .map(authorizedSample -> Pair.of(authorizedSample.getLakeId(), authorizedSample.getSpeciesId()))
                .collect(Collectors.toSet());
        Map<Pair<UUID, UUID>, Integer> authorizedSamplesMinSizes = new LinkedHashMap<>();
        authorizedSamples.stream().forEach(as ->
            authorizedSamplesMinSizes.put(Pair.of(as.getLakeId(), as.getSpeciesId()), as.getMinSize())
        );
        Map<Pair<UUID, UUID>, Integer> authorizedSamplesMaxSizes = new LinkedHashMap<>();
        authorizedSamples.stream().forEach(as ->
            authorizedSamplesMaxSizes.put(Pair.of(as.getLakeId(), as.getSpeciesId()), as.getMaxSize())
        );

        // On compile le tout
        Multimap<UUID, SpeciesWithAlias> result = HashMultimap.create();
        lakeIds.forEach(lakeId -> builtInSpecies.forEach(rawSpecies -> {
            Pair<UUID, UUID> lakePlusSpeciesIds = Pair.of(lakeId, rawSpecies.getId());
            Optional<String> alias = Optional.ofNullable(speciesByLakeIndex.get(lakePlusSpeciesIds)).map(SpeciesByLake::getAlias);
            boolean authorizedSample = authorizedSamplesSet.contains(lakePlusSpeciesIds);
            Integer minSize = 0;
            Integer maxSize = 1000;
            if (authorizedSamplesMinSizes.get(lakePlusSpeciesIds) != null) {
                minSize = authorizedSamplesMinSizes.get(lakePlusSpeciesIds);
            }
            if (authorizedSamplesMaxSizes.get(lakePlusSpeciesIds) != null) {
                maxSize = authorizedSamplesMaxSizes.get(lakePlusSpeciesIds);
            }
            SpeciesWithAlias speciesWithAlias = SpeciesWithAlias.of(rawSpecies, alias, authorizedSample, minSize, maxSize);
            result.put(lakeId, speciesWithAlias);
        }));
        return result.asMap();
    }

    @PUT
    @Path("/species-aliases-per-lake")
    public Response saveSpeciesAliasesPerLake(Map<UUID, Map<UUID, String>> aliases) {
        checkIsAdmin();

        // On transforme la map pour avoir en clé lakeId+speciesId et en valeur les alias
        Map<Pair<UUID, UUID>, String> aliasesMap = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, String>> byLakeEntry : aliases.entrySet()) {
            Map<UUID, String> bySpeciesEntries = byLakeEntry.getValue();
            for (Map.Entry<UUID, String> entry : bySpeciesEntries.entrySet()) {
                Pair<UUID, UUID> lakePluSpeciesId = Pair.of(byLakeEntry.getKey(), entry.getKey());
                String alias = entry.getValue();
                if (StringUtils.isNotEmpty(alias)) {
                    aliasesMap.put(lakePluSpeciesId, alias);
                }
            }
        }

        // On charge les alias par lac+espèce
        List<SpeciesByLake> speciesByLake = referentialDao.listSpeciesByLake();

        // On commence par mettre à jour ou supprimer les espèces par lac existantes
        for (SpeciesByLake entity : speciesByLake) {
            Pair<UUID, UUID> lakePluSpeciesId = Pair.of(entity.getLakeId(), entity.getSpeciesId());
            if (aliasesMap.containsKey(lakePluSpeciesId)) {
                String newAlias = aliases.get(entity.getLakeId()).get(entity.getSpeciesId());
                entity.setAlias(newAlias);
                referentialDao.updateSpeciesByLake(entity);
            } else {
                referentialDao.deleteSpeciesByLake(entity);
            }
            aliasesMap.remove(lakePluSpeciesId);
        }

        // Puis on créé les nouvelles
        aliasesMap.entrySet()
                .stream()
                .map(entry -> {
                    UUID lakeId = entry.getKey().getKey();
                    UUID speciesId = entry.getKey().getValue();
                    String alias = entry.getValue();
                    SpeciesByLake result = new SpeciesByLake(lakeId, speciesId, alias);
                    return result;
                })
                .forEach(referentialDao::createSpeciesByLake);

        Response response = Response.noContent().build();
        return response;
    }

    @PUT
    @Path("/authorized-samples")
    public Response saveAuthorizedSamples(List<Map<UUID, Map<UUID, Object>>> authorizationsAndMinMaxSizes) {
        checkIsAdmin();

        // On transforme la map pour avoir un Set des clé lakeId+speciesId autorisées
        Set<Pair<UUID, UUID>> authorizationsSet = new HashSet<>();
        Map<Pair<UUID, UUID>, Integer> minSizesMap = new LinkedHashMap<>();
        Map<Pair<UUID, UUID>, Integer> maxSizesMap = new LinkedHashMap<>();
        Map<UUID, Map<UUID, Object>> authorizations = authorizationsAndMinMaxSizes.get(0);
        Map<UUID, Map<UUID, Object>> minSizes = authorizationsAndMinMaxSizes.get(1);
        Map<UUID, Map<UUID, Object>> maxSizes = authorizationsAndMinMaxSizes.get(2);
        for (Map.Entry<UUID, Map<UUID, Object>> byLakeEntry : authorizations.entrySet()) {
            Map<UUID, Object> bySpeciesEntries = byLakeEntry.getValue();
            for (Map.Entry<UUID, Object> entry : bySpeciesEntries.entrySet()) {
                Pair<UUID, UUID> lakePluSpeciesId = Pair.of(byLakeEntry.getKey(), entry.getKey());
                Object authorized = entry.getValue();
                if (Boolean.TRUE.equals(authorized)) {
                    authorizationsSet.add(lakePluSpeciesId);
                }
                if (minSizes.get(byLakeEntry.getKey()) != null && minSizes.get(byLakeEntry.getKey()).get(entry.getKey()) != null) {
                    minSizesMap.put(lakePluSpeciesId, Integer.parseInt(minSizes.get(byLakeEntry.getKey()).get(entry.getKey()).toString()));
                }
                if (maxSizes.get(byLakeEntry.getKey()) != null && maxSizes.get(byLakeEntry.getKey()).get(entry.getKey()) != null) {
                    maxSizesMap.put(lakePluSpeciesId, Integer.parseInt(maxSizes.get(byLakeEntry.getKey()).get(entry.getKey()).toString()));
                }
            }
        }

        // On charge les autorisations par lac+espèce et on en fait un index
        List<AuthorizedSample> existingAuthorizations = referentialDao.listAuthorizedSamples();

        // On commence par supprimer les autorisations en trop
        for (AuthorizedSample entity : existingAuthorizations) {
            Pair<UUID, UUID> lakePluSpeciesId = Pair.of(entity.getLakeId(), entity.getSpeciesId());
            if (!authorizationsSet.contains(lakePluSpeciesId)) {
                referentialDao.deleteAuthorizedSample(entity);
            } else {
                // On met à jour uniquement la taille si l'autorisation existe dejà
                Integer minSize = 0;
                if (minSizesMap.get(lakePluSpeciesId) != null) {
                    minSize = minSizesMap.get(lakePluSpeciesId);
                }
                Integer maxSize = 0;
                if (maxSizesMap.get(lakePluSpeciesId) != null) {
                    maxSize = maxSizesMap.get(lakePluSpeciesId);
                }
                entity.setMinSize(minSize);
                entity.setMaxSize(maxSize);
                referentialDao.updateAuthorizeSample(entity);
            }
            authorizationsSet.remove(lakePluSpeciesId);
        }

        // Puis on créé les nouvelles
        authorizationsSet
            .stream()
            .map(entry -> {
                Integer minSize = 0;
                Integer maxSize = 0;
                if (minSizesMap.get(entry) != null) {
                    minSize = minSizesMap.get(entry);
                }
                if (maxSizesMap.get(entry) != null) {
                    maxSize = maxSizesMap.get(entry);
                }
                AuthorizedSample authorizedSample = new AuthorizedSample();
                authorizedSample.setLakeId(entry.getKey());
                authorizedSample.setSpeciesId(entry.getValue());
                authorizedSample.setMinSize(minSize);
                authorizedSample.setMaxSize(maxSize);
                return authorizedSample;
            })
            .forEach(referentialDao::createAuthorizedSample);

        Response response = Response.noContent().build();
        return response;
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

    @PUT
    @Path("/weathers/{weatherId}")
    public Response updateWeather(@PathParam("weatherId") UUID weatherId, Weather weather) {
        checkIsAdmin();
        Preconditions.checkArgument(weather != null, "Identifiant de météo obligatoire");
        Preconditions.checkArgument(weatherId.equals(weather.getId()), "L'identifiant ne correspond pas");
        referentialDao.updateWeather(weather);
        return Response.noContent().build();
    }

    @POST
    @Path("/weathers")
    public Response createWeather(Weather weather) {
        checkIsAdmin();
        referentialDao.createWeather(weather);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/weathers/{weatherId}")
    public Response deleteWeather(@PathParam("weatherId") UUID weatherId) {
        checkIsAdmin();
        referentialDao.deleteWeather(weatherId);
        return Response.noContent().build();
    }
    @GET
    @Path("/weathers/can-delete/{weatherId}")
    public Response canDeleteWeather(@PathParam("weatherId") UUID weatherId) {
        boolean canDelete = referentialDao.canDeleteWeather(weatherId);
        return Response.ok(canDelete).build();
    }

    @GET
    @Path("/authorized-samples")
    public List<AuthorizedSample> getAuthorizedSamples() {
        List<AuthorizedSample> result = referentialDao.listAuthorizedSamples();
        return result;
    }


}
