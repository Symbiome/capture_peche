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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.entities.tables.pojos.TripSocialReaction;
import fr.inrae.fishola.exceptions.AccessDeniedException;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import fr.inrae.fishola.rest.trips.CatchBean;
import fr.inrae.fishola.rest.trips.CatchMarker;
import fr.inrae.fishola.rest.trips.ImmutableTripLight;
import fr.inrae.fishola.rest.trips.PaginatedExportBean;
import fr.inrae.fishola.rest.trips.TripBean;
import fr.inrae.fishola.rest.trips.TripLight;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
