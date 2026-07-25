package fr.inrae.fishola.rest.department;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.DepartmentDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Department referential + offline packs (#54, "download a department" à la
 * Komoot). Symbiome scoped this to the <em>hydro entities</em> of a department
 * only — no basemap tiles. The pack is a plain spatial slice of the public hydro
 * referential, so this stays decoupled from the staff territorial partitioning
 * still under arbitration (#63).
 */
@Path("/api/v1/departments")
@Produces(MediaType.APPLICATION_JSON)
public class DepartmentResource extends AbstractFisholaResource {

    @Inject
    protected DepartmentDao departmentDao;

    @Inject
    protected ObjectMapper objectMapper;

    /**
     * Departments available for an offline pack (code, name, commune count).
     * Authenticated (no personal data, but avoids anonymous referential
     * scraping — same rationale as the hydro endpoints).
     */
    @GET
    public Response list() {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        List<DepartmentSummary> departments = departmentDao.listDepartments();
        return wrapEntity(departments, userIdAndRenewal);
    }

    /**
     * Offline pack for a department: a GeoJSON FeatureCollection of its water
     * entities (id + name + kind + simplified geometry). The client stores it
     * and renders it as a local source when the vector tiles are unreachable.
     * Authenticated. 400 on an unknown department code.
     */
    @GET
    @Path("/{code}/hydro")
    public Response hydroPack(@PathParam("code") String code) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();

        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        Preconditions.checkArgument(Departments.isValidCode(normalized),
                "Code département inconnu : " + code);

        String geoJson = departmentDao.departmentHydroGeoJson(normalized);
        // Parse the DB-built JSON into a tree so Jackson streams it as-is (a raw
        // String entity would be re-quoted under application/json).
        JsonNode pack;
        try {
            pack = objectMapper.readTree(geoJson);
        } catch (IOException e) {
            // The payload is produced by PostGIS json_build_object → always valid
            // JSON; a failure here is a server-side defect, not a client error.
            throw new IllegalStateException("Pack GeoJSON illisible pour " + normalized, e);
        }
        return wrapEntity(pack, userIdAndRenewal);
    }
}
