package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import com.google.common.collect.ListMultimap;
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.tables.daos.CatchDao;
import fr.inrae.fishola.entities.tables.daos.FisholaUserDao;
import fr.inrae.fishola.entities.tables.daos.LakeDao;
import fr.inrae.fishola.entities.tables.daos.SpeciesDao;
import fr.inrae.fishola.entities.tables.daos.TripDao;
import fr.inrae.fishola.entities.tables.daos.TripExpectedSpeciesDao;
import fr.inrae.fishola.entities.tables.daos.TripSocialReactionDao;
import fr.inrae.fishola.entities.tables.daos.TripTechniquesDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.entities.tables.pojos.TripExpectedSpecies;
import fr.inrae.fishola.entities.tables.pojos.TripSocialReaction;
import fr.inrae.fishola.entities.tables.pojos.TripTechniques;
import fr.inrae.fishola.entities.tables.records.TripRecord;
import fr.inrae.fishola.rest.social.ImmutableTripSocial;
import fr.inrae.fishola.rest.social.TripSocial;
import fr.inrae.fishola.rest.trips.ExportBean;
import fr.inrae.fishola.rest.trips.PaginatedExportBean;
import fr.inrae.fishola.rest.trips.PicturePerTripBean;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep2;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.nuiton.util.pagination.PaginationOrder;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class TripsDao extends AbstractFisholaDao {

    public static final String CATCHS_OPENADOM_EXPORT_VIEW = "catchs_openadom_export";
    @Inject
    protected CatchsDao catchsDao;

    public UUID create(Trip trip) {
        return withContext(context -> {
            TripRecord newRecord = context.newRecord(Tables.TRIP, trip);
            TripRecord recordInserted = context.insertInto(Tables.TRIP)
                    .set(newRecord)
                    .returning(Tables.TRIP.ID)
                    .fetchOne();
            UUID id = recordInserted.getId();
            return id;
        });
    }

    public int setSpecies(UUID tripId, Set<UUID> speciesIds) {
        deleteTripSpecies(tripId);
        Set<TripExpectedSpecies> expectedSpecies = speciesIds.stream()
                .map(speciesId -> new TripExpectedSpecies(tripId, speciesId))
                .collect(Collectors.toSet());
        withDaoNoResult(TripExpectedSpeciesDao.class, dao -> dao.insert(expectedSpecies));
        return expectedSpecies.size();
    }

    public int setTechniques(UUID tripId, Set<UUID> techniqueIds) {
        deleteTripTechniques(tripId);
        Set<TripTechniques> techniques = techniqueIds.stream()
                .map(techniqueId -> new TripTechniques(tripId, techniqueId))
                .collect(Collectors.toSet());
        withDaoNoResult(TripTechniquesDao.class, dao -> dao.insert(techniques));
        return techniques.size();
    }

    public List<Trip> listMyTrips(UUID userId, boolean orderDesc, Optional<String> searchTerm, Optional<Integer> yearFilter, Optional<List<UUID>> lakesFilter) {
        List<Trip> result = withContext(context -> {
            List<Condition> conditions = new LinkedList<>();
            conditions.add(Tables.TRIP.OWNER_ID.eq(userId));
            conditions.add(Tables.TRIP.HIDDEN.eq(false));
            searchTerm.map(term -> String.format("%%%s%%", term))
                    .map(Tables.TRIP.NAME::likeIgnoreCase)
                    .ifPresent(conditions::add);
            yearFilter.ifPresent(year -> {
                LocalDate min = LocalDate.of(year, Month.JANUARY, 1);
                LocalDate max = LocalDate.of(year, Month.DECEMBER, 31);
                conditions.add(Tables.TRIP.DAY.between(min, max));
            });
            lakesFilter.ifPresent(lakesIds -> conditions.add(Tables.TRIP.LAKE_ID.in(lakesFilter.get())));
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

    public PaginationResult<Trip> listMyTrips(UUID userId, PaginationParameter page, Optional<String> searchTerm, Optional<Integer> year, Optional<List<UUID>> lakesFilter) {
        // TODO AThimel 13/01/2020 La page doit être gérée au niveau de la requête
        boolean orderDesc = true;
        if (!page.getOrderClauses().isEmpty()) {
            PaginationOrder order = page.getOrderClauses().get(0);
            orderDesc = order.isDesc();
        }
        List<Trip> entities = listMyTrips(userId, orderDesc, searchTerm, year,lakesFilter);
        PaginationResult<Trip> result = PaginationResult.fromFullList(entities, page);
        return result;
    }

    public PaginationResult<Trip> listMyTrips(UUID userId, PaginationParameter page, Optional<String> searchTerm) {
        PaginationResult<Trip> result = listMyTrips(userId, page, searchTerm, Optional.empty(), Optional.empty());
        return result;
    }

    public Trip getTrip(UUID tripId) {
        Trip trip = withDao(TripDao.class, dao -> dao.fetchOneById(tripId));
        return trip;
    }

    public Set<UUID> getTripSpecies(UUID tripId) {
        // Pour plus de performances, on ne charge pas l'objet complet mais on fait une projection
        Set<UUID> speciesIds = withContext(context -> context.selectFrom(Tables.TRIP_EXPECTED_SPECIES)
                .where(Tables.TRIP_EXPECTED_SPECIES.TRIP_ID.eq(tripId))
                .fetchSet(Tables.TRIP_EXPECTED_SPECIES.SPECIES_ID));
        return speciesIds;
    }

    public Set<UUID> getTripTechniques(UUID tripId) {
        // Pour plus de performances, on ne charge pas l'objet complet mais on fait une projection
        Set<UUID> techniqueIds = withContext(context -> context.selectFrom(Tables.TRIP_TECHNIQUES)
                .where(Tables.TRIP_TECHNIQUES.TRIP_ID.eq(tripId))
                .fetchSet(Tables.TRIP_TECHNIQUES.TECHNIQUE_ID));
        return techniqueIds;
    }

    public void updateTrip(Trip existingTrip) {
        withDaoNoResult(TripDao.class, dao -> dao.update(existingTrip));
    }

    protected void deleteTripSpecies(UUID tripId) {
        withContextNoResult(context -> context.deleteFrom(Tables.TRIP_EXPECTED_SPECIES).where(Tables.TRIP_EXPECTED_SPECIES.TRIP_ID.eq(tripId)).execute());
    }

    protected void deleteTripTechniques(UUID tripId) {
        withContextNoResult(context -> context.deleteFrom(Tables.TRIP_TECHNIQUES).where(Tables.TRIP_TECHNIQUES.TRIP_ID.eq(tripId)).execute());
    }

    public void delete(UUID tripId) {
        catchsDao.deleteByTrip(tripId);
        deleteTripSpecies(tripId);
        deleteTripTechniques(tripId);
        withDaoNoResult(TripDao.class, dao -> dao.deleteById(tripId));
    }

    public void hide(UUID tripId) {
        withContext(context -> context.update(Tables.TRIP)
                .set(Tables.TRIP.HIDDEN, true)
                .where(Tables.TRIP.ID.eq(tripId))
                .execute());
    }

    public void unsetOwner(UUID userId) {
        withContext(context -> context.update(Tables.TRIP)
                .setNull(Tables.TRIP.OWNER_ID)
                .where(Tables.TRIP.OWNER_ID.eq(userId))
                .execute());
    }

    public String getTripsCSV() {
        return withContext(context -> {
            String result = context.selectFrom(CATCHS_OPENADOM_EXPORT_VIEW)
                    .where("a_exclure='non'")
                    .fetch()
                    .formatCSV(';');
            return result;
        });
    }

    /**
     * Paginated view of catchs_openadom_export.
     */
    public PaginatedExportBean getExportPaginated(Integer offset, String orderBy, String direction, MultivaluedMap<String, String> filters) {
        int catchesPerPage = 15;
        return withContext(context -> {
            PaginatedExportBean pcb = new PaginatedExportBean();
            // Compute sorting
            SortField<Object> orderByField;
            if ("asc".equals(direction)) {
                orderByField = DSL.field(orderBy).asc();
            } else {
                orderByField =  DSL.field(orderBy).desc();
            }

            // Compute filters
            List<Condition> conditions = new ArrayList<>();
            for (Map.Entry<String, List<String>> filter: filters.entrySet()) {
                String condition =filter.getKey() + "::varchar(255) LIKE '%" + filter.getValue().get(0) + "%'";
                conditions.add(DSL.condition(condition));
            }

            // Execute paginated query
            pcb.elements = context.selectFrom(CATCHS_OPENADOM_EXPORT_VIEW)
                    .where(conditions)
                    .orderBy(orderByField)
                    .limit(catchesPerPage)
                    .offset(offset * catchesPerPage)
                    .fetchInto(ExportBean.class);
            pcb.offset = offset;
            pcb.total =  context.selectFrom(CATCHS_OPENADOM_EXPORT_VIEW).where(conditions).stream().count();
            return pcb;
        });
    }


    public String getPersonalTripsCSV(UUID userId) {
        return withContext(context -> {
            String result = context.selectFrom("personal_catchs_export")
                    .where("id_login = ?", userId)
                    .fetch()
                    .formatCSV(';');
            return result;
        });
    }

    public int countTrips() {
        int result = withContext(context -> {
            // On compte les sorties des utilisateurs non exclus
            SelectConditionStep<Record1<UUID>> selectNonExcludedUsers = context.select(Tables.TRIP.ID)
                    .from(Tables.TRIP)
                    .innerJoin(Tables.FISHOLA_USER).on(Tables.FISHOLA_USER.ID.eq(Tables.TRIP.OWNER_ID))
                    .where(Tables.FISHOLA_USER.EXCLUDE_FROM_EXPORTS.eq(false));
            int countFromNonExcludedUsers = context.fetchCount(selectNonExcludedUsers);

            // On compte aussi les sorties dont l'utilisateur a été supprimé
            SelectConditionStep<Record1<UUID>> selectNoUser = context.select(Tables.TRIP.ID)
                    .from(Tables.TRIP)
                    .where(Tables.TRIP.OWNER_ID.isNull());
            int countNoUser = context.fetchCount(selectNoUser);

            return countFromNonExcludedUsers + countNoUser;
        });
        return result;
    }

    public List<Trip> findAll() {
        List<Trip> result = withDao(TripDao.class, TripDao::findAll);
        return result;
    }

    public List<PicturePerTripBean> getPicturesPerTripForYearAndLakes(UUID userId, Integer year, Optional<List<UUID>> lakesFilter) {
        List<PicturePerTripBean> picturesPerTripForYear = new ArrayList<>();
        List<Trip> tripsForYear = this.listMyTrips(userId, true, Optional.empty(), Optional.of(year), lakesFilter);
        for(Trip trip : tripsForYear) {
            Set<UUID> catchIds = catchsDao.listCatchIds(trip.getId());
            PicturePerTripBean picturesForTrip = new PicturePerTripBean();
            picturesForTrip.pictureURLs = new ArrayList<>();
            picturesForTrip.tripDate = trip.getDay();
            picturesForTrip.tripId = trip.getId();
            picturesForTrip.tripName = trip.getName();
            withDaoNoResult(LakeDao.class, lakeDao -> picturesForTrip.tripLakeName = lakeDao.findById(trip.getLakeId()).getName());
            ListMultimap<UUID, Integer> catchsWithPictures = catchsDao.getPictureIndexes(catchIds);
            for (Map.Entry<UUID, Integer> catchWithPicture : catchsWithPictures.entries()) {
                picturesForTrip.pictureURLs.add("/v1/pictures/" + catchWithPicture.getKey() + "/preview/" + catchWithPicture.getValue());
            }
            Set<UUID> measurementPictures = catchsDao.getMeasurementPictures(catchIds);
            for (UUID measurementPictureCatchId: measurementPictures) {
                picturesForTrip.pictureURLs.add("/v1/pictures/measure/"+ measurementPictureCatchId+"/preview");
            }

            if (!picturesForTrip.pictureURLs.isEmpty()) {
                picturesPerTripForYear.add(picturesForTrip);
            }
        }
        return picturesPerTripForYear;
    }

    public PaginationResult<TripSocial> socialTrips(UUID userId, Optional<List<UUID>> lakesFilter, PaginationParameter page) {
        return withContext(context -> {
                List<Condition> conditions = new LinkedList<>();
                conditions.add(Tables.TRIP.HIDDEN.eq(false));
                conditions.add(Tables.FISHOLA_USER.EXCLUDE_FROM_EXPORTS.eq(false));
                conditions.add(Tables.FISHOLA_USER.ACCEPTS_SHARE_TRIPS.eq(true));
                lakesFilter.ifPresent(lakesIds -> conditions.add(Tables.TRIP.LAKE_ID.in(lakesIds)));
                SelectConditionStep<Record> builder = context.select(Tables.TRIP.asterisk(),
                                Tables.FISHOLA_USER.ID.as("ownerId"),
                                Tables.FISHOLA_USER.EXCLUDE_FROM_EXPORTS,
                                Tables.FISHOLA_USER.ACCEPTS_SHARE_TRIPS
                        )
                        .from(Tables.TRIP)
                        .join(Tables.FISHOLA_USER)
                        .on(Tables.TRIP.OWNER_ID.eq(Tables.FISHOLA_USER.ID))
                        .where(conditions);
                SelectSeekStep2<Record, LocalDate, LocalDateTime> tripRecords =
                        builder.orderBy(Tables.TRIP.DAY.desc(), Tables.TRIP.CREATED_ON.desc());
                List<Trip> tripsWithoutSocial = tripRecords
                        .limit(page.getPageSize()).offset(page.getPageNumber())
                        .fetch()
                        .into(Trip.class);
                int totalCount = context.fetchCount(context.select(Tables.TRIP.ID).from(Tables.TRIP)
                        .join(Tables.FISHOLA_USER)
                        .on(Tables.TRIP.OWNER_ID.eq(Tables.FISHOLA_USER.ID)).where(conditions));

                List<TripSocial> tripsWithSocial = tripsWithoutSocial.stream().map( t -> {
                    FisholaUser user = withDao(FisholaUserDao.class, fisholaUserDao -> fisholaUserDao.findById(t.getOwnerId()));
                    String userName = user.getPseudo();
                    String lakeName = withDao(LakeDao.class, lakeDao -> lakeDao.fetchById(t.getLakeId()).getFirst().getName());
                    long durationInSeconds = Duration.between(t.getStartTime(), t.getEndTime()).toSeconds();
                    List<TripSocialReaction> socialReactions = withDao(TripSocialReactionDao.class, dao -> dao.fetchByTripId(t.getId()));
                    Map<String, ? extends Map<Maillage, Integer>> catchesCountPerMaillage = withDao(CatchDao.class, dao ->
                        dao.fetchByTripId(t.getId()).stream()
                        .collect(Collectors.groupingBy(
                            // Map key : specie Name
                            c -> withDao(SpeciesDao.class, speciesDao -> speciesDao.fetchOneById(c.getSpeciesId()).getName()),
                            LinkedHashMap::new,
                            Collectors.groupingBy(
                                    // Inner map key : Maillage
                                    Catch::getMaillee,
                                    LinkedHashMap::new,
                                    // Map value : count
                                    Collectors.summingInt(c -> 1)
                            )
                        ))
                    );

                    TripSocial socialTrip = ImmutableTripSocial.builder()
                           .id(t.getId())
                           .userName(userName)
                           .tripName(t.getName())
                           .lakeName(lakeName)
                           .durationInSeconds(durationInSeconds)
                           .date(t.getDay())
                           .socialReactions(socialReactions)
                           .catchesCountPerMaillage(catchesCountPerMaillage)
                           .build();
                    return socialTrip;
                }).toList();
            return PaginationResult.of(tripsWithSocial, totalCount, page);
        });
    }

    public void insertSocialReaction(TripSocialReaction reaction) {
        withDaoNoResult(TripSocialReactionDao.class, dao -> {
            if (dao.exists(reaction)) {
                dao.update(reaction);
            } else {
                dao.insert(reaction);
            }
        });
    }

    public void deleteSocialReaction(UUID userId, UUID tripId) {
        withContextNoResult(context -> {
                context.deleteFrom(Tables.TRIP_SOCIAL_REACTION).where(
                    Tables.TRIP_SOCIAL_REACTION.USER_ID.eq(userId)
                    .and(Tables.TRIP_SOCIAL_REACTION.TRIP_ID.eq(tripId))
                ).execute();
        });
    }
}
