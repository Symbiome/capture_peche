package fr.inrae.fishola.rest.trips;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import com.google.common.collect.ImmutableSet;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.JwtHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Sorties de plusieurs jours (#237) via l'API pêcheur : date de fin et date de capture,
 * rétrocompatibilité des applis qui n'envoient qu'une date, refus des captures hors sortie.
 */
@QuarkusTest
class MultiDayTripTest extends AbstractFisholaTest {

    private static final LocalDate THREE_DAYS_AGO = LocalDate.now().minusDays(3);
    private static final LocalDate TWO_DAYS_AGO = LocalDate.now().minusDays(2);
    private static final LocalDate YESTERDAY = LocalDate.now().minusDays(1);
    private static final TypeRef<Map<String, String>> REPLACEMENTS = new TypeRef<>() {};

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;
    @Inject
    protected CatchsDao catchsDao;
    @Inject
    protected JwtHelper jwtHelper;

    protected UUID waterEntityId;
    protected UUID speciesId;
    protected UUID techniqueId;
    protected String token;

    @BeforeEach
    @Transactional
    void loadReferentials() {
        this.waterEntityId = referentialDao.listWaterEntities().iterator().next().getId();
        this.speciesId = referentialDao.listBuiltInSpecies().iterator().next().getId();
        this.techniqueId = referentialDao.listBuiltInTechniques().iterator().next().getId();
    }

    @BeforeEach
    void login() {
        this.token = login("thimel@codelutin.com", "sispea");
    }

    /** Sortie de carpe commencée il y a trois jours à 18:00 ; {@code endDate} nul = format d'avant #237. */
    private TripBean carpTrip(LocalDate endDate, String finishedAt, CatchBean... catchs) {
        TripBean trip = new TripBean();
        trip.id = "multi-day";
        trip.date = THREE_DAYS_AGO;
        trip.startedAt = "18:00";
        trip.endDate = Optional.ofNullable(endDate);
        trip.finishedAt = finishedAt;
        trip.waterEntityId = waterEntityId;
        trip.name = "Session carpe";
        trip.type = TripType.Border;
        trip.mode = TripMode.Afterwards;
        trip.speciesIds = ImmutableSet.of(speciesId);
        trip.techniqueIds = ImmutableSet.of(techniqueId);
        trip.catchs = List.of(catchs);
        return trip;
    }

    private CatchBean catchAt(String id, LocalDate caughtOn, String caughtAt) {
        CatchBean aCatch = new CatchBean();
        aCatch.id = id;
        aCatch.speciesId = Optional.of(speciesId.toString());
        aCatch.techniqueId = techniqueId;
        aCatch.keep = false;
        aCatch.size = Optional.of(60);
        aCatch.caughtOn = Optional.ofNullable(caughtOn);
        aCatch.caughtAt = Optional.of(caughtAt);
        return aCatch;
    }

    private ValidatableResponse post(TripBean trip) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .when().post("/api/v1/trips")
                .then();
    }

    private UUID createAndGetId(TripBean trip) {
        String id = post(trip).statusCode(201).extract().path(trip.id);
        return UUID.fromString(id);
    }

    @Transactional
    protected Trip getTrip(UUID tripId) {
        return tripsDao.getTrip(tripId);
    }

    @Transactional
    protected Map<String, LocalDateTime> catchTimestampsByOriginalId(UUID tripId, Map<String, UUID> replacements) {
        Set<UUID> ids = Set.copyOf(replacements.values());
        Map<UUID, LocalDateTime> byId = catchsDao.listCatchs(tripId).stream()
                .filter(c -> ids.contains(c.getId()))
                .collect(Collectors.toMap(Catch::getId, Catch::getCatchTimestamp));
        return replacements.entrySet().stream()
                .filter(e -> byId.containsKey(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> byId.get(e.getValue())));
    }

    @Transactional
    protected int countTrips() {
        return tripsDao.countMyTrips(jwtHelper.verifyToken(token));
    }

    @Test
    void multiDayTripKeepsItsEndDateAndCatchDates() {
        TripBean trip = carpTrip(YESTERDAY, "10:00",
                catchAt("night-catch", TWO_DAYS_AGO, "03:00"),
                catchAt("legacy-catch", null, "20:00"));

        Map<String, String> replacements = post(trip).statusCode(201).extract().as(REPLACEMENTS);
        UUID tripId = UUID.fromString(replacements.get(trip.id));

        Trip saved = getTrip(tripId);
        Assertions.assertEquals(LocalDateTime.of(THREE_DAYS_AGO, LocalTime.of(18, 0)), saved.getBeginTimestamp());
        Assertions.assertEquals(LocalDateTime.of(YESTERDAY, LocalTime.of(10, 0)), saved.getEndTimestamp());

        Map<String, LocalDateTime> catchTimestamps = catchTimestampsByOriginalId(tripId, Map.of(
                "night-catch", UUID.fromString(replacements.get("night-catch")),
                "legacy-catch", UUID.fromString(replacements.get("legacy-catch"))));
        Assertions.assertEquals(LocalDateTime.of(TWO_DAYS_AGO, LocalTime.of(3, 0)), catchTimestamps.get("night-catch"));
        // Capture sans date (appli antérieure) : premier jour de la sortie où l'heure tombe dans la sortie.
        Assertions.assertEquals(LocalDateTime.of(THREE_DAYS_AGO, LocalTime.of(20, 0)), catchTimestamps.get("legacy-catch"));

        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .when().get("/api/v1/trips/" + tripId)
                .then().statusCode(200)
                .body("endDate", notNullValue())
                .body("catchs.find { it.caughtAt == '03:00' }.caughtOn", notNullValue());

        // 18:00 J-3 → 10:00 J-1 : 40 h.
        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .when().get("/api/v1/trips?pageNumber=0&pageSize=-1&desc=true")
                .then().statusCode(200)
                .body("elements.find { it.id == '" + tripId + "' }.durationInSeconds", equalTo(40 * 3600));
    }

    @Test
    void legacyTripCrossingMidnightStillEndsTheNextDay() {
        TripBean trip = carpTrip(null, "02:00", catchAt("after-midnight", null, "01:00"));
        trip.startedAt = "22:00";

        Map<String, String> replacements = post(trip).statusCode(201).extract().as(REPLACEMENTS);
        UUID tripId = UUID.fromString(replacements.get(trip.id));

        Assertions.assertEquals(LocalDateTime.of(TWO_DAYS_AGO, LocalTime.of(2, 0)), getTrip(tripId).getEndTimestamp());
        Map<String, LocalDateTime> catchTimestamps = catchTimestampsByOriginalId(tripId,
                Map.of("after-midnight", UUID.fromString(replacements.get("after-midnight"))));
        Assertions.assertEquals(LocalDateTime.of(TWO_DAYS_AGO, LocalTime.of(1, 0)), catchTimestamps.get("after-midnight"));
    }

    @Test
    void catchAfterTheEndOfAMultiDayTripIsRefused() {
        int countBefore = countTrips();

        post(carpTrip(YESTERDAY, "10:00", catchAt("too-late", YESTERDAY, "12:00")))
                .statusCode(400)
                .body("error", equalTo(TripTimestamps.CATCH_OUT_OF_TRIP));

        Assertions.assertEquals(countBefore, countTrips(), "aucune sortie ne doit être créée");
    }

    @Test
    void legacyCatchOutsideTheTripIsRefused() {
        TripBean trip = carpTrip(null, "10:00", catchAt("out-of-range", null, "12:00"));
        trip.date = YESTERDAY;
        trip.startedAt = "08:00";

        post(trip).statusCode(400).body("error", equalTo(TripTimestamps.CATCH_OUT_OF_TRIP));
    }

    @Test
    void endBeforeBeginIsRefused() {
        post(carpTrip(THREE_DAYS_AGO, "17:00")).statusCode(400)
                .body("error", equalTo(TripTimestamps.END_NOT_AFTER_BEGIN));
    }

    @Test
    void endDateInTheFutureIsRefused() {
        post(carpTrip(LocalDate.now().plusDays(2), "10:00")).statusCode(400)
                .body("error", equalTo(TripTimestamps.DATE_IN_FUTURE));
    }

    @Test
    void updateTurnsAOneDayTripIntoAMultiDayTrip() {
        TripBean trip = carpTrip(null, "23:00");
        UUID tripId = createAndGetId(trip);

        trip.id = tripId.toString();
        trip.endDate = Optional.of(YESTERDAY);
        trip.finishedAt = "10:00";
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .when().put("/api/v1/trips/" + tripId)
                .then().statusCode(200);

        Assertions.assertEquals(LocalDateTime.of(YESTERDAY, LocalTime.of(10, 0)), getTrip(tripId).getEndTimestamp());
    }

    @Test
    void tripSettingsExposeTheMaximumPlausibleDuration() {
        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .when().get("/api/v1/referential/trip-settings")
                .then().statusCode(200)
                .body("maxPlausibleTripDays", equalTo(7));
    }
}
