package fr.inra.fishola.rest;

import fr.inra.fishola.database.TripDao;
import fr.inra.fishola.entities.tables.pojos.Trip;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/trips")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TripResource {

    @Inject
    protected AuthenticationService authenticationService;

    @Inject
    protected TripDao tripDao;

    @GET
    @Path("/")
    public List<TripLight> getMyTrips() {
        ImmutableTripLight aTrip = ImmutableTripLight.builder()
                .catchs(12)
                .date(new Date())
                .id(UUID.randomUUID().toString())
                .lakeName("Toto")
                .name("azerty")
                .canBeModified(false)
                .build();
        LinkedList<TripLight> result = new LinkedList<>();
        result.add(aTrip);
        return result;
    }

    @PUT
    @Path("/")
    public void createTrip(@CookieParam(AuthenticationService.AUTHENTICATION_COOKIE_NAME) Cookie cookie, TripBean trip) {

        UUID userId = authenticationService.getUserId(cookie);

        System.out.println(trip);

        Trip entity = new Trip();
        entity.setDay(new java.sql.Date(trip.date.getTime()));
        entity.setStartTime(Timestamp.from(trip.startedAt.toInstant()));
        entity.setEndTime(Timestamp.from(trip.finishedAt.toInstant()));
        entity.setLake(trip.lakeId);
        entity.setName(trip.name);
        entity.setType(trip.type);
        entity.setMode(trip.mode);
        entity.setOwner(userId);

        tripDao.create(entity);
    }

}
