package fr.inrae.fishola.rest.security.admin;

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

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * Secteurs favoris des comptes staff (#236) : ajout, retrait et liste propres à chaque
 * compte, favoris en tête de la recherche de secteur, périmètre départemental respecté.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminFavoriteWaterEntitiesResourceTest {

    private static final String URI = "/api/v1/admin/favorite-water-entities";
    private static final String NAMES_SEARCH = "/api/v1/referential/waterEntities/names/search";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID nationalAdminId = UUID.randomUUID();
    private final UUID otherNationalAdminId = UUID.randomUUID();
    private final UUID operatorId = UUID.randomUUID();
    /** Correspondance exacte de la recherche : en tête sans favori. */
    private final UUID exactMatchId = UUID.randomUUID();
    /** Correspondance partielle, mise en favori pour passer en tête. */
    private final UUID partialMatchId = UUID.randomUUID();
    private String nationalToken;
    private String otherNationalToken;
    private String operatorToken;
    private UUID bourgetId;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        bourgetId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Bourget'").get("id", UUID.class);
        String annecyDepartment = ctx.fetchOne("SELECT department FROM water_entity WHERE name = 'Annecy'")
                .get("department", String.class);
        ctx.execute("INSERT INTO water_entity (id, name, export_as) VALUES (?, ?, ?), (?, ?, ?)",
                exactMatchId, "Zzfavtest", "zzfavtest-exact", partialMatchId, "Zzfavtest du Nord",
                "zzfavtest-partial");
        insertAdmin(ctx, nationalAdminId, "fav-test-national@fishola.test", true);
        insertAdmin(ctx, otherNationalAdminId, "fav-test-national-2@fishola.test", true);
        insertAdmin(ctx, operatorId, "fav-test-op@fishola.test", false);
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                operatorId, annecyDepartment);

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalAdminId);
            otherNationalToken = jwtHelper.createAdminToken(otherNationalAdminId);
            operatorToken = jwtHelper.createAdminToken(operatorId);
        } finally {
            requestContext.deactivate();
        }
    }

    private void insertAdmin(org.jooq.DSLContext ctx, UUID id, String email, boolean national) {
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, "
                + "is_operator) VALUES (?, ?, ?, now(), false, ?, ?)", id, email, "x", national, !national);
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?, ?)", nationalAdminId, otherNationalAdminId,
                operatorId);
        ctx.execute("DELETE FROM water_entity WHERE id IN (?, ?)", exactMatchId, partialMatchId);
    }

    private io.restassured.response.ValidatableResponse put(String token, UUID waterEntityId) {
        return given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .when().put(URI + "/" + waterEntityId)
                .then();
    }

    private io.restassured.response.ValidatableResponse search(String token) {
        return given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .when().get(NAMES_SEARCH + "?q=Zzfavtest")
                .then().statusCode(200);
    }

    @Test
    void favoriteComesFirstInSearchForItsOwnerOnly() {
        search(nationalToken).body("[0].id", equalTo(exactMatchId.toString()));

        put(nationalToken, partialMatchId).statusCode(204);
        try {
            search(nationalToken).body("[0].id", equalTo(partialMatchId.toString()));
            search(otherNationalToken).body("[0].id", equalTo(exactMatchId.toString()));
        } finally {
            given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                    .when().delete(URI + "/" + partialMatchId)
                    .then().statusCode(204);
        }
        search(nationalToken).body("[0].id", equalTo(exactMatchId.toString()));
    }

    @Test
    void favoritesAreListedAndRemoved() {
        put(otherNationalToken, partialMatchId).statusCode(204);
        put(otherNationalToken, partialMatchId).statusCode(204);
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, otherNationalToken)
                .when().get(URI)
                .then().statusCode(200)
                .body("id", hasItem(partialMatchId.toString()))
                .body("size()", equalTo(1));

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, otherNationalToken)
                .when().delete(URI + "/" + partialMatchId)
                .then().statusCode(204);
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, otherNationalToken)
                .when().get(URI)
                .then().statusCode(200)
                .body("", empty());
    }

    @Test
    void operatorCannotFavoriteOutsideItsPerimeter() {
        put(operatorToken, bourgetId).statusCode(403);
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(URI)
                .then().statusCode(200)
                .body("id", not(hasItem(bourgetId.toString())));
    }

    @Test
    void unknownWaterEntityIsNotFound() {
        put(nationalToken, UUID.randomUUID()).statusCode(404);
    }
}
