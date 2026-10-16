package fr.inrae.fishola.gamification;

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

import fr.inrae.fishola.database.AbstractFisholaDao;
import fr.inrae.fishola.rest.department.Departments;
import fr.inrae.fishola.rest.gamification.BadgeSouvenirBean;
import fr.inrae.fishola.rest.gamification.SouvenirCatchBean;
import fr.inrae.fishola.rest.gamification.SouvenirTripBean;
import fr.inrae.fishola.rest.hydro.GeoPoint;
import fr.inrae.fishola.rest.hydro.ImmutableGeoPoint;
import jakarta.inject.Singleton;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Données affichables de la carte souvenir d'un badge (#275) : résout les identifiants
 * d'une {@link UnlockProof} en captures et sorties, en ne retenant que celles qui
 * appartiennent encore au pêcheur connecté ({@code trip.owner_id}). Une capture ou une
 * sortie supprimée depuis le déblocage disparaît simplement du résultat.
 */
@Singleton
public class BadgeSouvenirDao extends AbstractFisholaDao {

    private static final String CATCHES_SQL = "SELECT c.id, c.trip_id, s.name AS species_name, c.size, c.weight, "
            + "c.quantity, c.catch_timestamp, c.latitude, c.longitude, "
            + "EXISTS (SELECT 1 FROM catch_picture cp WHERE cp.catch_id = c.id) AS has_picture "
            + "FROM catch c JOIN trip t ON t.id = c.trip_id LEFT JOIN species s ON s.id = c.species_id "
            + "WHERE t.owner_id = ? AND c.id = ANY(?::uuid[])";

    private static final String TRIPS_SQL = "SELECT t.id, t.begin_timestamp, t.end_timestamp, t.department, "
            + "t.begin_latitude, t.begin_longitude, t.end_latitude, t.end_longitude, "
            + "w.name AS water_entity_name, w.kind::text AS water_entity_kind, w.altitude_moyenne, "
            + "w.latitude AS water_entity_latitude, w.longitude AS water_entity_longitude, com.name AS commune "
            + "FROM trip t LEFT JOIN water_entity w ON w.id = t.water_entity_id "
            + "LEFT JOIN LATERAL ("
            + "  SELECT co.name FROM commune co "
            + "  WHERE ST_Contains(co.geom, ST_SetSRID(ST_MakePoint("
            + "        coalesce(t.begin_longitude, t.snapped_longitude, w.longitude), "
            + "        coalesce(t.begin_latitude, t.snapped_latitude, w.latitude)), 4326)) "
            + "  LIMIT 1"
            + ") com ON true "
            + "WHERE t.owner_id = ? AND t.id = ANY(?::uuid[])";

    private static final String CATCH_POSITIONS_SQL = "SELECT trip_id, latitude, longitude FROM catch "
            + "WHERE trip_id = ANY(?::uuid[]) AND latitude IS NOT NULL AND longitude IS NOT NULL "
            + "ORDER BY coalesce(catch_timestamp, created_on), id";

    /**
     * Carte souvenir d'un déblocage : captures dans l'ordre de la preuve, puis sorties de
     * la preuve complétées par celles des captures retenues (nom du milieu, date).
     */
    public BadgeSouvenirBean resolve(UUID unlockId, UUID userId, UnlockProof proof) {
        BadgeSouvenirBean souvenir = new BadgeSouvenirBean();
        souvenir.unlockId = unlockId;
        souvenir.catches = findOwnedCatches(userId, proof.catchIds());
        LinkedHashSet<UUID> tripIds = new LinkedHashSet<>(proof.tripIds());
        souvenir.catches.forEach(aCatch -> tripIds.add(aCatch.tripId));
        souvenir.trips = findOwnedTrips(userId, List.copyOf(tripIds));
        return souvenir;
    }

    private List<SouvenirCatchBean> findOwnedCatches(UUID userId, List<UUID> catchIds) {
        if (catchIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<UUID, SouvenirCatchBean> byId = new HashMap<>();
        withContext(ctx -> ctx.fetch(CATCHES_SQL, userId, catchIds.toArray(new UUID[0])))
                .forEach(r -> byId.put(r.get("id", UUID.class), toCatchBean(r)));
        return inOrder(catchIds, byId);
    }

    private SouvenirCatchBean toCatchBean(Record r) {
        SouvenirCatchBean bean = new SouvenirCatchBean();
        bean.id = r.get("id", UUID.class);
        bean.tripId = r.get("trip_id", UUID.class);
        bean.speciesName = r.get("species_name", String.class);
        bean.size = r.get("size", Integer.class);
        bean.weight = r.get("weight", Integer.class);
        bean.quantity = r.get("quantity", Integer.class);
        bean.caughtAt = r.get("catch_timestamp", LocalDateTime.class);
        bean.hasPicture = Boolean.TRUE.equals(r.get("has_picture", Boolean.class));
        bean.latitude = r.get("latitude", Double.class);
        bean.longitude = r.get("longitude", Double.class);
        return bean;
    }

    private List<SouvenirTripBean> findOwnedTrips(UUID userId, List<UUID> tripIds) {
        if (tripIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<UUID, SouvenirTripBean> byId = new HashMap<>();
        withContext(ctx -> ctx.fetch(TRIPS_SQL, userId, tripIds.toArray(new UUID[0])))
                .forEach(r -> byId.put(r.get("id", UUID.class), toTripBean(r)));
        if (byId.isEmpty()) {
            return new ArrayList<>();
        }
        withContext(ctx -> ctx.fetch(CATCH_POSITIONS_SQL, (Object) byId.keySet().toArray(new UUID[0])))
                .forEach(r -> byId.get(r.get("trip_id", UUID.class)).catchPositions.add(toGeoPoint(r)));
        return inOrder(tripIds, byId);
    }

    private SouvenirTripBean toTripBean(Record r) {
        SouvenirTripBean bean = new SouvenirTripBean();
        bean.id = r.get("id", UUID.class);
        bean.beginTimestamp = r.get("begin_timestamp", LocalDateTime.class);
        bean.endTimestamp = r.get("end_timestamp", LocalDateTime.class);
        bean.waterEntityName = r.get("water_entity_name", String.class);
        bean.waterEntityKind = r.get("water_entity_kind", String.class);
        bean.waterEntityAltitude = r.get("altitude_moyenne", Double.class);
        bean.waterEntityLatitude = r.get("water_entity_latitude", Double.class);
        bean.waterEntityLongitude = r.get("water_entity_longitude", Double.class);
        bean.commune = r.get("commune", String.class);
        bean.department = r.get("department", String.class);
        bean.departmentName = bean.department == null ? null : Departments.lookup(bean.department).orElse(null);
        bean.beginLatitude = r.get("begin_latitude", Double.class);
        bean.beginLongitude = r.get("begin_longitude", Double.class);
        bean.endLatitude = r.get("end_latitude", Double.class);
        bean.endLongitude = r.get("end_longitude", Double.class);
        return bean;
    }

    private GeoPoint toGeoPoint(Record r) {
        return ImmutableGeoPoint.builder()
                .lat(r.get("latitude", Double.class))
                .lng(r.get("longitude", Double.class))
                .build();
    }

    private static <T> List<T> inOrder(List<UUID> ids, Map<UUID, T> byId) {
        return ids.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
