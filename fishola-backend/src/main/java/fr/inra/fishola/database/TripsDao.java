package fr.inra.fishola.database;

import fr.inra.fishola.entities.Tables;
import fr.inra.fishola.entities.tables.daos.TripDao;
import fr.inra.fishola.entities.tables.pojos.Trip;
import fr.inra.fishola.entities.tables.records.TripRecord;

import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

@Singleton
public class TripsDao extends AbstractFisholaDao {

//    public void create(Trip trip) {
//        withDaoNoResult(TripDao.class, tripDao -> tripDao.insert(trip));
//    }

    public UUID create(Trip trip) {
        return withContext(context -> {
            TripRecord record = context.newRecord(Tables.TRIP, trip);
            TripRecord recordInserted = context.insertInto(Tables.TRIP)
                    .set(record)
                    .returning(Tables.TRIP.ID)
                    .fetchOne();
            UUID id = recordInserted.getId();
            return id;
        });
    }

    public int setSpecies(UUID tripId, List<UUID> speciesIds) {
        return withContext(context -> {
            context.deleteFrom(Tables.TRIP_EXPECTED_SPECIES)
                    .where(Tables.TRIP_EXPECTED_SPECIES.TRIP.eq(tripId))
                    .execute();
            int count = 0;
            for (UUID speciesId : speciesIds) {
                count += context.insertInto(Tables.TRIP_EXPECTED_SPECIES, Tables.TRIP_EXPECTED_SPECIES.TRIP, Tables.TRIP_EXPECTED_SPECIES.SPECIES)
                        .values(tripId, speciesId)
                        .execute();
            }
            return count;
        });
    }

    public List<Trip> listMyTrips(UUID userId) {
        List<Trip> result = withContext(context -> {
            List<Trip> trips = context.selectFrom(Tables.TRIP)
                    .where(Tables.TRIP.OWNER.eq(userId))
                    .orderBy(Tables.TRIP.DAY.desc(), Tables.TRIP.CREATED_ON.desc())
                    .fetch()
                    .into(Trip.class);
            return trips;
        });
        return result;
    }

    public Trip getTrip(UUID tripId) {
        return withDao(TripDao.class, dao -> dao.fetchOneById(tripId));
    }

}
