package fr.inrae.fishola.rest.trips;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.exceptions.AccessDeniedException;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/trips")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TripResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(TripResource.class);

    private static final Ordering<CatchBean> CATCH_ORDERING_ON_CAUGHT_AT = Ordering.natural()
            .nullsFirst()
            .onResultOf(c -> c.caughtAt.orElse(null));

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected ReferentialDao referentialDao;

    @Inject
    protected TripsDao tripsDao;

    @Inject
    protected CatchsDao catchsDao;

    @GET
    @Path("/")
    public PaginationResult<TripLight> getMyTrips(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie,
                                                  @QueryParam("pageNumber") int pageNumber,
                                                  @QueryParam("pageSize") int pageSize,
                                                  @QueryParam("desc") boolean desc,
                                                  @QueryParam("term") String term) {
        UUID userId = getUserId(cookie);
        PaginationParameter page = PaginationParameter.of(pageNumber, pageSize, "date", desc);
        Optional<String> searchTerm = Optional.ofNullable(StringUtils.trimToNull(term));
        PaginationResult<Trip> entities = tripsDao.listMyTrips(userId, page, searchTerm);
        PaginationResult<TripLight> result = entities.transform(this::toTripLight);
        return result;
    }

    @GET
    @Path("/count")
    public int countMyTrips(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        UUID userId = getUserId(cookie);
        int result = tripsDao.countMyTrips(userId);
        return result;
    }

    protected TripLight toTripLight(Trip trip) {

        UUID tripId = trip.getId();
        int catchsCount = catchsDao.countCatchs(tripId);

        LocalDate date = trip.getDay();
        LocalDateTime startTime = LocalDateTime.of(date, trip.getStartTime());
        LocalDateTime endTime = LocalDateTime.of(date, trip.getEndTime());
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

    protected boolean isStillModifiable(Trip trip, LocalDateTime now) {
        Optional<LocalDateTime> modifiableUntil = getModifiableUntil(trip, now);
        boolean stillModifiable = modifiableUntil.isPresent();
        return stillModifiable;
    }

    protected boolean isStillModifiable(Trip trip) {
        boolean stillModifiable = isStillModifiable(trip, LocalDateTime.now());
        return stillModifiable;
    }

    protected Optional<LocalDateTime> getModifiableUntil(Trip trip) {
        Optional<LocalDateTime> result = getModifiableUntil(trip, LocalDateTime.now());
        return result;
    }

    protected Optional<LocalDateTime> getModifiableUntil(Trip trip, LocalDateTime now) {
        LocalDateTime modifiableUntil = trip.getCreatedOn().plusHours(config.getTripModifiableHours());
        boolean canBeModified = modifiableUntil.isAfter(now);
        Optional<LocalDateTime> result = canBeModified ? Optional.of(modifiableUntil) : Optional.empty();
        return result;
    }

    @POST
    @Path("/")
    public Response createTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, TripBean trip) {

        Map<String, UUID> replacements = new HashMap<>();

        // TODO: 30/12/2019 Détection des doublons

        UUID userId = getUserId(cookie);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Nouvelle sortie à persister : id=%s", trip.id));
        }

        if (log.isTraceEnabled()) {
            log.trace(String.format("Détails de la sortie : %s", trip));
        }

        Trip entity = new Trip();
        entity.setCreatedOn(LocalDateTime.now());
        entity.setDay(trip.date);
        entity.setStartTime(LocalTime.parse(trip.startedAt));
        entity.setEndTime(LocalTime.parse(trip.finishedAt));
        entity.setLakeId(trip.lakeId);
        entity.setName(trip.name);
        entity.setType(trip.type);
        entity.setMode(trip.mode);
        entity.setOwnerId(userId);

        trip.weatherId.ifPresent(entity::setWeatherId);
        trip.beginLatitude.ifPresent(entity::setBeginLatitude);
        trip.beginLongitude.ifPresent(entity::setBeginLongitude);
        trip.endLatitude.ifPresent(entity::setEndLatitude);
        trip.endLongitude.ifPresent(entity::setEndLongitude);

        UUID tripId = tripsDao.create(entity);
        replacements.put(trip.id, tripId);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Sortie en cours de création : %s -> %s", trip.id, tripId));
        }

        Set<UUID> otherSpeciesIds = referentialDao.checkSpeciesOrCreateIfNecessary(trip.otherSpecies);
        Set<UUID> speciesIds = Sets.union(trip.speciesIds, otherSpeciesIds);

        int created = tripsDao.setSpecies(tripId, speciesIds);
        if (log.isDebugEnabled()) {
            log.debug(created + " espèce(s) recherchée(s) : " + speciesIds);
        }

        int techniquesCreated = tripsDao.setTechniques(tripId, trip.techniqueIds);
        if (log.isDebugEnabled()) {
            log.debug(techniquesCreated + " technique(s) utilisée(s) : " + trip.techniqueIds);
        }

        Collection<CatchBean> catchBeans = CollectionUtils.emptyIfNull(trip.catchs);
        for (CatchBean aCatch : catchBeans) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Détails de la capture : %s", aCatch));
            }

            UUID catchId = createCatch(tripId, aCatch);
            replacements.put(aCatch.id, catchId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Capture créée : %s -> %s", aCatch.id, catchId));
            }
        }

        Set<String> sampleIds = catchBeans.stream()
                .map(catchBean -> catchBean.sampleId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        if (!sampleIds.isEmpty()) {
            // On reçoit un échantillon, on incrémente l'identifiant de l'utilisateur
            usersDao.increaseSampleBaseId(userId);
        }

        URI uri = UriBuilder.fromPath("/api/v1/trips/" + tripId).build();
        if (log.isDebugEnabled()) {
            log.debug("URI de la sortie : " + uri);
        }

        Response response = Response.created(uri).entity(replacements).build();
        return response;
    }

    @PUT
    @Path("/{tripId}")
    public Response updateTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("tripId") UUID tripId, TripBean trip) {

        Map<String, UUID> replacements = new HashMap<>();

        UUID userId = getUserId(cookie);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Sortie à mettre à jour : id=%s", tripId));
        }

        if (log.isTraceEnabled()) {
            log.trace(String.format("Détails de la sortie : %s", trip));
        }

        Trip existingTrip = tripsDao.getTrip(UUID.fromString(trip.id));
        Preconditions.checkState(existingTrip != null, "Impossible de mettre à jour une sortie qui n'existe pas : " + tripId);
        AccessDeniedException.check(existingTrip.getOwnerId().equals(userId));

        boolean stillModifiable = isStillModifiable(existingTrip);
        if (!stillModifiable && trip.saveDelayMarker.isPresent()) {
            if (log.isWarnEnabled()) {
                log.warn("La sortie n'est plus modifiable. On vérifie si elle l'était à sa dernière sauvegarde");
            }
            stillModifiable = isStillModifiable(existingTrip, trip.saveDelayMarker.get());
        }
        AccessDeniedException.check(stillModifiable, "Il n'est plus possible de modifier la sortie");

        existingTrip.setDay(trip.date);
        existingTrip.setStartTime(LocalTime.parse(trip.startedAt));
        existingTrip.setEndTime(LocalTime.parse(trip.finishedAt));
        existingTrip.setLakeId(trip.lakeId);
        existingTrip.setName(trip.name);
        existingTrip.setType(trip.type);
        existingTrip.setMode(trip.mode);
        existingTrip.setOwnerId(userId);
        existingTrip.setWeatherId(trip.weatherId.orElse(null));

        // On ne met pas à jour les coordonnées de début/fin de sortie car ce n'est pas modifiable dans l'application

        tripsDao.updateTrip(existingTrip);

        if (log.isDebugEnabled()) {
            log.debug("Sortie mise à jour : " + tripId);
        }

        Set<UUID> otherSpeciesIds = referentialDao.checkSpeciesOrCreateIfNecessary(trip.otherSpecies);
        Set<UUID> speciesIds = Sets.union(trip.speciesIds, otherSpeciesIds);

        tripsDao.setSpecies(tripId, speciesIds);

        tripsDao.setTechniques(tripId, trip.techniqueIds);

        List<Catch> existingCatchs = catchsDao.listCatchs(tripId);
        ImmutableMap<UUID, Catch> existingCatchsIndex = Maps.uniqueIndex(existingCatchs, Catch::getId);
        Set<String> existingSampleIds = existingCatchs.stream()
                .map(Catch::getSampleId)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());

        Set<UUID> updatedCatchsIds = new LinkedHashSet<>();
        Collection<CatchBean> incomingCatchBeans = CollectionUtils.emptyIfNull(trip.catchs);
        for (CatchBean aCatch : incomingCatchBeans) {

            if (log.isTraceEnabled()) {
                log.trace(String.format("Détails de la capture : %s", aCatch));
            }

            Optional<UUID> parsedCatchId = tryToParseUUID(aCatch.id);
            if (parsedCatchId.isPresent() && existingCatchsIndex.containsKey(parsedCatchId.get())) {
                Catch existingCatch = existingCatchsIndex.get(parsedCatchId.get());
                updateCatch(existingCatch, aCatch);
                updatedCatchsIds.add(parsedCatchId.get());
                if (log.isDebugEnabled()) {
                    log.debug("Capture mise à jour : " + parsedCatchId.get());
                }
            } else {
                UUID catchId = createCatch(tripId, aCatch);
                replacements.put(aCatch.id, catchId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Capture créée : %s -> %s", aCatch.id, catchId));
                }
            }
        }

        Sets.SetView<UUID> toDeleteCatchsIds = Sets.difference(existingCatchsIndex.keySet(), updatedCatchsIds);
        toDeleteCatchsIds.forEach(catchId -> {
            catchsDao.delete(catchId);
            if (log.isDebugEnabled()) {
                log.debug("Capture supprimée : " + catchId);
            }
        });

        Set<String> incomingSampleIds = incomingCatchBeans.stream()
                .map(catchBean -> catchBean.sampleId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        Set<String> newSampleIds = Sets.difference(incomingSampleIds, existingSampleIds);
        if (!newSampleIds.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("Nouveaux échantillons: " + newSampleIds);
            }
            // On reçoit un échantillon, on incrémente l'identifiant de l'utilisateur
            usersDao.increaseSampleBaseId(userId);
        }

        Response response = Response.ok(replacements).build();
        return response;
    }

    @DELETE
    @Path("/{tripId}")
    public void deleteTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("tripId") UUID tripId) {

        UUID userId = getUserId(cookie);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Suppression de la sortie : id=%s", tripId));
        }

        deleteTrip(tripId, userId);
    }

    protected void deleteTrip(UUID tripId, UUID userId) {
        Trip existingTrip = tripsDao.getTrip(tripId);
        Preconditions.checkState(existingTrip != null, "Impossible de supprimer une sortie qui n'existe pas : " + tripId);
        AccessDeniedException.check(existingTrip.getOwnerId().equals(userId));

        boolean stillModifiable = isStillModifiable(existingTrip);
        if (stillModifiable) {
            tripsDao.delete(tripId);
            if (log.isDebugEnabled()) {
                log.debug("Sortie supprimée : " + tripId);
            }
        } else {
            tripsDao.hide(tripId);
            if (log.isDebugEnabled()) {
                log.debug("Sortie masquée : " + tripId);
            }
        }
    }

    @DELETE
    @Path("/")
    public void deleteTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, List<UUID> tripIds) {

        UUID userId = getUserId(cookie);

        for (UUID tripId : tripIds) {
            deleteTrip(tripId, userId);
        }
    }

    protected UUID createCatch(UUID tripId, CatchBean aCatch) {
        Catch catchPojo = new Catch();
        catchPojo.setTripId(tripId);
        catchPojo.setCreatedOn(LocalDateTime.now());
        aCatch.caughtAt.map(LocalTime::parse).ifPresent(catchPojo::setCatchTime);
        UUID speciesId = checkSpeciesOrCreateIfNecessary(aCatch.speciesId, aCatch.otherSpecies);
        catchPojo.setSpeciesId(speciesId);
        catchPojo.setTechniqueId(aCatch.techniqueId);
        aCatch.size.ifPresent(catchPojo::setSize);
        aCatch.weight.ifPresent(catchPojo::setWeight);
        catchPojo.setKept(aCatch.keep);
        if (!aCatch.keep) {
            aCatch.releasedStateId.ifPresent(catchPojo::setReleasedFishStateId);
        }
        aCatch.description.ifPresent(catchPojo::setDescription);
        aCatch.latitude.ifPresent(catchPojo::setLatitude);
        aCatch.longitude.ifPresent(catchPojo::setLongitude);
        aCatch.sampleId.ifPresent(catchPojo::setSampleId);

        UUID catchId = catchsDao.create(catchPojo);
        return catchId;
    }

    protected void updateCatch(Catch existingCatch, CatchBean aCatch) {

        existingCatch.setCatchTime(aCatch.caughtAt.map(LocalTime::parse).orElse(null));
        UUID speciesId = checkSpeciesOrCreateIfNecessary(aCatch.speciesId, aCatch.otherSpecies);
        existingCatch.setSpeciesId(speciesId);
        existingCatch.setTechniqueId(aCatch.techniqueId);
        existingCatch.setSize(aCatch.size.orElse(null));
        existingCatch.setWeight(aCatch.weight.orElse(null));
        existingCatch.setKept(aCatch.keep);
        existingCatch.setReleasedFishStateId(!aCatch.keep ? aCatch.releasedStateId.orElse(null) : null);
        existingCatch.setDescription(aCatch.description.map(StringUtils::trimToNull).orElse(null));
        existingCatch.setSampleId(aCatch.sampleId.orElse(null));

        catchsDao.update(existingCatch);

    }

    protected Optional<UUID> tryToParseUUID(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return Optional.of(uuid);
        } catch (Exception eee) {
            return Optional.empty();
        }
    }

    @GET
    @Path("/{tripId}")
    public TripBean getTrip(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("tripId") UUID tripId) {

        UUID userId = getUserId(cookie);
        Trip entity = tripsDao.getTrip(tripId);

        AccessDeniedException.check(userId.equals(entity.getOwnerId()), "Vous ne pouvez consulter que les sorties vous appartenant");

        TripBean result = new TripBean();
        result.createdOn = Optional.ofNullable(entity.getCreatedOn());
        result.id = entity.getId().toString();
        result.name = entity.getName();
        result.mode = entity.getMode();
        result.type = entity.getType();
        result.lakeId = entity.getLakeId();
        result.date = entity.getDay();
        result.startedAt = entity.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        result.finishedAt = entity.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        result.weatherId = Optional.ofNullable(entity.getWeatherId());

        result.speciesIds = tripsDao.getTripSpecies(tripId);
        result.techniqueIds = tripsDao.getTripTechniques(tripId);

        result.modifiableUntil = getModifiableUntil(entity);

        List<Catch> catchs = catchsDao.listCatchs(tripId);
        Set<UUID> catchIds = catchs.stream()
                .map(Catch::getId)
                .collect(Collectors.toSet());
        Set<UUID> catchsWithPictures = catchsDao.checkForPictures(catchIds);
        result.catchs = catchs.stream()
                .map(aCatch -> toCatchBean(aCatch, catchsWithPictures))
                .sorted(CATCH_ORDERING_ON_CAUGHT_AT)
                .collect(Collectors.toList());

//        Set<UUID> allSpeciesIds = new HashSet<>(result.speciesIds);
//        result.catchs
//                .stream()
//                .map(c -> c.speciesId)
//                .forEach(allSpeciesIds::add);
//        result.customSpecies = referentialDao.customSpeciesIndex(allSpeciesIds);

        return result;
    }

    protected UUID checkSpeciesOrCreateIfNecessary(Optional<UUID> speciesId, Optional<String> otherSpecies) {
        Preconditions.checkArgument(speciesId.isPresent() || otherSpecies.isPresent(), "Il faut au moins une espèce");
        if (speciesId.isPresent()) {
            return speciesId.get();
        }
        Set<UUID> uuidSet = referentialDao.checkSpeciesOrCreateIfNecessary(otherSpecies.get());
        Preconditions.checkArgument(uuidSet.size() == 1, "Il faut exactement 1 espèce : " + otherSpecies.get());
        UUID result = uuidSet.iterator().next();
        return result;
    }

    public static CatchBean toCatchBean(Catch aCatch, Set<UUID> catchsWithPictures) {
        CatchBean result = new CatchBean();
        result.tripId = Optional.of(aCatch.getTripId());
        UUID catchId = aCatch.getId();
        result.id = catchId.toString();
        result.speciesId = Optional.of(aCatch.getSpeciesId());
        result.size = Optional.ofNullable(aCatch.getSize());
        result.weight = Optional.ofNullable(aCatch.getWeight());
        result.keep = aCatch.getKept();
        result.releasedStateId = Optional.ofNullable(aCatch.getReleasedFishStateId());
        result.techniqueId = aCatch.getTechniqueId();
        result.description = Optional.ofNullable(aCatch.getDescription());
        result.caughtAt = Optional.ofNullable(aCatch.getCatchTime()).map(t -> t.format(DateTimeFormatter.ofPattern("HH:mm")));
        result.latitude = Optional.ofNullable(aCatch.getLatitude());
        result.longitude = Optional.ofNullable(aCatch.getLongitude());
        result.hasPicture = catchsWithPictures.contains(catchId);
        result.sampleId = Optional.ofNullable(aCatch.getSampleId());
        return result;
    }

}
