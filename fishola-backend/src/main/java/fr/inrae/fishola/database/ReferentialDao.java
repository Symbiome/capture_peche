package fr.inrae.fishola.database;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
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
import fr.inrae.fishola.entities.tables.daos.WeatherDao;
import fr.inrae.fishola.entities.tables.pojos.AuthorizedSample;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.ReleasedFishState;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.entities.tables.records.SpeciesRecord;
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
