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

import fr.inrae.fishola.rest.commune.CommuneResult;
import fr.inrae.fishola.rest.commune.ImmutableCommuneResult;
import fr.inrae.fishola.rest.hydro.ImmutableGeoPoint;
import fr.inrae.fishola.rest.hydro.ImmutableNearbyWaterEntity;
import fr.inrae.fishola.rest.hydro.ImmutableWaterEntitySearchResult;
import fr.inrae.fishola.rest.hydro.NearbyWaterEntity;
import fr.inrae.fishola.rest.hydro.WaterEntitySearchResult;
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

    // Accent-insensitive, typo-tolerant name search: a substring match
    // (ILIKE '%q%') OR a trigram-similar match (%), both on f_unaccent(name) so
    // they use the functional GIN index (V1.1.2). Prefix matches rank first,
    // then similarity. Bind order: q (ILIKE where), q (% where), q (prefix
    // order), q (similarity order), limit.
    private static final String SEARCH_COMMUNES_SQL = ""
            + "SELECT insee_com, name, latitude, longitude "
            + "FROM commune "
            + "WHERE f_unaccent(name) ILIKE '%' || f_unaccent(?) || '%' "
            + "   OR f_unaccent(name) % f_unaccent(?) "
            + "ORDER BY (f_unaccent(name) ILIKE f_unaccent(?) || '%') DESC, "
            + "         similarity(f_unaccent(name), f_unaccent(?)) DESC, name "
            + "LIMIT ?";

    /**
     * Communes matching the textual query (accent-insensitive, typo-tolerant),
     * prefix matches first then by descending trigram similarity.
     */
    public List<CommuneResult> searchCommunes(String query, int limit) {
        String q = forSearch(query);
        return withContext(context -> context
                .fetch(SEARCH_COMMUNES_SQL, q, q, q, q, limit)
                .map(rec -> (CommuneResult) ImmutableCommuneResult.builder()
                        .insee(rec.get("insee_com", String.class))
                        .name(rec.get("name", String.class))
                        .centroid(ImmutableGeoPoint.builder()
                                .lat(rec.get("latitude", Double.class))
                                .lng(rec.get("longitude", Double.class))
                                .build())
                        .build()));
    }

    // Water entity name search, accent-insensitive and typo-tolerant (same
    // strategy as communes), with an optional kind filter and only entities that
    // have a geometry (so a centroid is available). Bind order: q (ILIKE where),
    // q (% where), kind, kind, q (prefix order), q (similarity order), limit.
    private static final String SEARCH_ENTITIES_SQL = ""
            + "SELECT id, name, kind::text AS kind, latitude, longitude "
            + "FROM water_entity "
            + "WHERE geom IS NOT NULL "
            + "  AND (f_unaccent(name) ILIKE '%' || f_unaccent(?) || '%' "
            + "       OR f_unaccent(name) % f_unaccent(?)) "
            + "  AND (?::text IS NULL OR kind::text = ?) "
            + "ORDER BY (f_unaccent(name) ILIKE f_unaccent(?) || '%') DESC, "
            + "         similarity(f_unaccent(name), f_unaccent(?)) DESC, name "
            + "LIMIT ?";

    /**
     * Water entities matching the textual query (accent-insensitive, typo-
     * tolerant), optionally filtered by kind, prefix matches first then by
     * descending trigram similarity.
     */
    public List<WaterEntitySearchResult> searchWaterEntities(String query, Optional<String> kind, int limit) {
        String q = forSearch(query);
        String kindFilter = kind.orElse(null);
        return withContext(context -> context
                .fetch(SEARCH_ENTITIES_SQL, q, q, kindFilter, kindFilter, q, q, limit)
                .map(rec -> (WaterEntitySearchResult) ImmutableWaterEntitySearchResult.builder()
                        .waterEntityId(rec.get("id", UUID.class))
                        .name(rec.get("name", String.class))
                        .kind(rec.get("kind", String.class))
                        .centroid(ImmutableGeoPoint.builder()
                                .lat(rec.get("latitude", Double.class))
                                .lng(rec.get("longitude", Double.class))
                                .build())
                        .build()));
    }

    // Water entities intersecting a commune (or within `bufferM` of its boundary,
    // for anglers near the commune edge), closest section/surface per entity,
    // distance/closest point computed against the commune centroid for ordering.
    // Bind order: insee, bufferM (river), bufferM (surface).
    private static final String BY_COMMUNE_SQL = ""
            + "WITH c AS (SELECT geom, ST_Centroid(geom) AS ctr FROM commune WHERE insee_com = ?), "
            + "candidates AS ( "
            + "  SELECT rs.water_entity_id AS weid, "
            + "         ST_Distance(rs.geom::geography, c.ctr::geography) AS dist, "
            + "         ST_ClosestPoint(rs.geom, c.ctr) AS cp, "
            + "         rs.persistent AS persistent "
            + "  FROM river_section rs, c "
            + "  WHERE ST_DWithin(rs.geom::geography, c.geom::geography, ?) "
            + "  UNION ALL "
            + "  SELECT ws.water_entity_id, "
            + "         ST_Distance(ws.geom::geography, c.ctr::geography), "
            + "         ST_ClosestPoint(ws.geom, c.ctr), "
            + "         NULL::boolean "
            + "  FROM water_surface ws, c "
            + "  WHERE ST_DWithin(ws.geom::geography, c.geom::geography, ?) "
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
            + "ORDER BY r.dist";

    /**
     * Water entities located in (or within {@code bufferM} of) the commune
     * identified by its INSEE code, ordered by distance to the commune centroid.
     */
    public List<NearbyWaterEntity> findWaterEntitiesByCommune(String insee, double bufferM) {
        return withContext(context -> context
                .fetch(BY_COMMUNE_SQL, insee, bufferM, bufferM)
                .map(HydroSearchDao::toNearby));
    }

    // Strips LIKE metacharacters (% _ \) from a user search term so it cannot
    // inject ILIKE wildcards; entity / commune names never contain them.
    private static String forSearch(String query) {
        return query == null ? "" : query.replaceAll("[%_\\\\]", "");
    }

    // Mapbox Vector Tile of the hydrographic network for a slippy-map tile
    // (z/x/y). Two layers in one tile: river_section (lines: entity id, name,
    // persistent) and water_surface (polygons: entity id, name). PostGIS builds
    // each layer with ST_AsMVT and the two single-layer tiles are concatenated
    // (valid per the MVT/protobuf repeated-Layer encoding). The bounding-box
    // prefilter is expressed in 4326 (r.geom && ST_Transform(env,4326)) so it
    // uses the existing GIST(geom) index; ST_AsMVTGeom then works in 3857.
    // Bind order: z, x, y.
    private static final String HYDRO_TILE_SQL = ""
            + "WITH env AS (SELECT ST_TileEnvelope(?, ?, ?) AS g), "
            + "rs AS ( "
            + "  SELECT ST_AsMVTGeom(ST_Transform(r.geom, 3857), env.g, 4096, 64, true) AS geom, "
            + "         r.water_entity_id::text AS water_entity_id, we.name AS name, r.persistent AS persistent "
            + "  FROM river_section r JOIN water_entity we ON we.id = r.water_entity_id, env "
            + "  WHERE r.geom && ST_Transform(env.g, 4326) "
            + "), "
            + "ws AS ( "
            + "  SELECT ST_AsMVTGeom(ST_Transform(s.geom, 3857), env.g, 4096, 64, true) AS geom, "
            + "         s.water_entity_id::text AS water_entity_id, we.name AS name "
            + "  FROM water_surface s JOIN water_entity we ON we.id = s.water_entity_id, env "
            + "  WHERE s.geom && ST_Transform(env.g, 4326) "
            + ") "
            + "SELECT coalesce((SELECT ST_AsMVT(rs.*, 'river_section') FROM rs WHERE geom IS NOT NULL), ''::bytea) "
            + "    || coalesce((SELECT ST_AsMVT(ws.*, 'water_surface') FROM ws WHERE geom IS NOT NULL), ''::bytea) AS tile";

    /**
     * Mapbox Vector Tile (protobuf) of the hydro network for the given slippy-map
     * tile. Empty (zero-length) when the tile covers no feature.
     */
    public byte[] getHydroTile(int z, int x, int y) {
        return withContext(context -> {
            Record rec = context.fetchOne(HYDRO_TILE_SQL, z, x, y);
            byte[] tile = rec == null ? null : rec.get("tile", byte[].class);
            return tile == null ? new byte[0] : tile;
        });
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
