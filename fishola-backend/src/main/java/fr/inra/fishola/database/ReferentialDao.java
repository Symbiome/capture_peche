package fr.inra.fishola.database;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.inra.fishola.entities.Tables;
import fr.inra.fishola.entities.tables.daos.LakeDao;
import fr.inra.fishola.entities.tables.daos.ReleasedFishStateDao;
import fr.inra.fishola.entities.tables.daos.SpeciesDao;
import fr.inra.fishola.entities.tables.daos.TechniqueDao;
import fr.inra.fishola.entities.tables.daos.WeatherDao;
import fr.inra.fishola.entities.tables.pojos.Lake;
import fr.inra.fishola.entities.tables.pojos.ReleasedFishState;
import fr.inra.fishola.entities.tables.pojos.Species;
import fr.inra.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inra.fishola.entities.tables.pojos.Technique;
import fr.inra.fishola.entities.tables.pojos.Weather;
import fr.inra.fishola.entities.tables.records.SpeciesRecord;

import javax.inject.Singleton;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ReferentialDao extends AbstractFisholaDao {

    public List<Lake> listLakes() {
        List<Lake> result = withDao(LakeDao.class, LakeDao::findAll);
        return result;
    }

    public List<Weather> listWeathers() {
        List<Weather> result = withDao(WeatherDao.class, WeatherDao::findAll);
        return result;
    }

    public List<Technique> listBuiltInTechniques() {
        List<Technique> result = withDao(TechniqueDao.class, dao -> dao.fetchByBuiltIn(true));
        return result;
    }

    public List<Species> listBuiltInSpecies() {
        List<Species> result = withDao(SpeciesDao.class, dao -> dao.fetchByBuiltIn(true));
        return result;
    }

    public List<Species> listAllSpecies() {
        List<Species> result = withDao(SpeciesDao.class, SpeciesDao::findAll);
        return result;
    }

    public ImmutableMap<UUID, Species> speciesIndex() {
        List<Species> species = listBuiltInSpecies();
        ImmutableMap<UUID, Species> result = Maps.uniqueIndex(species, Species::getId);
        return result;
    }

    public List<SpeciesByLake> listSpeciesByLake() {
        List<SpeciesByLake> result = withContext(context ->
                context.selectFrom(Tables.SPECIES_BY_LAKE)
                    .fetch()
                    .into(SpeciesByLake.class)
        );
        return result;
    }

    public List<ReleasedFishState> listReleasedFishStates() {
        List<ReleasedFishState> result = withDao(ReleasedFishStateDao.class, ReleasedFishStateDao::findAll);
        return result;
    }

    public Set<UUID> checkSpeciesOrCreateIfNecessary(Set<String> speciesIds) {
        Set<String> existingSpeciesIds = listAllSpecies()
                .stream()
                .map(Species::getId)
                .map(UUID::toString)
                .collect(Collectors.toSet());
        Set<UUID> result = new HashSet<>();
        Sets.intersection(speciesIds, existingSpeciesIds)
                .stream()
                .map(UUID::fromString)
                .forEach(result::add);
        Set<String> speciesToCreate = Sets.difference(speciesIds, existingSpeciesIds)
                .stream()
                .flatMap(newSpecies -> Splitter.on(",")
                        .omitEmptyStrings()
                        .trimResults()
                        .splitToStream(newSpecies))
                .collect(Collectors.toSet());
        for (String speciesName : speciesToCreate) {
            UUID uuid = createCustomSpecies(speciesName);
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

    protected UUID createCustomSpecies(String name) {

        String exportAs = normalizeSpeciesExportAs(name);

        Species existingSpecies = withDao(SpeciesDao.class, dao -> dao.fetchOneByExportAs(exportAs));
        if (existingSpecies != null) {
            return existingSpecies.getId();
        }

        String speciesName = normalizeSpeciesName(name);

        Species species = new Species();
        species.setBuiltIn(false);
        species.setName(speciesName);
        species.setExportAs(exportAs);
        return withContext(context -> {
            SpeciesRecord record = context.newRecord(Tables.SPECIES, species);
            SpeciesRecord recordInserted = context.insertInto(Tables.SPECIES)
                    .set(record)
                    .returning(Tables.SPECIES.ID)
                    .fetchOne();
            UUID id = recordInserted.getId();
            return id;
        });
    }

}
