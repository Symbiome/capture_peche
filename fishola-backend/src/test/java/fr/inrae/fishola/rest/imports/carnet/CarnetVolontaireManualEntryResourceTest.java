package fr.inrae.fishola.rest.imports.carnet;

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
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Saisie manuelle « carnet volontaire » : position saisie sur la carte par l'opérateur
 * (#189). Le point devient la position de départ de la sortie et fixe son département ;
 * hors du périmètre de l'opérateur, la saisie est refusée.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CarnetVolontaireManualEntryResourceTest {

    private static final String URI = "/api/v1/admin/manual-entries/carnet-volontaire";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID operatorId = UUID.randomUUID();
    private String operatorToken;
    private UUID annecyId;
    private UUID techniqueId;
    private final List<UUID> createdTripIds = new ArrayList<>();

    @BeforeAll
    @Transactional
    void seedOperator() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        annecyId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Annecy'").get("id", UUID.class);
        techniqueId = ctx.fetchOne("SELECT id FROM technique WHERE name = 'Pêche au coup'").get("id", UUID.class);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "carnet-manual-test-op@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", operatorId);

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
        for (UUID tripId : createdTripIds) {
            ctx.execute("DELETE FROM catch WHERE trip_id = ?", tripId);
            ctx.execute("DELETE FROM trip WHERE id = ?", tripId);
        }
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
    }

    private CarnetVolontaireTripBean bredouilleTrip(Double latitude, Double longitude) {
        CarnetVolontaireTripBean trip = new CarnetVolontaireTripBean();
        trip.day = LocalDate.of(2026, 7, 1);
        trip.startTime = LocalTime.of(8, 0);
        trip.endTime = LocalTime.of(11, 0);
        trip.waterEntityId = annecyId;
        trip.latitude = latitude;
        trip.longitude = longitude;
        trip.fishingMode = "bord statique";
        trip.techniqueId = techniqueId;
        trip.rodCount = 1;
        trip.noExpectedSpecies = true;
        trip.bredouille = true;
        return trip;
    }

    @Test
    void mapPositionBecomesTheTripPosition() {
        String tripId = given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(bredouilleTrip(45.86, 6.18))
                .when().post(URI)
                .then().statusCode(201)
                .extract().jsonPath().getString("tripId");
        createdTripIds.add(UUID.fromString(tripId));

        var trip = DSL.using(dataSource, SQLDialect.POSTGRES)
                .fetchOne("SELECT ST_Y(begin_position) AS lat, ST_X(begin_position) AS lng, department FROM trip WHERE id = ?",
                        UUID.fromString(tripId));
        Assertions.assertEquals(45.86, trip.get("lat", Double.class), 1e-9);
        Assertions.assertEquals(6.18, trip.get("lng", Double.class), 1e-9);
        Assertions.assertEquals("74", trip.get("department", String.class));
    }

    @Test
    void positionOutsidePerimeterIsRejected() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(bredouilleTrip(45.71, 5.88))
                .when().post(URI)
                .then().statusCode(400)
                .body("errors.field", hasItem("position"));
    }
}
