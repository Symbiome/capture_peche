package fr.inrae.fishola.rest.security;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.rest.security.admin.ChangePasswordBean;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class AdminResourceTest extends AbstractFisholaTest {

    @Test
    void testNationalAdminLogin() {
        CookieHandler cookieHandler = new CookieHandler();
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("amorel@codelutin.com", "whatever"))
                .post("/api/v1/admin/login")
            .then()
                .statusCode(204)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, cookieHandler);
        Assertions.assertTrue(cookieHandler.getValue().isPresent());
    }

    @Test
    void testAdminLoginNotFound() {
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("amurel@codelutin.com", "whatever"))
                .post("/api/v1/admin/login")
            .then()
                .statusCode(404);
    }

    @Test
    void testAdminLoginFail() {
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("amorel@codelutin.com", "whatevor"))
                .post("/api/v1/admin/login")
            .then()
                .statusCode(401);
    }

    // --- Changement de mot de passe nominatif (#55) -------------------------
    // "whatever" = mot de passe national PARTAGÉ en test (cf. testNationalAdminLogin) ;
    // accepté comme « ancien » via le repli déprécié tant que le compte n'a pas de
    // mot de passe nominatif.

    @Test
    void changePassword_succeedsAndEnablesNominativeLogin() {
        String adminCookie = loginAsAdmin();
        String nominatif = "nominatif-test-123";

        // Change le mot de passe (ancien = partagé, accepté via le repli).
        given()
            .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminCookie)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChangePasswordBean("whatever", nominatif))
            .when()
                .put("/api/v1/admin/password")
            .then()
                .statusCode(204);

        // Le nouveau fonctionne à la connexion → auth nominative (bcrypt du compte).
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new LoginBean("amorel@codelutin.com", nominatif))
            .when()
                .post("/api/v1/admin/login")
            .then()
                .statusCode(204);

        // Hygiène : restaure le mot de passe partagé pour ne pas polluer les autres tests.
        given()
            .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminCookie)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChangePasswordBean(nominatif, "whatever"))
            .when()
                .put("/api/v1/admin/password")
            .then()
                .statusCode(204);
    }

    @Test
    void changePassword_wrongOldPassword_returns400() {
        String adminCookie = loginAsAdmin();
        given()
            .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminCookie)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChangePasswordBean("ancien-totalement-faux", "valide-123"))
            .when()
                .put("/api/v1/admin/password")
            .then()
                .statusCode(400)
                .body("oldPassword", notNullValue());
    }

    @Test
    void changePassword_invalidNewPassword_returns400() {
        String adminCookie = loginAsAdmin();
        given()
            .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminCookie)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ChangePasswordBean("whatever", "abc")) // < 6 caractères
            .when()
                .put("/api/v1/admin/password")
            .then()
                .statusCode(400)
                .body("newPassword", notNullValue());
    }

}
