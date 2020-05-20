package fr.inrae.fishola.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class StatusResourceTest {

    @Test
    public void testStatusEndpoint() {
        given()
                .when().get("/api/v1/status")
                .then()
                .statusCode(200);
    }

}
