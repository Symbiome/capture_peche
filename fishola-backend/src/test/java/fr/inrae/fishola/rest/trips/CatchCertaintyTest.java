package fr.inrae.fishola.rest.trips;

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
import fr.inrae.fishola.entities.enums.IdentificationCertainty;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Note de certitude sur l'identification d'une prise (#87). Auto-suffisant, gabarit
 * {@code DepartmentalScopeTest}/{@code OperatorAccessTest} : comptes et sortie/prise
 * provisionnés en base, jetons forgés -- ne dépend pas du seed de recette.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatchCertaintyTest {

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    TripsDao tripsDao;

    @Inject
    CatchsDao catchsDao;

    private final UUID operatorId = UUID.randomUUID();
    private String operatorToken;
    private UUID speciesId;
    private UUID otherSpeciesId;
    private UUID techniqueId;
    private UUID waterEntityId;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "certainty-test-operator@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", operatorId);

        speciesId = ctx.fetchOne("SELECT id FROM species ORDER BY name LIMIT 1").get("id", UUID.class);
        otherSpeciesId = ctx.fetchOne("SELECT id FROM species ORDER BY name OFFSET 1 LIMIT 1").get("id", UUID.class);
        techniqueId = ctx.fetchOne("SELECT id FROM technique LIMIT 1").get("id", UUID.class);
        waterEntityId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Annecy'").get("id", UUID.class);

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            operatorToken = jwtHelper.createAdminToken(operatorId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE name LIKE 'CERTAINTY-TEST-%')");
        ctx.execute("DELETE FROM trip WHERE name LIKE 'CERTAINTY-TEST-%'");
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
    }

    /**
     * Commit immédiat (transaction dédiée, comme {@code testEditedInBoFields}) : une prise
     * créée depuis un test lui-même {@code @Transactional} doit être visible des appels REST
     * qui suivent dans la même méthode -- exécutés dans un thread/une connexion séparés.
     */
    protected UUID createTripWithCatch(String name, IdentificationCertainty certainty) {
        return createTripWithCatch(name, certainty, LocalDateTime.now().minusDays(30));
    }

    /**
     * @param tripCreatedOn permet de simuler une sortie fraîchement saisie par un
     *                      pêcheur (proche de {@code now}), par opposition à une sortie
     *                      ancienne comme dans les autres tests -- c'est justement ce cas
     *                      que l'embargo d'export (V1.4.0) masquait à tort de la file
     *                      « Prises à valider » avant V2.6.0 (cf.
     *                      {@link #freshlySubmittedProbableCatchIsImmediatelyVisibleInQueue}).
     */
    protected UUID createTripWithCatch(String name, IdentificationCertainty certainty, LocalDateTime tripCreatedOn) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Trip trip = new Trip();
            trip.setCreatedOn(tripCreatedOn);
            trip.setBeginTimestamp(tripCreatedOn.minusHours(3));
            trip.setEndTimestamp(tripCreatedOn);
            trip.setWaterEntityId(waterEntityId);
            trip.setName(name);
            trip.setType(TripType.Border);
            trip.setHidden(false);
            trip.setMode(TripMode.Afterwards);
            trip.setSource(DeviceType.web);
            UUID tripId = tripsDao.create(trip);
            tripsDao.updatePositions(tripId, "POINT(6.17 45.85)", null);
            tripsDao.stampDepartment(tripId);

            Catch aCatch = new Catch();
            aCatch.setTripId(tripId);
            aCatch.setCreatedOn(tripCreatedOn);
            aCatch.setCatchTimestamp(tripCreatedOn);
            aCatch.setSpeciesId(speciesId);
            aCatch.setTechniqueId(techniqueId);
            aCatch.setSize(25);
            aCatch.setWeight(200);
            aCatch.setKept(true);
            aCatch.setMaillee(Maillage.NON_DEFINI);
            if (certainty != null) {
                aCatch.setCertainty(certainty);
            }
            UUID catchId = catchsDao.create(aCatch);
            catchsDao.stampDepartment(catchId);
            return catchId;
        });
    }

    @Test
    @Transactional
    void createDefaultsCertaintyToCertain() {
        // Aucune certitude fournie -- CatchsDao.create() doit appliquer CERTAIN par défaut
        // (comme editedSpeciesId/editedSize/editedWeight), la colonne étant NOT NULL (#87).
        UUID catchId = createTripWithCatch("CERTAINTY-TEST-DEFAULT", null);
        Catch aCatch = catchsDao.getCatch(catchId);
        Assertions.assertEquals(IdentificationCertainty.CERTAIN, aCatch.getCertainty());
        Assertions.assertNull(aCatch.getValidatedBy());
        Assertions.assertNull(aCatch.getValidatedAt());
    }

    @Test
    @Transactional
    void probableCatchIsExcludedFromPublicStatsAndQueueUntilOperatorValidates() {
        int catchsBefore = catchsDao.countCatchs();

        UUID catchId = createTripWithCatch("CERTAINTY-TEST-WORKFLOW", IdentificationCertainty.PROBABLE);

        // Pas encore revue par un opérateur : absente des statistiques publiques (#87).
        Assertions.assertEquals(catchsBefore, catchsDao.countCatchs());
        boolean presentBeforeValidation = catchsDao.findAll(Optional.empty(), Optional.empty()).values().stream()
                .anyMatch(c -> c.getId().equals(catchId));
        Assertions.assertFalse(presentBeforeValidation, "une prise PROBABLE non validée ne doit pas compter dans les stats publiques");

        // Visible dans la file « Prises à valider » (#87), ouverte à l'opérateur.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get("/api/v1/trips/catches/pending-validation/0/date_de_la_sortie/desc")
                .then().statusCode(200)
                .body("elements.catchId", hasItem(catchId.toString()));

        // L'opérateur corrige l'espèce et valide en un seul geste (PUT), comme editedSpeciesId (#87).
        CatchBean updatedCatch = new CatchBean();
        updatedCatch.editedSpeciesId = Optional.of(otherSpeciesId);
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatedCatch)
                .when().put("/api/v1/trips/catches/" + catchId)
                .then().statusCode(200);

        Catch validatedCatch = catchsDao.getCatch(catchId);
        Assertions.assertEquals(otherSpeciesId, validatedCatch.getEditedSpeciesId());
        Assertions.assertEquals(operatorId, validatedCatch.getValidatedBy());
        Assertions.assertNotNull(validatedCatch.getValidatedAt());

        // Désormais comptée dans les statistiques publiques.
        Assertions.assertEquals(catchsBefore + 1, catchsDao.countCatchs());
        boolean presentAfterValidation = catchsDao.findAll(Optional.empty(), Optional.empty()).values().stream()
                .anyMatch(c -> c.getId().equals(catchId));
        Assertions.assertTrue(presentAfterValidation, "une prise validée doit compter dans les stats publiques, quelle que soit la certitude déclarée");

        // Plus dans la file : déjà validée.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get("/api/v1/trips/catches/pending-validation/0/date_de_la_sortie/desc")
                .then().statusCode(200)
                .body("elements.catchId", org.hamcrest.CoreMatchers.not(hasItem(catchId.toString())));
    }

    @Test
    @Transactional
    void freshlySubmittedProbableCatchIsImmediatelyVisibleInQueue() {
        // Régression (#87 -> bug rapporté) : la file « Prises à valider » interrogeait
        // catchs_openadom_export, qui masque les sorties saisie_pecheur créées il y a
        // moins de 168h (embargo export, V1.4.0). Une prise incertaine tout juste saisie
        // par un pêcheur restait donc invisible de tout le staff -- national comme
        // régional -- pendant une semaine. Depuis V2.6.0, la file interroge
        // catchs_pending_validation, qui ne porte pas cet embargo.
        UUID catchId = createTripWithCatch("CERTAINTY-TEST-FRESH", IdentificationCertainty.UNCERTAIN, LocalDateTime.now());

        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get("/api/v1/trips/catches/pending-validation/0/date_de_la_sortie/desc")
                .then().statusCode(200)
                .body("elements.catchId", hasItem(catchId.toString()));
    }

    @Test
    void pendingValidationEndpointRejectsUnauthenticated() {
        given()
                .when().get("/api/v1/trips/catches/pending-validation/0/date_de_la_sortie/desc")
                .then().statusCode(401);
    }
}
