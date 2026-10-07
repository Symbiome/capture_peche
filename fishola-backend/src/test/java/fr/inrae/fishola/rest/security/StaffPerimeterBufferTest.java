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

import fr.inrae.fishola.database.AdminDao;
import fr.inrae.fishola.database.StaffPerimeterDao;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;

/**
 * Périmètre staff élargi (#231) : un staff régional gère tout milieu dont la
 * géométrie intersecte ses départements élargis du buffer (1 km par défaut).
 *
 * <p>S'appuie sur les contours de la fixture ({@code R__test_fixture.sql}) : 73
 * (Savoie) et 74 (Haute-Savoie) se touchent au méridien 6.0 (latitudes 45.70 à 45.75),
 * où 0,005° de longitude ≈ 390 m.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaffPerimeterBufferTest {

    private static final String NAMES_SEARCH = "/api/v1/referential/waterEntities/names/search";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    AdminDao adminDao;

    @Inject
    StaffPerimeterDao staffPerimeterDao;

    private final UUID nationalId = UUID.randomUUID();
    private final UUID regional74Id = UUID.randomUUID();
    private final UUID operator73Id = UUID.randomUUID();
    private String nationalToken;
    private String regional74Token;
    private String operator73Token;

    private UUID nearId;
    private UUID farId;
    private UUID frontierId;

    @BeforeAll
    @Transactional
    void seed() {
        insertAdmin(nationalId, "perimeter-national@fishola.test", true, false, null);
        insertAdmin(regional74Id, "perimeter-regional74@fishola.test", false, false, "74");
        insertAdmin(operator73Id, "perimeter-operator73@fishola.test", false, true, "73");

        nearId = insertWaterEntity("IT Perim Proche", "IT_PPROCHE", "73", "POINT(5.995 45.72)");
        farId = insertWaterEntity("IT Perim Loin", "IT_PLOIN", "73", "POINT(5.97 45.72)");
        frontierId = insertWaterEntity("IT Perim Frontiere", "IT_PFRONT", "74", "LINESTRING(5.95 45.72, 6.05 45.72)");

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalId);
            regional74Token = jwtHelper.createAdminToken(regional74Id);
            operator73Token = jwtHelper.createAdminToken(operator73Id);
        } finally {
            requestContext.deactivate();
        }
    }

    private void insertAdmin(UUID id, String email, boolean national, boolean operator, String department) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, ?, ?)", id, email, "x", national, operator);
        if (department != null) {
            ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                    id, department);
        }
    }

    private UUID insertWaterEntity(String name, String code, String department, String wkt) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchOne("INSERT INTO water_entity (name, export_as, water_entity_code, kind, department, geom) "
                        + "VALUES (?, ?, ?, 'STILL', ?, ST_SetSRID(ST_GeomFromText(?), 4326)) RETURNING id",
                name, name, code, department, wkt).get("id", UUID.class);
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM water_entity WHERE id IN (?, ?, ?)", nearId, farId, frontierId);
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?, ?)", nationalId, regional74Id, operator73Id);
    }

    private List<String> searchNames(String token) {
        return given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("q", "IT Perim")
                .when().get(NAMES_SEARCH)
                .then().statusCode(200)
                .extract().jsonPath().getList("name", String.class);
    }

    @Test
    void neighbourWithinBufferIsInPerimeterButNotBeyond() {
        List<String> names = searchNames(regional74Token);
        Assertions.assertTrue(names.contains("IT Perim Proche"), "milieu du 73 à ~390 m du 74 attendu : " + names);
        Assertions.assertFalse(names.contains("IT Perim Loin"), "milieu du 73 à ~2,3 km du 74 hors périmètre : " + names);
    }

    @Test
    void frontierRiverIsManagedByBothDepartments() {
        Assertions.assertTrue(searchNames(regional74Token).contains("IT Perim Frontiere"));
        Assertions.assertTrue(searchNames(operator73Token).contains("IT Perim Frontiere"));
    }

    @Test
    void nationalAdminIsNotRestricted() {
        Assertions.assertTrue(searchNames(nationalToken)
                .containsAll(List.of("IT Perim Proche", "IT Perim Loin", "IT Perim Frontiere")));
    }

    @Test
    @Transactional
    void allowedWaterEntitiesForImportsUseTheBuffer() {
        Set<UUID> allowed = adminDao.getAllowedWaterEntityIds(regional74Id);
        Assertions.assertTrue(allowed.containsAll(Set.of(nearId, frontierId)));
        Assertions.assertFalse(allowed.contains(farId));
    }

    @Test
    void waterEntityListingUsesTheBuffer() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, regional74Token)
                .when().get("/api/v1/referential/waterEntities/names")
                .then().statusCode(200)
                .body("name", hasItems("Annecy", "IT Perim Proche", "IT Perim Frontiere"))
                .body("name", not(hasItem("IT Perim Loin")))
                .body("name", not(hasItem("Bourget")));
    }

    @Test
    void bufferDistanceIsConfigurable() {
        try {
            Assertions.assertTrue(staffPerimeterDao.syncBufferDistance(3000));
            Assertions.assertTrue(searchNames(regional74Token).contains("IT Perim Loin"),
                    "milieu à ~2,3 km dans un buffer de 3 km");
            Assertions.assertFalse(staffPerimeterDao.syncBufferDistance(3000), "pas de recalcul sans changement");
        } finally {
            staffPerimeterDao.syncBufferDistance(1000);
        }
        Assertions.assertFalse(searchNames(regional74Token).contains("IT Perim Loin"));
    }
}
