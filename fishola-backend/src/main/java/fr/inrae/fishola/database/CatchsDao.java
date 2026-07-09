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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.tables.WaterEntity;
import fr.inrae.fishola.entities.tables.daos.CatchDao;
import fr.inrae.fishola.entities.tables.daos.CatchMeasurementPictureDao;
import fr.inrae.fishola.entities.tables.daos.CatchPictureDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.CatchMeasurementPicture;
import fr.inrae.fishola.entities.tables.pojos.CatchPicture;
import fr.inrae.fishola.entities.tables.records.CatchRecord;
import fr.inrae.fishola.rest.trips.CatchMarker;
import fr.inrae.fishola.rest.trips.ImmutableCatchMarker;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.or;
import static org.jooq.impl.DSL.trueCondition;

@Singleton
public class CatchsDao extends AbstractFisholaDao {

    public UUID create(Catch c) {
        if (c.getEditedSpeciesId() == null) {
            c.setEditedSpeciesId(c.getSpeciesId());
        }
        if (c.getEditedSize() == null || c.getEditedSize() == 0) {
            if (c.getSize() != null) {
                c.setEditedSize(c.getSize() * 10);
            } else {
                c.setEditedSize(0);
            }
        }
        if (c.getEditedWeight() == null || c.getEditedWeight() == 0) {
            c.setEditedWeight(c.getWeight());
        }
        if (c.getExcludeFromExports() == null) {
            c.setExcludeFromExports(false);
        }
        return withContext(context -> {
            CatchRecord record = context.newRecord(Tables.CATCH, c);
            // latitude/longitude are GENERATED ALWAYS AS ... STORED (derived from
            // position); newRecord() marks every field as changed regardless of
            // whether the POJO getter is null, so they must be excluded explicitly
            // or Postgres rejects the insert outright.
            record.changed(Tables.CATCH.LATITUDE, false);
            record.changed(Tables.CATCH.LONGITUDE, false);
            CatchRecord recordInserted = context.insertInto(Tables.CATCH)
                    .set(record)
                    .returning(Tables.CATCH.ID)
                    .fetchOne();
            UUID insertedId = recordInserted.getId();
            return insertedId;
        });
    }

    public int countCatchs(UUID tripId) {
        int result = withContext(context -> context.fetchCount(Tables.CATCH, Tables.CATCH.TRIP_ID.eq(tripId)));
        return result;
    }

    public List<Catch> listCatchs(UUID tripId) {
        List<Catch> result = withDao(CatchDao.class, dao -> dao.fetchByTripId(tripId));
        return result;
    }

    public List<CatchMarker> catchMarkersForUser(UUID userId) {
        List<CatchMarker> result = withContext(context -> context
                .select(Tables.CATCH.ID,
                        Tables.SPECIES.NAME.as("specieName"),
                        Tables.TRIP.ID.as("tripId"),
                        Tables.TRIP.NAME.as("tripName"),
                        Tables.TRIP.CREATED_ON.as("date"),
                        Tables.WATER_ENTITY.NAME.as("waterEntityName"),
                        coalesce(Tables.CATCH.LONGITUDE, Tables.WATER_ENTITY.LONGITUDE).as("longitude"),
                        coalesce(Tables.CATCH.LATITUDE, Tables.WATER_ENTITY.LATITUDE).as("latitude"),
                        Tables.WATER_ENTITY.LATITUDE.as("default_waterEntity_latitude"),
                        Tables.WATER_ENTITY.LONGITUDE.as("default_waterEntity_longitude"),
                        coalesce(Tables.CATCH.SIZE, 0).as("size"),
                        coalesce(Tables.CATCH.WEIGHT, 0).as("weight"),
                        Tables.CATCH.MAILLEE.as("maillage")
                )
                .from(Tables.CATCH)
                .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                .innerJoin(Tables.WATER_ENTITY).on(Tables.TRIP.WATER_ENTITY_ID.eq(Tables.WATER_ENTITY.ID))
                .innerJoin(Tables.SPECIES).on(Tables.CATCH.SPECIES_ID.eq(Tables.SPECIES.ID))
                .where(Tables.TRIP.OWNER_ID.eq(userId))
                .fetch()
                .map(markerRecord -> ImmutableCatchMarker.builder()
                        .id(markerRecord.get(Tables.CATCH.ID))
                        .tripId(markerRecord.get("tripId", UUID.class))
                        .tripName(markerRecord.get("tripName", String.class))
                        .specieName(markerRecord.get("specieName", String.class))
                        .date(markerRecord.get("date", LocalDateTime.class).toLocalDate())
                        .waterEntityName(markerRecord.get("waterEntityName", String.class))
                        .longitude(markerRecord.get("longitude", Double.class))
                        .latitude(markerRecord.get("latitude", Double.class))
                        .size(markerRecord.get("size", Double.class))
                        .weight(markerRecord.get("weight", Double.class))
                        .maillage(markerRecord.get("maillage", Maillage.class))
                        .hasValidCoordinates(
                                markerRecord.get("default_waterEntity_longitude") != null && !markerRecord.get("default_waterEntity_longitude").equals(markerRecord.get("longitude")) ||
                                markerRecord.get("default_waterEntity_latitude") != null && !markerRecord.get("default_waterEntity_latitude").equals(markerRecord.get("latitude"))
                        )
                        .build())
        );
        return result;
    }

