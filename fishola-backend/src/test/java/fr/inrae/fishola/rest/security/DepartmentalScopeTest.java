package fr.inrae.fishola.rest.security;

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

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;

/**
 * Cloisonnement départemental (#159) : un staff régional ne voit / n'édite que
 * les sorties, prises et entités hydro de ses départements ; un national voit
 * tout. Vérifie aussi l'estampillage {@code trip.department} / {@code catch.department}
 * par jointure spatiale sur {@code departement.geom}.
 *
 * <p>Auto-suffisant (gabarit {@code OperatorAccessTest}). S'appuie sur les
 * contours départementaux de la fixture ({@code R__test_fixture.sql}) : boîtes
 * autour d'Annecy/Léman (74) et Bourget (73).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepartmentalScopeTest {

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    TripsDao tripsDao;

    @Inject
    CatchsDao catchsDao;

    private final UUID nationalAdminId = UUID.randomUUID();
    private final UUID regional74Id = UUID.randomUUID();
    private String nationalToken;
    private String regional74Token;

    private UUID catchIn74;
    private UUID catchIn73;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalAdminId, "dept-scope-national@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, false)", regional74Id, "dept-scope-regional74@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", regional74Id);

        UUID speciesId = ctx.fetchOne("SELECT id FROM species LIMIT 1").get("id", UUID.class);
        UUID techniqueId = ctx.fetchOne("SELECT id FROM technique LIMIT 1").get("id", UUID.class);
        UUID annecyId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Annecy'").get("id", UUID.class);
        UUID bourgetId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Bourget'").get("id", UUID.class);

        catchIn74 = createTripWithCatch("DEPT-SCOPE-74", annecyId, speciesId, techniqueId, "POINT(6.17 45.85)");
        catchIn73 = createTripWithCatch("DEPT-SCOPE-73", bourgetId, speciesId, techniqueId, "POINT(5.87 45.72)");

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalAdminId);
            regional74Token = jwtHelper.createAdminToken(regional74Id);
        } finally {
            requestContext.deactivate();
        }
    }

    private UUID createTripWithCatch(String name, UUID waterEntityId, UUID speciesId, UUID techniqueId, String wktPoint) {
        LocalDateTime now = LocalDateTime.now();
        Trip trip = new Trip();
        trip.setCreatedOn(now.minusDays(30));
        trip.setBeginTimestamp(now.minusDays(30).minusHours(3));
        trip.setEndTimestamp(now.minusDays(30));
        trip.setWaterEntityId(waterEntityId);
        trip.setName(name);
        trip.setType(TripType.Border);
        trip.setHidden(false);
        trip.setMode(TripMode.Afterwards);
        trip.setSource(DeviceType.web);
        UUID tripId = tripsDao.create(trip);
        // Position via le chemin normal (ST_GeomFromText, SRID 4326) puis estampillage spatial.
        tripsDao.updatePositions(tripId, wktPoint, null);
        tripsDao.stampDepartment(tripId);

        Catch aCatch = new Catch();
        aCatch.setTripId(tripId);
        aCatch.setCreatedOn(now.minusDays(30));
        aCatch.setCatchTimestamp(now.minusDays(30));
        aCatch.setSpeciesId(speciesId);
        aCatch.setTechniqueId(techniqueId);
        aCatch.setSize(25);
        aCatch.setWeight(200);
        aCatch.setKept(true);
        aCatch.setMaillee(Maillage.NON_DEFINI);
        UUID catchId = catchsDao.create(aCatch);
        catchsDao.stampDepartment(catchId);
        return catchId;
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE name LIKE 'DEPT-SCOPE-%')");
        ctx.execute("DELETE FROM trip WHERE name LIKE 'DEPT-SCOPE-%'");
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?)", nationalAdminId, regional74Id);
    }

    @Test
    @Order(1)
    void spatialJoinStampsTripAndCatchDepartment() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        String tripDept = ctx.fetchOne("SELECT department FROM trip WHERE name = 'DEPT-SCOPE-74'").get("department", String.class);
        String catchDept = ctx.fetchOne("SELECT department FROM catch WHERE id = ?", catchIn74).get("department", String.class);
        Assertions.assertEquals("74", tripDept);
        Assertions.assertEquals("74", catchDept);
        String otherDept = ctx.fetchOne("SELECT department FROM trip WHERE name = 'DEPT-SCOPE-73'").get("department", String.class);
        Assertions.assertEquals("73", otherDept);
    }

    @Test
    @Order(2)
    void regionalExportIsScopedToItsDepartments() {
        // catchs_openadom_export : nom_du_site = export_as de l'entité (Annecy = 74, Bourget = 73).
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .when().get("/api/v1/trips/export")
                .then().statusCode(200)
                .body(containsString("annecy"))
                .body(not(containsString("bourget")));
    }

    @Test
    @Order(3)
    void nationalExportSeesEverything() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get("/api/v1/trips/export")
                .then().statusCode(200)
                .body(containsString("annecy"))
                .body(containsString("bourget"));
    }

    @Test
    @Order(5)
    void regionalCannotEditCatchOutsidePerimeter() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .contentType("application/json")
                .body("{\"excludeFromExport\": true}")
                .when().put("/api/v1/trips/catches/" + catchIn73)
                .then().statusCode(403);
    }

    @Test
    @Order(6)
    void regionalCanEditCatchInsidePerimeter() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .contentType("application/json")
                .body("{\"excludeFromExport\": true}")
                .when().put("/api/v1/trips/catches/" + catchIn74)
                .then().statusCode(200);
    }

    @Test
    @Order(4)
    void regionalWaterEntitiesAreScopedToItsDepartments() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .when().get("/api/v1/referential/waterEntities")
                .then().statusCode(200)
                .body("name", hasItem("Annecy"))
                .body("name", hasItem("Léman"))
                .body("name", not(hasItem("Bourget")));
    }
}
