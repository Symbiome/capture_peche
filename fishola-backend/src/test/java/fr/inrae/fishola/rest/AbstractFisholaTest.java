package fr.inrae.fishola.rest;

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
