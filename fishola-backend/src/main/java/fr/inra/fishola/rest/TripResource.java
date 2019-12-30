package fr.inra.fishola.rest;

import fr.inra.fishola.database.TripsDao;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/trips")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TripResource {

    @Inject
    protected AuthenticationService authenticationService;

    @Inject
    protected TripsDao tripsDao;

    @GET
    @Path("/")
    public List<TripLight> getMyTrips(@CookieParam(AuthenticationService.AUTHENTICATION_COOKIE_NAME) Cookie cookie) {

        UUID userId = authenticationService.getUserId(cookie);

        List<Trip> entities = tripsDao.listMyTrips(userId);
        List<TripLight> result = entities.stream()
                .map(trip -> ImmutableTripLight.builder()
                        .catchsCount(0)
                        .date(trip.getDay())
                        .id(trip.getId())
                        .lakeId(trip.getLake())
                        .name(trip.getName())
                        .startedAt(trip.getStartTime())
                        .finishedAt(trip.getEndTime())
                        .canBeModified(true)
                        .build())
                .collect(Collectors.toList());
        return result;
    }

    @PUT
    @Path("/")
    public void createTrip(@CookieParam(AuthenticationService.AUTHENTICATION_COOKIE_NAME) Cookie cookie, TripBean trip) {

        // TODO: 30/12/2019 Détection des doublons

        UUID userId = authenticationService.getUserId(cookie);

        Trip entity = new Trip();
        entity.setDay(new java.sql.Date(trip.date.getTime()));
        entity.setStartTime(Timestamp.from(trip.startedAt.toInstant()));
        entity.setEndTime(Timestamp.from(trip.finishedAt.toInstant()));
        entity.setLake(trip.lakeId);
        entity.setName(trip.name);
        entity.setType(trip.type);
        entity.setMode(trip.mode);
        entity.setOwner(userId);
        entity.setWeather(trip.weatherId);

        tripsDao.create(entity);
    }

}
