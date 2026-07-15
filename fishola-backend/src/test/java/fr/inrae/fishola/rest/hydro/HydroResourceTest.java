package fr.inrae.fishola.rest.hydro;

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

import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the nearby spatial endpoint (#5). Seeds a small set of
 * hydro geometries around a reference point on the Testcontainers PostGIS DB,
 * and cleans them up afterwards to avoid polluting the shared test database.
 *
 * Reference query point: lat=45.9025, lng=6.1005.
 *   - "IT Fier"  (FLOWING, permanent)   ~38 m
 *   - "IT Lac"   (STILL, water surface) ~756 m
 *   - "IT Ruisseau" (FLOWING, intermittent) far away, near lng=6.300
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HydroResourceTest extends AbstractFisholaTest {

    private static final String NEARBY = "/api/v1/waterEntities/nearby";

    @Inject
    protected AgroalDataSource dataSource;

    @Inject
    protected UsersDao usersDao;

    protected String token;

    // The integration test database is seeded only by the Flyway migrations,
    // which do not create any user. Make this test self-contained (isolable via
    // -Dtest=HydroResourceTest) by provisioning the login user if it is absent.
    @BeforeAll
    @Transactional
    void ensureTestUser() {
        if (usersDao.findByEmail("thimel@codelutin.com").isEmpty()) {
            usersDao.create("Thimel", "Test", "thimel-hydro-test",
                    "thimel@codelutin.com", usersDao.hashPassword("sispea"), false, false);
        }
    }

    @BeforeEach
    void login() {
        this.token = login("thimel@codelutin.com", "sispea");
    }

    @BeforeEach
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        // FLOWING entity with a permanent river section ~38 m from the point.
        ctx.execute("INSERT INTO water_entity (name, kind, geom, water_entity_code, export_as) "
                + "VALUES ('IT Fier', 'FLOWING', "
                + "ST_SetSRID(ST_GeomFromText('LINESTRING(6.101 45.900, 6.101 45.905)'), 4326), "
                + "'IT_FIER', 'IT Fier') ON CONFLICT DO NOTHING");
        ctx.execute("INSERT INTO river_section (water_entity_id, persistent, geom) "
                + "SELECT id, true, ST_SetSRID(ST_GeomFromText('LINESTRING(6.101 45.900, 6.101 45.905)'), 4326) "
                + "FROM water_entity WHERE water_entity_code = 'IT_FIER' "
                + "AND NOT EXISTS (SELECT 1 FROM river_section rs WHERE rs.water_entity_id = water_entity.id)");

        // STILL entity with a water surface ~756 m from the point.
        ctx.execute("INSERT INTO water_entity (name, kind, geom, water_entity_code, export_as) "
                + "VALUES ('IT Lac', 'STILL', "
                + "ST_SetSRID(ST_GeomFromText('MULTIPOLYGON(((6.110 45.900,6.111 45.900,6.111 45.901,6.110 45.901,6.110 45.900)))'), 4326), "
                + "'IT_LAC', 'IT Lac') ON CONFLICT DO NOTHING");
        ctx.execute("INSERT INTO water_surface (water_entity_id, geom) "
                + "SELECT id, ST_SetSRID(ST_GeomFromText('MULTIPOLYGON(((6.110 45.900,6.111 45.900,6.111 45.901,6.110 45.901,6.110 45.900)))'), 4326) "
                + "FROM water_entity WHERE water_entity_code = 'IT_LAC' "
                + "AND NOT EXISTS (SELECT 1 FROM water_surface ws WHERE ws.water_entity_id = water_entity.id)");

        // FLOWING entity with an intermittent (persistent=false) section, far
        // from the reference point (near lng=6.300) for the persistence test.
        ctx.execute("INSERT INTO water_entity (name, kind, geom, water_entity_code, export_as) "
                + "VALUES ('IT Ruisseau', 'FLOWING', "
                + "ST_SetSRID(ST_GeomFromText('LINESTRING(6.300 45.900, 6.300 45.905)'), 4326), "
                + "'IT_RUIS', 'IT Ruisseau') ON CONFLICT DO NOTHING");
        ctx.execute("INSERT INTO river_section (water_entity_id, persistent, geom) "
                + "SELECT id, false, ST_SetSRID(ST_GeomFromText('LINESTRING(6.300 45.900, 6.300 45.905)'), 4326) "
                + "FROM water_entity WHERE water_entity_code = 'IT_RUIS' "
                + "AND NOT EXISTS (SELECT 1 FROM river_section rs WHERE rs.water_entity_id = water_entity.id)");
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM river_section WHERE water_entity_id IN "
                + "(SELECT id FROM water_entity WHERE water_entity_code LIKE 'IT\\_%')");
        ctx.execute("DELETE FROM water_surface WHERE water_entity_id IN "
                + "(SELECT id FROM water_entity WHERE water_entity_code LIKE 'IT\\_%')");
        ctx.execute("DELETE FROM water_entity WHERE water_entity_code LIKE 'IT\\_%'");
    }

    @Test
    void ordersByDistanceWithClosestPointAndPersistence() {
        // AC1 + AC2: Fier (permanent, ~38 m) before Lac (~756 m).
        List<Map<String, Object>> items = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 45.9025)
                .queryParam("lng", 6.1005)
                .queryParam("radiusM", 2000)
                .when().get(NEARBY)
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertTrue(items.size() >= 2, "au moins Fier et Lac attendus");
        assertEquals("IT Fier", items.get(0).get("name"));
        assertEquals("FLOWING", items.get(0).get("kind"));
        assertEquals(Boolean.TRUE, items.get(0).get("persistent"));
        double fierDist = ((Number) items.get(0).get("distanceM")).doubleValue();
        assertTrue(fierDist < 100, "distance Fier attendue < 100 m, obtenue " + fierDist);

        // closest point projeté sur la géométrie (longitude ~6.101).
        Map<String, Object> closest = (Map<String, Object>) items.get(0).get("closestPoint");
        double cpLng = ((Number) closest.get("lng")).doubleValue();
        assertTrue(Math.abs(cpLng - 6.101) < 1e-3, "closestPoint.lng attendu ~6.101, obtenu " + cpLng);

        // Lac présent et plus loin que Fier.
        int lacIndex = indexOfName(items, "IT Lac");
        assertTrue(lacIndex > 0, "IT Lac attendu après IT Fier");
    }

    @Test
    void intermittentSectionFlaggedNonPersistent() {
        // AC2: près du ruisseau intermittent, persistent=false.
        List<Map<String, Object>> items = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 45.9025)
                .queryParam("lng", 6.300)
                .queryParam("radiusM", 500)
                .when().get(NEARBY)
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertEquals("IT Ruisseau", items.get(0).get("name"));
        assertEquals(Boolean.FALSE, items.get(0).get("persistent"));
    }

    @Test
    void emptyWhenNothingWithinRadius() {
        // AC3: aucune entité à moins de 100 m -> 200 + liste vide.
        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 48.0)
                .queryParam("lng", 2.0)
                .queryParam("radiusM", 100)
                .when().get(NEARBY)
                .then().statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    void kindFilterRestrictsResults() {
        // Filtre kind=STILL : Fier (FLOWING) exclu, Lac (STILL) présent.
        List<Map<String, Object>> items = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 45.9025)
                .queryParam("lng", 6.1005)
                .queryParam("radiusM", 2000)
                .queryParam("kind", "STILL")
                .when().get(NEARBY)
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertTrue(indexOfName(items, "IT Fier") < 0, "Fier (FLOWING) doit être exclu");
        assertTrue(indexOfName(items, "IT Lac") >= 0, "Lac (STILL) doit être présent");
    }

    @Test
    void rejectsMissingLatitude() {
        // AC4: lat manquant -> 400.
        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lng", 6.1005)
                .when().get(NEARBY)
                .then().statusCode(400);
    }

    @Test
    void rejectsRadiusOutOfBounds() {
        // AC4: radiusM au-delà de la borne -> 400.
        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 45.9025)
                .queryParam("lng", 6.1005)
                .queryParam("radiusM", 999999)
                .when().get(NEARBY)
                .then().statusCode(400);
    }

    private static int indexOfName(List<Map<String, Object>> items, String name) {
        for (int i = 0; i < items.size(); i++) {
            if (name.equals(items.get(i).get("name"))) {
                return i;
            }
        }
        return -1;
    }
}
