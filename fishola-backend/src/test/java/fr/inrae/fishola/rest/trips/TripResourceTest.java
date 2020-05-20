package fr.inrae.fishola.rest.trips;

import com.google.common.collect.ImmutableSet;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class TripResourceTest extends AbstractFisholaTest {

    @Inject
    protected ReferentialDao referentialDao;

    protected List<Lake> lakes;
    protected List<Weather> weathers;
    protected List<Species> species;
    protected List<Technique> techniques;
    protected String token;

    @BeforeEach
    @Transactional
    public void loadReferentials() {
        this.lakes = referentialDao.listLakes();
        this.weathers = referentialDao.listWeathers();
        this.species = referentialDao.listBuiltInSpecies();
        this.techniques = referentialDao.listBuiltInTechniques();
    }

    @BeforeEach
    public void login() {
        this.token = login("thimel@codelutin.com", "sispea");
    }

    @Test
    public void testListMyTrips() {
        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/trips?pageNumber=0&pageSize=-1&desc=false")
            .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"count\":0"));
    }

    @Test
    public void testCreateEmptyTrip() {
        TripBean trip = new TripBean();

        trip.id = "dontcare";
        trip.date = LocalDate.now();
        trip.startedAt = "00:00";
        trip.finishedAt = "00:01";
        trip.lakeId = this.lakes.iterator().next().getId();
        trip.weatherId = this.weathers.iterator().next().getId();
        trip.name = "Whatever";
        trip.type = TripType.Craft;
        trip.mode = TripMode.Live;
        trip.speciesIds = this.species.stream().limit(1).map(Species::getId).collect(ImmutableSet.toImmutableSet());
        trip.techniqueIds = this.techniques.stream().limit(1).map(Technique::getId).collect(ImmutableSet.toImmutableSet());

        given()
            .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .post("/api/v1/trips")
            .then()
                .statusCode(201)
                .body(CoreMatchers.containsString("{\"dontcare\":\""));
    }

}