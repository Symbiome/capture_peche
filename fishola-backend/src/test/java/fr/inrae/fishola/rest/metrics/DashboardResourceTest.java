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
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.dashboard.Dashboard;
import fr.inrae.fishola.rest.dashboard.GlobalDashboard;
import io.quarkus.test.junit.QuarkusTest;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
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
    @Inject
    protected Logger log;

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
        FisholaUser fisholaUser = this.usersDao.findByEmail("thimel@codelutin.com").get();
        this.userId = fisholaUser.getId();
        fisholaUser.setExcludeFromExports(false);
        this.usersDao.updateUser(fisholaUser);
    }

    @AfterEach
    @Transactional
    public void teardown() {
        FisholaUser fisholaUser = this.usersDao.findById(this.userId).get();
        fisholaUser.setExcludeFromExports(true);
        this.usersDao.updateUser(fisholaUser);
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
        comparePersonalDashboards(initialDashboard, this.getPersonalDashboard(), 0);


        // add a trip for current year
        createTripWithCatch(this.lakes.iterator().next().getId(),  LocalDateTime.now());
        // Should impact default dahsboard
        comparePersonalDashboards(initialDashboard, this.getPersonalDashboard(), 1);
    }

    @Test
    @Transactional
   public void testPersonalDashboardForYear() {
        // Goal : make sure that year selections for personnal dashboard works as expected
        Dashboard initialDashboardForCurrentYear = this.getPersonalDashboardForYear(LocalDateTime.now().getYear());
        Dashboard initialDashboardForLastYear = this.getPersonalDashboardForYear(LocalDateTime.now().getYear() - 1);

        // Add 2 trips for previous year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(this.lakes.iterator().next().getId(), oneYearAgo);
        createTripWithCatch(this.lakes.iterator().next().getId(), oneYearAgo);
        // Should not impact currentYear dashboard but previousYear one
        comparePersonalDashboards(initialDashboardForCurrentYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear()), 0);
        comparePersonalDashboards(initialDashboardForLastYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear() - 1), 2);
        initialDashboardForLastYear = this.getPersonalDashboardForYear(LocalDateTime.now().getYear() - 1);


        // Add 2 trips for current year
        createTripWithCatch(this.lakes.iterator().next().getId(),  LocalDateTime.now());
        createTripWithCatch(this.lakes.iterator().next().getId(),  LocalDateTime.now());
        // Should impact currentYear dashboard but not previousYear one
        comparePersonalDashboards(initialDashboardForCurrentYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear()), 2);
        comparePersonalDashboards(initialDashboardForLastYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear() - 1), 0);
    }

    @Test
    @Transactional
    public void testDefaultGlobalDashboard() {
        // Goal : Ensure that Global dashboards defaults to "all lakes" and "current year"
        GlobalDashboard initialDashboard = this.getGlobalDashboard();

        // Add a trip for previous year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(this.lakes.iterator().next().getId(), oneYearAgo);
        // Should not impact default dashboard
        compareGlobalDashboards(initialDashboard, this.getGlobalDashboard(), 0);


        // add a trip for current year
        createTripWithCatch(this.lakes.iterator().next().getId(),  LocalDateTime.now());
        // Should impact default dashboard
        compareGlobalDashboards(initialDashboard, this.getGlobalDashboard(), 1);
    }

    @Test
    @Transactional
    public void testGlobalDashboardForYear() {
        // Goal : make sure that year selections for personnal dashboard works as expected
        GlobalDashboard initialDashboardForCurrentYear = this.getGlobalDashboardForYear(LocalDateTime.now().getYear());
        GlobalDashboard initialDashboardForLastYear = this.getGlobalDashboardForYear(LocalDateTime.now().getYear() - 1);

        // Add 2 trips for previous year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(this.lakes.iterator().next().getId(), oneYearAgo);
        createTripWithCatch(this.lakes.iterator().next().getId(), oneYearAgo);
        // Should not impact currentYear dashboard but previousYear one
        compareGlobalDashboards(initialDashboardForCurrentYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear()), 0);
        compareGlobalDashboards(initialDashboardForLastYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear() - 1), 2);
        initialDashboardForLastYear = this.getGlobalDashboardForYear(LocalDateTime.now().getYear() - 1);


        // Add 2 trips for current year
        createTripWithCatch(this.lakes.iterator().next().getId(),  LocalDateTime.now());
        createTripWithCatch(this.lakes.iterator().next().getId(),  LocalDateTime.now());
        // Should impact currentYear dashboard but not previousYear one
        compareGlobalDashboards(initialDashboardForCurrentYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear()), 2);
        compareGlobalDashboards(initialDashboardForLastYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear() - 1), 0);
    }

    /**
     * Compares the two given dashboards. Fails if the second one does not have expectedAdditionalCatches more that the first.
     * @param d1 dashboard to compare
     * @param d2 dashboard to compare
     * @param expectedAdditionalCatches number of catches d2 should have in addition of d1 (if 0, d1 and d2 should be equal)
     */
    private void comparePersonalDashboards(Dashboard d1, Dashboard d2, Integer expectedAdditionalCatches) {
        if (expectedAdditionalCatches == 0) {
            Assertions.assertEquals(d1.toString(),d2.toString());
        } else {
            Assertions.assertNotEquals(d1.toString(),d2.toString());
            Assertions.assertNotEquals(d1.caughtSpeciesCount(), d2.caughtSpeciesCount());
            Assertions.assertNotEquals(d1.monthlySizes(), d2.monthlySizes());
            Assertions.assertNotEquals(d1.topBySize(), d2.topBySize());
            Assertions.assertNotEquals(d1.topByWeight(), d2.topByWeight());
            Assertions.assertEquals(d1.latestTripsCatchs().size() + expectedAdditionalCatches, d2.latestTripsCatchs().size());
        }
    }

    /**
     * Compares the two given dashboards. Fails if the second one does not have expectedAdditionalCatches more that the first.
     * @param d1 dashboard to compare
     * @param d2 dashboard to compare
     * @param expectedAdditionalCatches number of catches d2 should have in addition of d1 (if 0, d1 and d2 should be equal)
     */
    private void compareGlobalDashboards(GlobalDashboard d1, GlobalDashboard d2, Integer expectedAdditionalCatches) {
        if (expectedAdditionalCatches == 0) {
            Assertions.assertEquals(d1.caughtSpeciesCount(), d2.caughtSpeciesCount());
            Assertions.assertEquals(d1.monthlySizes(), d2.monthlySizes());
        } else {
            Assertions.assertNotEquals(d1.toString(),d2.toString());
            Assertions.assertNotEquals(d1.caughtSpeciesCount(), d2.caughtSpeciesCount());
            Assertions.assertNotEquals(d1.monthlySizes(), d2.monthlySizes());
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
        trip.setMode(TripMode.Live);
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
        aCatch.setSize((int)System.currentTimeMillis());
        aCatch.setWeight((int)System.currentTimeMillis());
        aCatch.setKept(true);
        catchsDao.create(aCatch);
    }

    private Dashboard getPersonalDashboard() {
        Optional<Integer> yearFilter = config.dashboardOnlyCurrentYear()
                ? Optional.of(Year.now().getValue())
                : Optional.empty();
        return this.dashboardDao.getPersonalDashboard(this.userId, yearFilter);
    }

    private Dashboard getPersonalDashboardForYear(Integer year) {
        return this.dashboardDao.getPersonalDashboard(this.userId, Optional.of(year));
    }

    private GlobalDashboard getGlobalDashboard() {
        Optional<Integer> yearFilter = config.dashboardOnlyCurrentYear()
                ? Optional.of(Year.now().getValue())
                : Optional.empty();
        return this.dashboardDao.computeGlobalDashboard(yearFilter, this.log);
    }

    private GlobalDashboard getGlobalDashboardForYear(Integer year) {
        return this.dashboardDao.computeGlobalDashboard(Optional.of(year), this.log);
    }
}
