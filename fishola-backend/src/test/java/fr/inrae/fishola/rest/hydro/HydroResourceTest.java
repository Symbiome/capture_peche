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

import fr.inrae.fishola.database.HydroSearchDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.trips.TripBean;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Inject
    protected HydroSearchDao hydroSearchDao;

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

        // Commune (INSEE test 99999) enveloppant le Fier, pour byCommune / search.
        ctx.execute("INSERT INTO commune (insee_com, name, geom) "
                + "VALUES ('99999', 'Annecy', "
                + "ST_SetSRID(ST_GeomFromText('MULTIPOLYGON(((6.098 45.898,6.106 45.898,6.106 45.907,6.098 45.907,6.098 45.898)))'), 4326)) "
                + "ON CONFLICT DO NOTHING");

        // Entité au nom accentué, pour la recherche insensible aux accents (#7).
        ctx.execute("INSERT INTO water_entity (name, kind, geom, water_entity_code, export_as) "
                + "VALUES ('IT Léman', 'STILL', ST_SetSRID(ST_GeomFromText('POINT(6.2 46.4)'), 4326), "
                + "'IT_LEMAN', 'IT Léman') ON CONFLICT DO NOTHING");
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
        ctx.execute("DELETE FROM commune WHERE insee_com = '99999'");
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

    @Test
    void byCommuneReturnsIntersectingEntities() {
        // AC1 + AC4: le Fier (traverse la commune) présent, le Ruisseau (loin) absent.
        List<Map<String, Object>> items = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("insee", "99999")
                .when().get("/api/v1/waterEntities/byCommune")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertTrue(indexOfName(items, "IT Fier") >= 0, "le Fier (traversant la commune) doit être présent");
        assertTrue(indexOfName(items, "IT Ruisseau") < 0, "le ruisseau lointain doit être absent");
    }

    @Test
    void communeSearchIsTypoTolerant() {
        // AC2: recherche trigram tolérante à la faute de frappe.
        List<Map<String, Object>> items = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("q", "anneci")
                .when().get("/api/v1/communes/search")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertTrue(items.size() >= 1, "au moins une commune attendue");
        assertEquals("Annecy", items.get(0).get("name"));
    }

    @Test
    void searchFindsEntityBySubstring() {
        // AC1: recherche par sous-chaîne du nom.
        List<Map<String, Object>> items = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("q", "fier")
                .when().get("/api/v1/waterEntities/search")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertTrue(indexOfName(items, "IT Fier") >= 0, "IT Fier attendu pour q=fier");
    }

    @Test
    void searchIsAccentInsensitive() {
        // AC2: 'leman' (sans accent) doit retrouver 'IT Léman'.
        List<Map<String, Object>> items = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("q", "leman")
                .when().get("/api/v1/waterEntities/search")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertTrue(indexOfName(items, "IT Léman") >= 0, "IT Léman attendu pour q=leman (unaccent)");
    }

    @Test
    void searchRejectsShortQuery() {
        // AC3: q d'un seul caractère -> 400.
        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("q", "a")
                .when().get("/api/v1/waterEntities/search")
                .then().statusCode(400);
    }

    @Test
    void hydroTileContainsFeaturesOverFier() {
        // La tuile couvrant le Fier doit être non vide (2 layers ST_AsMVT).
        int z = 12;
        int x = lon2tileX(6.101, z);
        int y = lat2tileY(45.9025, z);
        byte[] body = given()
                .when().get("/api/v1/tiles/hydro/" + z + "/" + x + "/" + y + ".pbf")
                .then().statusCode(200)
                .extract().body().asByteArray();
        assertTrue(body.length > 0, "tuile non vide attendue au-dessus du Fier");
    }

    @Test
    void hydroTileEmptyFarFromFeatures() {
        // Une tuile hors de toute géométrie renvoie 200 + corps vide.
        byte[] body = given()
                .when().get("/api/v1/tiles/hydro/12/0/0.pbf")
                .then().statusCode(200)
                .extract().body().asByteArray();
        assertEquals(0, body.length, "tuile vide attendue hors emprise");
    }

    @Test
    void hydroTileNotModifiedWithEtag() {
        // AC5 : seconde requête avec l'ETag -> 304.
        int z = 12;
        int x = lon2tileX(6.101, z);
        int y = lat2tileY(45.9025, z);
        String path = "/api/v1/tiles/hydro/" + z + "/" + x + "/" + y + ".pbf";
        String etag = given().when().get(path)
                .then().statusCode(200).extract().header("ETag");
        given().header("If-None-Match", etag)
                .when().get(path)
                .then().statusCode(304);
    }

    @Test
    void hydroTileEmptyBelowMinZoom() {
        // Sous le zoom minimum, tuile vide renvoyée sans requête (garde perf/DoS).
        byte[] body = given()
                .when().get("/api/v1/tiles/hydro/5/16/11.pbf")
                .then().statusCode(200)
                .extract().body().asByteArray();
        assertEquals(0, body.length, "tuile vide attendue sous le zoom minimum");
    }

    // ----- Attribution (#9) -------------------------------------------------

    private static final String ATTRIBUTION = "/api/v1/waterEntities/attribution";

    @Test
    void attributionProposesClosestWithSectionAndAlternatives() {
        // AC1 : près du Fier (permanent), la proposition = Fier, avec le tronçon
        // rattaché ; le Lac figure dans les alternatives (corriger sans re-chercher).
        var response = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 45.9025)
                .queryParam("lng", 6.1005)
                .when().get(ATTRIBUTION)
                .then().statusCode(200)
                .extract().jsonPath();

        assertEquals("IT Fier", response.getString("proposal.name"));
        assertEquals("FLOWING", response.getString("proposal.kind"));
        assertEquals(Boolean.TRUE, response.getBoolean("proposal.persistent"));
        assertNotNull(response.getString("proposal.riverSectionId"),
                "le tronçon le plus proche doit être renseigné pour un cours d'eau");
        double dist = response.getDouble("proposal.distanceM");
        assertTrue(dist < 100, "distance Fier attendue < 100 m, obtenue " + dist);

        List<Map<String, Object>> alternatives = response.getList("alternatives");
        assertTrue(indexOfName(alternatives, "IT Lac") >= 0,
                "le Lac doit figurer parmi les alternatives");
    }

    @Test
    void attributionWarnsNonPermanent() {
        // AC3 : près du ruisseau intermittent, la proposition porte persistent=false
        // (l'UI affiche l'avertissement, non bloquant).
        var response = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 45.9025)
                .queryParam("lng", 6.2995)
                .when().get(ATTRIBUTION)
                .then().statusCode(200)
                .extract().jsonPath();

        assertEquals("IT Ruisseau", response.getString("proposal.name"));
        assertEquals(Boolean.FALSE, response.getBoolean("proposal.persistent"));
    }

    @Test
    void attributionRequiresLatLng() {
        given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("lat", 45.9025)
                .when().get(ATTRIBUTION)
                .then().statusCode(400);
    }

    @Test
    @Transactional
    void computeTripAttributionConfirmsWhenChosenIsClosest() {
        // AC1 (logique serveur) : entité choisie = plus proche -> CONFIRMED, avec
        // point projeté sur la géométrie et tronçon renseigné.
        UUID fier = entityId("IT_FIER");
        HydroSearchDao.TripAttribution attr =
                hydroSearchDao.computeTripAttribution(45.9025, 6.1005, fier).orElseThrow();
        assertEquals("CONFIRMED", attr.hydroValidation());
        assertNotNull(attr.riverSectionId(), "tronçon attendu pour un cours d'eau");
        assertTrue(Math.abs(attr.snappedLng() - 6.101) < 1e-3,
                "point projeté attendu ~6.101, obtenu " + attr.snappedLng());
    }

    @Test
    @Transactional
    void computeTripAttributionOverriddenWhenChosenIsNotClosest() {
        // AC2 (logique serveur) : près du Fier mais on choisit le Lac -> OVERRIDDEN ;
        // snapped recalculé sur le Lac (surface -> pas de tronçon).
        UUID lac = entityId("IT_LAC");
        HydroSearchDao.TripAttribution attr =
                hydroSearchDao.computeTripAttribution(45.9025, 6.1005, lac).orElseThrow();
        assertEquals("OVERRIDDEN", attr.hydroValidation());
        assertNull(attr.riverSectionId(), "pas de tronçon pour une surface");
    }

    @Test
    void createTripPersistsAndReadsBackHydroAttribution() {
        // #9 bout en bout via l'API. AC1 + AC5 : sortie saisie sur la carte (point
        // près du Fier) -> CONFIRMED + point projeté + tronçon persistés et relus.
        // AC4 : sortie sans position -> champs hydro NULL. Espèces/techniques vides
        // pour rester indépendant du référentiel (seedé hors de ce test).
        UUID fier = entityId("IT_FIER");

        TripBean trip = new TripBean();
        trip.id = "dontcare";
        trip.date = LocalDate.now();
        trip.startedAt = "00:00";
        trip.finishedAt = "00:01";
        trip.waterEntityId = fier;
        trip.name = "IT Sortie carte";
        trip.type = TripType.Craft;
        trip.mode = TripMode.Live;
        trip.speciesIds = Set.of();
        trip.techniqueIds = Set.of();
        trip.beginLatitude = Optional.of(45.9025);
        trip.beginLongitude = Optional.of(6.1005);

        List<String> created = new ArrayList<>();
        try {
            String withPos = given()
                    .contentType("application/json")
                    .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                    .body(trip)
                    .when().post("/api/v1/trips")
                    .then().statusCode(201)
                    .extract().path("dontcare");
            created.add(withPos);

            given()
                    .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                    .when().get("/api/v1/trips/" + withPos)
                    .then().statusCode(200)
                    .body("hydroValidation", equalTo("CONFIRMED"))
                    .body("riverSectionId", notNullValue())
                    .body("snappedLatitude", notNullValue())
                    .body("snappedLongitude", notNullValue());

            // Sans position (AC4) : comportement inchangé, champs hydro NULL.
            trip.beginLatitude = Optional.empty();
            trip.beginLongitude = Optional.empty();
            trip.name = "IT Sortie sans carte";
            String noPos = given()
                    .contentType("application/json")
                    .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                    .body(trip)
                    .when().post("/api/v1/trips")
                    .then().statusCode(201)
                    .extract().path("dontcare");
            created.add(noPos);

            given()
                    .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                    .when().get("/api/v1/trips/" + noPos)
                    .then().statusCode(200)
                    .body("hydroValidation", nullValue())
                    .body("snappedLatitude", nullValue());
        } finally {
            // Sortie sans capture ni espèce/technique : suppression directe FK-safe.
            var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
            created.forEach(id -> ctx.execute("DELETE FROM trip WHERE id = ?::uuid", id));
        }
    }

    private UUID entityId(String code) {
        Record record = DSL.using(dataSource, SQLDialect.POSTGRES)
                .resultQuery("SELECT id FROM water_entity WHERE water_entity_code = ?", code)
                .fetchOne();
        return record == null ? null : record.get(0, UUID.class);
    }

    private static int lon2tileX(double lon, int z) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << z));
    }

    private static int lat2tileY(double lat, int z) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor(
                (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << z));
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
