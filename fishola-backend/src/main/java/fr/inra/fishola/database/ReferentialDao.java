package fr.inra.fishola.database;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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

import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

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

}
