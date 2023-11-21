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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.Lake;
import fr.inrae.fishola.entities.tables.daos.CatchDao;
import fr.inrae.fishola.entities.tables.daos.CatchMeasurementPictureDao;
import fr.inrae.fishola.entities.tables.daos.CatchPictureDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.CatchMeasurementPicture;
import fr.inrae.fishola.entities.tables.pojos.CatchPicture;
import fr.inrae.fishola.entities.tables.records.CatchRecord;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import javax.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;

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
            c.setEditedSize(c.getSize());
        }
        if (c.getEditedWeight() == null || c.getEditedWeight() == 0) {
            c.setEditedWeight(c.getWeight());
        }
        if (c.getExcludeFromExports() == null) {
            c.setExcludeFromExports(false);
        }
        return withContext(context -> {
            CatchRecord record = context.newRecord(Tables.CATCH, c);
            CatchRecord recordInserted = context.insertInto(Tables.CATCH)
                    .set(record)
                    .returning(Tables.CATCH.ID)
                    .fetchOne();
            UUID id = recordInserted.getId();
            return id;
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
        withDaoNoResult(CatchDao.class, dao -> dao.update(existingCatch));
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
        ListMultimap<UUID, Integer> multimap = getPictureIndexes(ImmutableSet.of(catchId));
        List<Integer> pictureIndexes = multimap.get(catchId);
        return pictureIndexes;
    }

    public ListMultimap<UUID, Integer> getPictureIndexes(Set<UUID> catchIds) {
        ListMultimap<UUID, Integer> result = withContext(context -> {
            Result<Record2<UUID, Integer>> records = context.select(Tables.CATCH_PICTURE.CATCH_ID, Tables.CATCH_PICTURE.PICTURE_INDEX)
                    .from(Tables.CATCH_PICTURE)
                    .where(Tables.CATCH_PICTURE.CATCH_ID.in(catchIds))
                    .fetch();
            LinkedListMultimap<UUID, Integer> multimap = LinkedListMultimap.create();
            for (Record2<UUID, Integer> record : records) {
                final UUID catchId = record.component1();
                final int order = record.component2();
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

    protected Multimap<Month, Catch> findMonthly0(Optional<UUID> userId, Optional<Integer> year, Optional<List<UUID>> lakesFilter) {
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
            if (lakesFilter.isPresent()) {
                selectStep = selectStep.and(Tables.TRIP.LAKE_ID.in(lakesFilter.get()));
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

    public Multimap<Month, Catch> findMonthlyByUserId(UUID userId, Optional<Integer> year, Optional<List<UUID>> lakesFilter) {
        Multimap<Month, Catch> result = findMonthly0(Optional.of(userId), year, lakesFilter);
        return result;
    }

    public Multimap<Month, Catch> findAll(Optional<Integer> year, Optional<List<UUID>> lakesFilter) {
        Multimap<Month, Catch> result = findMonthly0(Optional.empty(), year, lakesFilter);
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

    public Map<UUID, Integer> countCatchsByLakeId() {
        Map<UUID, Integer> result = withContext(context -> {
            Result<Record2<UUID, Integer>> fetched =
                    context.select(Lake.LAKE.ID, count())
                    .from(Tables.CATCH)
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .innerJoin(Tables.LAKE).on(Tables.LAKE.ID.eq(Tables.TRIP.LAKE_ID))
                    .groupBy(Lake.LAKE.ID)
                    .fetch();
            ImmutableMap.Builder<UUID, Integer> builder = ImmutableMap.builder();
            fetched.forEach(tuple -> builder.put(tuple.value1(), tuple.value2()));
            ImmutableMap<UUID, Integer> map = builder.build();
            return map;
        });
        return result;
    }

}
