package fr.inrae.fishola.rest.metrics;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.security.LoginBean;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class MetricsResourceTest {
    @Inject
    protected FisholaConfiguration config;

    @Test
    public void testMetricsAreNotVisibleForNonAdmins() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/metrics")
                .then()
                .statusCode(401);
    }

    @Test
    public void testMetricsBasicWiring() {
        // Loggin as admin
        AbstractFisholaTest.CookieHandler cookieHandler = new AbstractFisholaTest.CookieHandler();
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("", config.adminPassword()))
                .post("/api/v1/security/admin-login")
                .then()
                .statusCode(204)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, cookieHandler);
        String token = cookieHandler.getValue().orElseThrow();

        // Just test that all expected metrics are here and not null
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/metrics")
                .then()
                .statusCode(200)
                .body("activeUsersPerYear", notNullValue())
                .body("userRegistrationsPerYear",  notNullValue())
                .body("tripsPerLake",  notNullValue())
                .body("catchesPerLake", notNullValue())
                .body("automaticMeasuresPerLake",  notNullValue());

    }
}
