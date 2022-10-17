package fr.inrae.fishola.rest.metrics;

import com.google.common.collect.ImmutableSet;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.DashboardDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.dashboard.Dashboard;
import io.quarkus.test.junit.QuarkusTest;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DashboardResourceTest  extends AbstractFisholaTest {
    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;
    @Inject
    protected CatchsDao catchsDao;
    @Inject
    protected UsersDao usersDao;
    @Inject
    protected DashboardDao dashboardDao;
    @Inject
    protected FisholaConfiguration config;

    protected List<Lake> lakes;
    protected List<Species> species;
    protected List<Technique> techniques;
    protected List<Weather> weathers;
    protected String token;
    protected UUID userId;

    @BeforeEach
    @Transactional
    public void loadReferentials() {
        this.lakes = referentialDao.listLakes();
        this.species = referentialDao.listBuiltInSpecies();
        this.techniques = referentialDao.listBuiltInTechniques();
        this.weathers = referentialDao.listWeathers();
    }

    @BeforeEach
    @Transactional
    public void login() {
        this.token = login("thimel@codelutin.com", "sispea");
        this.userId = this.usersDao.findByEmail("thimel@codelutin.com").get().getId();
    }

    @Test
    @Transactional
    public void testDefaultPersonalDashboard() {
        // Goal : Ensure that personal dashboards defaults to "all lakes" and "current year"
        Dashboard initialDashboard = this.getPersonalDashboard();

        // Add a trip for previous year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(this.lakes.iterator().next().getId(), oneYearAgo);
        // Should not impact default dashboard
        compareDashboard(initialDashboard, this.getPersonalDashboard(), 0);


        // add a trip for current year
        createTripWithCatch(this.lakes.iterator().next().getId(),  LocalDateTime.now());
        // Should impact default dahsboard
        compareDashboard(initialDashboard, this.getPersonalDashboard(), 1);
    }

    /**
     * Compares the two given dashboards. Fails if the second one does not have expectedAdditionalCatches more that the first.
     * @param d1 dashboard to compare
     * @param d2 dashboard to compare
     * @param expectedAdditionalCatches number of catches d2 should have in addition of d1 (if 0, d1 and d2 should be equal)
     */
    private void compareDashboard(Dashboard d1, Dashboard d2, Integer expectedAdditionalCatches) {
        if (expectedAdditionalCatches == 0) {
            Assertions.assertEquals(d1.toString(),d2.toString());
        } else {
            Assertions.assertNotEquals(d1.toString(),d2.toString());
            Assertions.assertNotEquals(d1.averageCatchsPerTrip(), d2.averageCatchsPerTrip());
            Assertions.assertNotEquals(d1.caughtSpeciesCount(), d2.caughtSpeciesCount());
            Assertions.assertNotEquals(d1.monthlySizes(), d2.monthlySizes());
            Assertions.assertNotEquals(d1.topBySize(), d2.topBySize());
            Assertions.assertNotEquals(d1.topByWeight(), d2.topByWeight());
            Assertions.assertEquals(d1.latestTripsCatchs().size() + expectedAdditionalCatches, d2.latestTripsCatchs().size());
        }

    }

    private void createTripWithCatch(UUID lakeId, LocalDateTime tripDate) {
        Trip trip = new Trip();
        trip.setCreatedOn(tripDate);
        trip.setDay(tripDate.toLocalDate());
        trip.setStartTime(tripDate.toLocalTime().minusHours(4));
        trip.setEndTime(tripDate.toLocalTime());
        trip.setLakeId(lakeId);
        trip.setName("Trip for lake " + lakeId + " " + tripDate.toString());
        trip.setType(TripType.Craft);
        trip.setHidden(false);
        trip.setMode(TripMode.Afterwards);
        trip.setOwnerId(this.userId);
        trip.setWeatherId(this.weathers.stream().limit(1).map(Weather::getId).collect(ImmutableSet.toImmutableSet()).iterator().next());
        trip.setBeginLatitude(42d);
        trip.setEndLatitude(42d);
        trip.setBeginLongitude(42d);
        trip.setEndLongitude(42d);
        trip.setSource(DeviceType.web);
        trip.setFrontendVersion("1.0");
        UUID tripId = tripsDao.create(trip);

        Catch aCatch = new Catch();
        aCatch.setTripId(tripId);
        aCatch.setCreatedOn(tripDate);
        aCatch.setCatchTime(tripDate.toLocalTime());
        aCatch.setSpeciesId(this.species.stream().limit(1).map(Species::getId).collect(ImmutableSet.toImmutableSet()).iterator().next());
        aCatch.setTechniqueId(this.techniques.stream().limit(1).map(Technique::getId).collect(ImmutableSet.toImmutableSet()).iterator().next());
        aCatch.setSize(456);
        aCatch.setWeight(456);
        aCatch.setKept(true);
        catchsDao.create(aCatch);
    }

    private Dashboard getPersonalDashboard() {
        Optional<Integer> yearFilter = config.dashboardOnlyCurrentYear()
                ? Optional.of(Year.now().getValue())
                : Optional.empty();
        return this.dashboardDao.getPersonalDashboard(this.userId, yearFilter);
    }
}
