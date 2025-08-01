package fr.inrae.fishola.rest.security;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class SecurityResourceTest extends AbstractFisholaTest {

    @Test
    void testLogin() {
        CookieHandler cookieHandler = new CookieHandler();
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("thimel@codelutin.com", "sispea"))
                .post("/api/v1/security/login")
            .then()
                .statusCode(200)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, cookieHandler);
        Assertions.assertTrue(cookieHandler.getValue().isPresent());
    }

    @Test
    void testLoginNotFound() {
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("thumel@codelutin.com", "sispea"))
                .post("/api/v1/security/login")
            .then()
                .statusCode(404);
    }

    @Test
    void testLoginFail() {
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("thimel@codelutin.com", "sispeO"))
                .post("/api/v1/security/login")
            .then()
                .statusCode(401);
    }

}
