package fr.inra.fishola.rest.referential;

import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class ReferentialResourceTest {

    @Test
    public void testGetLakes() {
        given()
                .when().get("/api/v1/referential/lakes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"Lac d'Annecy\""));
        given()
                .when().get("/api/v1/referential/lakes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"Léman\""));
        given()
                .when().get("/api/v1/referential/lakes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"Lac du Bourget\""));
        given()
                .when().get("/api/v1/referential/lakes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"Lac d'Aiguebelette\""));
    }

}