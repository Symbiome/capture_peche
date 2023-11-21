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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.DashboardDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.JwtHelper;
import fr.inrae.fishola.rest.dashboard.Dashboard;
import fr.inrae.fishola.rest.dashboard.DashboardLastTrip;
import fr.inrae.fishola.rest.dashboard.GlobalDashboard;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.response.ResponseBodyExtractionOptions;
import io.restassured.response.ValidatableResponse;
import java.time.Year;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.text.html.Option;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.Opt;
import org.jboss.logging.Logger;
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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.nuiton.util.pagination.PaginationParameter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
public class TripResourceTest extends AbstractFisholaTest {

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;
    @Inject
    protected DashboardDao dashboardDao;
    @Inject
    protected UsersDao usersDao;
    @Inject
    protected Logger log;
    @Inject
    protected JwtHelper jwtHelper;

    protected List<Lake> lakes;
    protected List<Species> species;
    protected List<Technique> techniques;
    protected List<Weather> weathers;
    protected String token;

    @BeforeEach
    @Transactional
    public void loadReferentials() {
        this.lakes = referentialDao.listLakes();
        this.species = referentialDao.listBuiltInSpecies();
        this.techniques = referentialDao.listBuiltInTechniques();
        this.weathers = referentialDao.listWeathers();
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
        trip.source = DeviceType.application;
        trip.weatherId = this.weathers.stream().limit(1).map(Weather::getId).findAny();
        CatchBean c = new CatchBean();
        c.id = "abc";
        c.speciesId = Optional.of(trip.speciesIds.iterator().next().toString());
        c.techniqueId = trip.techniqueIds.iterator().next();
        c.keep = true;
        c.weight = Optional.of(666);
        c.size = Optional.of(21);
        c.description = Optional.of("Poisson taiste");
        c.caughtAt = Optional.of("21:05");
        c.latitude = Optional.of(41.1);
        c.longitude = Optional.of(3.3);
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

        LocalDateTime createdOn = LocalDateTime.now();

        int countAfter = countTrips();
        Assertions.assertEquals(countBefore + 1, countAfter);

        // Pas encore d'image -> 404
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId)
            .then()
                .statusCode(404);

        // {"id":"0986edd8-a9c0-4d55-9c77-66d34438612a","createdOn":[2021,11,4,9,44,17,432989000],"mode":"Live","type":"Craft","name":"Whatever","lakeId":"95077a6a-09c7-4dd0-a200-439feb9dd9c4","speciesIds":["f52b832c-f336-47cb-8d4d-09ef85c2a3ef"],"otherSpecies":null,"date":[2021,11,4],"startedAt":"00:00","finishedAt":"00:01","weatherId":"35659df6-5244-4571-bd3f-da37464b3463","catchs":[{"id":"bcf2687f-bcc8-486f-864b-4c63737c3d72","speciesId":"f52b832c-f336-47cb-8d4d-09ef85c2a3ef","otherSpecies":null,"size":21,"automaticMeasure":null,"weight":666,"keep":true,"releasedStateId":null,"techniqueId":"3f82a047-56c6-412a-b817-8835b465dbfa","description":"Poisson taiste","caughtAt":"21:05","sampleId":null,"latitude":41.1,"longitude":3.3,"hasPicture":false,"tripId":"0986edd8-a9c0-4d55-9c77-66d34438612a"}],"techniqueIds":["3f82a047-56c6-412a-b817-8835b465dbfa"],"beginLatitude":null,"beginLongitude":null,"endLatitude":null,"endLongitude":null,"source":null,"saveDelayMarker":null,"modifiableUntil":[2021,11,11,9,44,17,432989000]}
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("name", equalTo("Whatever"))
                .body("lakeId", equalTo(trip.lakeId.toString()))
                .body("speciesIds[0]", equalTo(trip.speciesIds.iterator().next().toString()))
                .body("techniqueIds[0]", equalTo(trip.techniqueIds.iterator().next().toString()))
                .body("weatherId", equalTo(trip.weatherId.get().toString()))
                .body("createdOn[0]", equalTo(createdOn.getYear()))
                .body("createdOn[1]", equalTo(createdOn.getMonthValue()))
                .body("createdOn[2]", equalTo(createdOn.getDayOfMonth()))
                .body("createdOn[3]", equalTo(createdOn.getHour()))
                .body("createdOn[4]", equalTo(createdOn.getMinute()))
                .body("createdOn[5]", notNullValue())
                .body("createdOn[6]", notNullValue())
                .body("date[0]", equalTo(trip.date.getYear()))
                .body("date[1]", equalTo(trip.date.getMonthValue()))
                .body("date[2]", equalTo(trip.date.getDayOfMonth()))
                .body("startedAt", equalTo("00:00"))
                .body("finishedAt", equalTo("00:01"))
                .body("mode", equalTo(trip.mode.getLiteral()))
                .body("type", equalTo(trip.type.getLiteral()))
                .body("source", nullValue()) // La source n'est pas renvoyée au client. Sinon : equalTo(DeviceType.application.getLiteral())
                .body("catchs[0].speciesId", equalTo(c.speciesId.get()))
                .body("catchs[0].otherSpecies", nullValue())
                .body("catchs[0].keep", equalTo(true))
                .body("catchs[0].weight", equalTo(666))
                .body("catchs[0].size", equalTo(21))
                .body("catchs[0].description", equalTo("Poisson taiste"))
                .body("catchs[0].caughtAt", equalTo("21:05"))
                .body("catchs[0].latitude", equalTo(41.1f))
                .body("catchs[0].longitude", equalTo(3.3f))
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

