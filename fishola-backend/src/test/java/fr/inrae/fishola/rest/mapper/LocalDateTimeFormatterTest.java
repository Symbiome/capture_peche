package fr.inrae.fishola.rest.mapper;

import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;

/**
 * Ce test permet de valider qu'il n'y a pas de régression sur le format des dates envoyées au front
 * Le format attendu pour la sérialisation des LocalDateTime est un tableau tel que : [année,mois,jour,heure,minute,seconde,...]
 * Ce test doit rester tel quel à moins de changer le code du front !
 */
@QuarkusTest
class LocalDateTimeFormatterTest {

    @Test
    void testOnGlobalDashboard() {
        String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy,M,d,H,m"));
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/global-dashboard")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"computedOn\":[" + formatted));
    }

    @Test
    void testOnKeyFigures() {
        String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy,M,d,H,m"));
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/about/key-figures")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"computedOn\":[" + formatted));
    }

}
