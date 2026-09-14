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
    private String operatorToken;
    private UUID annecyId;
    private UUID bourgetId;
    private UUID perchSpeciesId;
    private UUID techniqueId;

    private final List<UUID> createdTripIds = new ArrayList<>();

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

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "survey-manual-test-op@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                operatorId, department);

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            operatorToken = jwtHelper.createAdminToken(operatorId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
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
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
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
}
