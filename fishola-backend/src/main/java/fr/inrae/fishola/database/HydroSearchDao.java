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

import fr.inrae.fishola.rest.hydro.ImmutableGeoPoint;
import fr.inrae.fishola.rest.hydro.ImmutableNearbyWaterEntity;
import fr.inrae.fishola.rest.hydro.NearbyWaterEntity;
import jakarta.inject.Singleton;
import org.jooq.Record;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spatial searches over the hydrographic network. Queries the fine geometries
 * ({@code river_section} lines, {@code water_surface} polygons) rather than the
 * water entity centroid, since a river centroid is often kilometres away from
 * the angler.
 */
@Singleton
public class HydroSearchDao extends AbstractFisholaDao {

    // Nearby search: candidate river sections and water surfaces within the
    // metric radius, aggregated to the closest one per water entity (DISTINCT ON
    // ... ORDER BY dist), joined to water_entity for name/kind, ordered by
    // distance, paged. The ST_DWithin(geom::geography, ...) filters are index-
    // accelerated by the functional geography GIST indexes added in V1.1.0, and
    // exact in metres (no latitude approximation). Bind order: lng, lat,
    // radiusM, radiusM, kind, kind, limit, offset.
    private static final String NEARBY_SQL = ""
            + "WITH pt AS (SELECT ST_SetSRID(ST_MakePoint(?, ?), 4326) AS g), "
            + "candidates AS ( "
            + "  SELECT rs.water_entity_id AS weid, "
            + "         ST_Distance(rs.geom::geography, pt.g::geography) AS dist, "
            + "         ST_ClosestPoint(rs.geom, pt.g) AS cp, "
            + "         rs.persistent AS persistent "
            + "  FROM river_section rs, pt "
            + "  WHERE ST_DWithin(rs.geom::geography, pt.g::geography, ?) "
            + "  UNION ALL "
            + "  SELECT ws.water_entity_id, "
            + "         ST_Distance(ws.geom::geography, pt.g::geography), "
            + "         ST_ClosestPoint(ws.geom, pt.g), "
            + "         NULL::boolean "
            + "  FROM water_surface ws, pt "
            + "  WHERE ST_DWithin(ws.geom::geography, pt.g::geography, ?) "
            + "), "
            + "ranked AS ( "
            + "  SELECT DISTINCT ON (weid) weid, dist, "
            + "         ST_Y(cp) AS cp_lat, ST_X(cp) AS cp_lng, persistent "
            + "  FROM candidates WHERE weid IS NOT NULL "
            + "  ORDER BY weid, dist "
            + ") "
            + "SELECT r.weid AS water_entity_id, we.name AS name, we.kind::text AS kind, "
            + "       r.dist AS distance_m, r.cp_lat AS cp_lat, r.cp_lng AS cp_lng, "
            + "       r.persistent AS persistent "
            + "FROM ranked r JOIN water_entity we ON we.id = r.weid "
            + "WHERE (?::text IS NULL OR we.kind::text = ?) "
            + "ORDER BY r.dist "
            + "LIMIT ? OFFSET ?";

    /**
     * Water entities whose fine geometry lies within {@code radiusM} metres of
     * the point, ordered by ascending distance.
     *
     * @param lat     latitude of the queried point (WGS 84)
     * @param lng     longitude of the queried point (WGS 84)
     * @param radiusM search radius in metres
     * @param kind    optional filter on the entity kind (STILL / FLOWING)
     * @param limit   page size
     * @param offset  page offset (rows to skip)
     */
    public List<NearbyWaterEntity> nearby(double lat, double lng, double radiusM,
                                          Optional<String> kind, int limit, int offset) {
        String kindFilter = kind.orElse(null);
        return withContext(context -> context
                .fetch(NEARBY_SQL, lng, lat, radiusM, radiusM, kindFilter, kindFilter, limit, offset)
                .map(HydroSearchDao::toNearby));
    }

    // Shared mapping of a nearby/by-commune result row to the DTO.
    private static NearbyWaterEntity toNearby(Record rec) {
        return ImmutableNearbyWaterEntity.builder()
                .waterEntityId(rec.get("water_entity_id", UUID.class))
                .name(rec.get("name", String.class))
                .kind(rec.get("kind", String.class))
                .distanceM(rec.get("distance_m", Double.class))
                .closestPoint(ImmutableGeoPoint.builder()
                        .lat(rec.get("cp_lat", Double.class))
                        .lng(rec.get("cp_lng", Double.class))
                        .build())
                .persistent(Optional.ofNullable(rec.get("persistent", Boolean.class)))
                .build();
    }
}
