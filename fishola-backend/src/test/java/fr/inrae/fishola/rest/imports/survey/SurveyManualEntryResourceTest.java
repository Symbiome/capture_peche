package fr.inrae.fishola.rest.imports.survey;

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

import fr.inrae.fishola.rest.AbstractFisholaResource;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.empty;

/**
 * Saisie manuelle opérateur « enquête terrain » de bout en bout (#144), pendant REST de
 * {@link SurveyImportXlsxTest} pour le formulaire assistant : une sortie enquêtée valide
 * doit créer une {@code Trip} par pêcheur (+ une par session souvenir facultative), les
 * erreurs structurel/référentiel/métier doivent être renvoyées sans rien persister, et le
 * périmètre départemental de l'opérateur doit être respecté (même règle que l'import).
 *
 * <p>Auto-suffisant : provisionne son opérateur (scope Annecy uniquement) et nettoie, à
 * l'issue de la classe, exactement les lignes créées par ses propres appels (identifiées
 * via les {@code tripId} renvoyés par chaque réponse — les codes {@code survey_session}/
 * {@code surveyed_angler} sont générés aléatoirement par le serveur, cf. #144).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SurveyManualEntryResourceTest {

    private static final String URI = "/api/v1/admin/manual-entries/survey";

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
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "survey-manual-test-op@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                operatorId, department);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalAdminId, "survey-manual-test-national@fishola.test", "x");

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
        sortie.unsurveyedShoreAnglers = 0;
        sortie.unsurveyedBoatAnglers = 0;
        sortie.anglers = List.of(validAngler());
        return sortie;
    }

    private SurveyAnglerBean validAngler() {
        SurveyAnglerBean angler = new SurveyAnglerBean();
        angler.origin = "74";
        angler.fishingMode = "bord statique";
        angler.techniqueId = techniqueId;
        angler.rodCount = 1;
        angler.noExpectedSpecies = true;
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
        var response = given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(validSortie())
                .when().post(URI)
                .then().statusCode(201)
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
        souvenir.noExpectedSpecies = true;
        souvenir.bredouille = false;
        souvenir.capture = validCatch();
        sortie.anglers.get(0).souvenir = souvenir;

        var response = given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(sortie)
                .when().post(URI)
                .then().statusCode(201)
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
    }

    @Test
    void missingRequiredFieldsAreRejectedWithoutPersisting() {
        SurveySortieBean sortie = new SurveySortieBean();
        sortie.waterEntityId = annecyId;
        sortie.anglers = List.of();

        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(sortie)
                .when().post(URI)
                .then().statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("day"))
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

        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(sortie)
                .when().post(URI)
                .then().statusCode(400)
                .body("tripIds", empty())
                .body("errors.field", hasItem("captures[0].lotMinSize"));
    }

    @Test
    void waterEntityOutsideOperatorScopeIsRejectedWithoutPersisting() {
        SurveySortieBean sortie = validSortie();
        sortie.waterEntityId = bourgetId;

        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(sortie)
                .when().post(URI)
                .then().statusCode(400)
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
        souvenir.noExpectedSpecies = true;
        souvenir.bredouille = true;
        return souvenir;
    }

    private io.restassured.response.ValidatableResponse submitAs(String token, SurveySortieBean sortie) {
        return given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .contentType("application/json")
                .body(sortie)
                .when().post(URI)
                .then();
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
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(uri + "?lat=" + LAT_74 + "&lng=" + LNG_74)
                .then().statusCode(200)
                .body("proposal.name", equalTo("Annecy"));
        // Bourget (73) est hors périmètre de l'opérateur : jamais proposé.
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(uri + "?lat=" + LAT_73 + "&lng=" + LNG_73)
                .then().statusCode(200)
                .body("proposal.name", org.hamcrest.Matchers.not(equalTo("Bourget")))
                .body("alternatives.name", org.hamcrest.Matchers.not(hasItem("Bourget")));
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get(uri + "?lat=" + LAT_73 + "&lng=" + LNG_73)
                .then().statusCode(200)
                .body("proposal.name", equalTo("Bourget"));
    }
}
