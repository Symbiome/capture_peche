package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.daos.AuthorizedSampleDao;
import fr.inrae.fishola.entities.tables.daos.LakeDao;
import fr.inrae.fishola.entities.tables.daos.ReleasedFishStateDao;
import fr.inrae.fishola.entities.tables.daos.SpeciesByLakeDao;
import fr.inrae.fishola.entities.tables.daos.SpeciesDao;
import fr.inrae.fishola.entities.tables.daos.TechniqueDao;
import fr.inrae.fishola.entities.tables.daos.TripExpectedSpeciesDao;
import fr.inrae.fishola.entities.tables.daos.WeatherDao;
import fr.inrae.fishola.entities.tables.pojos.AuthorizedSample;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.ReleasedFishState;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.TripExpectedSpecies;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.entities.tables.records.SpeciesRecord;
import java.util.LinkedHashSet;
import javax.ws.rs.core.Link;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Singleton;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ReferentialDao extends AbstractFisholaDao {

    private static final Log log = LogFactory.getLog(ReferentialDao.class);

    public List<Lake> listLakes() {
        List<Lake> result = withDao(LakeDao.class, LakeDao::findAll);
        return result;
    }

    public void updateLake(Lake lake) {
        withDaoNoResult(LakeDao.class, dao -> dao.update(lake));
    }

    public void createLake(Lake lake) {
        withDaoNoResult(LakeDao.class, dao -> {
            dao.insert(lake);
            UUID lakeId = lake.getId();
            // By default, add all species to the created lake
            List<Species> allSpecies = withDao(SpeciesDao.class, SpeciesDao::findAll);
            Set<SpeciesByLake> lakeToSpecies = allSpecies.stream()
                    .map(specie -> new SpeciesByLake(lakeId, specie.getId(), specie.getName()))
                    .collect(Collectors.toSet());
            withDaoNoResult(SpeciesByLakeDao.class, speciesByLakeDao -> speciesByLakeDao.insert(lakeToSpecies));
        });
    }

    public void createSpecie(Species species) {
        withDaoNoResult(SpeciesDao.class, dao -> dao.insert(species));
    }

    public boolean canDeleteSpecie(UUID specieId) {
        boolean hasReferences = withContext(context -> {
            // Has catch
            boolean result = context.select(Tables.CATCH.SPECIES_ID)
                    .from(Tables.CATCH)
                    .where(Tables.CATCH.SPECIES_ID.eq(specieId))
                    .fetch().isNotEmpty();
            // Has expected
            result = result || context.select(Tables.TRIP_EXPECTED_SPECIES.SPECIES_ID)
                    .from(Tables.TRIP_EXPECTED_SPECIES)
                    .where(Tables.TRIP_EXPECTED_SPECIES.SPECIES_ID.eq(specieId))
                    .fetch().isNotEmpty();
            return result;
        });
        return !hasReferences;
    }

    public void deleteSpecie(UUID specieId) {
        // Delete all links between this specie and lakes
        withContextNoResult(context -> {
            context.deleteFrom(Tables.SPECIES_BY_LAKE).where(Tables.SPECIES_BY_LAKE.SPECIES_ID.eq(specieId)).execute();
            context.deleteFrom(Tables.AUTHORIZED_SAMPLE).where(Tables.AUTHORIZED_SAMPLE.SPECIES_ID.eq(specieId)).execute();
            withDaoNoResult(SpeciesDao.class, dao -> dao.deleteById(specieId));
        });

    }



    public List<Weather> listWeathers() {
        List<Weather> result = withDao(WeatherDao.class, WeatherDao::findAll);
        return result;
    }

    public void updateWeather(Weather weather) {
        withDaoNoResult(WeatherDao.class, dao -> dao.update(weather));
    }
    public void createWeather(Weather weather) {
        withDaoNoResult(WeatherDao.class, dao -> dao.insert(weather));
    }

    public boolean canDeleteWeather(UUID weatherId) {
        boolean hasReferences = withContext(context -> {
            // Has trips
            boolean result = context.select(Tables.TRIP.WEATHER_ID)
                    .from(Tables.TRIP)
                    .where(Tables.TRIP.WEATHER_ID.eq(weatherId))
                    .fetch().isNotEmpty();
            return result;
        });
        return !hasReferences;
    }

    public void deleteWeather(UUID weatherId) {
        withDaoNoResult(WeatherDao.class, dao -> dao.deleteById(weatherId));
    }

    public List<Technique> listBuiltInTechniques() {
        List<Technique> result = withDao(TechniqueDao.class, dao -> dao.fetchByBuiltIn(true));
        return result;
    }

    public void updateTechnique(Technique technique) {
        withDaoNoResult(TechniqueDao.class, dao -> dao.update(technique));
    }
    public void createTechnique(Technique techniques) {
        withDaoNoResult(TechniqueDao.class, dao -> dao.insert(techniques));
    }

    public boolean canDeleteTechnique(UUID techniqueId) {
        boolean hasReferences = withContext(context -> {
            // Has catch
            boolean result = context.select(Tables.CATCH.TECHNIQUE_ID)
                    .from(Tables.CATCH)
                    .where(Tables.CATCH.TECHNIQUE_ID.eq(techniqueId))
                    .fetch().isNotEmpty();
            // Has trip
            result = result || context.select(Tables.TRIP_TECHNIQUES.TECHNIQUE_ID)
                    .from(Tables.TRIP_TECHNIQUES)
                    .where(Tables.TRIP_TECHNIQUES.TECHNIQUE_ID.eq(techniqueId))
                    .fetch().isNotEmpty();
            return result;
        });
        return !hasReferences;
    }

    public void deleteTechnique(UUID techniqueId) {
        withDaoNoResult(TechniqueDao.class, dao -> dao.deleteById(techniqueId));
    }

    public List<Species> listAllSpecies() {
        List<Species> result = withDao(SpeciesDao.class, SpeciesDao::findAll);
        return result;
    }

    public void updateSpecies(Species species) {
        withDaoNoResult(SpeciesDao.class, dao -> dao.update(species));
    }

    public List<Species> listBuiltInSpecies() {
        List<Species> result = withDao(SpeciesDao.class, dao -> dao.fetchByBuiltIn(true));
        return result;
    }

    public ImmutableMap<UUID, Species> speciesIndex() {
        List<Species> species = listBuiltInSpecies();
        ImmutableMap<UUID, Species> result = Maps.uniqueIndex(species, Species::getId);
        return result;
    }

    public List<Species> listCustomSpecies() {
        List<Species> result = withDao(SpeciesDao.class, dao -> dao.fetchByBuiltIn(false));
        return result;
    }

    public Map<UUID, Species> customSpeciesIndex(Set<UUID> filterSpeciesIds) {
        // XXX AThimel 06/03/2020 Si jamais on a besoin de plus de perfs, on pourra filtrer directement dans la requête
        List<Species> species = listCustomSpecies();
        ImmutableMap<UUID, Species> allCustomSpecies = Maps.uniqueIndex(species, Species::getId);
        Map<UUID, Species> result = Maps.filterKeys(allCustomSpecies, filterSpeciesIds::contains);
        return result;
    }

    public List<SpeciesByLake> listSpeciesByLake() {
        List<SpeciesByLake> result = withDao(SpeciesByLakeDao.class, SpeciesByLakeDao::findAll);
        return result;
    }

    public List<SpeciesByLake> listSpeciesWithAliases() {
        List<SpeciesByLake> result = listSpeciesByLake().stream()
                .filter(sbl -> StringUtils.isNotEmpty(sbl.getAlias()))
                .collect(ImmutableList.toImmutableList());
        return result;
    }
    public List<ReleasedFishState> listReleasedFishStates() {
        List<ReleasedFishState> result = withDao(ReleasedFishStateDao.class, ReleasedFishStateDao::findAll);
        return result;
    }

    public Set<UUID> checkSpeciesOrCreateIfNecessary(String speciesIds) {
        if (StringUtils.isEmpty(StringUtils.trimToNull(speciesIds))) {
            return ImmutableSet.of();
        }
        List<String> speciesToCreate = Splitter.on(",")
                        .omitEmptyStrings()
                        .trimResults()
                        .splitToList(speciesIds);
        Set<UUID> result = new HashSet<>();
        for (String speciesName : speciesToCreate) {
            UUID uuid = findOrCreateCustomSpecies(speciesName);
            result.add(uuid);
        }
        return result;
    }

    protected static String normalizeSpeciesName(String rawName) {

        List<String> dashParts = Splitter.on("-")
                .trimResults()
                .omitEmptyStrings()
                .splitToList(rawName);

        if (dashParts.isEmpty()) {
            return null;
        }

        if (dashParts.size() > 1) {
            List<String> list = dashParts.stream()
                    .map(ReferentialDao::normalizeSpeciesName)
                    .collect(Collectors.toList());
            String result = Joiner.on("-")
                    .skipNulls()
                    .join(list);
            return result;
        }

        String lastPart = dashParts.get(0);
        List<String> spaceParts = Splitter.on(" ")
                .trimResults()
                .omitEmptyStrings()
                .splitToList(lastPart);
        String result = Joiner.on(" ")
                .skipNulls()
                .join(spaceParts)
                .toLowerCase();
        result = result.substring(0, 1).toUpperCase() + result.substring(1);
        return result;
    }

    protected static String normalizeSpeciesExportAs(String rawName) {
        String normalizedSpeciesName = normalizeSpeciesName(rawName);
        String result = Normalizer.normalize(normalizedSpeciesName, Normalizer.Form.NFD);
        result = result.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        result = result.replaceAll("[^a-zA-Z- ]", "");
        result = result.trim().toLowerCase();
        return result;
    }

    protected UUID findOrCreateCustomSpecies(String name) {

        String exportAs = normalizeSpeciesExportAs(name);

        Species existingSpecies = withDao(SpeciesDao.class, dao -> dao.fetchOneByExportAs(exportAs));
        if (existingSpecies != null) {
            UUID existingSpeciesId = existingSpecies.getId();

            if (log.isDebugEnabled()) {
                log.debug("Espèce trouvée avec pour exportAs=" + exportAs + " => " + existingSpeciesId);
            }

            return existingSpeciesId;
        }

        String speciesName = normalizeSpeciesName(name);

        Species species = new Species();
        species.setBuiltIn(false);
        species.setName(speciesName);
        species.setExportAs(exportAs);
        UUID result = withContext(context -> {
            SpeciesRecord record = context.newRecord(Tables.SPECIES, species);
            SpeciesRecord recordInserted = context.insertInto(Tables.SPECIES)
                    .set(record)
                    .returning(Tables.SPECIES.ID)
                    .fetchOne();
            UUID id = recordInserted.getId();
            return id;
        });

        if (log.isDebugEnabled()) {
            log.debug("Espèce créée pour exportAs=" + exportAs + " => " + result);
        }

        return result;
    }

    public List<AuthorizedSample> listAuthorizedSamples() {
        List<AuthorizedSample> result = withDao(AuthorizedSampleDao.class, AuthorizedSampleDao::findAll);
        return result;
    }

}
