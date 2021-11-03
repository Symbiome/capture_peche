package fr.inrae.fishola.rest.trips;

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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.JwtHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class TripResourceTest extends AbstractFisholaTest {

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;
    @Inject
    protected JwtHelper jwtHelper;

    protected List<Lake> lakes;
    protected List<Species> species;
    protected List<Technique> techniques;
    protected String token;

    @BeforeEach
    @Transactional
    public void loadReferentials() {
        this.lakes = referentialDao.listLakes();
        this.species = referentialDao.listBuiltInSpecies();
        this.techniques = referentialDao.listBuiltInTechniques();
    }

    @BeforeEach
    public void login() {
        this.token = login("thimel@codelutin.com", "sispea");
    }

    @Transactional
    protected int countTrips() {
        UUID userId = jwtHelper.verifyToken(token);
        int result = tripsDao.countMyTrips(userId);
        return result;
    }

    @Test
    public void testListMyTrips() {
        int count = countTrips();

        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips?pageNumber=0&pageSize=-1&desc=false")
            .then()
                .statusCode(200)
                .body("count", equalTo(count));
    }

    protected TripBean buildValidTripBean() {
        TripBean trip = new TripBean();

        trip.id = "dontcare";
        trip.date = LocalDate.now();
        trip.startedAt = "00:00";
        trip.finishedAt = "00:01";
        trip.lakeId = this.lakes.iterator().next().getId();
        trip.name = "Whatever";
        trip.type = TripType.Craft;
        trip.mode = TripMode.Live;
        trip.speciesIds = this.species.stream().limit(1).map(Species::getId).collect(ImmutableSet.toImmutableSet());
        trip.techniqueIds = this.techniques.stream().limit(1).map(Technique::getId).collect(ImmutableSet.toImmutableSet());
        return trip;
    }

    @Test
    public void testCreateEmptyTrip() {
        int countBefore = countTrips();

        TripBean trip = buildValidTripBean();

        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .post("/api/v1/trips")
            .then()
                .statusCode(201)
                // On reçoit la map de replacement dontcare -> nouvel-id telle que :
                //  {"dontcare": "2244331f-f9dc-4102-b832-be7d69b7c377"}
                .body(trip.id, notNullValue());


        int countAfter = countTrips();
        Assertions.assertEquals(countBefore + 1, countAfter);

    }

    /**
     * Le test se base sur le fait que la création d'une sortie et de ses captures se fait en 2 temps : d'abord la
     * création de la sortie, puis création des captures.
     * Dans le test ci-dessous, la sortie est valide mais pas la capture, la création de la sortie en base est donc bien
     * faite mais comme la création de la capture échoue, le rollback vient annuler la création de la sortie.
     */
    @Test
    public void testTransaction() {
        int countBefore = countTrips();

        TripBean trip = buildValidTripBean();

        trip.catchs = new LinkedList<>();
        CatchBean catchBean = new CatchBean();
        catchBean.keep = true;
        catchBean.weight = Optional.of(123);
        catchBean.size = Optional.of(123);
        catchBean.techniqueId = this.techniques.iterator().next().getId();
        // On met un faux identifiant d'espèce
        catchBean.speciesId = Optional.of(UUID.randomUUID().toString());
        trip.catchs.add(catchBean);

        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .post("/api/v1/trips")
            .then()
                .statusCode(500);

        // La création n'a pas pu se faire, on vérifie que la sortie n'a pas été créée
        int countAfter = countTrips();
        Assertions.assertEquals(countBefore, countAfter);

    }

    @Test
    public void testTripWithPicture() throws IOException {

        int countBefore = countTrips();

        TripBean trip = buildValidTripBean();
        CatchBean c = new CatchBean();
        c.id = "abc";
        c.speciesId = Optional.of(trip.speciesIds.iterator().next().toString());
        c.techniqueId = trip.techniqueIds.iterator().next();
        trip.catchs = Collections.singletonList(c);

        ResponseBodyExtractionOptions body = given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .post("/api/v1/trips")
            .then()
                .statusCode(201)
                // On reçoit la map de replacement :
                //   dontcare -> nouvel-id-de-trip
                //   abc -> nouvel-id-de-capture
                // telle que :
                //  {"dontcare": "2244331f-f9dc-4102-b832-be7d69b7c377", "abc": "c2ec85c3-7b03-4cc7-ad80-5e17cfa29772"}
                .body(trip.id, notNullValue())
                .body(c.id, notNullValue())
            .extract()
                .body();
        String tripId = body.path(trip.id);
        String catchId = body.path(c.id);

        int countAfter = countTrips();
        Assertions.assertEquals(countBefore + 1, countAfter);

        // Pas encore d'image -> 404
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId)
            .then()
                .statusCode(404);

        // {id='70d6bed0-72e4-49c4-8e1d-8c1278c94e17', createdOn=Optional[2021-11-03T19:08:34.338806], mode=Live, type=Craft, name='Whatever', lakeId=658c488f-b982-4f4c-8610-527a1684b3be, speciesIds=[d0176668-c2b5-4182-863c-f950b908d96a], otherSpecies='null', date=2021-11-03, startedAt=00:00, finishedAt=00:01, weatherId=Optional.empty, catchs=1, techniqueIds=[9929f857-ecc1-40a9-859f-91b897cdb12c], beginLatitude=Optional.empty, beginLongitude=Optional.empty, endLatitude=Optional.empty, endLongitude=Optional.empty, source=null, saveDelayMarker=Optional.empty, modifiableUntil=Optional[2021-11-10T19:08:34.338806]}
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("date[0]", equalTo(trip.date.getYear()))
                .body("date[1]", equalTo(trip.date.getMonthValue()))
                .body("date[2]", equalTo(trip.date.getDayOfMonth()))
                .body("startedAt", equalTo("00:00"))
                .body("finishedAt", equalTo("00:01"))
                .body("mode", equalTo(trip.mode.getLiteral()))
                .body("type", equalTo(trip.type.getLiteral()))
                .body("catchs[0].hasPicture", equalTo(false));

        InputStream resource = this.getClass().getResourceAsStream("/about-fishes.jpg");
        Preconditions.checkState(resource != null, "Image non trouvée");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IOUtils.copy(resource, stream);
        byte[] originalByteArray = stream.toByteArray();
        String base64Content = Base64.getEncoder().encodeToString(originalByteArray);

        given()
            .when()
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body("data:image/jpeg;base64," + base64Content)
                .put("/api/v1/pictures/" + catchId)
            .then()
                .statusCode(204);

        // On vérifie que la 404 a disparu
        byte[] bytes =
            given()
                .when()
                    .body(trip)
                    .get("/api/v1/pictures/" + catchId)
                .then()
                    .statusCode(200)
                .extract()
                    .body()
                    .asByteArray();

        // On compare les dimensions de l'image lue avec l'image d'origine
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream(originalByteArray);
        BufferedImage originalImage = ImageIO.read(originalInputStream);

        ByteArrayInputStream newInputStream = new ByteArrayInputStream(bytes);
        BufferedImage newImage = ImageIO.read(newInputStream);

        Assertions.assertEquals(1920, originalImage.getWidth());
        Assertions.assertEquals(originalImage.getWidth(), newImage.getWidth());
        Assertions.assertEquals(680, originalImage.getHeight());
        Assertions.assertEquals(originalImage.getHeight(), newImage.getHeight());

        // On vérifie que la miniature est générée
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview")
            .then()
                .statusCode(200);

        // {id='70d6bed0-72e4-49c4-8e1d-8c1278c94e17', createdOn=Optional[2021-11-03T19:08:34.338806], mode=Live, type=Craft, name='Whatever', lakeId=658c488f-b982-4f4c-8610-527a1684b3be, speciesIds=[d0176668-c2b5-4182-863c-f950b908d96a], otherSpecies='null', date=2021-11-03, startedAt=00:00, finishedAt=00:01, weatherId=Optional.empty, catchs=1, techniqueIds=[9929f857-ecc1-40a9-859f-91b897cdb12c], beginLatitude=Optional.empty, beginLongitude=Optional.empty, endLatitude=Optional.empty, endLongitude=Optional.empty, source=null, saveDelayMarker=Optional.empty, modifiableUntil=Optional[2021-11-10T19:08:34.338806]}
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasPicture", equalTo(true));

    }

}
