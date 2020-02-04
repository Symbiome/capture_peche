package fr.inra.fishola.rest.trips;

import com.google.common.base.Preconditions;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.database.CatchsDao;
import fr.inra.fishola.database.TripsDao;
import fr.inra.fishola.entities.tables.pojos.Catch;
import fr.inra.fishola.entities.tables.pojos.Trip;
import fr.inra.fishola.exceptions.AccessDeniedException;
import fr.inra.fishola.rest.AbstractFisholaResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/trips")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TripResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(TripResource.class);

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected TripsDao tripsDao;

    @Inject
    protected CatchsDao catchsDao;

    @GET
    @Path("/")
    public PaginationResult<TripLight> getMyTrips(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        PaginationResult<TripLight>  result = getMyTrips(cookie, PaginationParameter.ALL);
        return result;
    }

    @POST
    @Path("/")
    public PaginationResult<TripLight> getMyTrips(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, PaginationParameter page) {

        UUID userId = getUserId(cookie);

        PaginationResult<Trip> entities = tripsDao.listMyTrips(userId, page);
        PaginationResult<TripLight> result = entities.transform(this::toTripLight);
        return result;
    }

    protected TripLight toTripLight(Trip trip) {

        UUID tripId = trip.getId();
        int catchsCount = catchsDao.countCatchs(tripId);

        LocalDate date = trip.getDay().toLocalDate();
        LocalDateTime startTime = LocalDateTime.of(date, trip.getStartTime().toLocalTime());
        LocalDateTime endTime = LocalDateTime.of(date, trip.getEndTime().toLocalTime());
        if (endTime.isBefore(startTime)) {
            endTime = endTime.plusDays(1);
        }
        long durationInSeconds = Duration.between(startTime, endTime).toSeconds();

        ImmutableTripLight.Builder builder = ImmutableTripLight.builder()
                .catchsCount(catchsCount)
                .date(date)
                .id(tripId)
                .lakeId(trip.getLakeId())
                .name(trip.getName())
                .durationInSeconds(durationInSeconds);

        boolean stillModifiable = isStillModifiable(trip);
        builder.modifiable(stillModifiable);

        TripLight result = builder.build();
        return result;
    }

    protected boolean isStillModifiable(Trip trip) {
        Optional<Date> modifiableUntil = getModifiableUntil(trip);
        boolean stillModifiable = modifiableUntil.isPresent();
        return stillModifiable;
    }

    protected Optional<Date> getModifiableUntil(Trip trip) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(trip.getCreatedOn());
        calendar.add(Calendar.HOUR, config.getTripModifiableHours());
        Date modifiableUntil = calendar.getTime();
        boolean canBeModified = modifiableUntil.after(new Date());
        Optional<Date> result = canBeModified ? Optional.of(modifiableUntil) : Optional.empty();
        return result;
    }

    @PUT
    @Path("/")
    public void createTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, TripBean trip) {

        // TODO: 30/12/2019 Détection des doublons
        // TODO: 03/02/2020 Transaction !

        UUID userId = getUserId(cookie);

        Trip entity = new Trip();
        entity.setCreatedOn(Timestamp.from(Instant.now()));
        entity.setDay(new java.sql.Date(trip.date.getTime()));
        entity.setStartTime(Time.valueOf(LocalTime.ofInstant(trip.startedAt.toInstant(), ZoneId.systemDefault())));
        entity.setEndTime(Time.valueOf(LocalTime.ofInstant(trip.finishedAt.toInstant(), ZoneId.systemDefault())));
        entity.setLakeId(trip.lakeId);
        entity.setName(trip.name);
        entity.setType(trip.type);
        entity.setMode(trip.mode);
        entity.setOwnerId(userId);
        entity.setWeatherId(trip.weatherId);

        UUID tripId = tripsDao.create(entity);
        if (log.isDebugEnabled()) {
            log.debug("Trip créé avec l'ID :" + tripId);
        }

        int created = tripsDao.setSpecies(tripId, trip.speciesIds);
        if (log.isDebugEnabled()) {
            log.debug(created + " espèce(s) recherchée(s)");
        }

        // TODO: 02/01/2020 Catchs
        for (CatchBean aCatch : trip.catchs) {
            Catch catchPojo = new Catch();
            catchPojo.setTripId(tripId);
            catchPojo.setCreatedOn(Timestamp.from(Instant.now()));
            aCatch.caughtAt.ifPresent(caughtAt -> catchPojo.setCatchTime(Time.valueOf(LocalTime.ofInstant(caughtAt.toInstant(), ZoneId.systemDefault()))));
            catchPojo.setSpeciesId(aCatch.speciesId);
            catchPojo.setTechniqueId(aCatch.techniqueId);
            catchPojo.setSize(aCatch.size);
            aCatch.weight.ifPresent(catchPojo::setWeight);
            catchPojo.setKept(aCatch.keep);
            if (!aCatch.keep) {
                Preconditions.checkState(aCatch.releasedStateId.isPresent(), "On ne garde pas le poisson, il faut préciser son état");
                catchPojo.setReleasedFishStateId(aCatch.releasedStateId.get());
            }
            aCatch.description.ifPresent(catchPojo::setDescription);

            UUID catchId = catchsDao.create(catchPojo);
            if (log.isDebugEnabled()) {
                log.debug("Catch créé avec l'ID :" + catchId);
            }
        }

    }

    @POST
    @Path("/{tripId}")
    public void updateTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("tripId") UUID tripId, TripBean trip) {

        UUID userId = getUserId(cookie);

        Trip existingTrip = tripsDao.getTrip(UUID.fromString(trip.id));
        Preconditions.checkState(existingTrip != null);
        AccessDeniedException.check(existingTrip.getOwnerId().equals(userId));

        AccessDeniedException.check(isStillModifiable(existingTrip), "Il n'est plus possible de modifier la sortie");

        // TODO: 13/01/2020 Implement ...

        existingTrip.setDay(new java.sql.Date(trip.date.getTime()));
        existingTrip.setStartTime(Time.valueOf(LocalTime.ofInstant(trip.startedAt.toInstant(), ZoneId.systemDefault())));
        existingTrip.setEndTime(Time.valueOf(LocalTime.ofInstant(trip.finishedAt.toInstant(), ZoneId.systemDefault())));
        existingTrip.setLakeId(trip.lakeId);
        existingTrip.setName(trip.name);
        existingTrip.setType(trip.type);
        existingTrip.setMode(trip.mode);
        existingTrip.setOwnerId(userId);
        existingTrip.setWeatherId(trip.weatherId);

        tripsDao.updateTrip(existingTrip);

        tripsDao.setSpecies(tripId, trip.speciesIds);

        // TODO: 02/01/2020 Catchs
    }


    @GET
    @Path("/{tripId}")
    public TripBean getTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("tripId") UUID tripId) {

        UUID userId = getUserId(cookie);
        Trip entity = tripsDao.getTrip(tripId);

        AccessDeniedException.check(userId.equals(entity.getOwnerId()), "Vous ne pouvez consulter que les sorties vous appartenant");

        TripBean result = new TripBean();
        result.createdOn = Optional.ofNullable(entity.getCreatedOn())
                .map(Date::toInstant)
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
        result.id = entity.getId().toString();
        result.name = entity.getName();
        result.mode = entity.getMode();
        result.type = entity.getType();
        result.lakeId = entity.getLakeId();
        result.date = entity.getDay();
        result.startedAt = entity.getStartTime();
        result.finishedAt = entity.getEndTime();
        result.weatherId = entity.getWeatherId();

        result.speciesIds = tripsDao.getTripSpecies(tripId);

        result.modifiableUntil = getModifiableUntil(entity)
                .map(Date::toInstant)
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));

        List<Catch> catchs = catchsDao.listCatchs(tripId);
        result.catchs = catchs.stream()
                .map(this::toCatchBean)
                .collect(Collectors.toList());

        return result;
    }

    private CatchBean toCatchBean(Catch aCatch) {
        CatchBean result = new CatchBean();
        result.id = aCatch.getId().toString();
        result.speciesId = aCatch.getSpeciesId();
        result.size = aCatch.getSize();
        result.weight = Optional.ofNullable(aCatch.getWeight());
        result.keep = aCatch.getKept();
        result.releasedStateId = Optional.ofNullable(aCatch.getReleasedFishStateId());
        result.techniqueId = aCatch.getTechniqueId();
        result.description = Optional.ofNullable(aCatch.getDescription());
        result.caughtAt = Optional.ofNullable(aCatch.getCatchTime());
        return result;
    }

}
