package fr.inrae.fishola.rest.referential;

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

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Retrait d'un élément de référentiel déjà utilisé (#202) : sa suppression est refusée
 * avec le détail de son usage (captures, sorties), il peut être archivé puis restauré,
 * et reste servi aux clients (flag {@code archived}) pour afficher l'historique.
 *
 * <p>Auto-suffisant : provisionne son admin national, un pêcheur, une espèce et une
 * technique dédiées et une sortie avec une capture qui les utilise.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReferentialArchiveTest {

    private static final String SPECIES_URI = "/api/v1/referential/raw-species";
    private static final String TECHNIQUES_URI = "/api/v1/referential/techniques";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    TripsDao tripsDao;

    @Inject
    CatchsDao catchsDao;

    private final UUID adminId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID usedSpeciesId = UUID.randomUUID();
    private final UUID usedTechniqueId = UUID.randomUUID();
    private String adminToken;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", adminId, "archive-test-national@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_user (id, first_name, last_name, email, password, created_on, pseudo) "
                + "VALUES (?, 'Archive', 'Test', 'archive-test@fishola.test', 'x', now(), 'archive-test')", userId);
        insertSpecies(ctx, usedSpeciesId, "Espèce archivage utilisée");
        ctx.execute("INSERT INTO technique (id, name, export_as, built_in) VALUES (?, ?, ?, true)",
                usedTechniqueId, "Technique archivage", "TechniqueArchivageTest");

        UUID waterEntityId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Annecy'").get("id", UUID.class);
        UUID tripId = tripsDao.create(trip(waterEntityId));
        ctx.execute("INSERT INTO trip_techniques (trip_id, technique_id) VALUES (?, ?)", tripId, usedTechniqueId);
        catchsDao.create(aCatch(tripId));

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            adminToken = jwtHelper.createAdminToken(adminId);
        } finally {
            requestContext.deactivate();
        }
    }

    private static void insertSpecies(org.jooq.DSLContext ctx, UUID id, String name) {
        ctx.execute("INSERT INTO species (id, name, export_as, built_in) VALUES (?, ?, ?, true)",
                id, name, name.replace(' ', '_'));
    }

    private Trip trip(UUID waterEntityId) {
        Trip trip = new Trip();
        trip.setCreatedOn(LocalDateTime.now());
        trip.setOwnerId(userId);
        trip.setMode(TripMode.Afterwards);
        trip.setName("Sortie de test #202");
        trip.setType(TripType.Border);
        trip.setWaterEntityId(waterEntityId);
        trip.setSource(DeviceType.web);
        trip.setBeginTimestamp(LocalDateTime.now().minusHours(3));
        trip.setEndTimestamp(LocalDateTime.now().minusHours(1));
        return trip;
    }

    private Catch aCatch(UUID tripId) {
        Catch aCatch = new Catch();
        aCatch.setTripId(tripId);
        aCatch.setCreatedOn(LocalDateTime.now());
        aCatch.setCatchTimestamp(LocalDateTime.now().minusHours(2));
        aCatch.setSpeciesId(usedSpeciesId);
        aCatch.setTechniqueId(usedTechniqueId);
        aCatch.setSize(30);
        aCatch.setKept(false);
        return aCatch;
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE owner_id = ?)", userId);
        ctx.execute("DELETE FROM trip_techniques WHERE trip_id IN (SELECT id FROM trip WHERE owner_id = ?)", userId);
        ctx.execute("DELETE FROM trip WHERE owner_id = ?", userId);
        ctx.execute("DELETE FROM species WHERE id = ?", usedSpeciesId);
        ctx.execute("DELETE FROM technique WHERE id = ?", usedTechniqueId);
        ctx.execute("DELETE FROM fishola_user WHERE id = ?", userId);
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", adminId);
    }

    private RequestSpecification asAdmin() {
        return given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken);
    }

    @Test
    void usageCountsCatchesAndTrips() {
        asAdmin().when().get(SPECIES_URI + "/usage/" + usedSpeciesId)
                .then().statusCode(200)
                .body("catches", equalTo(1))
                .body("trips", equalTo(0));
        asAdmin().when().get(TECHNIQUES_URI + "/usage/" + usedTechniqueId)
                .then().statusCode(200)
                .body("catches", equalTo(1))
                .body("trips", equalTo(1));
    }

    @Test
    void deletingUsedItemsIsRefusedWithTheirUsage() {
        asAdmin().when().get(SPECIES_URI + "/can-delete/" + usedSpeciesId)
                .then().statusCode(200).body(equalTo("false"));
        asAdmin().when().delete(SPECIES_URI + "/" + usedSpeciesId)
                .then().statusCode(409).body("catches", equalTo(1));
        asAdmin().when().delete(TECHNIQUES_URI + "/" + usedTechniqueId)
                .then().statusCode(409).body("trips", equalTo(1));
    }

    @Test
    void usedSpeciesCanBeArchivedAndRestoredWithoutLosingCatches() {
        setArchived(usedSpeciesId, true);
        asAdmin().when().get("/api/v1/referential/species")
                .then().statusCode(200)
                .body("find { it.id == '" + usedSpeciesId + "' }.archived", equalTo(true));

        setArchived(usedSpeciesId, false);
        asAdmin().when().get(SPECIES_URI + "/usage/" + usedSpeciesId)
                .then().statusCode(200).body("catches", equalTo(1));
    }

    @Test
    void unusedSpeciesIsStillDeleted() {
        UUID id = UUID.randomUUID();
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        insertSpecies(ctx, id, "Espèce archivage à supprimer");
        asAdmin().when().delete(SPECIES_URI + "/" + id).then().statusCode(204);
        asAdmin().when().get(SPECIES_URI)
                .then().statusCode(200)
                .body("id", org.hamcrest.Matchers.not(hasItem(id.toString())));
    }

    private void setArchived(UUID speciesId, boolean archived) {
        Map<String, Object> species = asAdmin().when().get(SPECIES_URI)
                .then().statusCode(200)
                .extract().jsonPath().getMap("find { it.id == '" + speciesId + "' }");
        species.put("archived", archived);
        asAdmin().contentType(MediaType.APPLICATION_JSON).body(species)
                .when().put(SPECIES_URI + "/" + speciesId)
                .then().statusCode(204);
    }
}
