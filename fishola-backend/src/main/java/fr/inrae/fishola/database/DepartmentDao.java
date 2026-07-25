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

import fr.inrae.fishola.rest.department.DepartmentSummary;
import fr.inrae.fishola.rest.department.Departments;
import fr.inrae.fishola.rest.department.ImmutableDepartmentSummary;
import jakarta.inject.Singleton;
import org.jooq.Record;

import java.util.List;

/**
 * Queries backing the offline "download a department" packs (#54). The
 * department maille is <em>derived</em> from the commune INSEE code prefix (no
 * dedicated table): metropolitan departments are the first two characters
 * ("2A"/"2B" for Corsica fall out naturally), overseas departments (INSEE
 * starting with 97/98) the first three. This keeps the pack decoupled from the
 * still-open "structure" territorial model (#63): it is a plain spatial filter
 * over the public hydro referential, not an access-control boundary.
 */
@Singleton
public class DepartmentDao extends AbstractFisholaDao {

    // Department code derived from a commune INSEE code. Overseas (97x/98x) need
    // three characters; everything else two ("2A"/"2B" work with two).
    private static final String DEPT_CODE_EXPR =
            "(CASE WHEN %1$s LIKE '97%%' OR %1$s LIKE '98%%' "
                    + "THEN LEFT(%1$s, 3) ELSE LEFT(%1$s, 2) END)";

    // Departments present in the commune referential, with their commune count
    // (a cheap size indicator — no water_entity spatial join here). Names are
    // enriched in Java from the embedded referential.
    private static final String LIST_DEPARTMENTS_SQL = ""
            + "SELECT " + String.format(DEPT_CODE_EXPR, "insee_com") + " AS code, "
            + "       count(*) AS commune_count "
            + "FROM commune "
            + "GROUP BY 1 "
            + "ORDER BY 1";

    // FeatureCollection (built entirely by PostGIS) of the water entities whose
    // geometry intersects a commune of the requested department. One feature per
    // water_entity, carrying its simplified representative geometry — enough to
    // render and select a water body offline, without the fine river_section /
    // water_surface tiles (out of scope per Symbiome). The EXISTS keeps the join
    // index-friendly (GIST on commune.geom and water_entity.geom); geometry is
    // trimmed to ~0.1 m precision to bound the payload. Bind order: code, code.
    private static final String DEPARTMENT_HYDRO_GEOJSON_SQL = ""
            + "WITH we_in_dept AS ( "
            + "  SELECT we.id, we.name, we.kind, we.geom "
            + "  FROM water_entity we "
            + "  WHERE we.geom IS NOT NULL "
            + "    AND EXISTS ( "
            + "      SELECT 1 FROM commune c "
            + "      WHERE " + String.format(DEPT_CODE_EXPR, "c.insee_com") + " = ? "
            + "        AND ST_Intersects(we.geom, c.geom) "
            + "    ) "
            + ") "
            + "SELECT json_build_object( "
            + "  'type', 'FeatureCollection', "
            + "  'department', ?::text, "
            + "  'features', COALESCE(json_agg(json_build_object( "
            + "     'type', 'Feature', "
            + "     'id', f.id::text, "
            + "     'geometry', ST_AsGeoJSON(f.geom, 6)::json, "
            + "     'properties', json_build_object( "
            + "        'water_entity_id', f.id::text, "
            + "        'name', f.name, "
            + "        'kind', f.kind::text "
            + "     ) "
            + "  )), '[]'::json) "
            + ")::text AS pack "
            + "FROM we_in_dept f";

    /**
     * Departments present in the commune referential, ordered by code, each with
     * its human-readable name and commune count.
     */
    public List<DepartmentSummary> listDepartments() {
        return withContext(context -> context
                .fetch(LIST_DEPARTMENTS_SQL)
                .map(rec -> {
                    String code = rec.get("code", String.class);
                    return (DepartmentSummary) ImmutableDepartmentSummary.builder()
                            .code(code)
                            .name(Departments.nameOf(code))
                            .communeCount(rec.get("commune_count", Integer.class))
                            .build();
                }));
    }

    /**
     * Offline pack for a department: a GeoJSON FeatureCollection (raw JSON text)
     * of every water entity intersecting the department. Never null — an empty
     * FeatureCollection when the department carries no entity.
     *
     * @param code department code, expected already validated by the caller
     */
    public String departmentHydroGeoJson(String code) {
        return withContext(context -> {
            Record rec = context.fetchOne(DEPARTMENT_HYDRO_GEOJSON_SQL, code, code);
            String pack = rec == null ? null : rec.get("pack", String.class);
            return pack != null ? pack
                    : "{\"type\":\"FeatureCollection\",\"department\":\"" + code
                            + "\",\"features\":[]}";
        });
    }
}
