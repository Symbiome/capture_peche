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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Bout en bout HTTP des concours (#90) : création par un opérateur cloisonnée par
 * département (#159, comme {@code TripResource}/{@code NewsResource}), et attribution du
 * badge CONCOURS -- y compris le cas qui a motivé le changement de contrainte unique sur
 * {@code gamification_badge_unlock} (V2.5.0) : un même pêcheur débloquant deux fois le même
 * badge pour deux concours différents, sans collision.
 *
 * <p>Auto-suffisant (gabarit {@code OperatorAccessTest}) : provisionne ses propres comptes
 * (opérateur cantonné au département '74' de la fixture, pêcheur dédié).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompetitionResourceTest {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    JwtHelper jwtHelper;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private String operatorToken;
    private String userToken;
    private UUID waterEntityIdInPerimeter;
    private UUID waterEntityIdOutOfPerimeter;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), false, false, true)", operatorId, "competition-test-operator@fishola.test");
        // Périmètre départemental (#159) : sans cette ligne, un compte non-national mais
        // sans aucun département configuré verrait tout (getAllowedAdminDepartments vide).
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", operatorId);

        ctx.execute("INSERT INTO fishola_user (id, first_name, last_name, email, password, created_on, pseudo) "
                        + "VALUES (?, 'Competition', 'Resource', 'competition-resource-test@fishola.test', "
                        + "'$2a$10$j3eTBbEJO9IutgxGIJFMx.uauNH6Z4UR/UeiL62eL5oYMNCSPWYS.', now(), 'competition-resource-test')",
                userId);

        waterEntityIdInPerimeter = ctx.fetchOne("SELECT id FROM water_entity WHERE department = '74' LIMIT 1")
                .get("id", UUID.class);
        waterEntityIdOutOfPerimeter = ctx.fetchOne("SELECT id FROM water_entity WHERE department = '73' LIMIT 1")
                .get("id", UUID.class);

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            operatorToken = jwtHelper.createAdminToken(operatorId);
            userToken = jwtHelper.createUserToken(userId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM gamification_badge_unlock WHERE fishola_user_id = ?", userId);
        ctx.execute("DELETE FROM competition WHERE created_by = ?", operatorId);
        ctx.execute("DELETE FROM fishola_user WHERE id = ?", userId);
        ctx.execute("DELETE FROM fishola_admin_departments WHERE fishola_admin_id = ?", operatorId);
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
    }

    @Test
    void operatorCanCreateAndAttributeACompetitionInOwnPerimeter() {
        UUID competitionId = createCompetition("Concours de la Truite", waterEntityIdInPerimeter, 200);

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(Map.of("userId", userId.toString()))
                .when().post("/api/v1/admin/competitions/" + competitionId + "/attribute")
                .then().statusCode(204);

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get("/api/v1/admin/competitions/" + competitionId + "/participants")
                .then().statusCode(200)
                .body("userId", hasItem(userId.toString()));

        given().cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, userToken)
                .when().get("/api/v1/gamification/me/badges")
                .then().statusCode(200)
                .body("find { it.code == 'CONCOURS' }.unlocked", equalTo(true))
                .body("find { it.code == 'CONCOURS' }.competitionName", equalTo("Concours de la Truite"));
    }

    @Test
    void operatorCannotCreateACompetitionOutsideOwnPerimeter() {
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(Map.of("name", "Concours hors périmètre", "date", "2026-06-01",
                        "waterEntityId", waterEntityIdOutOfPerimeter.toString(), "federationName", "Fédération 73"))
                .when().post("/api/v1/admin/competitions")
                .then().statusCode(403);
    }

    @Test
    void sameFishermanCanUnlockTheConcoursBadgeTwiceForTwoDifferentCompetitions() {
        UUID competition1 = createCompetition("Concours du printemps", waterEntityIdInPerimeter, 200);
        UUID competition2 = createCompetition("Concours d'été", waterEntityIdInPerimeter, 200);

        attribute(competition1, userId);
        attribute(competition2, userId);

        given().cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, userToken)
                .when().get("/api/v1/gamification/me/badges")
                .then().statusCode(200)
                .body("findAll { it.code == 'CONCOURS' }.competitionName", hasItem("Concours du printemps"))
                .body("findAll { it.code == 'CONCOURS' }.competitionName", hasItem("Concours d'été"));
    }

    @Test
    void attributingTwiceToTheSameCompetitionIsIdempotent() {
        UUID competitionId = createCompetition("Concours idempotence", waterEntityIdInPerimeter, 200);

        attribute(competitionId, userId);
        attribute(competitionId, userId);

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get("/api/v1/admin/competitions/" + competitionId + "/participants")
                .then().statusCode(200)
                .body("userId", equalTo(java.util.List.of(userId.toString())));
    }

    @Test
    void searchUsersFindsByPseudo() {
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get("/api/v1/admin/competitions/search-users?q=competition-resource-test")
                .then().statusCode(200)
                .body("id", hasItem(userId.toString()));
    }

    private UUID createCompetition(String name, UUID waterEntityId, int expectedStatus) {
        String id = given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(Map.of("name", name, "date", "2026-06-01",
                        "waterEntityId", waterEntityId.toString(), "federationName", "Fédération 74"))
                .when().post("/api/v1/admin/competitions")
                .then().statusCode(expectedStatus)
                .extract().jsonPath().getString("id");
        return id == null ? null : UUID.fromString(id);
    }

    private void attribute(UUID competitionId, UUID targetUserId) {
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/json")
                .body(Map.of("userId", targetUserId.toString()))
                .when().post("/api/v1/admin/competitions/" + competitionId + "/attribute")
                .then().statusCode(204);
    }
}
