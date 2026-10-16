package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.entities.Tables;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Périmètre de gestion des milieux d'un compte staff (#231) : un milieu relève
 * de tout département dont le contour, élargi du buffer configuré
 * ({@code fishola.staff-perimeter-buffer-m}), intersecte sa géométrie — plus
 * son département principal {@code water_entity.department}. Correspondance
 * précalculée dans {@code water_entity_department} (migration V2.11.0).
 * <p>
 * Ne concerne que la gestion des milieux : les données (sorties, prises,
 * exports) restent cloisonnées par {@code trip.department} /
 * {@code catch.department}.
 */
@Singleton
public class StaffPerimeterDao extends AbstractFisholaDao {

    /**
     * Aligne le buffer stocké en base sur la configuration et, s'il a changé,
     * recalcule toute la correspondance milieu ↔ départements.
     *
     * @param bufferM distance du buffer en mètres
     * @return {@code true} si un recalcul a eu lieu
     */
    @Transactional
    public boolean syncBufferDistance(double bufferM) {
        return withContext(context -> {
            int updated = context.update(Tables.STAFF_PERIMETER_SETTING)
                    .set(Tables.STAFF_PERIMETER_SETTING.BUFFER_M, bufferM)
                    .where(Tables.STAFF_PERIMETER_SETTING.BUFFER_M.ne(bufferM))
                    .execute();
            if (updated > 0) {
                context.execute("SELECT refresh_staff_perimeter()");
            }
            return updated > 0;
        });
    }

    /**
     * Condition « le milieu {@code waterEntityId} relève de l'un des
     * départements », à combiner dans une requête jOOQ.
     *
     * @param waterEntityId colonne portant l'identifiant du milieu
     * @param departmentCodes périmètre du staff, non vide
     */
    public static Condition inPerimeter(Field<UUID> waterEntityId, Set<String> departmentCodes) {
        return DSL.exists(DSL.selectOne()
                .from(Tables.WATER_ENTITY_DEPARTMENT)
                .where(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID.eq(waterEntityId))
                .and(Tables.WATER_ENTITY_DEPARTMENT.DEPARTMENT_CODE.in(departmentCodes)));
    }

    /**
     * Milieux, parmi ceux demandés, qui relèvent de l'un des départements.
     *
     * @param waterEntityIds milieux à filtrer
     * @param departmentCodes périmètre du staff ; vide = aucun milieu
     */
    public Set<UUID> filterInPerimeter(Collection<UUID> waterEntityIds, Set<String> departmentCodes) {
        if (waterEntityIds.isEmpty() || departmentCodes.isEmpty()) {
            return Set.of();
        }
        return withContext(context -> context
                .selectDistinct(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID)
                .from(Tables.WATER_ENTITY_DEPARTMENT)
                .where(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID.in(waterEntityIds))
                .and(Tables.WATER_ENTITY_DEPARTMENT.DEPARTMENT_CODE.in(departmentCodes))
                .fetchSet(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID));
    }

    /**
     * Vrai si tous les milieux demandés relèvent du périmètre.
     *
     * @param waterEntityIds milieux à vérifier, identifiants nuls ignorés
     * @param departmentCodes périmètre du staff ; vide = aucun milieu
     */
    public boolean allInPerimeter(Collection<UUID> waterEntityIds, Set<String> departmentCodes) {
        Set<UUID> requested = waterEntityIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return filterInPerimeter(requested, departmentCodes).containsAll(requested);
    }

    /**
     * Tous les milieux qui relèvent de l'un des départements.
     *
     * @param departmentCodes périmètre du staff ; vide = aucun milieu
     */
    public Set<UUID> listWaterEntityIds(Set<String> departmentCodes) {
        if (departmentCodes.isEmpty()) {
            return Set.of();
        }
        return withContext(context -> context
                .selectDistinct(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID)
                .from(Tables.WATER_ENTITY_DEPARTMENT)
                .where(Tables.WATER_ENTITY_DEPARTMENT.DEPARTMENT_CODE.in(departmentCodes))
                .fetchSet(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID));
    }
}