        // Supprime l'image
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .delete("/api/v1/pictures/" + catchId + "/0")
            .then()
                .statusCode(204);

        // Vérifie que l'image est bien supprimée
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasPicture", equalTo(false));

    }

    @Test
    public void testTripWithPictureGallery() throws IOException {

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

        // {"id":"0986edd8-a9c0-4d55-9c77-66d34438612a","createdOn":[2021,11,4,9,44,17,432989000],"mode":"Live","type":"Craft","name":"Whatever","lakeId":"95077a6a-09c7-4dd0-a200-439feb9dd9c4","speciesIds":["f52b832c-f336-47cb-8d4d-09ef85c2a3ef"],"otherSpecies":null,"date":[2021,11,4],"startedAt":"00:00","finishedAt":"00:01","weatherId":"35659df6-5244-4571-bd3f-da37464b3463","catchs":[{"id":"bcf2687f-bcc8-486f-864b-4c63737c3d72","speciesId":"f52b832c-f336-47cb-8d4d-09ef85c2a3ef","otherSpecies":null,"size":21,"automaticMeasure":null,"weight":666,"keep":true,"releasedStateId":null,"techniqueId":"3f82a047-56c6-412a-b817-8835b465dbfa","description":"Poisson taiste","caughtAt":"21:05","sampleId":null,"latitude":41.1,"longitude":3.3,"hasPicture":false,"tripId":"0986edd8-a9c0-4d55-9c77-66d34438612a"}],"techniqueIds":["3f82a047-56c6-412a-b817-8835b465dbfa"],"beginLatitude":null,"beginLongitude":null,"endLatitude":null,"endLongitude":null,"source":null,"saveDelayMarker":null,"modifiableUntil":[2021,11,11,9,44,17,432989000]}
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("catchs[0].hasPicture", equalTo(false));

        // Pas encore de miniature
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview")
            .then()
                .statusCode(404);

        InputStream resource = this.getClass().getResourceAsStream("/about-fishes.jpg");
        Preconditions.checkState(resource != null, "Image non trouvée");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IOUtils.copy(resource, stream);
        byte[] originalByteArray = stream.toByteArray();
        String base64Content = Base64.getEncoder().encodeToString(originalByteArray);

        // Ajoute une image en '0'
        given()
            .when()
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body("data:image/jpeg;base64," + base64Content)
                .put("/api/v1/pictures/" + catchId + "/0")
            .then()
                .statusCode(204);

        // Vérifie qu'elle est bien listée ...
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasPicture", equalTo(true))
                .body("catchs[0].pictureOrders", hasItems(0));

        // ... et téléchargeable
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/0")
            .then()
                .statusCode(200);

        // On vérifie que la miniature est générée
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview")
            .then()
                .statusCode(200);

        // On vérifie que la miniature '0' est générée
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview/0")
            .then()
                .statusCode(200);

        // Ajoute une image en '12'
        given()
            .when()
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body("data:image/jpeg;base64," + base64Content)
                .put("/api/v1/pictures/" + catchId + "/12")
            .then()
                .statusCode(204);

        // Vérifie qu'elle est bien listée ...
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasPicture", equalTo(true))
                .body("catchs[0].pictureOrders", hasItems(0, 12));

        // ... et téléchargeable
        given()
                .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/12")
                .then()
                .statusCode(200);

        // Supprime l'image '0'
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .delete("/api/v1/pictures/" + catchId + "/0")
            .then()
                .statusCode(204);

        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasPicture", equalTo(true))
                .body("catchs[0].pictureOrders", hasItems(12));

        // On vérifie qu'on a toujours une miniature
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview")
            .then()
                .statusCode(200);

        // On vérifie que la miniature '12' est générée
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview/12")
            .then()
                .statusCode(200);

        // On vérifie que la miniature '0' n'est plus dispo
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview/0")
            .then()
                .statusCode(404);

        // Supprime l'image
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .delete("/api/v1/pictures/" + catchId + "/12")
            .then()
                .statusCode(204);

        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasPicture", equalTo(false))
                .body("catchs[0].pictureOrders", equalTo(new LinkedList<>()));

        // Plus de miniature
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview")
            .then()
                .statusCode(404);

        // On vérifie que la miniature '0' n'est plus dispo
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview/0")
            .then()
                .statusCode(404);

        // On vérifie que la miniature '12' n'est plus dispo
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/" + catchId + "/preview/12")
            .then()
                .statusCode(404);

    }

    @Test
    public void testTripWithMeasurementPicture() throws IOException {

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

        // {"id":"0986edd8-a9c0-4d55-9c77-66d34438612a","createdOn":[2021,11,4,9,44,17,432989000],"mode":"Live","type":"Craft","name":"Whatever","lakeId":"95077a6a-09c7-4dd0-a200-439feb9dd9c4","speciesIds":["f52b832c-f336-47cb-8d4d-09ef85c2a3ef"],"otherSpecies":null,"date":[2021,11,4],"startedAt":"00:00","finishedAt":"00:01","weatherId":"35659df6-5244-4571-bd3f-da37464b3463","catchs":[{"id":"bcf2687f-bcc8-486f-864b-4c63737c3d72","speciesId":"f52b832c-f336-47cb-8d4d-09ef85c2a3ef","otherSpecies":null,"size":21,"automaticMeasure":null,"weight":666,"keep":true,"releasedStateId":null,"techniqueId":"3f82a047-56c6-412a-b817-8835b465dbfa","description":"Poisson taiste","caughtAt":"21:05","sampleId":null,"latitude":41.1,"longitude":3.3,"hasPicture":false,"tripId":"0986edd8-a9c0-4d55-9c77-66d34438612a"}],"techniqueIds":["3f82a047-56c6-412a-b817-8835b465dbfa"],"beginLatitude":null,"beginLongitude":null,"endLatitude":null,"endLongitude":null,"source":null,"saveDelayMarker":null,"modifiableUntil":[2021,11,11,9,44,17,432989000]}
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("catchs[0].hasPicture", equalTo(false))
                .body("catchs[0].hasMeasurementPicture", equalTo(false));

        // Pas encore téléchargeable
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId)
            .then()
                .statusCode(404);

        // Pas encore de miniature
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId + "/preview")
            .then()
                .statusCode(404);

        InputStream resource = this.getClass().getResourceAsStream("/about-fishes.jpg");
        Preconditions.checkState(resource != null, "Image non trouvée");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IOUtils.copy(resource, stream);
        byte[] originalByteArray = stream.toByteArray();
        String base64Content = Base64.getEncoder().encodeToString(originalByteArray);

        // Ajoute l'image de mesure
        given()
            .when()
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body("data:image/jpeg;base64," + base64Content)
                .put("/api/v1/pictures/measure/" + catchId)
            .then()
                .statusCode(204);

        // Vérifie qu'elle est bien listée ...
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasPicture", equalTo(false))
                .body("catchs[0].hasMeasurementPicture", equalTo(true));

        // ... et téléchargeable
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId)
            .then()
                .statusCode(200);

        // On vérifie que la miniature est générée
        given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId + "/preview")
            .then()
                .statusCode(200);

        // Supprime la sortie
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .delete("/api/v1/trips/" + tripId)
            .then()
                .statusCode(204);

    }

    @Test
    public void testReplaceMeasurementPicture() throws IOException {

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

        InputStream resource = this.getClass().getResourceAsStream("/about-fishes.jpg");
        Preconditions.checkState(resource != null, "Image non trouvée");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IOUtils.copy(resource, stream);
        byte[] originalByteArray = stream.toByteArray();
        String base64Content = Base64.getEncoder().encodeToString(originalByteArray);

        // Ajoute l'image de mesure
        given()
            .when()
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body("data:image/jpeg;base64," + base64Content)
                .put("/api/v1/pictures/measure/" + catchId)
            .then()
                .statusCode(204);

        // Vérifie qu'elle est bien listée ...
        given()
            .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips/" + tripId)
            .then()
                .statusCode(200)
                .body("id", equalTo(tripId))
                .body("catchs[0].hasMeasurementPicture", equalTo(true));

        // ... et téléchargeable
        byte[] getBytes = given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId)
            .then()
                .statusCode(200)
            .extract()
                .body()
                .asByteArray();

        // On compare les dimensions de l'image lue avec l'image d'origine
        ByteArrayInputStream originalInputStream = new ByteArrayInputStream(originalByteArray);
        BufferedImage originalImage = ImageIO.read(originalInputStream);

        ByteArrayInputStream getInputStream = new ByteArrayInputStream(getBytes);
        BufferedImage getImage = ImageIO.read(getInputStream);

        Assertions.assertEquals(originalImage.getWidth(), getImage.getWidth());
        Assertions.assertEquals(originalImage.getHeight(), getImage.getHeight());

        Assertions.assertEquals(computeRatio(originalImage), computeRatio(getImage));

        // On vérifie que la miniature est générée
        byte[] previewBytes = given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId + "/preview")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .asByteArray();

        ByteArrayInputStream previewInputStream = new ByteArrayInputStream(previewBytes);
        BufferedImage previewImage = ImageIO.read(previewInputStream);

        // La preview ayant été redimensionnée, le ratio peut être significativement différent
        Assertions.assertEquals(computeRatio(originalImage), computeRatio(previewImage), 0.005f);

        InputStream replacementResource = this.getClass().getResourceAsStream("/talkie-reine-des-neiges.jpeg");
        Preconditions.checkState(replacementResource != null, "Image non trouvée");
        ByteArrayOutputStream replacementStream = new ByteArrayOutputStream();
        IOUtils.copy(replacementResource, replacementStream);
        byte[] replacementByteArray = replacementStream.toByteArray();
        String replacementBase64Content = Base64.getEncoder().encodeToString(replacementByteArray);

        // Ajoute l'image de mesure
        given()
            .when()
                .contentType(MediaType.TEXT_PLAIN)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body("data:image/jpeg;base64," + replacementBase64Content)
                .put("/api/v1/pictures/measure/" + catchId)
            .then()
                .statusCode(204);

        // ... et téléchargeable
        byte[] getReplacementBytes = given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId)
            .then()
                .statusCode(200)
            .extract()
                .body()
                .asByteArray();

        // On compare les dimensions de l'image lue avec l'image d'origine
        ByteArrayInputStream replacementInputStream = new ByteArrayInputStream(replacementByteArray);
        BufferedImage replacementImage = ImageIO.read(replacementInputStream);

        ByteArrayInputStream getReplacementInputStream = new ByteArrayInputStream(getReplacementBytes);
        BufferedImage getReplacementImage = ImageIO.read(getReplacementInputStream);

        Assertions.assertEquals(replacementImage.getWidth(), getReplacementImage.getWidth());
        Assertions.assertEquals(replacementImage.getHeight(), getReplacementImage.getHeight());

        Assertions.assertEquals(computeRatio(replacementImage), computeRatio(getReplacementImage));

        // On s'assure que les dimensions sont bien différentes de l'original
        Assertions.assertNotEquals(getImage.getWidth(), getReplacementImage.getWidth());
        Assertions.assertNotEquals(getImage.getHeight(), getReplacementImage.getHeight());
        Assertions.assertNotEquals(computeRatio(getImage), computeRatio(getReplacementImage));

        // On vérifie que la miniature est générée
        byte[] previewReplacementBytes = given()
            .when()
                .body(trip)
                .get("/api/v1/pictures/measure/" + catchId + "/preview")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .asByteArray();

        ByteArrayInputStream previewReplacementInputStream = new ByteArrayInputStream(previewReplacementBytes);
        BufferedImage previewReplacementImage = ImageIO.read(previewReplacementInputStream);

        Assertions.assertEquals(computeRatio(replacementImage), computeRatio(previewReplacementImage), 0.005f);

    }

    @Test
    @Transactional
    public void testEditedInBoFields() {

        // Warm up - create a new lake, and on it a trip and catch
        String adminToken = loginAsAdmin();
        Lake newLake = new Lake();
        UUID newLakeId = UUID.randomUUID();
        newLake.setId(newLakeId);
        newLake.setName("New lake");
        newLake.setExportAs("New lake");
        newLake.setLatitude(42d);
        newLake.setLongitude(1d);
        given()
                .when()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(newLake)
                .post("/api/v1/referential/lakes")
                .then()
                .statusCode(204);
        Optional<List<UUID>> newLakeFilter = Optional.of(Lists.newArrayList(newLakeId));
        List<UUID> twoRandomSpecies = this.species.stream().limit(2).map(Species::getId).collect(ImmutableList.toImmutableList());
        int year = Year.now().getValue();
        Optional<Integer> yearFilter = Optional.of(year);
        UUID userId = jwtHelper.verifyToken(token);
        TripBean trip = buildValidTripBean();
        trip.lakeId = newLakeId;
        Optional<Integer> initialSize = Optional.of(10);
        Optional<Integer> initialWeight = Optional.of(10);
        Optional<String> initialSpeciesId = Optional.of(twoRandomSpecies.get(0).toString());
        Optional<Integer> editedSizeInBo = Optional.of(100);
        Optional<Integer> editedWeightInBo = Optional.of(100);
        UUID editedSpeciesId = twoRandomSpecies.get(1);
        CatchBean catchBean = new CatchBean();
        catchBean.id = "abc";
        catchBean.speciesId = initialSpeciesId;
        catchBean.size = initialSize;
        catchBean.weight = initialWeight;
        catchBean.techniqueId = trip.techniqueIds.iterator().next();
        trip.catchs = Collections.singletonList(catchBean);
        ResponseBodyExtractionOptions body = given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .post("/api/v1/trips");
        String catchId = body.path(catchBean.id);
        // Make sur current user is not excluded from exports
        FisholaUser fisholaUser = this.usersDao.findById(userId).get();
        fisholaUser.setExcludeFromExports(false);
        this.usersDao.updateUser(fisholaUser);


        // Both personnal and global Dashboard should include this catch
        checkPersonnalDashboardInformation(initialSize, initialWeight, initialSpeciesId, userId, yearFilter, newLakeFilter);
        checkGlobalDashboardInformation(true, initialSize, initialWeight, initialSpeciesId.get(), yearFilter, newLakeFilter);


        // List all catches as a regular user : should be a 401
        given().when().get("/api/v1/trips/export/0/longueur_totale_du_poisson/desc")
            .then().statusCode(401);

        // Try to update an export catch as a regular user : should be a 401
        given().when()
            .body(catchBean)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
            .put("/api/v1/trips/catches/" + catchId)
            .then().statusCode(401);

        // List all catches as admin : should be a 200
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/trips/export/0/longueur_totale_du_poisson/desc")
                .then().statusCode(200);

        // Try to update an export catch as admin : should be 200
        catchBean.editedSpeciesId =  Optional.of(editedSpeciesId);
        catchBean.editedWeight = editedWeightInBo;
        catchBean.editedSize = editedSizeInBo;
        given().when()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(catchBean)
                .put("/api/v1/trips/catches/" + catchId)
                .then().statusCode(200);

        // Global Dashboard should include the edited specie, size and weight (not original)
        checkGlobalDashboardInformation(true, editedSizeInBo, editedWeightInBo, editedSpeciesId.toString(), yearFilter, newLakeFilter);

        // Personal dashboard should include the original specie, size and weight (not edited)
        checkPersonnalDashboardInformation(initialSize, initialWeight, initialSpeciesId, userId, yearFilter, newLakeFilter);

        // Exclude catch from export
        catchBean.excludeFromExport = true;
        given().when()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(catchBean)
                .put("/api/v1/trips/catches/" + catchId)
                .then().statusCode(200);

        // Dashboard should not include this catch anymore
        checkGlobalDashboardInformation(false, initialSize, initialWeight, initialSpeciesId.toString(), yearFilter, newLakeFilter);

        // Personal dashboard should still include this catch
        checkPersonnalDashboardInformation(initialSize, initialWeight, initialSpeciesId, userId, yearFilter, newLakeFilter);


    }

    private void checkGlobalDashboardInformation(boolean expectedIsPresent, Optional<Integer> expectedSize, Optional<Integer> expectedWeight, String expectedSpeciesId, Optional<Integer> yearFilter, Optional<List<UUID>> newLakeFilter) {
        GlobalDashboard globalDashboardForYear = this.dashboardDao.computeGlobalDashboard(yearFilter, newLakeFilter, this.log);
        if (expectedIsPresent) {
            Assertions.assertEquals(1, globalDashboardForYear.caughtSpeciesCount().size());
            Assertions.assertEquals(1, globalDashboardForYear.caughtSpeciesDistribution().size());
            Assertions.assertEquals(expectedSpeciesId, globalDashboardForYear.caughtSpeciesDistribution().keySet().iterator().next().toString());
            Assertions.assertEquals(expectedSpeciesId, globalDashboardForYear.caughtAndReleasedSpeciesDistribution().keySet().iterator().next().toString());
            Assertions.assertEquals(expectedSize.get().doubleValue(), globalDashboardForYear.monthlySizes().entrySet().iterator().next().getValue().values().iterator().next());
            Assertions.assertEquals(expectedSpeciesId,globalDashboardForYear.monthlySizesPerMaillage().keySet().iterator().next().toString());
            Assertions.assertEquals(expectedSize.get().doubleValue(), globalDashboardForYear.monthlySizesPerMaillage().values().iterator().next().values().iterator().next().values().iterator().next());
        } else {
            Assertions.assertEquals(0, globalDashboardForYear.caughtSpeciesCount().size());
            Assertions.assertEquals(0, globalDashboardForYear.caughtSpeciesDistribution().size());
            Assertions.assertEquals(0, globalDashboardForYear.caughtSpeciesDistribution().keySet().size());
            Assertions.assertEquals(0, globalDashboardForYear.caughtAndReleasedSpeciesDistribution().keySet().size());
            Assertions.assertEquals(0, globalDashboardForYear.monthlySizes().entrySet().size());
            Assertions.assertEquals(0,globalDashboardForYear.monthlySizesPerMaillage().keySet().size());
            Assertions.assertEquals(0, globalDashboardForYear.monthlySizesPerMaillage().size());
        }


     }

    private void checkPersonnalDashboardInformation(Optional<Integer> expectedSize, Optional<Integer> expectedWeight, Optional<String> expectedSpeciesId, UUID userId, Optional<Integer> yearFilter, Optional<List<UUID>> lakesFilter) {
        Dashboard personalDashboardForYear = this.dashboardDao.getPersonalDashboard(userId,yearFilter, lakesFilter);
        Assertions.assertEquals(1, personalDashboardForYear.caughtSpeciesCount().size());
        Assertions.assertEquals(1, personalDashboardForYear.caughtSpeciesDistribution().size());
        Assertions.assertEquals(1, personalDashboardForYear.latestTripsCatchs().iterator().next().catchsCount());
        Assertions.assertEquals(expectedSpeciesId.get(), personalDashboardForYear.caughtSpeciesDistribution().keySet().iterator().next().toString());
        Assertions.assertEquals(expectedSpeciesId.get(), personalDashboardForYear.caughtAndReleasedSpeciesDistribution().keySet().iterator().next().toString());
        Assertions.assertEquals(expectedSize.get().doubleValue(), personalDashboardForYear.monthlySizes().entrySet().iterator().next().getValue().values().iterator().next());
        Assertions.assertEquals(expectedSpeciesId.get(),personalDashboardForYear.monthlySizesPerMaillage().keySet().iterator().next().toString());
        Assertions.assertEquals(expectedSize.get().doubleValue(),personalDashboardForYear.monthlySizesPerMaillage().values().iterator().next().values().iterator().next().values().iterator().next());
        Assertions.assertEquals(expectedSize, personalDashboardForYear.topBySize().values().iterator().next().iterator().next().size);
        Assertions.assertEquals(expectedWeight,personalDashboardForYear.topByWeight().values().iterator().next().iterator().next().weight);
    }

    protected float computeRatio(BufferedImage image) {
        float result = Integer.valueOf(image.getWidth()).floatValue() / Integer.valueOf(image.getHeight()).floatValue();
        return result;
    }
}
