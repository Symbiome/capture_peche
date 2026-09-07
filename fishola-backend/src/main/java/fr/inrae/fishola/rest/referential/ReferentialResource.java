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
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.pojos.AuthorizedSample;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.WaterEntity;
import fr.inrae.fishola.entities.tables.pojos.ReleasedFishState;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByWaterEntity;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import fr.inrae.fishola.rest.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
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

@Path("/api/v1/referential")
@Produces(MediaType.APPLICATION_JSON)
public class ReferentialResource extends AbstractFisholaResource {

    public static final String NO_MATCHING_ID = "L'identifiant ne correspond pas";
    @Inject
    protected ReferentialDao referentialDao;

    @GET
    @Path("/waterEntities")
    public List<WaterEntity> getAllWaterEntities() {
        if (adminToken == null) {
            return referentialDao.listWaterEntities();
        }
        // Lecture ouverte au staff (l'opérateur en a besoin pour la saisie) ; scopée au périmètre.
        FisholaAdmin fisholaAdmin = this.checkIsStaff();
        if (fisholaAdmin.getIsNationalAdmin()) {
            return referentialDao.listWaterEntities();
        } else {
            Set<UUID> allowedAdminWaterEntities = getAllowedAdminWaterEntities();
            return referentialDao.fetchWaterEntitiesById(allowedAdminWaterEntities);
        }
    }

    // Listing léger (sans géométrie) pour les formulaires de saisie (plan d'eau,
    // sortie) : getAllWaterEntities() sérialise la géométrie complète, ~1,2 Go
    // pour le réseau France entière, largement au-delà du timeout du client
    // mobile (5 s côté AbstractFisholaService.timeout). Même périmètre public
    // que getAllWaterEntities() côté pêcheur (pas de scoping admin ici, ce
    // listing n'est pas consommé par le back-office).
    @GET
    @Path("/waterEntities/summary")
    public List<WaterEntitySummary> getAllWaterEntitiesSummary() {
        return referentialDao.listWaterEntitiesSummary();
    }

    @GET
    @Path("/waterEntities/favorites")
    public List<WaterEntity> getFavoriteWaterEntities() {
        UserIdAndRenewal userIdOrRenew = this.getUserIdOrRenew();
        return usersDao.getFavoriteWaterEntities(userIdOrRenew.userId());
    }

    @PUT
    @Path("/waterEntities/{waterEntityId}")
    @Audited(value = "waterEntity.update", entityType = "water_entity", entityIdParam = "waterEntityId")
    public Response updateWaterEntity(@PathParam("waterEntityId") UUID waterEntityId, WaterEntity waterEntity) {
        Preconditions.checkArgument(waterEntityId != null, "Identifiant de plan d'eau obligatoire");
        Preconditions.checkArgument(waterEntityId.equals(waterEntity.getId()), NO_MATCHING_ID);
        checkIsNationalAdmin();
        referentialDao.updateWaterEntity(waterEntity);
        return Response.noContent().build();
    }

    // Retiré : les plans d'eau sont désormais gérés via la BD TOPO IGN, plus de création manuelle (#88).
    @POST
    @Path("/waterEntities")
    public Response createWaterEntity() {
        return Response.status(Response.Status.GONE).build();
    }

    @GET
    @Path("/techniques")
    public List<Technique> getTechniques() {
        List<Technique> result = referentialDao.listBuiltInTechniques();
        return result;
    }

    @PUT
    @Path("/techniques/{techniqueId}")
    @Audited(value = "technique.update", entityType = "technique", entityIdParam = "techniqueId")
    public Response updateTechnique(@PathParam("techniqueId") UUID techniqueId, Technique technique) {
        Preconditions.checkArgument(techniqueId != null, "Identifiant de technique obligatoire");
        Preconditions.checkArgument(techniqueId.equals(technique.getId()), NO_MATCHING_ID);
        checkIsNationalAdmin();
        referentialDao.updateTechnique(technique);
        return Response.noContent().build();
    }

