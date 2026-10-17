package fr.inrae.fishola.rest.imports.survey;

/*-
 * #%L
 * Carnet de pêche :: Backend
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

import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.empty;

/**
 * Saisie manuelle opérateur « enquête terrain » de bout en bout (#144), pendant REST de
 * {@link SurveyImportXlsxTest} pour le formulaire assistant : une sortie enquêtée valide,
 * ajoutée à une session ouverte (#236), doit créer une {@code Trip} par pêcheur (+ une par
 * session souvenir facultative), les erreurs structurel/référentiel/métier doivent être
 * renvoyées sans rien persister, et le périmètre départemental de l'opérateur doit être
 * respecté (même règle que l'import).
 *
 * <p>Auto-suffisant : provisionne son opérateur (scope Annecy uniquement) et nettoie, à
 * l'issue de la classe, exactement les lignes créées par ses propres appels (identifiées
 * via les {@code tripId} renvoyés par chaque réponse — les codes {@code survey_session}/
 * {@code surveyed_angler} sont générés aléatoirement par le serveur, cf. #144).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SurveyManualEntryResourceTest {

    private static final String SESSIONS_URI = "/api/v1/admin/manual-entries/survey/sessions";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID nationalAdminId = UUID.randomUUID();
    private String operatorToken;
    private String nationalToken;
    private UUID annecyId;
    private UUID bourgetId;
    private UUID perchSpeciesId;
    private UUID techniqueId;

    private final List<UUID> createdTripIds = new ArrayList<>();
    private final List<UUID> createdSessionIds = new ArrayList<>();
    /** Session ouverte réutilisée par compte, pour les tests qui ne portent pas sur la session elle-même. */
    private final Map<String, UUID> sessionByToken = new HashMap<>();
    /** Surfaces en eau ajoutées par ce test (#189) : l'attribution hydro s'appuie sur water_surface. */
    private final List<UUID> seededSurfaceEntityIds = new ArrayList<>();

    @BeforeAll
    @Transactional
    void seedOperator() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var annecy = ctx.fetchOne("SELECT id, department FROM water_entity WHERE name = 'Annecy'");
        annecyId = annecy.get("id", UUID.class);
        String department = annecy.get("department", String.class);
        bourgetId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Bourget'").get("id", UUID.class);
        perchSpeciesId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Perche'").get("id", UUID.class);
        techniqueId = ctx.fetchOne("SELECT id FROM technique WHERE name = 'Pêche au coup'").get("id", UUID.class);
        seedWaterSurface(ctx, annecyId, "MULTIPOLYGON(((6.165 45.845,6.175 45.845,6.175 45.855,6.165 45.855,6.165 45.845)))");
        seedWaterSurface(ctx, bourgetId, "MULTIPOLYGON(((5.865 45.715,5.875 45.715,5.875 45.725,5.865 45.725,5.865 45.715)))");

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "survey-manual-test-op@carnet-peche.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                operatorId, department);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalAdminId, "survey-manual-test-national@carnet-peche.test", "x");

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            operatorToken = jwtHelper.createAdminToken(operatorId);
            nationalToken = jwtHelper.createAdminToken(nationalAdminId);
        } finally {
            requestContext.deactivate();
        }
    }

    private void seedWaterSurface(org.jooq.DSLContext ctx, UUID waterEntityId, String wkt) {
        boolean exists = ctx.fetchOne("SELECT count(*) FROM water_surface WHERE water_entity_id = ?", waterEntityId)
                .get(0, Integer.class) > 0;
        if (!exists) {
            ctx.execute("INSERT INTO water_surface (water_entity_id, geom) VALUES (?, ST_SetSRID(ST_GeomFromText(?), 4326))",
                    waterEntityId, wkt);
            seededSurfaceEntityIds.add(waterEntityId);
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        for (UUID waterEntityId : seededSurfaceEntityIds) {
            ctx.execute("DELETE FROM water_surface WHERE water_entity_id = ?", waterEntityId);
        }
        Set<UUID> sessionIds = new HashSet<>();
        Set<UUID> anglerIds = new HashSet<>();
        for (UUID tripId : createdTripIds) {
            var row = ctx.fetchOne("SELECT survey_session_id, surveyed_angler_id FROM trip WHERE id = ?", tripId);
            if (row == null) {
                continue;
            }
            UUID sessionId = row.get("survey_session_id", UUID.class);
            UUID anglerId = row.get("surveyed_angler_id", UUID.class);
            if (sessionId != null) {
                sessionIds.add(sessionId);
            }
            if (anglerId != null) {
                anglerIds.add(anglerId);
            }
            ctx.execute("DELETE FROM catch WHERE trip_id = ?", tripId);
            ctx.execute("DELETE FROM trip WHERE id = ?", tripId);
        }
        for (UUID anglerId : anglerIds) {
            ctx.execute("DELETE FROM surveyed_angler WHERE id = ?", anglerId);
        }
        sessionIds.addAll(createdSessionIds);
        for (UUID sessionId : sessionIds) {
            ctx.execute("DELETE FROM survey_session WHERE id = ?", sessionId);
        }
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?)", operatorId, nationalAdminId);
    }

    private SurveySortieBean validSortie() {
        SurveySortieBean sortie = new SurveySortieBean();
        sortie.waterEntityId = annecyId;
        sortie.day = LocalDate.of(2026, 7, 1);
        sortie.controlTime = LocalTime.of(9, 0);
        sortie.startTime = LocalTime.of(8, 0);
        sortie.endTime = LocalTime.of(11, 0);
        sortie.anglers = List.of(validAngler());
        return sortie;
    }

    private SurveyAnglerBean validAngler() {
        SurveyAnglerBean angler = new SurveyAnglerBean();
        angler.origin = "74";
        angler.fishingMode = "bord statique";
        angler.techniqueId = techniqueId;
        angler.rodCount = 1;
        angler.bredouille = false;
        angler.captures = List.of(validCatch());
        return angler;
    }

    private SurveyCatchBean validCatch() {
        SurveyCatchBean c = new SurveyCatchBean();
        c.speciesId = perchSpeciesId;
        c.quantity = 1;
        c.size = 25;
        c.kept = true;
        return c;
    }

    private void trackTripIds(io.restassured.response.Response response) {
        List<String> tripIds = response.jsonPath().getList("tripIds", String.class);
        if (tripIds != null) {
            tripIds.forEach(id -> createdTripIds.add(UUID.fromString(id)));
        }
    }

    @Test
    void validSubmissionCreatesTripAndCatch() {
        var response = submitAs(operatorToken, validSortie()).statusCode(201)
                .body("tripIds.size()", equalTo(1))
                .body("captures", equalTo(1))
                .body("errors", empty())
                .extract().response();
        trackTripIds(response);

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID tripId = UUID.fromString(response.jsonPath().getList("tripIds", String.class).get(0));
        int linked = ctx.fetchOne("SELECT count(*) FROM trip t JOIN surveyed_angler sa ON sa.id = t.surveyed_angler_id "
                + "JOIN survey_session ss ON ss.id = t.survey_session_id WHERE t.id = ?", tripId).get(0, Integer.class);
        Assertions.assertEquals(1, linked, "la trip doit être rattachée à un pêcheur enquêté et à une session");
    }

    @Test
    void souvenirBlockCreatesItsOwnTrip() {
        SurveySortieBean sortie = validSortie();
        SurveySouvenirBean souvenir = new SurveySouvenirBean();
        souvenir.day = LocalDate.of(2026, 6, 15);
        souvenir.dayPeriod = "matin";
        souvenir.waterEntityId = annecyId;
        souvenir.fishingMode = "bord statique";
        souvenir.techniqueId = techniqueId;
        souvenir.rodCount = 1;
        souvenir.bredouille = false;
        souvenir.captures = List.of(validCatch());
        sortie.anglers.get(0).souvenir = souvenir;

        var response = submitAs(operatorToken, sortie).statusCode(201)
                .body("tripIds.size()", equalTo(2))
                .body("captures", equalTo(2))
                .extract().response();
        trackTripIds(response);

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        long souvenirTrips = response.jsonPath().getList("tripIds", String.class).stream()
                .map(UUID::fromString)
                .filter(tripId -> ctx.fetchOne(
                        "SELECT count(*) FROM trip WHERE collection_method = 'enquete_souvenir' AND id = ?", tripId)
                        .get(0, Integer.class) == 1)
                .count();
        Assertions.assertEquals(1, souvenirTrips, "la session souvenir doit produire sa propre trip");

        // #235 : les deux sorties portent le code de la session d'enquête générée.
        List<String> sessionCodes = response.jsonPath().getList("tripIds", String.class).stream()
                .map(id -> ctx.fetchOne("SELECT session_code FROM trip WHERE id = ?", UUID.fromString(id))
                        .get(0, String.class))
                .distinct()
                .toList();
        Assertions.assertEquals(1, sessionCodes.size(), "un seul code session pour les deux sorties");
        Assertions.assertTrue(sessionCodes.get(0).startsWith("MANUEL-"), sessionCodes.get(0));
    }

    @Test
    void missingRequiredFieldsAreRejectedWithoutPersisting() {
        SurveySortieBean sortie = new SurveySortieBean();
        sortie.waterEntityId = annecyId;
        sortie.anglers = List.of();

        submitAs(operatorToken, sortie).statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("controlTime"))
                .body("errors.field", hasItem("startTime"))
                .body("errors.field", hasItem("anglers"));
    }

    @Test
    void lotWithoutBoundsIsRejectedWithoutPersisting() {
        SurveySortieBean sortie = validSortie();
        SurveyCatchBean lot = new SurveyCatchBean();
        lot.speciesId = perchSpeciesId;
        lot.quantity = 2;
        lot.kept = true;
        sortie.anglers.get(0).captures = List.of(lot);

        submitAs(operatorToken, sortie).statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("captures[0].lotMinSize"));
    }

    @Test
    void controlTimeBeforeStartTimeIsRejectedWithoutPersisting() {
        SurveySortieBean sortie = validSortie();
        sortie.controlTime = LocalTime.of(7, 30);

        submitAs(operatorToken, sortie).statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("controlTime"));
    }

    /**
     * #237 : carpiste contrôlé le 01/07, fin de pêche prévue le 03/07 à 10:00. Une fin prévue
     * peut être dans le futur : seul l'ordre début / fin est contrôlé.
     */
    @Test
    void plannedEndDayCreatesMultiDayTrip() {
        SurveySortieBean sortie = validSortie();
        sortie.endDay = LocalDate.of(2026, 7, 3);
        sortie.endTime = LocalTime.of(10, 0);

        var response = submitAs(operatorToken, sortie).statusCode(201).extract().response();
        trackTripIds(response);

        UUID tripId = UUID.fromString(response.jsonPath().getList("tripIds", String.class).get(0));
        var trip = DSL.using(dataSource, SQLDialect.POSTGRES)
                .fetchOne("SELECT end_timestamp FROM trip WHERE id = ?", tripId);
        Assertions.assertEquals(LocalDateTime.of(2026, 7, 3, 10, 0), trip.get("end_timestamp", LocalDateTime.class));
    }

    @Test
    void plannedEndDayBeforeStartIsRejectedWithoutPersisting() {
        SurveySortieBean sortie = validSortie();
        sortie.endDay = LocalDate.of(2026, 6, 30);

        submitAs(operatorToken, sortie).statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("endDay"));
    }

    @Test
    void waterEntityOutsideOperatorScopeIsRejectedWithoutPersisting() {
        SurveySortieBean sortie = validSortie();
        sortie.waterEntityId = bourgetId;

        submitAs(operatorToken, sortie).statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("waterEntityId"));
    }

    // --- #189 : position saisie sur la carte par le staff. ---

    /** Point dans la boîte « 74 » de la fixture, à côté d'Annecy. */
    private static final double LAT_74 = 45.86;
    private static final double LNG_74 = 6.18;
    /** Point dans la boîte « 73 » de la fixture, à côté du Bourget. */
    private static final double LAT_73 = 45.71;
    private static final double LNG_73 = 5.88;

    private SurveySouvenirBean validSouvenir() {
        SurveySouvenirBean souvenir = new SurveySouvenirBean();
        souvenir.day = LocalDate.of(2026, 6, 15);
        souvenir.dayPeriod = "matin";
        souvenir.waterEntityId = annecyId;
        souvenir.fishingMode = "bord statique";
        souvenir.techniqueId = techniqueId;
        souvenir.rodCount = 1;
        souvenir.bredouille = true;
        return souvenir;
    }

    private io.restassured.response.ValidatableResponse submitAs(String token, SurveySortieBean sortie) {
        return submitTo(sessionFor(token), token, sortie);
    }

    private io.restassured.response.ValidatableResponse submitTo(UUID sessionId, String token, SurveySortieBean sortie) {
        return given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .contentType("application/json")
                .body(sortie)
                .when().post(SESSIONS_URI + "/" + sessionId + "/sorties")
                .then();
    }

    private UUID sessionFor(String token) {
        return sessionByToken.computeIfAbsent(token, this::openSession);
    }

    private SurveySessionBean validSession() {
        SurveySessionBean session = new SurveySessionBean();
        session.day = LocalDate.of(2026, 7, 1);
        session.startTime = LocalTime.of(7, 0);
        session.waterEntityId = annecyId;
        session.operatorName = "J. Dupont";
        return session;
    }

    private UUID openSession(String token) {
        String id = given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .contentType("application/json")
                .body(validSession())
                .when().post(SESSIONS_URI)
                .then().statusCode(201)
                .body("session.status", equalTo("ouverte"))
                .extract().path("session.id");
        UUID sessionId = UUID.fromString(id);
        createdSessionIds.add(sessionId);
        return sessionId;
    }

    @Test
    void mapPositionIsStoredSnappedAndStampsItsDepartment() {
        SurveySortieBean sortie = validSortie();
        sortie.latitude = LAT_74;
        sortie.longitude = LNG_74;
        SurveySouvenirBean souvenir = validSouvenir();
        souvenir.latitude = LAT_74;
        souvenir.longitude = LNG_74;
        sortie.anglers.get(0).souvenir = souvenir;

        var response = submitAs(operatorToken, sortie).statusCode(201).extract().response();
        trackTripIds(response);

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        for (String tripId : response.jsonPath().getList("tripIds", String.class)) {
            var trip = ctx.fetchOne("SELECT ST_Y(begin_position) AS lat, ST_X(begin_position) AS lng, "
                    + "snapped_position IS NOT NULL AS snapped, hydro_validation, department FROM trip WHERE id = ?",
                    UUID.fromString(tripId));
            Assertions.assertEquals(LAT_74, trip.get("lat", Double.class), 1e-9);
            Assertions.assertEquals(LNG_74, trip.get("lng", Double.class), 1e-9);
            Assertions.assertTrue(trip.get("snapped", Boolean.class));
            Assertions.assertEquals("CONFIRMED", trip.get("hydro_validation", String.class));
            Assertions.assertEquals("74", trip.get("department", String.class));
        }
    }

    @Test
    void departmentComesFromThePointRatherThanTheWaterEntity() {
        // Arbitrage A5 : la sortie relève du département où tombe son point (ici 73), même
        // rattachée à une entité d'un autre département (Annecy, 74). National : sans périmètre.
        SurveySortieBean sortie = validSortie();
        sortie.latitude = LAT_73;
        sortie.longitude = LNG_73;

        var response = submitAs(nationalToken, sortie).statusCode(201).extract().response();
        trackTripIds(response);

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID tripId = UUID.fromString(response.jsonPath().getList("tripIds", String.class).get(0));
        Assertions.assertEquals("73", ctx.fetchOne("SELECT department FROM trip WHERE id = ?", tripId)
                .get("department", String.class));
    }

    @Test
    void operatorPositionOutsidePerimeterIsRejected() {
        SurveySortieBean sortie = validSortie();
        sortie.latitude = LAT_73;
        sortie.longitude = LNG_73;
        SurveySouvenirBean souvenir = validSouvenir();
        souvenir.latitude = LAT_73;
        souvenir.longitude = LNG_73;
        sortie.anglers.get(0).souvenir = souvenir;

        submitAs(operatorToken, sortie).statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("position"))
                .body("errors.field", hasItem("souvenir.position"));
    }

    @Test
    void incompletePositionIsRejected() {
        SurveySortieBean sortie = validSortie();
        sortie.latitude = LAT_74;

        submitAs(operatorToken, sortie).statusCode(400)
                .body("errors.field", hasItem("position"));
    }

    @Test
    void staffAttributionIsLimitedToThePerimeter() {
        String uri = "/api/v1/referential/waterEntities/attribution";
        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(uri + "?lat=" + LAT_74 + "&lng=" + LNG_74)
                .then().statusCode(200)
                .body("proposal.name", equalTo("Annecy"));
        // Bourget (73) est hors périmètre de l'opérateur : jamais proposé.
        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(uri + "?lat=" + LAT_73 + "&lng=" + LNG_73)
                .then().statusCode(200)
                .body("proposal.name", org.hamcrest.Matchers.not(equalTo("Bourget")))
                .body("alternatives.name", org.hamcrest.Matchers.not(hasItem("Bourget")));
        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get(uri + "?lat=" + LAT_73 + "&lng=" + LNG_73)
                .then().statusCode(200)
                .body("proposal.name", equalTo("Bourget"));
    }

    // --- #236 : saisie pyramidale session -> sorties -> pêcheurs -> captures. ---

    private io.restassured.response.ValidatableResponse getSession(String token, UUID sessionId) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .when().get(SESSIONS_URI + "/" + sessionId)
                .then();
    }

    private io.restassured.response.ValidatableResponse closeSession(String token, UUID sessionId,
                                                                     SurveySessionCloseBean close) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .contentType("application/json")
                .body(close)
                .when().post(SESSIONS_URI + "/" + sessionId + "/close")
                .then();
    }

    private SurveySessionCloseBean validClose() {
        SurveySessionCloseBean close = new SurveySessionCloseBean();
        close.endTime = LocalTime.of(12, 0);
        close.unsurveyedShoreAnglers = 2;
        close.unsurveyedBoatAnglers = 1;
        return close;
    }

    @Test
    void sessionReceivesSeveralSorties() {
        UUID sessionId = openSession(operatorToken);
        trackTripIds(submitTo(sessionId, operatorToken, validSortie()).statusCode(201).extract().response());
        trackTripIds(submitTo(sessionId, operatorToken, validSortie()).statusCode(201).extract().response());

        getSession(operatorToken, sessionId).statusCode(200)
                .body("sortiesCount", equalTo(2))
                .body("anglersCount", equalTo(2))
                .body("operatorName", equalTo("J. Dupont"))
                .body("startTime", equalTo("07:00"))
                .body("day", equalTo("2026-07-01"));
    }

    @Test
    void sortieInheritsDayAndSectorFromSession() {
        SurveySortieBean sortie = validSortie();
        sortie.day = null;
        sortie.waterEntityId = null;

        var response = submitAs(operatorToken, sortie).statusCode(201).extract().response();
        trackTripIds(response);

        UUID tripId = UUID.fromString(response.jsonPath().getList("tripIds", String.class).get(0));
        var trip = DSL.using(dataSource, SQLDialect.POSTGRES)
                .fetchOne("SELECT begin_timestamp, water_entity_id FROM trip WHERE id = ?", tripId);
        Assertions.assertEquals(LocalDateTime.of(2026, 7, 1, 8, 0), trip.get("begin_timestamp", LocalDateTime.class));
        Assertions.assertEquals(annecyId, trip.get("water_entity_id", UUID.class));
    }

    @Test
    void openSessionIsListedUntilClosed() {
        UUID sessionId = openSession(operatorToken);
        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(SESSIONS_URI)
                .then().statusCode(200)
                .body("id", hasItem(sessionId.toString()));

        closeSession(operatorToken, sessionId, validClose()).statusCode(200)
                .body("session.status", equalTo("cloturee"))
                .body("session.endTime", equalTo("12:00"))
                .body("session.unsurveyedShoreAnglers", equalTo(2))
                .body("session.unsurveyedBoatAnglers", equalTo(1));

        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(SESSIONS_URI)
                .then().statusCode(200)
                .body("id", org.hamcrest.Matchers.not(hasItem(sessionId.toString())));
    }

    @Test
    void closedSessionRefusesNewSorties() {
        UUID sessionId = openSession(operatorToken);
        closeSession(operatorToken, sessionId, validClose()).statusCode(200);

        submitTo(sessionId, operatorToken, validSortie()).statusCode(400)
                .body("tripIds", empty())
                .body("errors.message", hasItem(SurveySessionService.SESSION_CLOSED));
        closeSession(operatorToken, sessionId, validClose()).statusCode(400);
    }

    @Test
    void sessionEndBeforeStartIsRejected() {
        UUID sessionId = openSession(operatorToken);
        SurveySessionCloseBean close = validClose();
        close.endTime = LocalTime.of(6, 0);
        close.unsurveyedShoreAnglers = -1;

        closeSession(operatorToken, sessionId, close).statusCode(400)
                .body("errors.field", hasItem("endTime"))
                .body("errors.field", hasItem("unsurveyedShoreAnglers"));
        getSession(operatorToken, sessionId).statusCode(200).body("status", equalTo("ouverte"));
    }

    @Test
    void sessionOfAnotherAccountIsNotAccessible() {
        UUID sessionId = openSession(operatorToken);

        getSession(nationalToken, sessionId).statusCode(404);
        submitTo(sessionId, nationalToken, validSortie()).statusCode(404);
        closeSession(nationalToken, sessionId, validClose()).statusCode(404);
    }

    @Test
    void incompleteSessionIsRejectedWithoutPersisting() {
        SurveySessionBean session = validSession();
        session.startTime = null;
        session.operatorName = " ";
        session.waterEntityId = bourgetId;

        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(session)
                .when().post(SESSIONS_URI)
                .then().statusCode(400)
                .body("session", org.hamcrest.Matchers.nullValue())
                .body("errors.field", hasItem("startTime"))
                .body("errors.field", hasItem("operatorName"))
                .body("errors.field", hasItem("waterEntityId"));
    }

    @Test
    void controlTimeAfterPlannedEndIsRejectedWithoutPersisting() {
        SurveySortieBean sortie = validSortie();
        sortie.controlTime = LocalTime.of(11, 30);

        submitAs(operatorToken, sortie).statusCode(400)
                .body("tripIds", empty())
                .body("errors.message", hasItem(SurveyManualEntryService.CONTROL_AFTER_END));
    }

    @Test
    void controlTimeDuringMultiDayTripIsAccepted() {
        SurveySortieBean sortie = validSortie();
        sortie.controlTime = LocalTime.of(23, 0);
        sortie.endDay = LocalDate.of(2026, 7, 3);
        sortie.endTime = LocalTime.of(10, 0);

        trackTripIds(submitAs(operatorToken, sortie).statusCode(201).extract().response());
    }

    @Test
    void souvenirAcceptsSeveralCaptures() {
        SurveySortieBean sortie = validSortie();
        SurveySouvenirBean souvenir = validSouvenir();
        souvenir.bredouille = false;
        SurveyCatchBean pike = validCatch();
        pike.size = 30;
        souvenir.captures = List.of(validCatch(), pike);
        sortie.anglers.get(0).souvenir = souvenir;

        var response = submitAs(operatorToken, sortie).statusCode(201)
                .body("tripIds.size()", equalTo(2))
                .body("captures", equalTo(3))
                .extract().response();
        trackTripIds(response);

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        int souvenirCatches = response.jsonPath().getList("tripIds", String.class).stream()
                .mapToInt(id -> ctx.fetchOne("SELECT count(*) FROM catch c JOIN trip t ON t.id = c.trip_id "
                        + "WHERE t.collection_method = 'enquete_souvenir' AND t.id = ?", UUID.fromString(id))
                        .get(0, Integer.class))
                .sum();
        Assertions.assertEquals(2, souvenirCatches);
    }

    @Test
    void souvenirWithoutCaptureMustBeBredouille() {
        SurveySortieBean sortie = validSortie();
        SurveySouvenirBean souvenir = validSouvenir();
        souvenir.bredouille = false;
        sortie.anglers.get(0).souvenir = souvenir;

        submitAs(operatorToken, sortie).statusCode(400)
                .body("errors.field", hasItem("souvenir.captures"));
    }

    @Test
    void expectedSpeciesDefaultsToAllSpecies() {
        var response = submitAs(operatorToken, validSortie()).statusCode(201).extract().response();
        trackTripIds(response);

        UUID tripId = UUID.fromString(response.jsonPath().getList("tripIds", String.class).get(0));
        Assertions.assertNull(DSL.using(dataSource, SQLDialect.POSTGRES)
                .fetchOne("SELECT expected_species_id FROM trip WHERE id = ?", tripId)
                .get("expected_species_id", UUID.class));
    }
}