    public Set<UUID> listCatchIds(UUID tripId) {
        List<UUID> result = withContext(context -> context.select(Tables.CATCH.ID).from(Tables.CATCH).where(Tables.CATCH.TRIP_ID.eq(tripId)).fetch(Tables.CATCH.ID));
        return new LinkedHashSet<>(result);
    }

    public void delete(UUID catchId) {
        withContextNoResult(context -> {
            context.deleteFrom(Tables.CATCH_MEASUREMENT_PICTURE).where(Tables.CATCH_MEASUREMENT_PICTURE.CATCH_ID.eq(catchId)).execute();
            context.deleteFrom(Tables.CATCH_PICTURE).where(Tables.CATCH_PICTURE.CATCH_ID.eq(catchId)).execute();
            context.deleteFrom(Tables.CATCH).where(Tables.CATCH.ID.eq(catchId)).execute();
        });
    }

    public void update(Catch existingCatch) {
        withContextNoResult(context -> {
            CatchRecord record = context.newRecord(Tables.CATCH, existingCatch);
            // latitude/longitude are GENERATED ALWAYS AS ... STORED; see create().
            record.changed(Tables.CATCH.LATITUDE, false);
            record.changed(Tables.CATCH.LONGITUDE, false);
            record.update();
        });
    }

    /**
     * position is a PostGIS geometry column; the generated DAO's typed setter can't
     * bind a WKT string into it directly (no implicit cast on a JDBC bind parameter),
     * so it's set explicitly via ST_GeomFromText instead.
     */
    public void updatePosition(UUID catchId, String wktPoint) {
        withContextNoResult(context ->
                context.execute("UPDATE catch SET position = ST_GeomFromText(?, 4326) WHERE id = ?", wktPoint, catchId));
    }

    public Catch getCatch(UUID catchId) {
        Catch aCatch = withDao(CatchDao.class, dao -> dao.fetchOneById(catchId));
        return aCatch;
    }

    protected Record2<UUID, Integer> asPictureId(UUID catchId, int pictureIndex) {
        DSLContext dslContext = newContext();
        Record2<UUID, Integer> someRecord = dslContext.newRecord(Tables.CATCH_PICTURE.CATCH_ID, Tables.CATCH_PICTURE.PICTURE_INDEX);
        Record2<UUID, Integer> pictureId = someRecord.values(catchId, pictureIndex);
        return pictureId;
    }

    public void setPicture(UUID catchId, int pictureIndex, byte[] bytes) {
        final Record2<UUID, Integer> pictureId = asPictureId(catchId, pictureIndex);
        CatchPicture newCatchPicture = new CatchPicture(catchId, bytes, pictureIndex);
        withDaoNoResult(CatchPictureDao.class, dao -> {
            dao.deleteById(pictureId);
            dao.insert(newCatchPicture);
        });
    }

    public void setMeasurementPicture(UUID catchId, byte[] bytes) {
        CatchMeasurementPicture newCatchPicture = new CatchMeasurementPicture(catchId, bytes);
        withDaoNoResult(CatchMeasurementPictureDao.class, dao -> {
            dao.deleteById(catchId);
            dao.insert(newCatchPicture);
        });
    }

    public Optional<byte[]> getPicture(UUID catchId, int pictureIndex) {
        final Record2<UUID, Integer> pictureId = asPictureId(catchId, pictureIndex);
        CatchPicture picture = withDao(CatchPictureDao.class, dao -> dao.findById(pictureId));
        Optional<byte[]> result = Optional.ofNullable(picture).map(CatchPicture::getContent);
        return result;
    }

    public Optional<byte[]> getMeasurementPicture(UUID catchId) {
        CatchMeasurementPicture picture = withDao(CatchMeasurementPictureDao.class, dao -> dao.findById(catchId));
        Optional<byte[]> result = Optional.ofNullable(picture).map(CatchMeasurementPicture::getContent);
        return result;
    }

