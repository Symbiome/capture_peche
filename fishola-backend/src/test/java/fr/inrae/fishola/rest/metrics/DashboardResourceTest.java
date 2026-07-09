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
import fr.inrae.fishola.entities.tables.pojos.WaterEntity;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.dashboard.Dashboard;
import fr.inrae.fishola.rest.dashboard.GlobalDashboard;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    protected List<WaterEntity> waterEntities;
    protected List<Species> species;
    protected List<Technique> techniques;
    protected List<Weather> weathers;
    protected String token;
    protected UUID userId;

    @BeforeEach
    @Transactional
    void loadReferentials() {
        this.waterEntities = referentialDao.listWaterEntities();
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
        // Goal : Ensure that personal dashboards defaults to "all waterEntities" and "current year"
        Dashboard initialDashboard = this.getPersonalDashboard();

        // Add a trip for previous year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(this.waterEntities.iterator().next().getId(), oneYearAgo);
        // Should not impact default dashboard
        comparePersonalDashboards(initialDashboard, this.getPersonalDashboard(), 0);


        // add a trip for current year
        createTripWithCatch(this.waterEntities.iterator().next().getId(),  LocalDateTime.now());
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
        createTripWithCatch(this.waterEntities.iterator().next().getId(), oneYearAgo);
        createTripWithCatch(this.waterEntities.iterator().next().getId(), oneYearAgo);
        // Should not impact currentYear dashboard but previousYear one
        comparePersonalDashboards(initialDashboardForCurrentYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear()), 0);
        comparePersonalDashboards(initialDashboardForLastYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear() - 1), 2);
        initialDashboardForLastYear = this.getPersonalDashboardForYear(LocalDateTime.now().getYear() - 1);


        // Add 2 trips for current year
        createTripWithCatch(this.waterEntities.iterator().next().getId(),  LocalDateTime.now());
        createTripWithCatch(this.waterEntities.iterator().next().getId(),  LocalDateTime.now());
        // Should impact currentYear dashboard but not previousYear one
        comparePersonalDashboards(initialDashboardForCurrentYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear()), 2);
        comparePersonalDashboards(initialDashboardForLastYear, this.getPersonalDashboardForYear(LocalDateTime.now().getYear() - 1), 0);
    }

    @Test
    @Transactional
    void testPersonalDashboardForWaterEntity() {
        // Goal : make sure that waterEntity selections for personnal dashboard works as expected
        UUID waterEntityId1 = this.waterEntities.get(1).getId();
        UUID waterEntityId2 = this.waterEntities.get(2).getId();
        Dashboard initialDashboardForWaterEntity1 = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1);
        Dashboard initialDashboardForWaterEntity2 = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2);
        Dashboard initialDashboardForAllWaterEntities = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear());
        Dashboard initialDashboardForWaterEntity1LastYear = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1);
        Dashboard initialDashboardForWaterEntity2LastYear = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2);
        Dashboard initialDashboardForAllWaterEntitiesLastYear = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear());

        // Add 1 trip to waterEntity 1
        createTripWithCatch(waterEntityId1, LocalDateTime.now());
        // Should not impact waterEntity 2 dashboard but other ones
        comparePersonalDashboards(initialDashboardForWaterEntity1, this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1), 1);
        comparePersonalDashboards(initialDashboardForAllWaterEntities, this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear()), 1);
        comparePersonalDashboards(initialDashboardForWaterEntity2, this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2), 0);
        initialDashboardForWaterEntity1 = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1);
        initialDashboardForAllWaterEntities = this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear());

        // Add 1 trips to waterEntity 2 - last year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(waterEntityId2, oneYearAgo);
        // For current year : should not be impacted
        comparePersonalDashboards(initialDashboardForWaterEntity1, this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1), 0);
        comparePersonalDashboards(initialDashboardForWaterEntity2, this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2), 0);
        comparePersonalDashboards(initialDashboardForAllWaterEntities, this.getPersonalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear()), 0);

        // For last year : Should not impact waterEntity 1 dashboard but other ones
        comparePersonalDashboards(initialDashboardForWaterEntity1LastYear, this.getPersonalDashboardForWaterEntityAndYear(oneYearAgo.getYear(), waterEntityId1), 0);
        comparePersonalDashboards(initialDashboardForWaterEntity2LastYear, this.getPersonalDashboardForWaterEntityAndYear(oneYearAgo.getYear(), waterEntityId2), 1);
        comparePersonalDashboards(initialDashboardForAllWaterEntitiesLastYear, this.getPersonalDashboardForWaterEntityAndYear(oneYearAgo.getYear()), 1);
    }

    @Test
    @Transactional
    void testDefaultGlobalDashboard() {
        // Goal : Ensure that Global dashboards defaults to "all waterEntities" and "current year"
        GlobalDashboard initialDashboard = this.getGlobalDashboard();

        // Add a trip for previous year
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        createTripWithCatch(this.waterEntities.iterator().next().getId(), oneYearAgo);
        // Should not impact default dashboard
        compareGlobalDashboards(initialDashboard, this.getGlobalDashboard(), 0);


        // add a trip for current year
        createTripWithCatch(this.waterEntities.iterator().next().getId(),  LocalDateTime.now());
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
        createTripWithCatch(this.waterEntities.iterator().next().getId(), oneYearAgo);
        createTripWithCatch(this.waterEntities.iterator().next().getId(), oneYearAgo);
        // Should not impact currentYear dashboard but previousYear one
        compareGlobalDashboards(initialDashboardForCurrentYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear()), 0);
        compareGlobalDashboards(initialDashboardForLastYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear() - 1), 2);
        initialDashboardForLastYear = this.getGlobalDashboardForYear(LocalDateTime.now().getYear() - 1);


        // Add 2 trips for current year
        createTripWithCatch(this.waterEntities.iterator().next().getId(),  LocalDateTime.now());
        createTripWithCatch(this.waterEntities.iterator().next().getId(),  LocalDateTime.now());
        // Should impact currentYear dashboard but not previousYear one
        compareGlobalDashboards(initialDashboardForCurrentYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear()), 2);
        compareGlobalDashboards(initialDashboardForLastYear, this.getGlobalDashboardForYear(LocalDateTime.now().getYear() - 1), 0);
    }

    @Test
    @Transactional
    void testGlobalDashboardForWaterEntity() {
        // Goal : make sure that waterEntity selections for global dashboard works as expected
        UUID waterEntityId1 = this.waterEntities.get(0).getId();
        UUID waterEntityId2 = this.waterEntities.get(1).getId();
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        GlobalDashboard initialDashboardForWaterEntity1 = this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1);
        GlobalDashboard initialDashboardForWaterEntity2 = this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2);
        GlobalDashboard initialDashboardForAllWaterEntities = this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear());
        GlobalDashboard initialDashboardForWaterEntity1LastYear = this.getGlobalDashboardForWaterEntityAndYear(oneYearAgo.getYear(), waterEntityId1);
        GlobalDashboard initialDashboardForWaterEntity2LastYear = this.getGlobalDashboardForWaterEntityAndYear(oneYearAgo.getYear(), waterEntityId2);
        GlobalDashboard initialDashboardForAllWaterEntitiesLastYear = this.getGlobalDashboardForWaterEntityAndYear(oneYearAgo.getYear());

        // Add 1 trip to waterEntity 1
        createTripWithCatch(waterEntityId1, LocalDateTime.now());
        // Should not impact waterEntity 2 dashboard but other ones
        compareGlobalDashboards(initialDashboardForWaterEntity2, this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2), 0);
        compareGlobalDashboards(initialDashboardForWaterEntity1, this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1), 1);
        compareGlobalDashboards(initialDashboardForAllWaterEntities, this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear()), 1);
        initialDashboardForWaterEntity1 = this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1);
        initialDashboardForWaterEntity2 = this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2);
        initialDashboardForAllWaterEntities = this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear());

        // Add 1 trips to waterEntity 2 - last year
        createTripWithCatch(waterEntityId2, oneYearAgo);
        // For current year : should not be impacted
        compareGlobalDashboards(initialDashboardForWaterEntity1, this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId1), 0);
        compareGlobalDashboards(initialDashboardForWaterEntity2, this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear(), waterEntityId2), 0);
        compareGlobalDashboards(initialDashboardForAllWaterEntities, this.getGlobalDashboardForWaterEntityAndYear(LocalDateTime.now().getYear()), 0);

        // For last year : Should not impact waterEntity 1 dashboard but other ones
        compareGlobalDashboards(initialDashboardForWaterEntity1LastYear, this.getGlobalDashboardForWaterEntityAndYear(oneYearAgo.getYear(), waterEntityId1), 0);
        compareGlobalDashboards(initialDashboardForWaterEntity2LastYear, this.getGlobalDashboardForWaterEntityAndYear(oneYearAgo.getYear(), waterEntityId2), 1);
        compareGlobalDashboards(initialDashboardForAllWaterEntitiesLastYear, this.getGlobalDashboardForWaterEntityAndYear(oneYearAgo.getYear()), 1);
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

    private void createTripWithCatch(UUID waterEntityId, LocalDateTime tripDate) {
        Trip trip = new Trip();
        trip.setCreatedOn(tripDate);
        trip.setDay(tripDate.toLocalDate());
        trip.setStartTime(tripDate.toLocalTime().minusHours(4));
        trip.setEndTime(tripDate.toLocalTime());
        trip.setWaterEntityId(waterEntityId);
        trip.setName("Trip for waterEntity " + waterEntityId + " " + tripDate.toString());
        trip.setType(TripType.Craft);
        trip.setHidden(false);
        trip.setMode(TripMode.Live);
        trip.setOwnerId(this.userId);
        trip.setWeatherId(this.weathers.stream().limit(1).map(Weather::getId).collect(ImmutableSet.toImmutableSet()).iterator().next());
        trip.setBeginPosition("POINT(42 42)");
        trip.setEndPosition("POINT(42 42)");
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

    private Dashboard getPersonalDashboardForWaterEntityAndYear(Integer year, UUID... waterEntityIds) {
        Optional<List<UUID>> waterEntitiesFilter = Optional.empty();
        if (waterEntityIds.length > 0) {
            waterEntitiesFilter = Optional.of(Lists.newArrayList(waterEntityIds));
        }
        return this.dashboardDao.getPersonalDashboard(this.userId, Optional.of(year), waterEntitiesFilter);
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

    private GlobalDashboard getGlobalDashboardForWaterEntityAndYear(Integer year, UUID... waterEntityIds) {
        Optional<List<UUID>> waterEntitiesFilter = Optional.empty();
        if (waterEntityIds.length > 0) {
            waterEntitiesFilter = Optional.of(Lists.newArrayList(waterEntityIds));
        }
        return this.dashboardDao.computeGlobalDashboard(Optional.of(year), waterEntitiesFilter);
    }
}
