package fr.inrae.fishola.rest.referential;

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
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;

/**
 * Maillage et taille maximale par défaut au département (#246) : la valeur du
 * département s'applique à toutes ses entités hydrographiques, une valeur propre
 * à l'entité reste prioritaire, et un admin régional ne configure que ses
 * départements. S'appuie sur la fixture ({@code R__test_fixture.sql}) : Annecy et
 * Léman en 74, Bourget en 73, Annecy × Carpe commune réglementée (60 cm / 10 cm).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepartmentSizeDefaultsTest {

    private static final String REFERENTIAL = "/api/v1/referential/authorized-samples";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID nationalAdminId = UUID.randomUUID();
    private final UUID regional74Id = UUID.randomUUID();
    private String nationalToken;
    private String regional74Token;

    private UUID carpeId;
    private UUID otherSpeciesId;
    private UUID annecyId;
    private UUID lemanId;
    private UUID bourgetId;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalAdminId, "dept-sizes-national@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, false)", regional74Id, "dept-sizes-regional74@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", regional74Id);

        carpeId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Carpe commune'").get("id", UUID.class);
        otherSpeciesId = ctx.fetchOne("SELECT id FROM species WHERE name <> 'Carpe commune' ORDER BY name LIMIT 1")
                .get("id", UUID.class);
        annecyId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Annecy'").get("id", UUID.class);
        lemanId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Léman'").get("id", UUID.class);
        bourgetId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Bourget'").get("id", UUID.class);

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalAdminId);
            regional74Token = jwtHelper.createAdminToken(regional74Id);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM department_authorized_sample");
        ctx.execute("DELETE FROM authorized_sample WHERE water_entity_id = ? AND species_id = ?", lemanId, otherSpeciesId);
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?)", nationalAdminId, regional74Id);
    }

    @Test
    @Order(1)
    void nationalAdminSavesDepartmentDefaults() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .contentType(ContentType.JSON)
                .body(List.of(
                        Map.of("speciesId", otherSpeciesId, "maxSize", 70, "meshSize", 5),
                        Map.of("speciesId", carpeId, "maxSize", 80, "meshSize", 20)))
                .when().put(REFERENTIAL + "/department/74")
                .then().statusCode(204);

        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get(REFERENTIAL + "/department/74")
                .then().statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @Order(2)
    void entityWithoutOwnValueInheritsDepartmentDefault() {
        assertMaxSize(lemanId, otherSpeciesId, 70);
        assertMeshSize(lemanId, otherSpeciesId, 5);
    }

    @Test
    @Order(3)
    void entityValueOverridesDepartmentDefault() {
        assertMaxSize(annecyId, carpeId, 60);
        assertMeshSize(annecyId, carpeId, 10);
    }

    @Test
    @Order(4)
    void entityInAnotherDepartmentDoesNotInherit() {
        assertMaxSize(bourgetId, otherSpeciesId, 1000);
        given()
                .queryParam("waterEntityId", bourgetId)
                .queryParam("speciesId", otherSpeciesId)
                .when().get(REFERENTIAL + "/mesh-size")
                .then().statusCode(204);
    }

    /** Une ligne historique sans taille max ni maillage (min seule) ne masque pas le département. */
    @Test
    @Order(5)
    @Transactional
    void legacyEntityRowWithoutSizesStillInherits() {
        DSL.using(dataSource, SQLDialect.POSTGRES).execute(
                "INSERT INTO authorized_sample (water_entity_id, species_id, min_size, max_size, mesh_size) "
                        + "VALUES (?, ?, 25, 1000, NULL)", lemanId, otherSpeciesId);
    }

    @Test
    @Order(6)
    void legacyEntityRowResolvesToDepartmentDefault() {
        assertMaxSize(lemanId, otherSpeciesId, 70);
        assertMeshSize(lemanId, otherSpeciesId, 5);
    }

    @Test
    @Order(7)
    void regionalAdminCannotConfigureAnotherDepartment() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .contentType(ContentType.JSON)
                .body(List.of(Map.of("speciesId", otherSpeciesId, "maxSize", 50)))
                .when().put(REFERENTIAL + "/department/73")
                .then().statusCode(403);

        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .when().get(REFERENTIAL + "/department/73")
                .then().statusCode(200)
                .body("", empty());
    }

    @Test
    @Order(8)
    void maxSizeIsMandatory() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .contentType(ContentType.JSON)
                .body(List.of(Map.of("speciesId", otherSpeciesId, "maxSize", 0)))
                .when().put(REFERENTIAL + "/department/74")
                .then().statusCode(400);
    }

    private void assertMaxSize(UUID waterEntityId, UUID speciesId, int expected) {
        given()
                .queryParam("waterEntityId", waterEntityId)
                .queryParam("speciesId", speciesId)
                .when().get(REFERENTIAL + "/max-size")
                .then().statusCode(200)
                .body(equalTo(String.valueOf(expected)));
    }

    private void assertMeshSize(UUID waterEntityId, UUID speciesId, int expected) {
        given()
                .queryParam("waterEntityId", waterEntityId)
                .queryParam("speciesId", speciesId)
                .when().get(REFERENTIAL + "/mesh-size")
                .then().statusCode(200)
                .body(equalTo(String.valueOf(expected)));
    }
}
