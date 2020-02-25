package fr.inra.fishola.database;

import fr.inra.fishola.entities.Tables;
import fr.inra.fishola.entities.tables.daos.TripDao;
import fr.inra.fishola.entities.tables.pojos.Trip;
import fr.inra.fishola.entities.tables.records.TripRecord;
import org.jooq.Condition;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep2;
import org.nuiton.util.pagination.PaginationOrder;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Singleton;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class TripsDao extends AbstractFisholaDao {

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
                    .where(Tables.TRIP_EXPECTED_SPECIES.TRIP_ID.eq(tripId))
                    .execute();
            int count = 0;
            for (UUID speciesId : speciesIds) {
                count += context.insertInto(Tables.TRIP_EXPECTED_SPECIES, Tables.TRIP_EXPECTED_SPECIES.TRIP_ID, Tables.TRIP_EXPECTED_SPECIES.SPECIES_ID)
                        .values(tripId, speciesId)
                        .execute();
            }
            return count;
        });
    }

    public List<Trip> listMyTrips(UUID userId, boolean orderDesc, Optional<String> searchTerm) {
        List<Trip> result = withContext(context -> {
            List<Condition> conditions = new LinkedList<>();
            conditions.add(Tables.TRIP.OWNER_ID.eq(userId));
            conditions.add(Tables.TRIP.HIDDEN.eq(false));
            searchTerm.ifPresent(term -> conditions.add(Tables.TRIP.NAME.likeIgnoreCase(term)));
            SelectConditionStep<TripRecord> builder = context.selectFrom(Tables.TRIP)
                    .where(conditions);
            SelectSeekStep2<TripRecord, Date, Timestamp> tripRecords =
                    orderDesc
                            ? builder.orderBy(Tables.TRIP.DAY.desc(), Tables.TRIP.CREATED_ON.desc())
                            : builder.orderBy(Tables.TRIP.DAY.asc(), Tables.TRIP.CREATED_ON.asc());
            List<Trip> trips = tripRecords
                    .fetch()
                    .into(Trip.class);
            return trips;
        });
        return result;
    }

    public PaginationResult<Trip> listMyTrips(UUID userId, PaginationParameter page, Optional<String> searchTerm) {
        // TODO AThimel 13/01/2020 La page doit être gérée au niveau de la requête
        boolean orderDesc = true;
        if (!page.getOrderClauses().isEmpty()) {
            PaginationOrder order = page.getOrderClauses().get(0);
            orderDesc = order.isDesc();
        }
        List<Trip> entities = listMyTrips(userId, orderDesc, searchTerm);
        PaginationResult<Trip> result = PaginationResult.fromFullList(entities, page);
        return result;
    }

    public Trip getTrip(UUID tripId) {
        Trip trip = withDao(TripDao.class, dao -> dao.fetchOneById(tripId));
        return trip;
    }

    public List<UUID> getTripSpecies(UUID tripId) {
        List<UUID> speciesIds = withContext(context -> context.selectFrom(Tables.TRIP_EXPECTED_SPECIES)
                .where(Tables.TRIP_EXPECTED_SPECIES.TRIP_ID.eq(tripId))
                .fetch(Tables.TRIP_EXPECTED_SPECIES.SPECIES_ID));
        return speciesIds;
    }

    public void updateTrip(Trip existingTrip) {
        withDaoNoResult(TripDao.class, dao -> dao.update(existingTrip));
    }

    public void delete(UUID tripId) {
        withContextNoResult(context -> {
            context.deleteFrom(Tables.CATCH).where(Tables.CATCH.TRIP_ID.eq(tripId)).execute();
            context.deleteFrom(Tables.TRIP_EXPECTED_SPECIES).where(Tables.TRIP_EXPECTED_SPECIES.TRIP_ID.eq(tripId)).execute();
            context.deleteFrom(Tables.TRIP_TECHNIQUES).where(Tables.TRIP_TECHNIQUES.TRIP_ID.eq(tripId)).execute();
            context.deleteFrom(Tables.TRIP).where(Tables.TRIP.ID.eq(tripId)).execute();
        });
    }

    public void hide(UUID tripId) {
        withContext(context -> context.update(Tables.TRIP)
                .set(Tables.TRIP.HIDDEN, true)
                .where(Tables.TRIP.ID.eq(tripId)));
    }
}
