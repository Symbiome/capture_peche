package fr.inrae.fishola.rest.trips;

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
import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;

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
        UUID userId = jwtHelper.tokenToUserID(token);
        int result = tripsDao.countMyTrips(userId);
        return result;
    }

    @Test
    public void testListMyTrips() {
        int count = countTrips();
        Assertions.assertEquals(0, count);
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips?pageNumber=0&pageSize=-1&desc=false")
            .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"count\":" + count));
    }

    @NotNull
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
                .cookie(AbstractFisholaResource.AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .post("/api/v1/trips")
            .then()
                .statusCode(201)
                .body(CoreMatchers.containsString("{\"dontcare\":\""));


        int countAfter = countTrips();
        Assertions.assertEquals(countBefore + 1, countAfter);

    }

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
        catchBean.speciesId = Optional.of(UUID.randomUUID());
        trip.catchs.add(catchBean);

        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .post("/api/v1/trips")
            .then()
                .statusCode(500);

        // La création n'a pas pu se faire, on vérifie que la sortie n'a pas été créée
        int countAfter = countTrips();
        Assertions.assertEquals(countBefore, countAfter);

    }

}
