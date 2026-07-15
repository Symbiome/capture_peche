package fr.inrae.fishola.rest.hydro;

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

import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.HydroSearchDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;

/**
 * Vector tiles of the hydrographic network for MapLibre (#8). The rendered
 * geometry derives from open BD TOPO data, so the tiles are public (not
 * authenticated) and cacheable — MapLibre fetches them directly.
 */
@Path("/api/v1/tiles")
public class TileResource extends AbstractFisholaResource {

    private static final String MVT_MEDIA_TYPE = "application/vnd.mapbox-vector-tile";
    private static final int MAX_ZOOM = 22;
    // Below this zoom a single tile covers a huge area; rendering the whole
    // hydro network into one tile would be prohibitively expensive on a France-
    // scale table (and the endpoint is public). The layer only shows at detailed
    // zooms, so lower zooms are served an empty tile without touching the DB.
    private static final int MIN_ZOOM = 10;

    @Inject
    protected HydroSearchDao hydroSearchDao;

    @GET
    @Path("/hydro/{z}/{x}/{y}.pbf")
    @Produces(MVT_MEDIA_TYPE)
    public Response hydroTile(@PathParam("z") int z,
                             @PathParam("x") int x,
                             @PathParam("y") int y,
                             @HeaderParam("If-None-Match") String ifNoneMatch) {
        Preconditions.checkArgument(z >= 0 && z <= MAX_ZOOM,
                "z doit être dans l'intervalle [0, " + MAX_ZOOM + "].");
        long gridSize = 1L << z;
        Preconditions.checkArgument(x >= 0 && x < gridSize && y >= 0 && y < gridSize,
                "x/y hors des limites de la grille pour ce niveau de zoom.");

        byte[] tile = z < MIN_ZOOM ? new byte[0] : hydroSearchDao.getHydroTile(z, x, y);
        String etag = "\"" + Integer.toHexString(Arrays.hashCode(tile)) + "\"";
        if (etag.equals(ifNoneMatch)) {
            return Response.notModified(etag).build();
        }
        return Response.ok(tile)
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=86400")
                .build();
    }
}
