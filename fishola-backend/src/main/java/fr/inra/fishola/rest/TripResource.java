package fr.inra.fishola.rest;

import fr.inra.fishola.FisholaConfiguration;
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
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/api/v1/trips")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TripResource {

    @Inject
    protected FisholaConfiguration config;

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
                .map(this::toTripLight)
                .collect(Collectors.toList());
        return result;
    }

    protected TripLight toTripLight(Trip trip) {
        Function<Time, Optional<LocalTime>> toLocalTime = timestamp -> Optional.ofNullable(timestamp)
                .map(Time::toLocalTime);

        ImmutableTripLight.Builder builder = ImmutableTripLight.builder()
                .catchsCount(0)
                .date(trip.getDay().toLocalDate())
                .id(trip.getId())
                .lakeId(trip.getLake())
                .name(trip.getName())
                .startedAt(toLocalTime.apply(trip.getStartTime()))
                .finishedAt(toLocalTime.apply(trip.getEndTime()));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(trip.getCreatedOn());
        calendar.add(Calendar.HOUR, config.getTripModifiableHours());
        Date modifiableUntil = calendar.getTime();
        boolean canBeModified = modifiableUntil.after(new Date());
        if (canBeModified) {
            builder.modifiableUntil(modifiableUntil);
        }

        TripLight result = builder.build();
        return result;
    }

    @PUT
    @Path("/")
    public void createTrip(@CookieParam(AuthenticationService.AUTHENTICATION_COOKIE_NAME) Cookie cookie, TripBean trip) {

        // TODO: 30/12/2019 Détection des doublons

        UUID userId = authenticationService.getUserId(cookie);

        Trip entity = new Trip();
        entity.setCreatedOn(Timestamp.from(Instant.now()));
        entity.setDay(new java.sql.Date(trip.date.getTime()));
        entity.setStartTime(Time.valueOf(LocalTime.ofInstant(trip.startedAt.toInstant(), ZoneId.systemDefault())));
        entity.setEndTime(Time.valueOf(LocalTime.ofInstant(trip.finishedAt.toInstant(), ZoneId.systemDefault())));
        entity.setLake(trip.lakeId);
        entity.setName(trip.name);
        entity.setType(trip.type);
        entity.setMode(trip.mode);
        entity.setOwner(userId);
        entity.setWeather(trip.weatherId);

        UUID tripId = tripsDao.create(entity);

        int created = tripsDao.setSpecies(tripId, trip.speciesIds);
        System.out.println("Espèces recherchées: " + created);

        // TODO: 02/01/2020 Catchs

    }

}
