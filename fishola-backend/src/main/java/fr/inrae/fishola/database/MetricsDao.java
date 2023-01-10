package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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


import fr.inrae.fishola.rest.metrics.CountPerlakeAndPerYear;
import fr.inrae.fishola.rest.metrics.MetricBean;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class MetricsDao extends AbstractFisholaDao {

    public MetricBean getMetrics() {
        MetricBean result = new MetricBean();

        String activeUsersPerYearSQL = "select lake.name as lac, extract(year from t.created_on)::INTEGER as annee, count(distinct u.id) as total from trip t join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by lake.name, extract(year from t.created_on) order by lake.name;";
        List<CountPerlakeAndPerYear> activeUsersPerYear = withContext(context -> context.fetch(activeUsersPerYearSQL).into(CountPerlakeAndPerYear.class));
        result.activeUsersPerYear = activeUsersPerYear;

        String userRegistrationsPerYearSQL = "select '-' as lac, extract(year from created_on)::INTEGER as annee, count(*) as total from fishola_user where exclude_from_exports = false group by extract(year from created_on);";
        List<CountPerlakeAndPerYear> userRegistrationsPerYear = withContext(context -> context.fetch(userRegistrationsPerYearSQL).into(CountPerlakeAndPerYear.class));
        result.userRegistrationsPerYear = userRegistrationsPerYear;
        result.userRegistrationsPerYear.add(getTotalRow(userRegistrationsPerYear));

        String tripsPerLakeSQL = "select lake.name as lac, extract(year from t.created_on)::INTEGER as annee, count(*) as total from trip t join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by lake.name, extract(year from t.created_on) order by lake.name;";
        List<CountPerlakeAndPerYear> tripsPerLake = withContext(context -> context.fetch(tripsPerLakeSQL).into(CountPerlakeAndPerYear.class));
        result.tripsPerLake = tripsPerLake;
        result.tripsPerLake.add(getTotalRow(tripsPerLake));

        String catchesPerLakeSQL = "select lake.name as lac, extract(year from c.created_on)::INTEGER as annee, count(*) as total from catch c join trip t on c.trip_id = t.id join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by lake.name, extract(year from c.created_on) order by lake.name;";
        List<CountPerlakeAndPerYear> catchesPerLake = withContext(context -> context.fetch(catchesPerLakeSQL).into(CountPerlakeAndPerYear.class));
        result.catchesPerLake = catchesPerLake;
        result.catchesPerLake.add(getTotalRow(catchesPerLake));

        String automaticMeasuresPerLakeSQL = "select lake.name as lac, extract(year from c.created_on)::INTEGER as annee, count(*) as total from catch c join trip t on c.trip_id = t.id join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false and automatic_measure > 0 group by lake.name, extract(year from c.created_on) order by lake.name;";
        List<CountPerlakeAndPerYear> automaticMeasuresPerLake = withContext(context -> context.fetch(automaticMeasuresPerLakeSQL).into(CountPerlakeAndPerYear.class));
        result.automaticMeasuresPerLake = automaticMeasuresPerLake;
        result.automaticMeasuresPerLake.add(getTotalRow(automaticMeasuresPerLake));
        return result;
    }

    public CountPerlakeAndPerYear getTotalRow(List<CountPerlakeAndPerYear> rows) {
        CountPerlakeAndPerYear total = new CountPerlakeAndPerYear();
        total.annee = "Toutes";
        total.lac = "Tous";
        total.total = rows.stream().mapToInt(r -> r.total).sum();
        return total;
    }
}
