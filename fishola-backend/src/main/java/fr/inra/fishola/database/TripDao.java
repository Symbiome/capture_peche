package fr.inra.fishola.database;

import fr.inra.fishola.entities.tables.pojos.Trip;

import javax.inject.Singleton;

@Singleton
public class TripDao extends AbstractFisholaDao {

    public void create(Trip trip) {
        withDaoNoResult(fr.inra.fishola.entities.tables.daos.TripDao.class, tripDao -> {
            tripDao.insert(trip);
        });
    }

}
