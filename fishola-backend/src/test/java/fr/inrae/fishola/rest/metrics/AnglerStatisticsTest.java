package fr.inrae.fishola.rest.metrics;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.database.AnglerStatisticsDao;
import fr.inrae.fishola.database.AnglerStatisticsDao.TripFilter;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import fr.inrae.fishola.rest.dashboard.AnglerEffortStatistics.MonthlyEffort;
import fr.inrae.fishola.rest.dashboard.AnglerEffortStatistics.SectorContribution;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Statistiques d'effort de pêche du pêcheur (#210) : sessions / heures par
 * mois, temps par technique, CPUE par espèce, contribution au secteur.
 * Données sur l'année 2019, sur des milieux dédiés, pour rester indépendant
 * des autres tests.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnglerStatisticsTest extends AbstractFisholaTest {

    private static final int YEAR = 2019;
    private static final String EMAIL_PREFIX = "angler-stats-";

    @Inject
    AgroalDataSource dataSource;

    @Inject
    UsersDao usersDao;

    @Inject
    TripsDao tripsDao;

    @Inject
    CatchsDao catchsDao;

    @Inject
    AnglerStatisticsDao anglerStatisticsDao;

    private UUID anglerId;
    private UUID busyLakeId;
    private UUID quietLakeId;
    private UUID techniqueA;
    private UUID techniqueB;
    private UUID frequentSpecies;
    private UUID rareSpecies;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        busyLakeId = insertLake(ctx, "IT Stats Lac", "IT_STATS1");
        quietLakeId = insertLake(ctx, "IT Stats Etang", "IT_STATS2");
        List<UUID> techniques = ctx.fetch("SELECT id FROM technique ORDER BY name LIMIT 2").getValues("id", UUID.class);
        techniqueA = techniques.get(0);
        techniqueB = techniques.get(1);
        List<UUID> species = ctx.fetch("SELECT id FROM species ORDER BY name LIMIT 2").getValues("id", UUID.class);
        frequentSpecies = species.get(0);
        rareSpecies = species.get(1);

        anglerId = createAngler("me");
        UUID march = createTrip(anglerId, busyLakeId, LocalDateTime.of(YEAR, 3, 10, 8, 0), 2, techniqueA, techniqueB);
        createCatch(march, frequentSpecies, 12);
        createCatch(march, rareSpecies, 3);
        createTrip(anglerId, busyLakeId, LocalDateTime.of(YEAR, 3, 20, 8, 0), 1, techniqueA);
        createTrip(anglerId, busyLakeId, LocalDateTime.of(YEAR, 5, 2, 8, 0), 3);
        createTrip(anglerId, quietLakeId, LocalDateTime.of(YEAR, 6, 2, 8, 0), 1);

        for (int i = 0; i < 4; i++) {
            UUID other = createAngler("other" + i);
            UUID trip = createTrip(other, busyLakeId, LocalDateTime.of(YEAR, 4, 1 + i, 8, 0), 2);
            createCatch(trip, frequentSpecies, 5);
        }
        UUID lonelyNeighbour = createAngler("neighbour");
        createTrip(lonelyNeighbour, quietLakeId, LocalDateTime.of(YEAR, 6, 3, 8, 0), 1);
    }

    private static UUID insertLake(org.jooq.DSLContext ctx, String name, String code) {
        return ctx.fetchOne("INSERT INTO water_entity (name, export_as, water_entity_code, kind) "
                + "VALUES (?, ?, ?, 'STILL') RETURNING id", name, name, code).get("id", UUID.class);
    }

    private UUID createAngler(String suffix) {
        String email = EMAIL_PREFIX + suffix + "@fishola.test";
        usersDao.create("Angler", suffix, EMAIL_PREFIX + suffix, email, usersDao.hashPassword("sispea"),
                false, "74000", 1990);
        UUID id = usersDao.findByEmail(email).orElseThrow().getId();
        DSL.using(dataSource, SQLDialect.POSTGRES)
                .execute("UPDATE fishola_user SET exclude_from_exports = false WHERE id = ?", id);
        return id;
    }

    private UUID createTrip(UUID ownerId, UUID lakeId, LocalDateTime begin, int hours, UUID... techniqueIds) {
        Trip trip = new Trip();
        trip.setOwnerId(ownerId);
        trip.setCreatedOn(begin);
        trip.setBeginTimestamp(begin);
        trip.setEndTimestamp(begin.plusHours(hours));
        trip.setWaterEntityId(lakeId);
        trip.setName("ANGLER-STATS");
        trip.setType(TripType.Border);
        trip.setHidden(false);
        trip.setMode(TripMode.Afterwards);
        trip.setSource(DeviceType.web);
        UUID tripId = tripsDao.create(trip);
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        for (UUID techniqueId : techniqueIds) {
            ctx.execute("INSERT INTO trip_techniques (trip_id, technique_id) VALUES (?, ?)", tripId, techniqueId);
        }
        return tripId;
    }

    private void createCatch(UUID tripId, UUID speciesId, int quantity) {
        Catch aCatch = new Catch();
        aCatch.setTripId(tripId);
        aCatch.setCreatedOn(LocalDateTime.of(YEAR, 1, 1, 0, 0));
        aCatch.setSpeciesId(speciesId);
        aCatch.setTechniqueId(techniqueA);
        aCatch.setSize(25);
        aCatch.setKept(false);
        aCatch.setQuantity(quantity);
        aCatch.setMaillee(Maillage.NON_DEFINI);
        catchsDao.create(aCatch);
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE name = 'ANGLER-STATS')");
        ctx.execute("DELETE FROM trip_techniques WHERE trip_id IN (SELECT id FROM trip WHERE name = 'ANGLER-STATS')");
        ctx.execute("DELETE FROM trip WHERE name = 'ANGLER-STATS'");
        ctx.execute("DELETE FROM fishola_user WHERE email LIKE ?", EMAIL_PREFIX + "%");
        ctx.execute("DELETE FROM water_entity WHERE id IN (?, ?)", busyLakeId, quietLakeId);
    }

    private static TripFilter onLake(UUID lakeId) {
        return new TripFilter(Optional.of(YEAR), Optional.of(lakeId));
    }

    @Test
    @Transactional
    void countsSessionsAndHoursPerMonth() {
        Map<Month, MonthlyEffort> effort = anglerStatisticsDao.monthlyEffort(anglerId, onLake(busyLakeId));
        Assertions.assertEquals(Map.of(Month.MARCH, 2, Month.MAY, 1),
                Map.of(Month.MARCH, effort.get(Month.MARCH).tripsCount(), Month.MAY, effort.get(Month.MAY).tripsCount()));
        Assertions.assertEquals(3.0, effort.get(Month.MARCH).hours(), 0.001);
        Assertions.assertEquals(3.0, effort.get(Month.MAY).hours(), 0.001);
        Assertions.assertEquals(2, effort.size());
    }

    @Test
    @Transactional
    void splitsSessionTimeBetweenItsTechniques() {
        Map<UUID, Double> hours = anglerStatisticsDao.hoursPerTechnique(anglerId, onLake(busyLakeId));
        Assertions.assertEquals(2.0, hours.get(techniqueA), 0.001);
        Assertions.assertEquals(1.0, hours.get(techniqueB), 0.001);
    }

    @Test
    @Transactional
    void countsLotQuantitiesPerSpecies() {
        Map<UUID, Integer> catches = anglerStatisticsDao.catchesPerSpecies(anglerId, onLake(busyLakeId));
        Assertions.assertEquals(Map.of(frequentSpecies, 12, rareSpecies, 3), catches);
    }

    @Test
    @Transactional
    void sectorContributionIsAnAggregateAboveTheAnglerThreshold() {
        SectorContribution contribution = anglerStatisticsDao
                .sectorContribution(anglerId, onLake(busyLakeId), 5).orElseThrow();
        Assertions.assertEquals(5, contribution.anglersCount());
        Assertions.assertEquals(3, contribution.myTripsCount());
        Assertions.assertEquals(7, contribution.totalTripsCount());
        Assertions.assertEquals(15, contribution.myCatchesCount());
        Assertions.assertEquals(35, contribution.totalCatchesCount());
    }

    @Test
    @Transactional
    void sectorContributionIsHiddenBelowTheAnglerThresholdOrWithoutSector() {
        Assertions.assertTrue(anglerStatisticsDao.sectorContribution(anglerId, onLake(quietLakeId), 5).isEmpty());
        Assertions.assertTrue(anglerStatisticsDao
                .sectorContribution(anglerId, new TripFilter(Optional.of(YEAR), Optional.empty()), 5).isEmpty());
    }

    @Test
    void endpointUnlocksCpueOnlyAboveTheCatchThreshold() {
        String token = login(EMAIL_PREFIX + "me@fishola.test", "sispea");
        var json = given()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("year", YEAR)
                .queryParam("waterEntity", busyLakeId.toString())
                .when().get("/api/v1/dashboard/effort")
                .then().statusCode(200)
                .extract().jsonPath();

        Assertions.assertEquals(6.0, json.getDouble("totalHours"), 0.001);
        Assertions.assertEquals(10, json.getInt("cpueMinCatches"));
        Assertions.assertEquals(2.0, json.getDouble("cpuePerSpecies.find { it.speciesId == '" + frequentSpecies
                + "' }.catchesPerHour"), 0.001);
        Assertions.assertNull(json.get("cpuePerSpecies.find { it.speciesId == '" + rareSpecies + "' }.catchesPerHour"));
        Assertions.assertEquals(5, json.getInt("sectorContribution.anglersCount"));
    }
}
