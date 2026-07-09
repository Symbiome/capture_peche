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


import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.rest.metrics.CountPerWaterEntityAndPerYear;
import fr.inrae.fishola.rest.metrics.MetricBean;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class MetricsDao extends AbstractFisholaDao {

    public MetricBean getMetrics(FisholaAdmin fisholaAdmin, Set<UUID> allowedAdminWaterEntities) {
        MetricBean result = new MetricBean();

        String onlyAllowedWaterEntitiesJoinCondition = "";
        if (Boolean.FALSE.equals(fisholaAdmin.getIsNationalAdmin())) {
            String waterEntityStringList = "(" + allowedAdminWaterEntities.stream()
                    .map(uuid -> "'" + uuid.toString() + "'")
                    .collect(Collectors.joining(",")) +")";
            onlyAllowedWaterEntitiesJoinCondition = "water_entity.id in " + waterEntityStringList + " and ";
        }

        String activeUsersPerYearSQL = "select water_entity.name as lac, extract(year from t.created_on)::INTEGER as annee, count(distinct u.id) as total from trip t join water_entity on " + onlyAllowedWaterEntitiesJoinCondition + " water_entity.id = t.water_entity_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by water_entity.name, extract(year from t.created_on) order by water_entity.name;";
        result.activeUsersPerYear = withContext(context -> context.fetch(activeUsersPerYearSQL).into(CountPerWaterEntityAndPerYear.class));

        String userRegistrationsPerYearSQL = "select '-' as lac, extract(year from created_on)::INTEGER as annee, count(*) as total from fishola_user where exclude_from_exports = false group by extract(year from created_on);";
        List<CountPerWaterEntityAndPerYear> userRegistrationsPerYear = withContext(context -> context.fetch(userRegistrationsPerYearSQL).into(CountPerWaterEntityAndPerYear.class));
        result.userRegistrationsPerYear = userRegistrationsPerYear;
        result.userRegistrationsPerYear.add(getTotalRow(userRegistrationsPerYear));

        String tripsPerWaterEntitySQL = "select water_entity.name as lac, extract(year from t.created_on)::INTEGER as annee, count(*) as total from trip t join water_entity on " + onlyAllowedWaterEntitiesJoinCondition + " water_entity.id = t.water_entity_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by water_entity.name, extract(year from t.created_on) order by water_entity.name;";
        List<CountPerWaterEntityAndPerYear> tripsPerWaterEntity = withContext(context -> context.fetch(tripsPerWaterEntitySQL).into(CountPerWaterEntityAndPerYear.class));
        result.tripsPerWaterEntity = tripsPerWaterEntity;
        result.tripsPerWaterEntity.add(getTotalRow(tripsPerWaterEntity));

        String catchesPerWaterEntitySQL = "select water_entity.name as lac, extract(year from c.created_on)::INTEGER as annee, count(*) as total from catch c join trip t on c.trip_id = t.id join water_entity on " + onlyAllowedWaterEntitiesJoinCondition + " water_entity.id = t.water_entity_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by water_entity.name, extract(year from c.created_on) order by water_entity.name;";
        List<CountPerWaterEntityAndPerYear> catchesPerWaterEntity = withContext(context -> context.fetch(catchesPerWaterEntitySQL).into(CountPerWaterEntityAndPerYear.class));
        result.catchesPerWaterEntity = catchesPerWaterEntity;
        result.catchesPerWaterEntity.add(getTotalRow(catchesPerWaterEntity));

        String automaticMeasuresPerWaterEntitySQL = "select water_entity.name as lac, extract(year from c.created_on)::INTEGER as annee, count(*) as total from catch c join trip t on c.trip_id = t.id join water_entity on " + onlyAllowedWaterEntitiesJoinCondition + " water_entity.id = t.water_entity_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false and automatic_measure > 0 group by water_entity.name, extract(year from c.created_on) order by water_entity.name;";
        List<CountPerWaterEntityAndPerYear> automaticMeasuresPerWaterEntity = withContext(context -> context.fetch(automaticMeasuresPerWaterEntitySQL).into(CountPerWaterEntityAndPerYear.class));
        result.automaticMeasuresPerWaterEntity = automaticMeasuresPerWaterEntity;
        result.automaticMeasuresPerWaterEntity.add(getTotalRow(automaticMeasuresPerWaterEntity));
        return result;
    }

    public CountPerWaterEntityAndPerYear getTotalRow(List<CountPerWaterEntityAndPerYear> rows) {
        CountPerWaterEntityAndPerYear total = new CountPerWaterEntityAndPerYear();
        total.annee = "Toutes";
        total.lac = "Tous";
        total.total = rows.stream().mapToInt(r -> r.total).sum();
        return total;
    }
}
