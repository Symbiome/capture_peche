package fr.inrae.fishola.rest;

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

import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.rest.security.LoginBean;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.hamcrest.CustomMatcher;

import java.util.Optional;

import static io.restassured.RestAssured.given;

public abstract class AbstractFisholaTest {

    @Inject
    FisholaConfiguration config;

    public static class CookieHandler extends CustomMatcher {

        protected String value;

        public Optional<String> getValue() {
            return Optional.ofNullable(value);
        }

        public CookieHandler() {
            super("CookieHandler");
        }

        @Override
        public boolean matches(Object actual) {
            if (!(actual instanceof String)) {
                return false;
            }
            this.value = (String)actual;
            return true;
        }
    }

    protected String login(String email, String password) {
        CookieHandler cookieHandler = new CookieHandler();
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean(email, password))
                .post("/api/v1/security/login")
            .then()
                .statusCode(200)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, cookieHandler);
        String result = cookieHandler.getValue().orElseThrow();
        return result;
    }

    protected String loginAsAdmin() {
        AbstractFisholaTest.CookieHandler cookieHandler = new AbstractFisholaTest.CookieHandler();
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("amorel@codelutin.com", config.adminPassword()))
                .post("/api/v1/admin/login")
                .then()
                .statusCode(204)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, cookieHandler);
        String result = cookieHandler.getValue().orElseThrow();
        return result;
    }

}
