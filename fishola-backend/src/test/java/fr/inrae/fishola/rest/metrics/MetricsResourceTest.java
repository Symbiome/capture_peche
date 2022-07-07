package fr.inrae.fishola.rest.metrics;

import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.security.LoginBean;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

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
                .body("usersPerYear.size()", not(equalTo(0)))
                .body("tripsPerLake.size()", not(equalTo(0)))
                .body("catchesPerLake.size()",not(equalTo(0)))
                .body("automaticMeasuresPerLake.size()", not(equalTo(0)));

    }
}
