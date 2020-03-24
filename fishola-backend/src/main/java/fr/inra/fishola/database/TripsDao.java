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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
public class TripsDao extends AbstractFisholaDao {

    @Inject
    protected CatchsDao catchsDao;

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

    public int setSpecies(UUID tripId, Set<UUID> speciesIds) {
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

    public int setTechniques(UUID tripId, Set<UUID> techniqueIds) {
        return withContext(context -> {
            context.deleteFrom(Tables.TRIP_TECHNIQUES)
                    .where(Tables.TRIP_TECHNIQUES.TRIP_ID.eq(tripId))
                    .execute();
            int count = 0;
            for (UUID techniqueId : techniqueIds) {
                count += context.insertInto(Tables.TRIP_TECHNIQUES, Tables.TRIP_TECHNIQUES.TRIP_ID, Tables.TRIP_TECHNIQUES.TECHNIQUE_ID)
                        .values(tripId, techniqueId)
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
            searchTerm.map(term -> String.format("%%%s%%", term))
                    .map(Tables.TRIP.NAME::likeIgnoreCase)
                    .ifPresent(conditions::add);
            SelectConditionStep<TripRecord> builder = context.selectFrom(Tables.TRIP)
                    .where(conditions);
            SelectSeekStep2<TripRecord, LocalDate, LocalDateTime> tripRecords =
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

    public int countMyTrips(UUID userId) {
        int result = withContext(context -> {
            List<Condition> conditions = new LinkedList<>();
            conditions.add(Tables.TRIP.OWNER_ID.eq(userId));
            conditions.add(Tables.TRIP.HIDDEN.eq(false));
            int count = context.fetchCount(Tables.TRIP, conditions);
            return count;
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

    public Set<UUID> getTripSpecies(UUID tripId) {
        Set<UUID> speciesIds = withContext(context -> context.selectFrom(Tables.TRIP_EXPECTED_SPECIES)
                .where(Tables.TRIP_EXPECTED_SPECIES.TRIP_ID.eq(tripId))
                .fetchSet(Tables.TRIP_EXPECTED_SPECIES.SPECIES_ID));
        return speciesIds;
    }

    public Set<UUID> getTripTechniques(UUID tripId) {
        Set<UUID> techniqueIds = withContext(context -> context.selectFrom(Tables.TRIP_TECHNIQUES)
                .where(Tables.TRIP_TECHNIQUES.TRIP_ID.eq(tripId))
                .fetchSet(Tables.TRIP_TECHNIQUES.TECHNIQUE_ID));
        return techniqueIds;
    }

    public void updateTrip(Trip existingTrip) {
        withDaoNoResult(TripDao.class, dao -> dao.update(existingTrip));
    }

    public void delete(UUID tripId) {
        catchsDao.deleteByTrip(tripId);
        withContextNoResult(context -> {
            context.deleteFrom(Tables.TRIP_EXPECTED_SPECIES).where(Tables.TRIP_EXPECTED_SPECIES.TRIP_ID.eq(tripId)).execute();
            context.deleteFrom(Tables.TRIP_TECHNIQUES).where(Tables.TRIP_TECHNIQUES.TRIP_ID.eq(tripId)).execute();
            context.deleteFrom(Tables.TRIP).where(Tables.TRIP.ID.eq(tripId)).execute();
        });
    }

    public void hide(UUID tripId) {
        withContext(context -> context.update(Tables.TRIP)
                .set(Tables.TRIP.HIDDEN, true)
                .where(Tables.TRIP.ID.eq(tripId))
                .execute());
    }
}