    public Optional<byte[]> getLastPicture(UUID catchId) {
        List<Integer> indexes = getPictureIndexes(catchId);
        if (CollectionUtils.isEmpty(indexes)) {
            return Optional.empty();
        }
        final OptionalInt max = indexes.stream().mapToInt(a -> a).max();
        final int maximalIndex = max.getAsInt();
        return this.getPicture(catchId, maximalIndex);
    }

    public void deletePicture(UUID catchId, int pictureIndex) {
        final Record2<UUID, Integer> pictureId = asPictureId(catchId, pictureIndex);
        withDaoNoResult(CatchPictureDao.class, dao -> dao.deleteById(pictureId));
    }

    public List<Integer> getPictureIndexes(UUID catchId) {
        ListMultimap<UUID, Integer> multimap = getPictureIndexes(Set.of(catchId));
        List<Integer> pictureIndexes = multimap.get(catchId);
        return pictureIndexes;
    }

    public ListMultimap<UUID, Integer> getPictureIndexes(Set<UUID> catchIds) {
        ListMultimap<UUID, Integer> result = withContext(context -> {
            Result<Record2<UUID, Integer>> catchIdToPictureIndexes = context.select(Tables.CATCH_PICTURE.CATCH_ID, Tables.CATCH_PICTURE.PICTURE_INDEX)
                    .from(Tables.CATCH_PICTURE)
                    .where(Tables.CATCH_PICTURE.CATCH_ID.in(catchIds))
                    .fetch();
            LinkedListMultimap<UUID, Integer> multimap = LinkedListMultimap.create();
            for (Record2<UUID, Integer> catchIdToPictureIndex : catchIdToPictureIndexes) {
                final UUID catchId = catchIdToPictureIndex.component1();
                final int order = catchIdToPictureIndex.component2();
                multimap.put(catchId, order);
            }
            return multimap;
        });
        return result;
    }

    /**
     * Calcule la liste des captures ayant une photo de mesure parmi la liste donnée
     */
    public Set<UUID> getMeasurementPictures(Set<UUID> catchIds) {
        Set<UUID> result = withContext(context -> {
            Set<UUID> records = context.select(Tables.CATCH_MEASUREMENT_PICTURE.CATCH_ID)
                    .from(Tables.CATCH_MEASUREMENT_PICTURE)
                    .where(Tables.CATCH_MEASUREMENT_PICTURE.CATCH_ID.in(catchIds))
                    .fetchSet(Tables.CATCH_MEASUREMENT_PICTURE.CATCH_ID);
            return records;
        });
        return result;
    }

    public void deleteByTrip(UUID tripId) {
        List<Catch> catchList = listCatchs(tripId);
        catchList.stream()
                .map(Catch::getId)
                .forEach(this::delete);
    }

    protected Multimap<Month, Catch> findMonthly0(Optional<UUID> userId, Optional<Integer> year, Optional<List<UUID>> waterEntitiesFilter) {
        Multimap<Month, Catch> result = withContext(context -> {
            SelectConditionStep<Record> selectStep = context
                    .select(Tables.TRIP.DAY)
                    .select(Tables.CATCH.fields())
                    .from(Tables.CATCH)
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .leftJoin(Tables.FISHOLA_USER).on(Tables.FISHOLA_USER.ID.eq(Tables.TRIP.OWNER_ID))
                    .where(trueCondition());
            if (userId.isPresent()) {
                // Si on précise l'utilisateur, on ne prend que ses sorties non masquées
                selectStep = selectStep.and(Tables.TRIP.OWNER_ID.eq(userId.get()))
                        .and(Tables.TRIP.HIDDEN.eq(false));
            } else {
                // Sinon on inclut seulement les sorties dont les utilisateurs ne sont pas à exclure
                Condition nonExcludedUserCondition = Tables.FISHOLA_USER.EXCLUDE_FROM_EXPORTS.eq(false);
                nonExcludedUserCondition = nonExcludedUserCondition.and(Tables.CATCH.EXCLUDE_FROM_EXPORTS.eq((false)));
                // ... ou les sorties où il n'y a pas d'utilisateur
                Condition noUserCondition = Tables.TRIP.OWNER_ID.isNull();
                selectStep = selectStep.and(or(nonExcludedUserCondition, noUserCondition));
            }
            if (year.isPresent()) {
                LocalDate min = LocalDate.of(year.get(), Month.JANUARY, 1);
                LocalDate max = LocalDate.of(year.get(), Month.DECEMBER, 31);
                selectStep = selectStep.and(Tables.TRIP.DAY.between(min, max));
            }
            if (waterEntitiesFilter.isPresent()) {
                selectStep = selectStep.and(Tables.TRIP.WATER_ENTITY_ID.in(waterEntitiesFilter.get()));
            }
            Multimap<Month, Catch> multimap = selectStep
                    .stream()
                    .collect(Multimaps.toMultimap(
                            r -> r.into(Tables.TRIP.DAY).component1().getMonth(),
                            r -> r.into(Tables.CATCH).into(Catch.class),
                            ArrayListMultimap::create));
            return multimap;
        });
        return result;
    }

