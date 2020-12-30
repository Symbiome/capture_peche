package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import com.google.common.collect.ImmutableMap;
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.Lake;
import fr.inrae.fishola.entities.tables.daos.CatchDao;
import fr.inrae.fishola.entities.tables.daos.CatchPictureDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.CatchPicture;
import fr.inrae.fishola.entities.tables.records.CatchRecord;
import org.jooq.Record2;
import org.jooq.Result;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.count;

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

    public List<Catch> findAllByUserId(UUID userId) {
        List<Catch> result = withContext(context -> {
            Set<UUID> tripIds = context.select(Tables.TRIP.ID)
                    .from(Tables.TRIP)
                    .where(Tables.TRIP.OWNER_ID.eq(userId))
                    .fetchSet(Tables.TRIP.ID);
            List<Catch> catches = context.selectFrom(Tables.CATCH)
                    .where(Tables.CATCH.TRIP_ID.in(tripIds))
                    .fetchInto(Catch.class);
            return catches;
        });
        return result;
    }

    public List<Catch> findAll() {
        List<Catch> result = withDao(CatchDao.class, CatchDao::findAll);
        return result;
    }

    public int countCatchs() {
        int result = withDao(CatchDao.class, CatchDao::count).intValue();
        return result;
    }

    public int countPictures() {
        int result = withDao(CatchPictureDao.class, CatchPictureDao::count).intValue();
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
