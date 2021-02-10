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
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.Lake;
import fr.inrae.fishola.entities.tables.daos.CatchDao;
import fr.inrae.fishola.entities.tables.daos.CatchPictureDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.CatchPicture;
import fr.inrae.fishola.entities.tables.records.CatchRecord;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;

import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.trueCondition;

@Singleton
public class CatchsDao extends AbstractFisholaDao {

    public UUID create(Catch c) {
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

    public void delete(UUID catchId) {
        withContextNoResult(context -> {
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

    public void setPicture(UUID catchId, byte[] bytes) {
        CatchPicture newCatchPicture = new CatchPicture(catchId, bytes);
        withDaoNoResult(CatchPictureDao.class, dao -> {
            dao.deleteById(catchId);
            dao.insert(newCatchPicture);
        });
    }

    public Optional<byte[]> getPicture(UUID catchId) {
        CatchPicture picture = withDao(CatchPictureDao.class, dao -> dao.findById(catchId));
        Optional<byte[]> result = Optional.ofNullable(picture).map(CatchPicture::getContent);
        return result;
    }

    public Set<UUID> checkForPictures(Set<UUID> catchIds) {
        Set<UUID> result = withContext(context -> {
            Set<UUID> records = context.select(Tables.CATCH_PICTURE.CATCH_ID)
                    .from(Tables.CATCH_PICTURE)
                    .where(Tables.CATCH_PICTURE.CATCH_ID.in(catchIds))
                    .fetchSet(Tables.CATCH_PICTURE.CATCH_ID);
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

    protected Multimap<Month, Catch> findMonthly0(Optional<UUID> userId, Optional<Integer> year) {
        Multimap<Month, Catch> result = withContext(context -> {
            SelectConditionStep<Record> selectStep = context
                    .select(Tables.TRIP.DAY)
                    .select(Tables.CATCH.fields())
                    .from(Tables.CATCH)
                    .innerJoin(Tables.TRIP).on(Tables.TRIP.ID.eq(Tables.CATCH.TRIP_ID))
                    .where(trueCondition());
            if (userId.isPresent()) {
                selectStep = selectStep.and(Tables.TRIP.OWNER_ID.eq(userId.get()))
                        .and(Tables.TRIP.HIDDEN.eq(false));
            }
            if (year.isPresent()) {
                LocalDate min = LocalDate.of(year.get(), Month.JANUARY, 1);
                LocalDate max = LocalDate.of(year.get(), Month.DECEMBER, 31);
                selectStep = selectStep.and(Tables.TRIP.DAY.between(min, max));
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

    public Multimap<Month, Catch> findMonthlyByUserId(UUID userId, Optional<Integer> year) {
        Multimap<Month, Catch> result = findMonthly0(Optional.of(userId), year);
        return result;
    }

    public Multimap<Month, Catch> findAll(Optional<Integer> year) {
        Multimap<Month, Catch> result = findMonthly0(Optional.empty(), year);
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
