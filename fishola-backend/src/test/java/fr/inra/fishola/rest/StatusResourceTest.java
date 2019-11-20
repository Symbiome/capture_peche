package fr.inra.fishola.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class StatusResourceTest {

    @Test
    public void testStatusEndpoint() {
        given()
          .when().get("/status")
          .then()
             .statusCode(200)
             .body(CoreMatchers.containsString("\"javaVersion\":\"13"));
    }

}