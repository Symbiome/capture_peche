package fr.inrae.fishola.rest.commune;

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

/**
 * Commune referential endpoints (IGN ADMIN EXPRESS). Supports searching a
 * fishing spot "by commune" (note Q1).
 */
@Path("/api/v1/communes")
@Produces(MediaType.APPLICATION_JSON)
public class CommuneResource extends AbstractFisholaResource {

    private static final int MAX_LIMIT = 50;

    @Inject
    protected HydroSearchDao hydroSearchDao;

    /**
     * Communes matching the query, best trigram match first. Authenticated.
     */
    @GET
    @Path("/search")
    public Response search(@QueryParam("q") String q,
                           @QueryParam("limit") @DefaultValue("10") int limit) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();

        Preconditions.checkArgument(q != null && q.trim().length() >= 2,
                "Le paramètre q doit contenir au moins 2 caractères.");
        Preconditions.checkArgument(limit > 0 && limit <= MAX_LIMIT,
                "limit doit être dans l'intervalle [1, " + MAX_LIMIT + "].");

        List<CommuneResult> result = hydroSearchDao.searchCommunes(q.trim(), limit);
        return wrapEntity(result, userIdAndRenewal);
    }
}
