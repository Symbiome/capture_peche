package fr.inrae.fishola.rest.social;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.TripSocialReaction;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/social")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SocialResource extends AbstractFisholaResource {

    @Inject
    protected ReferentialDao referentialDao;

    @Inject
    protected TripsDao tripsDao;

    @Inject
    protected CatchsDao catchsDao;

    @GET
    @Path("/")
    public Response getTripsAroundMe(@QueryParam("lakeId") String lakeId,
                                     @QueryParam("pageNumber") int pageNumber,
                                     @QueryParam("pageSize") int pageSize) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<List<UUID>> lakesFilter = Optional.empty();
        if (lakeId != null && !lakeId.isEmpty()) {
            lakesFilter = Optional.of(Arrays.asList(UUID.fromString(lakeId)));
        }
        PaginationParameter page = PaginationParameter.of(pageNumber, pageSize, "date", true);
        PaginationResult<TripSocial> entities = tripsDao.socialTrips(userId, lakesFilter, page);
        Response response = wrapEntity(entities, userIdAndRenewal);
        return response;
    }

    @POST
    @Path("/{tripId}")
    public Response reactToTrip(@PathParam("tripId") UUID tripId, TripSocialReaction reaction) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        reaction.setUserId(userId);
        tripsDao.insertSocialReaction(reaction);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{tripId}")
    public Response deleteTripReaction(@PathParam("tripId") UUID tripId) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        tripsDao.deleteSocialReaction(userId, tripId);
        return Response.ok().build();
    }
}
