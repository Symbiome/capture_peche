package fr.inra.fishola.database;

import fr.inra.fishola.entities.Tables;
import fr.inra.fishola.entities.tables.daos.CatchDao;
import fr.inra.fishola.entities.tables.daos.CatchPictureDao;
import fr.inra.fishola.entities.tables.pojos.Catch;
import fr.inra.fishola.entities.tables.pojos.CatchPicture;
import fr.inra.fishola.entities.tables.records.CatchRecord;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

}
