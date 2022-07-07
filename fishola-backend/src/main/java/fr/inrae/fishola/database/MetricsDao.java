package fr.inrae.fishola.database;

import fr.inrae.fishola.rest.metrics.CountPerlakeAndPerYear;
import fr.inrae.fishola.rest.metrics.MetricBean;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class MetricsDao extends AbstractFisholaDao {

    public MetricBean getMetrics() {
        MetricBean result = new MetricBean();

        String usersPerYearSQL = "select '-' as lac, extract(year from created_on) as annee, count(*) as total from fishola_user where exclude_from_exports = false group by extract(year from created_on);";
        List<CountPerlakeAndPerYear> usersPerYears = withContext(context -> context.fetch(usersPerYearSQL).into(CountPerlakeAndPerYear.class));
        result.usersPerYear = usersPerYears;

        String tripsPerLakeSQL = "select lake.name as lac, extract(year from t.created_on) as annee, count(*) as total from trip t join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by lake.name, extract(year from t.created_on) order by lake.name;";
        List<CountPerlakeAndPerYear> tripsPerLake = withContext(context -> context.fetch(tripsPerLakeSQL).into(CountPerlakeAndPerYear.class));
        result.tripsPerLake = tripsPerLake;

        String catchesPerLakeSQL = "select lake.name as lac, extract(year from c.created_on) as annee, count(*) as total from catch c join trip t on c.trip_id = t.id join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by lake.name, extract(year from c.created_on) order by lake.name;";
        List<CountPerlakeAndPerYear> catchesPerLake = withContext(context -> context.fetch(catchesPerLakeSQL).into(CountPerlakeAndPerYear.class));
        result.catchesPerLake = catchesPerLake;

        String automaticMeasuresPerLakeSQL = "select lake.name as lac, extract(year from c.created_on) as annee, count(*) as total from catch c join trip t on c.trip_id = t.id join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false and automatic_measure > 0 group by lake.name, extract(year from c.created_on) order by lake.name;";
        List<CountPerlakeAndPerYear> automaticMeasuresPerLake = withContext(context -> context.fetch(automaticMeasuresPerLakeSQL).into(CountPerlakeAndPerYear.class));
        result.automaticMeasuresPerLake = automaticMeasuresPerLake;

        /*
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
         */
        return result;
    }
}
