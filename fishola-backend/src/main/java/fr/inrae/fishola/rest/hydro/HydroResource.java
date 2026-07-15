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
import fr.inrae.fishola.rest.UserIdAndRenewal;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Spatial endpoints over the hydrographic network (water entities, river
 * sections, water surfaces). First spatial API of the backend — underpins the
 * "water bodies around me" user story (note Q1) and the trip hydrographic
 * validation (#9).
 */
@Path("/api/v1/waterEntities")
@Produces(MediaType.APPLICATION_JSON)
public class HydroResource extends AbstractFisholaResource {

    // Defaults / bounds mirrored in the parameter validation below.
    private static final double MAX_RADIUS_M = 20000.0;
    private static final int MAX_PAGE_SIZE = 100;
    // Bounds pageNumber so pageNumber * pageSize cannot overflow int (which would
    // yield a negative OFFSET rejected by PostgreSQL → 500 instead of 400).
    private static final int MAX_PAGE_NUMBER = 10000;

    @Inject
    protected HydroSearchDao hydroSearchDao;

    /**
     * Water entities near a point, ordered by distance. Authenticated (no
     * personal data, but avoids anonymous scraping of the referential).
     */
    @GET
    @Path("/nearby")
    public Response nearby(@QueryParam("lat") Double lat,
                           @QueryParam("lng") Double lng,
                           @QueryParam("radiusM") @DefaultValue("2000") double radiusM,
                           @QueryParam("kind") String kind,
                           @QueryParam("pageNumber") @DefaultValue("0") int pageNumber,
                           @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();

        Preconditions.checkArgument(lat != null && lng != null,
                "Les paramètres lat et lng sont obligatoires.");
        Preconditions.checkArgument(lat >= -90 && lat <= 90,
                "lat doit être compris entre -90 et 90.");
        Preconditions.checkArgument(lng >= -180 && lng <= 180,
                "lng doit être compris entre -180 et 180.");
        Preconditions.checkArgument(radiusM > 0 && radiusM <= MAX_RADIUS_M,
                "radiusM doit être dans l'intervalle ]0, " + (long) MAX_RADIUS_M + "].");
        Preconditions.checkArgument(pageNumber >= 0 && pageNumber <= MAX_PAGE_NUMBER,
                "pageNumber doit être dans l'intervalle [0, " + MAX_PAGE_NUMBER + "].");
        Preconditions.checkArgument(pageSize > 0 && pageSize <= MAX_PAGE_SIZE,
                "pageSize doit être dans l'intervalle [1, " + MAX_PAGE_SIZE + "].");

        Optional<String> kindFilter = Optional.ofNullable(kind)
                .map(k -> k.trim().toUpperCase(Locale.ROOT))
                .filter(k -> !k.isEmpty());
        kindFilter.ifPresent(k -> Preconditions.checkArgument(
                k.equals("STILL") || k.equals("FLOWING"),
                "kind doit valoir STILL ou FLOWING."));

        int offset = pageNumber * pageSize;
        List<NearbyWaterEntity> result =
                hydroSearchDao.nearby(lat, lng, radiusM, kindFilter, pageSize, offset);
        return wrapEntity(result, userIdAndRenewal);
    }

    /**
     * Water entities in (or within {@code bufferM} of) a commune, ordered by
     * distance to the commune centroid. Authenticated.
     */
    @GET
    @Path("/byCommune")
    public Response byCommune(@QueryParam("insee") String insee,
                             @QueryParam("bufferM") @DefaultValue("500") double bufferM) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();

        Preconditions.checkArgument(insee != null && !insee.isBlank(),
                "Le paramètre insee est obligatoire.");
        Preconditions.checkArgument(bufferM >= 0 && bufferM <= MAX_RADIUS_M,
                "bufferM doit être dans l'intervalle [0, " + (long) MAX_RADIUS_M + "].");

        List<NearbyWaterEntity> result =
                hydroSearchDao.findWaterEntitiesByCommune(insee.trim(), bufferM);
        return wrapEntity(result, userIdAndRenewal);
    }
}
