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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final UUID operator74Id = UUID.randomUUID();
    private String nationalToken;
    private String regional74Token;
    private String operator74Token;

    private UUID catchIn74;
    private UUID catchIn73;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalAdminId, "dept-scope-national@fishola.test", "x");
        // can_create_admin=true : requis par regionalOperatorUpdateForcesCreatorDepartments (#164).
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, false, false)", regional74Id, "dept-scope-regional74@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", regional74Id);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operator74Id, "dept-scope-operator74@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", operator74Id);

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
            operator74Token = jwtHelper.createAdminToken(operator74Id);
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
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?, ?)", nationalAdminId, regional74Id, operator74Id);
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
    void operatorExportIsScopedToItsDepartments() {
        // #188 (E5) : « Voir toutes les sessions / prises » et « Exporter (CSV) » : Oui (dép.).
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                .when().get("/api/v1/trips/export")
                .then().statusCode(200)
                .body(containsString("annecy"))
                .body(not(containsString("bourget")));
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                .when().get("/api/v1/trips/export/0/date_de_la_sortie/desc")
                .then().statusCode(200)
                .body("elements.catchId", hasItem(catchIn74.toString()))
                .body("elements.catchId", not(hasItem(catchIn73.toString())));
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

    /**
     * #164 : un opérateur édité par un admin régional est rattaché à l'intégralité de son
     * périmètre, quels que soient les {@code departmentCodes} envoyés dans le payload — ici
     * "73" (hors périmètre de regional74, qui n'a que "74") est ignoré au profit de "74".
     * L'opérateur est déjà dans le périmètre : sinon la modification est refusée (#188).
     */
    @Test
    @Order(7)
    void regionalOperatorUpdateForcesCreatorDepartments() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID operatorId = UUID.randomUUID();
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "dept-scope-operator@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", operatorId);

        try {
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                    .contentType("application/json")
                    .body("{\"departmentCodes\":[\"73\"]}")
                    .when().put("/api/v1/admin/operators/" + operatorId)
                    .then().statusCode(204);

            Set<String> departments = ctx.fetch("SELECT department_code FROM fishola_admin_departments WHERE fishola_admin_id = ?", operatorId)
                    .stream().map(r -> r.get("department_code", String.class)).collect(Collectors.toSet());
            Assertions.assertEquals(Set.of("74"), departments);
        } finally {
            ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
        }
    }

    // --- Matrice des droits (#188) : cloisonnement départemental du rôle opérateur. ---

    @Test
    @Order(8)
    void operatorWaterEntitiesAreScopedToItsDepartments() {
        // Entités hydrographiques — « Voir les lacs / cours d'eau » (écran staff) : borné.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                .when().get("/api/v1/referential/waterEntities")
                .then().statusCode(200)
                .body("name", hasItem("Annecy"))
                .body("name", not(hasItem("Bourget")));
    }

    @Test
    @Order(9)
    void operatorCannotOpenCatchOutsidePerimeter() {
        // Prises — « Voir toutes les prises » : borné à ses départements.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                .when().get("/api/v1/trips/catches/" + catchIn73)
                .then().statusCode(403);
    }

    @Test
    @Order(10)
    void operatorCannotValidateCatchOutsidePerimeter() {
        // Prises — « Valider / corriger une prise incertaine » : borné à ses départements.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                .contentType("application/json")
                .body("{\"excludeFromExport\": true}")
                .when().put("/api/v1/trips/catches/" + catchIn73)
                .then().statusCode(403);
    }

    /** #188 (E8) : l'opérateur ne corrige que les prises à valider (non certaines, non validées). */
    @Test
    @Order(11)
    void operatorCorrectsOnlyCatchesPendingValidation() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("UPDATE catch SET certainty = 'CERTAIN', validated_at = NULL, validated_by = NULL WHERE id = ?", catchIn74);
        putCatchAsOperator74(catchIn74).then().statusCode(403);

        ctx.execute("UPDATE catch SET certainty = 'UNCERTAIN' WHERE id = ?", catchIn74);
        putCatchAsOperator74(catchIn74).then().statusCode(200);
        Assertions.assertNotNull(ctx.fetchOne("SELECT validated_at FROM catch WHERE id = ?", catchIn74)
                .get("validated_at", LocalDateTime.class));

        // Déjà validée : plus à valider, l'opérateur ne peut plus la modifier.
        putCatchAsOperator74(catchIn74).then().statusCode(403);

        // Témoin : l'administrateur régional corrige toute prise de son périmètre.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .contentType("application/json")
                .body("{\"excludeFromExport\": true}")
                .when().put("/api/v1/trips/catches/" + catchIn74)
                .then().statusCode(200);
    }

    private io.restassured.response.Response putCatchAsOperator74(UUID catchId) {
        return given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                .contentType("application/json")
                .body("{\"excludeFromExport\": true}")
                .when().put("/api/v1/trips/catches/" + catchId);
    }

    // --- #188 : un compte non national sans département ne voit rien (fail-closed). ---

    @Test
    @Order(12)
    void regionalCannotEmptyItsOwnPerimeter() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .contentType("application/json")
                .body("{\"canCreateAdmin\":true,\"departmentCodes\":[]}")
                .when().put("/api/v1/admin/" + regional74Id)
                .then().statusCode(400);
        Set<String> departments = ctx.fetch("SELECT department_code FROM fishola_admin_departments WHERE fishola_admin_id = ?", regional74Id)
                .stream().map(r -> r.get("department_code", String.class)).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("74"), departments);
    }

    @Test
    @Order(13)
    void staffWithoutDepartmentSeesNothing() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID regionalWithoutDepartmentId = UUID.randomUUID();
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, false, false)", regionalWithoutDepartmentId, "dept-scope-empty@fishola.test", "x");
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        String token;
        try {
            token = jwtHelper.createAdminToken(regionalWithoutDepartmentId);
        } finally {
            requestContext.deactivate();
        }

        try {
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                    .when().get("/api/v1/trips/export")
                    .then().statusCode(200)
                    .body(not(containsString("annecy")))
                    .body(not(containsString("bourget")));
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                    .when().get("/api/v1/trips/catches/" + catchIn74)
                    .then().statusCode(403);
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                    .when().get("/api/v1/referential/waterEntities")
                    .then().statusCode(200)
                    .body("name", not(hasItem("Annecy")))
                    .body("name", not(hasItem("Bourget")));
        } finally {
            ctx.execute("DELETE FROM fishola_admin WHERE id = ?", regionalWithoutDepartmentId);
        }
    }

    // --- #188 (E3) : un régional ne modifie que des comptes inclus dans son périmètre. ---

    private UUID insertOperatorIn(String department, String email) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID operatorId = UUID.randomUUID();
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, email, "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)", operatorId, department);
        return operatorId;
    }

    private Set<String> departmentsOf(UUID adminId) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetch("SELECT department_code FROM fishola_admin_departments WHERE fishola_admin_id = ?", adminId)
                .stream().map(r -> r.get("department_code", String.class)).collect(Collectors.toSet());
    }

    @Test
    @Order(14)
    void regionalCannotEditOperatorOutsidePerimeter() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID operatorId = insertOperatorIn("73", "dept-scope-operator73@fishola.test");
        try {
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                    .contentType("application/json")
                    .body("{\"departmentCodes\":[\"74\"]}")
                    .when().put("/api/v1/admin/operators/" + operatorId)
                    .then().statusCode(403);
            Assertions.assertEquals(Set.of("73"), departmentsOf(operatorId));

            // Témoin : le national modifie ce même opérateur.
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                    .contentType("application/json")
                    .body("{\"departmentCodes\":[\"73\",\"74\"]}")
                    .when().put("/api/v1/admin/operators/" + operatorId)
                    .then().statusCode(204);
            Assertions.assertEquals(Set.of("73", "74"), departmentsOf(operatorId));

            // Recouvrement partiel (73 + 74) : toujours refusé, le régional retirerait le 73.
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                    .contentType("application/json")
                    .body("{\"departmentCodes\":[\"74\"]}")
                    .when().put("/api/v1/admin/operators/" + operatorId)
                    .then().statusCode(403);
            Assertions.assertEquals(Set.of("73", "74"), departmentsOf(operatorId));
        } finally {
            ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
        }
    }

    @Test
    @Order(15)
    void regionalCannotEditNationalAdmin() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .contentType("application/json")
                .body("{\"canCreateAdmin\":true,\"departmentCodes\":[\"74\"]}")
                .when().put("/api/v1/admin/" + nationalAdminId)
                .then().statusCode(403);
        Assertions.assertEquals(Set.of(), departmentsOf(nationalAdminId));
    }

    // --- #188 : la recherche de pêcheurs pour un concours est nationale, y compris pour l'opérateur. ---

    private UUID insertAnglerOwning(String tripName, String email) {
        return insertAngler(tripName, null, email);
    }

    private UUID insertAngler(String ownedTripName, String postalCode, String email) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID userId = UUID.randomUUID();
        ctx.execute("INSERT INTO fishola_user (id, first_name, last_name, pseudo, email, password, created_on, postal_code) "
                + "VALUES (?, 'Pêcheur', 'Recherchedept', ?, ?, 'x', now(), ?)", userId, email, email, postalCode);
        if (ownedTripName != null) {
            ctx.execute("UPDATE trip SET owner_id = ? WHERE name = ?", userId, ownedTripName);
        }
        return userId;
    }

    @Test
    @Order(16)
    void operatorFindsAnglersOfAllDepartmentsForCompetitions() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID angler74 = insertAnglerOwning("DEPT-SCOPE-74", "recherchedept-74@fishola.test");
        UUID angler73 = insertAnglerOwning("DEPT-SCOPE-73", "recherchedept-73@fishola.test");
        try {
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                    .when().get("/api/v1/admin/competitions/search-users?q=recherchedept")
                    .then().statusCode(200)
                    .body("email", hasItem("recherchedept-74@fishola.test"))
                    .body("email", hasItem("recherchedept-73@fishola.test"));
        } finally {
            ctx.execute("UPDATE trip SET owner_id = NULL WHERE owner_id IN (?, ?)", angler74, angler73);
            ctx.execute("DELETE FROM fishola_user WHERE id IN (?, ?)", angler74, angler73);
        }
    }

    // --- #188 (E1) : liste des pêcheurs bornée (a pêché dans le département OU y a son code postal). ---

    @Test
    @Order(17)
    void anglerListIsScopedToDepartments() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID fishedIn74 = insertAngler("DEPT-SCOPE-74", "73000", "liste-fished74@fishola.test");
        UUID livesIn74 = insertAngler(null, "74000", "liste-lives74@fishola.test");
        UUID fishedAndLivesIn73 = insertAngler("DEPT-SCOPE-73", "73100", "liste-73@fishola.test");
        UUID noDepartment = insertAngler(null, null, "liste-none@fishola.test");
        try {
            for (String token : new String[]{operator74Token, regional74Token}) {
                given()
                        .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                        .when().get("/api/v1/security/users")
                        .then().statusCode(200)
                        .body("email", hasItem("liste-fished74@fishola.test"))
                        .body("email", hasItem("liste-lives74@fishola.test"))
                        .body("email", not(hasItem("liste-73@fishola.test")))
                        .body("email", not(hasItem("liste-none@fishola.test")));
            }
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                    .when().get("/api/v1/security/users")
                    .then().statusCode(200)
                    .body("email", hasItem("liste-73@fishola.test"))
                    .body("email", hasItem("liste-none@fishola.test"));
            // Modification d'un pêcheur : toujours réservée au national.
            given()
                    .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operator74Token)
                    .when().delete("/api/v1/security/users/" + livesIn74)
                    .then().statusCode(403);
        } finally {
            ctx.execute("UPDATE trip SET owner_id = NULL WHERE owner_id IN (?, ?)", fishedIn74, fishedAndLivesIn73);
            ctx.execute("DELETE FROM fishola_user WHERE id IN (?, ?, ?, ?)", fishedIn74, livesIn74, fishedAndLivesIn73, noDepartment);
        }
    }
}
