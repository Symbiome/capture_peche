package fr.inrae.fishola.rest;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.rest.security.LoginBean;
import org.hamcrest.CustomMatcher;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

import static io.restassured.RestAssured.given;

public abstract class AbstractFisholaTest {

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
                .body(new LoginBean("thimel@codelutin.com", "sispea"))
                .post("/api/v1/security/login")
            .then()
                .statusCode(200)
                .cookie(AbstractFisholaResource.AUTHENTICATION_COOKIE_NAME, cookieHandler);
        String result = cookieHandler.getValue().orElseThrow();
        return result;
    }
}
