package fr.inrae.fishola.rest.gamification;

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
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Bout en bout HTTP (#146) : le point d'accroche dans {@code TripResource} déclenche
 * réellement l'évaluation des badges à la création d'une sortie pêcheur, et l'attribution
 * manuelle admin fonctionne (« Testeur émérite »). Pendant de {@code GamificationEngineTest}
 * (logique pure) pour la plomberie REST.
 *
 * <p>Auto-suffisant : provisionne son propre pêcheur (distinct de {@code thimel}, réutilisé
 * par d'autres suites pour des sorties qui fausseraient les comptages).
 *
 * <p>Les jetons sont forgés via {@link JwtHelper} plutôt qu'obtenus par un aller-retour HTTP
 * réel dans {@code @BeforeAll} (gabarit {@code OperatorAccessTest}/{@code AuditLogTest} :
 * un login HTTP réel à ce stade du cycle de vie JUnit se heurte à la synchronisation du port
 * RestAssured et échoue en connexion refusée).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GamificationResourceTest extends AbstractFisholaTest {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    JwtHelper jwtHelper;

    private final UUID userId = UUID.randomUUID();
    private String userToken;
    private String adminToken;
    private UUID waterEntityId;
    private UUID techniqueId;
    private UUID speciesId;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        // Hash bcrypt de « sispea », réutilisé de R__test_fixture.sql (pêcheur thimel) --
        // ce compte-ci est dédié à cette suite, jamais utilisé pour créer des sorties ailleurs.
        ctx.execute("INSERT INTO fishola_user (id, first_name, last_name, email, password, created_on, pseudo) "
                        + "VALUES (?, 'Gamification', 'Resource', 'gamification-resource-test@fishola.test', "
                        + "'$2a$10$j3eTBbEJO9IutgxGIJFMx.uauNH6Z4UR/UeiL62eL5oYMNCSPWYS.', now(), 'gamif-resource-test')",
                userId);
        waterEntityId = ctx.fetchOne("SELECT id FROM water_entity LIMIT 1").get("id", UUID.class);
        techniqueId = ctx.fetchOne("SELECT id FROM technique LIMIT 1").get("id", UUID.class);
        speciesId = ctx.fetchOne("SELECT id FROM species LIMIT 1").get("id", UUID.class);
        UUID nationalAdminId = ctx.fetchOne("SELECT id FROM fishola_admin WHERE email = 'amorel@codelutin.com'")
                .get("id", UUID.class);

        // JwtHelper est @RequestScoped : on active un contexte de requête le temps de
        // forger les jetons (hors flux HTTP réel dans @BeforeAll). Idem AuditLogTest.
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            userToken = jwtHelper.createUserToken(userId);
            adminToken = jwtHelper.createAdminToken(nationalAdminId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM gamification_badge_unlock WHERE fishola_user_id = ?", userId);
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE owner_id = ?)", userId);
        ctx.execute("DELETE FROM trip WHERE owner_id = ?", userId);
        ctx.execute("DELETE FROM fishola_user WHERE id = ?", userId);
    }

    @Test
    void creatingATripThroughTheRealEndpointTriggersBadgeEvaluation() {
        Map<String, Object> catchBean = Map.of(
                "id", "c1",
                "speciesId", speciesId.toString(),
                "techniqueId", techniqueId.toString(),
                "size", 25,
                "quantity", 1,
                "keep", true,
                "caughtAt", "08:05");
        Map<String, Object> tripBean = Map.ofEntries(
                Map.entry("id", "t1"),
                Map.entry("mode", "Afterwards"),
                Map.entry("type", "Border"),
                Map.entry("name", "Sortie #146"),
                Map.entry("waterEntityId", waterEntityId.toString()),
                Map.entry("speciesIds", List.of()),
                Map.entry("techniqueIds", List.of()),
                Map.entry("date", "2026-07-05"),
                Map.entry("startedAt", "08:00"),
                Map.entry("finishedAt", "11:00"),
                Map.entry("catchs", List.of(catchBean)),
                Map.entry("source", "web"));

        given().cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, userToken)
                .contentType("application/json")
                .body(tripBean)
                .when().post("/api/v1/trips/")
                .then().statusCode(201);

        given().cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, userToken)
                .when().get("/api/v1/gamification/me/badges")
                .then().statusCode(200)
                .body("find { it.code == 'PRISE_ECLAIR' }.unlocked", org.hamcrest.CoreMatchers.equalTo(true))
                .body("find { it.code == 'RECORD_TAILLE' }.unlocked", org.hamcrest.CoreMatchers.equalTo(true));
    }

    @Test
    void adminCanManuallyAttributeTheManualBadge() {
        UUID badgeId = given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/admin/gamification/badges")
                .then().statusCode(200)
                .body("code", hasItem("TESTEUR_EMERITE"))
                .extract().jsonPath().getList("findAll { it.code == 'TESTEUR_EMERITE' }.id", String.class)
                .stream().findFirst().map(UUID::fromString).orElseThrow();

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType("application/json")
                .body(Map.of("userId", userId.toString()))
                .when().post("/api/v1/admin/gamification/badges/" + badgeId + "/attribute")
                .then().statusCode(204);

        given().cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, userToken)
                .when().get("/api/v1/gamification/me/badges")
                .then().statusCode(200)
                .body("find { it.code == 'TESTEUR_EMERITE' }.unlocked", org.hamcrest.CoreMatchers.equalTo(true));
    }

    @Test
    void adminCannotManuallyAttributeAnAutomaticBadge() {
        UUID badgeId = given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/admin/gamification/badges")
                .then().statusCode(200)
                .extract().jsonPath().getList("findAll { it.code == 'RECORD_POIDS' }.id", String.class)
                .stream().findFirst().map(UUID::fromString).orElseThrow();

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType("application/json")
                .body(Map.of("userId", userId.toString()))
                .when().post("/api/v1/admin/gamification/badges/" + badgeId + "/attribute")
                .then().statusCode(400);
    }

    @Test
    void attributingToAnUnknownUserIsRejected() {
        UUID badgeId = given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/admin/gamification/badges")
                .then().statusCode(200)
                .extract().jsonPath().getList("findAll { it.code == 'TESTEUR_EMERITE' }.id", String.class)
                .stream().findFirst().map(UUID::fromString).orElseThrow();

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType("application/json")
                .body(Map.of("userId", UUID.randomUUID().toString()))
                .when().post("/api/v1/admin/gamification/badges/" + badgeId + "/attribute")
                .then().statusCode(404);
    }
}
