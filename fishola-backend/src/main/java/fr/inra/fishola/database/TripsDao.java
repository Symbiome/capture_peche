package fr.inra.fishola.database;

import fr.inra.fishola.entities.tables.daos.TripDao;
import fr.inra.fishola.entities.tables.pojos.Trip;

import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

@Singleton
public class TripsDao extends AbstractFisholaDao {

    public void create(Trip trip) {
        withDaoNoResult(fr.inra.fishola.entities.tables.daos.TripDao.class, tripDao -> tripDao.insert(trip));
    }

    public List<Trip> listMyTrips(UUID userId) {
        List<Trip> result = withDao(TripDao.class, tripDao -> tripDao.fetchByOwner(userId));
        return result;
    }
}