    @POST
    @Path("/techniques")
    @Audited(value = "technique.create", entityType = "technique")
    public Response createTechnique(Technique technique) {
        checkIsNationalAdmin();
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
    @Audited(value = "technique.delete", entityType = "technique", entityIdParam = "techniqueId")
    public Response deleteTechnique(@PathParam("techniqueId") UUID techniqueId) {
        checkIsNationalAdmin();
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
    @Audited(value = "species.update", entityType = "species", entityIdParam = "speciesId")
    public Response updateSpecie(@PathParam("speciesId") UUID speciesId, Species species) {
        Preconditions.checkArgument(speciesId != null, "Identifiant d'espèce obligatoire");
        Preconditions.checkArgument(speciesId.equals(species.getId()), NO_MATCHING_ID);
        checkIsNationalAdmin();
        referentialDao.updateSpecies(species);
        return Response.noContent().build();
    }

    @POST
    @Path("/raw-species")
    @Audited(value = "species.create", entityType = "species")
    public Response createSpecie( Species species) {
        checkIsNationalAdmin();
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
    @Audited(value = "species.delete", entityType = "species", entityIdParam = "speciesId")
    public Response deleteSpecie(@PathParam("speciesId") UUID speciesId) {
        checkIsNationalAdmin();
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

    // Matrice espèces × entités hydrographiques du back-office « Maillages et
    // tailles maximales ». Le périmètre est OBLIGATOIREMENT borné (#154) : soit
    // une liste explicite d'entités (waterEntityId, après sélection dans l'UI),
    // soit un département. Sans borne, on renvoie une map vide plutôt que de
    // sérialiser tout le référentiel (~181 000 entités, #134), ce qui faisait
    // tomber le backend en OutOfMemoryError.
    @GET
    @Path("/species-per-waterEntity")
    public Map<UUID, Collection<SpeciesWithAlias>> getSpeciesPerWaterEntity(
            @QueryParam("department") String department,
            @QueryParam("waterEntityId") List<UUID> waterEntityIdParams) {

        Set<UUID> waterEntityIds = resolvePerimeter(department, waterEntityIdParams);
        if (waterEntityIds.isEmpty()) {
            return Map.of();
        }

        // On récupère la liste des toutes les espèces builtIn
        List<Species> builtInSpecies = referentialDao.listBuiltInSpecies();

        // On charge les alias par lac+espèce et on en fait un index
        List<SpeciesByWaterEntity> speciesByWaterEntity = referentialDao.listSpeciesByWaterEntity();
        Map<Pair<UUID, UUID>, SpeciesByWaterEntity> speciesByWaterEntityIndex = Maps.uniqueIndex(speciesByWaterEntity, sbl -> Pair.of(sbl.getWaterEntityId(), sbl.getSpeciesId()));

        // On charge les autorisations de prélèvement par lac+espèce et on en fait un index
        List<AuthorizedSample> authorizedSamples = getAuthorizedSamples();
        Set<Pair<UUID, UUID>> authorizedSamplesSet = authorizedSamples.stream()
                .map(authorizedSample -> Pair.of(authorizedSample.getWaterEntityId(), authorizedSample.getSpeciesId()))
                .collect(Collectors.toSet());
        Map<Pair<UUID, UUID>, Integer> authorizedSamplesMinSizes = new LinkedHashMap<>();
        authorizedSamples.stream().forEach(as ->
            authorizedSamplesMinSizes.put(Pair.of(as.getWaterEntityId(), as.getSpeciesId()), as.getMinSize())
        );
        Map<Pair<UUID, UUID>, Integer> authorizedSamplesMaxSizes = new LinkedHashMap<>();
        authorizedSamples.stream().forEach(as ->
            authorizedSamplesMaxSizes.put(Pair.of(as.getWaterEntityId(), as.getSpeciesId()), as.getMaxSize())
        );
        Map<Pair<UUID, UUID>, Integer> authorizedSamplesMeshSizes = new LinkedHashMap<>();
        authorizedSamples.stream().forEach(as ->
            authorizedSamplesMeshSizes.put(Pair.of(as.getWaterEntityId(), as.getSpeciesId()), as.getMeshSize())
        );

        // On compile le tout
        Multimap<UUID, SpeciesWithAlias> result = HashMultimap.create();
        waterEntityIds.forEach(waterEntityId -> builtInSpecies.forEach(rawSpecies -> {
            Pair<UUID, UUID> waterEntityPlusSpeciesIds = Pair.of(waterEntityId, rawSpecies.getId());
            Optional<String> alias = Optional.ofNullable(speciesByWaterEntityIndex.get(waterEntityPlusSpeciesIds)).map(SpeciesByWaterEntity::getAlias);
            Optional<Boolean> present = Optional.ofNullable(speciesByWaterEntityIndex.get(waterEntityPlusSpeciesIds)).map(SpeciesByWaterEntity::getPresent);
            boolean authorizedSample = authorizedSamplesSet.contains(waterEntityPlusSpeciesIds);
            Integer minSize = 0;
            Integer maxSize = 1000;
            if (authorizedSamplesMinSizes.get(waterEntityPlusSpeciesIds) != null) {
                minSize = authorizedSamplesMinSizes.get(waterEntityPlusSpeciesIds);
            }
            if (authorizedSamplesMaxSizes.get(waterEntityPlusSpeciesIds) != null) {
                maxSize = authorizedSamplesMaxSizes.get(waterEntityPlusSpeciesIds);
            }
            Integer meshSize = authorizedSamplesMeshSizes.get(waterEntityPlusSpeciesIds);
            SpeciesWithAlias speciesWithAlias = SpeciesWithAlias.of(rawSpecies, alias, present, authorizedSample, minSize, maxSize, meshSize);
            result.put(waterEntityId, speciesWithAlias);
        }));
        return result.asMap();
    }

    // Résout le périmètre du back-office « Maillages et tailles maximales »
    // (#154) : une liste explicite d'entités l'emporte sur le département ;
    // dans les deux cas, l'admin régional reste borné à son propre périmètre.
    private Set<UUID> resolvePerimeter(String department, List<UUID> waterEntityIdParams) {
        Set<UUID> perimeter;
        if (waterEntityIdParams != null && !waterEntityIdParams.isEmpty()) {
            perimeter = new HashSet<>(waterEntityIdParams);
        } else if (StringUtils.isNotBlank(department)) {
            perimeter = new HashSet<>(referentialDao.listWaterEntityIdsByDepartment(department));
        } else {
            return Set.of();
        }
        Set<UUID> allowedAdminWaterEntities = getAllowedAdminWaterEntities();
        if (!allowedAdminWaterEntities.isEmpty()) {
            perimeter.retainAll(allowedAdminWaterEntities);
        }
        return perimeter;
    }

    @GET
    @Path("/departments")
    public List<String> getDepartments() {
        return referentialDao.listDepartments();
    }

    @GET
    @Path("/waterEntities/by-department/{department}")
    public List<WaterEntitySummary> getWaterEntitiesByDepartment(@PathParam("department") String department) {
        List<WaterEntitySummary> summaries = referentialDao.listWaterEntitiesSummaryByDepartment(department);
        Set<UUID> allowedAdminWaterEntities = getAllowedAdminWaterEntities();
        if (allowedAdminWaterEntities.isEmpty()) {
            return summaries;
        }
        return summaries.stream()
                .filter(summary -> allowedAdminWaterEntities.contains(summary.id()))
                .toList();
    }

    @PUT
    @Path("/species-aliases-per-waterEntity")
    @Audited(value = "speciesAliases.save", entityType = "species_aliases_per_water_entity")
    public Response saveSpeciesAliasesPerWaterEntity(SpeciesAliasesPerWaterEntityBean salp) {
        checkIsAdmin();

        // On transforme les maps pour avoir en clé waterEntityId+speciesId et en valeur les alias
        Map<Pair<UUID, UUID>, String> aliasesMap = new HashMap<>();
        List<Pair<UUID, UUID>> absentList = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, String>> byWaterEntityEntry : salp.speciesPerWaterEntityAliases.entrySet()) {
            Map<UUID, String> bySpeciesEntries = byWaterEntityEntry.getValue();
            for (Map.Entry<UUID, String> entry : bySpeciesEntries.entrySet()) {
                Pair<UUID, UUID> waterEntityPluSpeciesId = Pair.of(byWaterEntityEntry.getKey(), entry.getKey());
                String alias = entry.getValue();
                if (StringUtils.isNotEmpty(alias)) {
                    aliasesMap.put(waterEntityPluSpeciesId, alias);
                }
            }
        }
        for (Map.Entry<UUID, List<UUID>> byWaterEntityEntry : salp.speciesPerWaterEntityAbsent.entrySet()) {
            for (UUID entry : byWaterEntityEntry.getValue()) {
                Pair<UUID, UUID> waterEntityPluSpeciesId = Pair.of(byWaterEntityEntry.getKey(), entry);
                absentList.add(waterEntityPluSpeciesId);
            }
        }


        // On charge les alias par lac+espèce
        List<SpeciesByWaterEntity> speciesByWaterEntity = referentialDao.listSpeciesByWaterEntity();

        // On commence par mettre à jour ou supprimer les espèces par lac existantes
        for (SpeciesByWaterEntity entity : speciesByWaterEntity) {
            if (salp.targetWaterEntities.contains(entity.getWaterEntityId())) {
                Pair<UUID, UUID> waterEntityPluSpeciesId = Pair.of(entity.getWaterEntityId(), entity.getSpeciesId());
                if (aliasesMap.containsKey(waterEntityPluSpeciesId)) {
                    String newAlias = salp.speciesPerWaterEntityAliases.get(entity.getWaterEntityId()).get(entity.getSpeciesId());
                    entity.setAlias(newAlias);
                    entity.setPresent(!absentList.contains(waterEntityPluSpeciesId));
                    referentialDao.updateSpeciesByWaterEntity(entity);
                } else {
                    if (!absentList.contains(waterEntityPluSpeciesId)) {
                        referentialDao.deleteSpeciesByWaterEntity(entity);
                    } else {
                        entity.setPresent(false);
                        referentialDao.updateSpeciesByWaterEntity(entity);
                    }
                }
                aliasesMap.remove(waterEntityPluSpeciesId);
                absentList.remove(waterEntityPluSpeciesId);
            }
        }

        // Puis on créé les nouvelles
        aliasesMap.entrySet()
                .stream()
                .filter(entry -> salp.targetWaterEntities.contains(entry.getKey().getKey()))
                .map(entry -> {
                    UUID waterEntityId = entry.getKey().getKey();
                    UUID speciesId = entry.getKey().getValue();
                    String alias = entry.getValue();
                    SpeciesByWaterEntity result = new SpeciesByWaterEntity(waterEntityId, speciesId, alias, true);
                    return result;
                })
                .forEach(spl -> {
                    referentialDao.createSpeciesByWaterEntity(spl);
                    Pair<UUID, UUID> waterEntityPluSpeciesId = Pair.of(spl.getWaterEntityId(), spl.getSpeciesId());
                    absentList.remove(waterEntityPluSpeciesId);
                });
        absentList.stream()
                .filter(entry -> salp.targetWaterEntities.contains(entry.getKey()))
                .map(entry -> {
                    UUID waterEntityId = entry.getKey();
                    UUID speciesId = entry.getValue();
                    SpeciesByWaterEntity result = new SpeciesByWaterEntity(waterEntityId, speciesId, "", false);
                    return result;
                })
                .forEach(spl -> {
                    referentialDao.createSpeciesByWaterEntity(spl);
                });

        Response response = Response.noContent().build();
        return response;
    }

    @PUT
    @Path("/authorized-samples")
    @Audited(value = "authorizedSamples.save", entityType = "authorized_samples")
    public Response saveAuthorizedSamples(AuthorizedSamplesModificationBean authorizedSamples) {
        FisholaAdmin fisholaAdmin = checkIsAdmin();
        Set<UUID> allowedAdminWaterEntities = getAllowedAdminWaterEntities();
        Set<UUID> waterEntityScope =  authorizedSamples.targetWaterEntities.stream()
            .filter(l -> fisholaAdmin.getIsNationalAdmin() || allowedAdminWaterEntities.contains(l))
            .collect(Collectors.toSet());
        
        // On transforme la map pour avoir un Set des clé waterEntityId+speciesId autorisées
        Set<Pair<UUID, UUID>> authorizationsSet = new HashSet<>();
        Map<Pair<UUID, UUID>, Integer> minSizesMap = new LinkedHashMap<>();
        Map<Pair<UUID, UUID>, Integer> maxSizesMap = new LinkedHashMap<>();
        Map<Pair<UUID, UUID>, Integer> meshSizesMap = new LinkedHashMap<>();
        computeMinMaxMaps(authorizedSamples, authorizationsSet, minSizesMap, maxSizesMap, meshSizesMap);

        // On charge les autorisations par lac+espèce et on en fait un index
        List<AuthorizedSample> existingAuthorizations = referentialDao.listAuthorizedSamples();

        // On commence par supprimer les autorisations en trop
        updateAndDeleteAuthorizations(existingAuthorizations, waterEntityScope, authorizationsSet, minSizesMap, maxSizesMap, meshSizesMap);

        // Puis on créé les nouvelles
        authorizationsSet
            .stream()
            .filter(entry -> waterEntityScope.contains(entry.getKey()))
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
                authorizedSample.setWaterEntityId(entry.getKey());
                authorizedSample.setSpeciesId(entry.getValue());
                authorizedSample.setMinSize(minSize);
                authorizedSample.setMaxSize(maxSize);
                authorizedSample.setMeshSize(nullIfZero(meshSizesMap.get(entry)));
                return authorizedSample;
            })
            .forEach(referentialDao::createAuthorizedSample);

        Response response = Response.noContent().build();
        return response;
    }

    private void updateAndDeleteAuthorizations(List<AuthorizedSample> existingAuthorizations, Set<UUID> waterEntityScope, Set<Pair<UUID, UUID>> authorizationsSet, Map<Pair<UUID, UUID>, Integer> minSizesMap, Map<Pair<UUID, UUID>, Integer> maxSizesMap, Map<Pair<UUID, UUID>, Integer> meshSizesMap) {
        for (AuthorizedSample entity : existingAuthorizations) {
            Pair<UUID, UUID> waterEntityPluSpeciesId = Pair.of(entity.getWaterEntityId(), entity.getSpeciesId());
            if (waterEntityScope.contains(entity.getWaterEntityId())) {
                if (!authorizationsSet.contains(waterEntityPluSpeciesId)) {
                    referentialDao.deleteAuthorizedSample(entity);
                } else {
                    // On met à jour uniquement la taille si l'autorisation existe dejà
                    Integer minSize = 0;
                    if (minSizesMap.get(waterEntityPluSpeciesId) != null) {
                        minSize = minSizesMap.get(waterEntityPluSpeciesId);
                    }
                    Integer maxSize = 0;
                    if (maxSizesMap.get(waterEntityPluSpeciesId) != null) {
                        maxSize = maxSizesMap.get(waterEntityPluSpeciesId);
                    }
                    entity.setMinSize(minSize);
                    entity.setMaxSize(maxSize);
                    entity.setMeshSize(nullIfZero(meshSizesMap.get(waterEntityPluSpeciesId)));
                    referentialDao.updateAuthorizeSample(entity);
                }
                authorizationsSet.remove(waterEntityPluSpeciesId);
            }
        }
    }

    // Le maillage est facultatif (#154) : 0 ou absent => pas de maillage défini,
    // stocké NULL en base.
    private static Integer nullIfZero(Integer value) {
        return value == null || value == 0 ? null : value;
    }

    private static void computeMinMaxMaps(AuthorizedSamplesModificationBean authorizedSamples, Set<Pair<UUID, UUID>> authorizationsSet, Map<Pair<UUID, UUID>, Integer> minSizesMap, Map<Pair<UUID, UUID>, Integer> maxSizesMap, Map<Pair<UUID, UUID>, Integer> meshSizesMap) {
        Map<UUID, Map<UUID, Object>> authorizations = authorizedSamples.authorizations;
        Map<UUID, Map<UUID, Object>> minSizes = authorizedSamples.minSizes;
        Map<UUID, Map<UUID, Object>> maxSizes = authorizedSamples.maxSizes;
        Map<UUID, Map<UUID, Object>> meshSizes = authorizedSamples.meshSizes;
        for (Map.Entry<UUID, Map<UUID, Object>> byWaterEntityEntry : authorizations.entrySet()) {
            Map<UUID, Object> bySpeciesEntries = byWaterEntityEntry.getValue();
            for (Map.Entry<UUID, Object> entry : bySpeciesEntries.entrySet()) {
                Pair<UUID, UUID> waterEntityPluSpeciesId = Pair.of(byWaterEntityEntry.getKey(), entry.getKey());
                Object authorized = entry.getValue();
                if (Boolean.TRUE.equals(authorized)) {
                    authorizationsSet.add(waterEntityPluSpeciesId);
                }
                putSize(minSizes, byWaterEntityEntry.getKey(), entry.getKey(), waterEntityPluSpeciesId, minSizesMap);
                putSize(maxSizes, byWaterEntityEntry.getKey(), entry.getKey(), waterEntityPluSpeciesId, maxSizesMap);
                putSize(meshSizes, byWaterEntityEntry.getKey(), entry.getKey(), waterEntityPluSpeciesId, meshSizesMap);
            }
        }
    }

    private static void putSize(Map<UUID, Map<UUID, Object>> source, UUID waterEntityId, UUID speciesId, Pair<UUID, UUID> key, Map<Pair<UUID, UUID>, Integer> target) {
        if (source == null || source.get(waterEntityId) == null || source.get(waterEntityId).get(speciesId) == null) {
            return;
        }
        String raw = source.get(waterEntityId).get(speciesId).toString().trim();
        if (raw.isEmpty()) {
            return;
        }
        // Le front envoie soit un nombre JSON, soit une chaîne (b-input type=number) :
        // on passe par Double pour tolérer « 30.0 » avant de tronquer en entier.
        target.put(key, (int) Math.round(Double.parseDouble(raw)));
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
    @Audited(value = "weather.update", entityType = "weather", entityIdParam = "weatherId")
    public Response updateWeather(@PathParam("weatherId") UUID weatherId, Weather weather) {
        checkIsNationalAdmin();
        Preconditions.checkArgument(weather != null, "Identifiant de météo obligatoire");
        Preconditions.checkArgument(weatherId.equals(weather.getId()), NO_MATCHING_ID);
        referentialDao.updateWeather(weather);
        return Response.noContent().build();
    }

    @POST
    @Path("/weathers")
    @Audited(value = "weather.create", entityType = "weather")
    public Response createWeather(Weather weather) {
        checkIsNationalAdmin();
        referentialDao.createWeather(weather);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/weathers/{weatherId}")
    @Audited(value = "weather.delete", entityType = "weather", entityIdParam = "weatherId")
    public Response deleteWeather(@PathParam("weatherId") UUID weatherId) {
        checkIsNationalAdmin();
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

    // #131 : équivalent de la taille min déjà utilisée par TripResource pour le
    // flag "maillée" (referentialDao.getMinSize), scopé à un seul plan d'eau.
    // Remplace le passage par l'ensemble de la map species-per-waterEntity
    // (tout le bassin RM&C) pour le seul contrôle de plausibilité côté saisie.
    @GET
    @Path("/authorized-samples/max-size")
    public Integer getMaxSize(@QueryParam("waterEntityId") UUID waterEntityId, @QueryParam("speciesId") UUID speciesId) {
        return referentialDao.getMaxSize(waterEntityId, speciesId);
    }

}
