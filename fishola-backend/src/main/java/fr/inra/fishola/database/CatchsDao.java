package fr.inra.fishola.database;

import fr.inra.fishola.entities.Tables;
import fr.inra.fishola.entities.tables.daos.CatchDao;
import fr.inra.fishola.entities.tables.pojos.Catch;
import fr.inra.fishola.entities.tables.records.CatchRecord;

import javax.inject.Singleton;
import java.util.List;
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

}
