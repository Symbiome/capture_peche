package fr.inrae.fishola.rest.metrics;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2023 INRAE - UMR CARRTEL
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.DashboardDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.Maillage;
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
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DashboardResourceTest  extends AbstractFisholaTest {
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
    void loadReferentials() {
        this.lakes = referentialDao.listLakes();
        this.species = referentialDao.listBuiltInSpecies();
        this.techniques = referentialDao.listBuiltInTechniques();
        this.weathers = referentialDao.listWeathers();
    }

    @BeforeEach
    @Transactional
    void login() {
        this.token = login("thimel@codelutin.com", "sispea");
        FisholaUser fisholaUser = this.usersDao.findByEmail("thimel@codelutin.com").get();
        this.userId = fisholaUser.getId();
        fisholaUser.setExcludeFromExports(false);
        this.usersDao.updateUser(fisholaUser);
    }

    @AfterEach
    @Transactional
    void teardown() {
        FisholaUser fisholaUser = this.usersDao.findById(this.userId).get();
        fisholaUser.setExcludeFromExports(true);
        this.usersDao.updateUser(fisholaUser);
    }

    @Test
    @Transactional
    void testDefaultPersonalDashboard() {
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
   void testPersonalDashboardForYear() {
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
    void testPersonalDashboardForLake() {
        // Goal : make sure that lake selections for personnal dashboard works as expected
        UUID lakeId1 = this.lakes.get(1).getId();
        UUID lakeId2 = this.lakes.get(2).getId();
        Dashboard initialDashboardForLake1 = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1);
        Dashboard initialDashboardForLake2 = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2);
        Dashboard initialDashboardForAllLakes = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear());
        Dashboard initialDashboardForLake1LastYear = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1);
        Dashboard initialDashboardForLake2LastYear = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2);
        Dashboard initialDashboardForAllLakesLastYear = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear());

        // Add 1 trip to lake 1
        createTripWithCatch(lakeId1, LocalDateTime.now());
        // Should not impact lake 2 dashboard but other ones
        comparePersonalDashboards(initialDashboardForLake1, this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1), 1);
        comparePersonalDashboards(initialDashboardForAllLakes, this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear()), 1);
        comparePersonalDashboards(initialDashboardForLake2, this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2), 0);
        initialDashboardForLake1 = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1);
        initialDashboardForAllLakes = this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear());

        // Add 1 trips to lake 2 - last year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(lakeId2, oneYearAgo);
        // For current year : should not be impacted
        comparePersonalDashboards(initialDashboardForLake1, this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1), 0);
        comparePersonalDashboards(initialDashboardForLake2, this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2), 0);
        comparePersonalDashboards(initialDashboardForAllLakes, this.getPersonalDashboardForLakeAndYear(LocalDateTime.now().getYear()), 0);

        // For last year : Should not impact lake 1 dashboard but other ones
        comparePersonalDashboards(initialDashboardForLake1LastYear, this.getPersonalDashboardForLakeAndYear(oneYearAgo.getYear(), lakeId1), 0);
        comparePersonalDashboards(initialDashboardForLake2LastYear, this.getPersonalDashboardForLakeAndYear(oneYearAgo.getYear(), lakeId2), 1);
        comparePersonalDashboards(initialDashboardForAllLakesLastYear, this.getPersonalDashboardForLakeAndYear(oneYearAgo.getYear()), 1);
    }

    @Test
    @Transactional
    void testDefaultGlobalDashboard() {
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
    void testGlobalDashboardForYear() {
        // Goal : make sure that year selections for global dashboard works as expected
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

    @Test
    @Transactional
    void testGlobalDashboardForLake() {
        // Goal : make sure that lake selections for global dashboard works as expected
        UUID lakeId1 = this.lakes.get(0).getId();
        UUID lakeId2 = this.lakes.get(1).getId();
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        GlobalDashboard initialDashboardForLake1 = this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1);
        GlobalDashboard initialDashboardForLake2 = this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2);
        GlobalDashboard initialDashboardForAllLakes = this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear());
        GlobalDashboard initialDashboardForLake1LastYear = this.getGlobalDashboardForLakeAndYear(oneYearAgo.getYear(), lakeId1);
        GlobalDashboard initialDashboardForLake2LastYear = this.getGlobalDashboardForLakeAndYear(oneYearAgo.getYear(), lakeId2);
        GlobalDashboard initialDashboardForAllLakesLastYear = this.getGlobalDashboardForLakeAndYear(oneYearAgo.getYear());

        // Add 1 trip to lake 1
        createTripWithCatch(lakeId1, LocalDateTime.now());
        // Should not impact lake 2 dashboard but other ones
        compareGlobalDashboards(initialDashboardForLake2, this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2), 0);
        compareGlobalDashboards(initialDashboardForLake1, this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1), 1);
        compareGlobalDashboards(initialDashboardForAllLakes, this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear()), 1);
        initialDashboardForLake1 = this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1);
        initialDashboardForLake2 = this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2);
        initialDashboardForAllLakes = this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear());

        // Add 1 trips to lake 2 - last year
        createTripWithCatch(lakeId2, oneYearAgo);
        // For current year : should not be impacted
        compareGlobalDashboards(initialDashboardForLake1, this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId1), 0);
        compareGlobalDashboards(initialDashboardForLake2, this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear(), lakeId2), 0);
        compareGlobalDashboards(initialDashboardForAllLakes, this.getGlobalDashboardForLakeAndYear(LocalDateTime.now().getYear()), 0);

        // For last year : Should not impact lake 1 dashboard but other ones
        compareGlobalDashboards(initialDashboardForLake1LastYear, this.getGlobalDashboardForLakeAndYear(oneYearAgo.getYear(), lakeId1), 0);
        compareGlobalDashboards(initialDashboardForLake2LastYear, this.getGlobalDashboardForLakeAndYear(oneYearAgo.getYear(), lakeId2), 1);
        compareGlobalDashboards(initialDashboardForAllLakesLastYear, this.getGlobalDashboardForLakeAndYear(oneYearAgo.getYear()), 1);
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
        aCatch.setMaillee(Maillage.NON_DEFINI);
        catchsDao.create(aCatch);
    }

    private Dashboard getPersonalDashboard() {
        Optional<Integer> yearFilter = config.dashboardOnlyCurrentYear()
                ? Optional.of(Year.now().getValue())
                : Optional.empty();
        return this.dashboardDao.getPersonalDashboard(this.userId, yearFilter, Optional.empty());
    }

    private Dashboard getPersonalDashboardForYear(Integer year) {
        return this.dashboardDao.getPersonalDashboard(this.userId, Optional.of(year), Optional.empty());
    }

    private Dashboard getPersonalDashboardForLakeAndYear(Integer year, UUID... lakeIds) {
        Optional<List<UUID>> lakesFilter = Optional.empty();
        if (lakeIds.length > 0) {
            lakesFilter = Optional.of(Lists.newArrayList(lakeIds));
        }
        return this.dashboardDao.getPersonalDashboard(this.userId, Optional.of(year), lakesFilter);
    }

    private GlobalDashboard getGlobalDashboard() {
        Optional<Integer> yearFilter = config.dashboardOnlyCurrentYear()
                ? Optional.of(Year.now().getValue())
                : Optional.empty();
        return this.dashboardDao.computeGlobalDashboard(yearFilter, Optional.empty());
    }

    private GlobalDashboard getGlobalDashboardForYear(Integer year) {
        return this.dashboardDao.computeGlobalDashboard(Optional.of(year), Optional.empty());
    }

    private GlobalDashboard getGlobalDashboardForLakeAndYear(Integer year, UUID... lakeIds) {
        Optional<List<UUID>> lakesFilter = Optional.empty();
        if (lakeIds.length > 0) {
            lakesFilter = Optional.of(Lists.newArrayList(lakeIds));
        }
        return this.dashboardDao.computeGlobalDashboard(Optional.of(year), lakesFilter);
    }
}