    public Multimap<Month, Catch> findMonthlyByUserId(UUID userId, Optional<Integer> year, Optional<List<UUID>> waterEntitiesFilter) {
        Multimap<Month, Catch> result = findMonthly0(Optional.of(userId), year, waterEntitiesFilter);
        return result;
    }

    public Multimap<Month, Catch> findAll(Optional<Integer> year, Optional<List<UUID>> waterEntitiesFilter) {
        Multimap<Month, Catch> result = findMonthly0(Optional.empty(), year, waterEntitiesFilter);
        return result;
    }

    public int countCatchs() {
        int result = withContext(context -> {
            // On compte les sorties des utilisateurs non exclus
            SelectConditionStep<Record1<UUID>> selectNonExcludedUsers = context.select(Tables.CATCH.ID)
                    .from(Tables.CATCH)
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .innerJoin(Tables.FISHOLA_USER).on(Tables.FISHOLA_USER.ID.eq(Tables.TRIP.OWNER_ID))
                    .where(Tables.FISHOLA_USER.EXCLUDE_FROM_EXPORTS.eq(false));
            int countFromNonExcludedUsers = context.fetchCount(selectNonExcludedUsers);

            // On compte aussi les sorties dont l'utilisateur a été supprimé
            SelectConditionStep<Record1<UUID>> selectNoUser = context.select(Tables.CATCH.ID)
                    .from(Tables.CATCH)
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .where(Tables.TRIP.OWNER_ID.isNull());
            int countNoUser = context.fetchCount(selectNoUser);

            return countFromNonExcludedUsers + countNoUser;
        });
        return result;
    }

    public int countPictures() {
        int result = withContext(context -> {
            // On compte les sorties des utilisateurs non exclus
            SelectConditionStep<Record1<UUID>> selectNonExcludedUsers = context.select(Tables.CATCH_PICTURE.CATCH_ID)
                    .from(Tables.CATCH_PICTURE)
                    .innerJoin(Tables.CATCH).on(Tables.CATCH.ID.eq(Tables.CATCH_PICTURE.CATCH_ID))
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .innerJoin(Tables.FISHOLA_USER).on(Tables.FISHOLA_USER.ID.eq(Tables.TRIP.OWNER_ID))
                    .where(Tables.FISHOLA_USER.EXCLUDE_FROM_EXPORTS.eq(false));
            int countFromNonExcludedUsers = context.fetchCount(selectNonExcludedUsers);

            // On compte aussi les sorties dont l'utilisateur a été supprimé
            SelectConditionStep<Record1<UUID>> selectNoUser = context.select(Tables.CATCH_PICTURE.CATCH_ID)
                    .from(Tables.CATCH_PICTURE)
                    .innerJoin(Tables.CATCH).on(Tables.CATCH.ID.eq(Tables.CATCH_PICTURE.CATCH_ID))
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .where(Tables.TRIP.OWNER_ID.isNull());
            int countNoUser = context.fetchCount(selectNoUser);

            return countFromNonExcludedUsers + countNoUser;
        });
        return result;
    }

    public Map<UUID, Integer> countCatchsByWaterEntityId() {
        Map<UUID, Integer> result = withContext(context -> {
            Result<Record2<UUID, Integer>> fetched =
                    context.select(WaterEntity.WATER_ENTITY.ID, count())
                    .from(Tables.CATCH)
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .innerJoin(Tables.WATER_ENTITY).on(Tables.WATER_ENTITY.ID.eq(Tables.TRIP.WATER_ENTITY_ID))
                    .groupBy(WaterEntity.WATER_ENTITY.ID)
                    .fetch();
            ImmutableMap.Builder<UUID, Integer> builder = ImmutableMap.builder();
            fetched.forEach(tuple -> builder.put(tuple.value1(), tuple.value2()));
            ImmutableMap<UUID, Integer> map = builder.build();
            return map;
        });
        return result;
    }

}
