package fr.inra.fishola.database;

import fr.inra.fishola.entities.tables.daos.LakeDao;
import fr.inra.fishola.entities.tables.daos.MethodDao;
import fr.inra.fishola.entities.tables.daos.SpeciesDao;
import fr.inra.fishola.entities.tables.daos.WeatherDao;
import fr.inra.fishola.entities.tables.pojos.Lake;
import fr.inra.fishola.entities.tables.pojos.Method;
import fr.inra.fishola.entities.tables.pojos.Species;
import fr.inra.fishola.entities.tables.pojos.Weather;
import org.jooq.impl.DAOImpl;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class ReferentialDao extends AbstractFisholaDao {

    public List<Lake> listLakes() {
        List<Lake> result = withDao(LakeDao.class, DAOImpl::findAll);
        return result;
    }

    public List<Weather> listWeathers() {
        List<Weather> result = withDao(WeatherDao.class, DAOImpl::findAll);
        return result;
    }

    public List<Method> listBuiltInMethods() {
        List<Method> result = withDao(MethodDao.class, dao -> dao.fetchByBuiltIn(true));
        return result;
    }

    public List<Species> listBuiltInSpecies() {
        List<Species> result = withDao(SpeciesDao.class, dao -> dao.fetchByBuiltIn(true));
        return result;
    }

}
