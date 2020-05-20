package fr.inrae.fishola.rest.security;

import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.assertion.CookieMatcher;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import java.util.Optional;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class SecurityResourceTest extends AbstractFisholaTest {

    @Test
    public void testLogin() {
        CookieHandler cookieHandler = new CookieHandler();
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("thimel@codelutin.com", "sispea"))
                .post("/api/v1/security/login")
            .then()
                .statusCode(200)
                .cookie(AbstractFisholaResource.AUTHENTICATION_COOKIE_NAME, cookieHandler);
    }

    @Test
    public void testLoginNotFound() {
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("thumel@codelutin.com", "sispea"))
                .post("/api/v1/security/login")
            .then()
                .statusCode(404);
    }

    @Test
    public void testLoginFail() {
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean("thimel@codelutin.com", "sispeO"))
                .post("/api/v1/security/login")
            .then()
                .statusCode(401);
    }

}
