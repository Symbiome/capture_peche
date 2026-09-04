package fr.inrae.fishola.rest.imports.carnet;

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

import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Import CSV opérateur « carnet volontaire » de bout en bout (#143), pendant de
 * {@code ImportCsvTest} (#71) pour le pipeline dédié : le fichier doit réellement créer
 * des sorties en base, un ré-import doit être détecté comme doublon, et les règles
 * métier propres à ce format (bornes de lot, marquage) doivent rejeter les lignes fautives.
 *
 * <p>Auto-suffisant : provisionne son opérateur et son périmètre à partir des
 * entités de la fixture de test.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CarnetVolontaireImportCsvTest {

    private static final String HEADER = String.join(";", CarnetVolontaireSchema.EXPECTED_HEADER);
    private static final String URI = "/api/v1/admin/imports/carnet-volontaire";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID operatorId = UUID.randomUUID();
    private String operatorToken;
    private String waterEntityName;

    @BeforeAll
    @Transactional
    void seedOperator() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var rec = ctx.fetchOne("SELECT id, name FROM water_entity WHERE name = 'Annecy' LIMIT 1");
        UUID waterEntityId = rec.get("id", UUID.class);
        waterEntityName = rec.get("name", String.class);

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "carnet-test-op@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_water_entities (fishola_admin_id, water_entity_id) VALUES (?, ?)",
                operatorId, waterEntityId);

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
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE name LIKE '%CARNET-TEST%')");
        ctx.execute("DELETE FROM trip WHERE name LIKE '%CARNET-TEST%'");
        ctx.execute("DELETE FROM import_row_error WHERE import_id IN (SELECT id FROM import_job WHERE file_name LIKE 'carnet-test%')");
        ctx.execute("DELETE FROM import_job WHERE file_name LIKE 'carnet-test%'");
        ctx.execute("DELETE FROM fishola_admin_water_entities WHERE fishola_admin_id = ?", operatorId);
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
    }

    /** Une sortie avec une capture individuelle valide (quantité 1), sur l'entité du périmètre. */
    private String csv(String sessionRef) {
        String row = String.join(";",
                sessionRef, waterEntityName, "", "01/07/2026", "bord statique", "08:00", "11:30",
                "Pêche au coup", "", "1", "", "Aucune", "", "non",
                "Perche", "", "", "", "", "25", "210", "1", "oui", "", "", "non", "");
        return HEADER + "\n" + row + "\n";
    }

    /** Une sortie avec un lot (quantité 2) SANS taille min/max : doit être rejetée. */
    private String csvLotSansBornes(String sessionRef) {
        String row = String.join(";",
                sessionRef, waterEntityName, "", "01/07/2026", "bord statique", "08:00", "11:30",
                "Pêche au coup", "", "1", "", "Aucune", "", "non",
                "Perche", "", "", "", "", "", "", "2", "oui", "", "", "non", "");
        return HEADER + "\n" + row + "\n";
    }

    /** Une capture marquée/baguée SANS numéro de marquage : doit être rejetée. */
    private String csvMarqueSansNumero(String sessionRef) {
        String row = String.join(";",
                sessionRef, waterEntityName, "", "01/07/2026", "bord statique", "08:00", "11:30",
                "Pêche au coup", "", "1", "", "Aucune", "", "non",
                "Perche", "", "", "", "", "25", "210", "1", "oui", "", "", "oui", "");
        return HEADER + "\n" + row + "\n";
    }

    private int countJobs(String fileName) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchOne("SELECT count(*) FROM import_job WHERE file_name = ?", fileName).get(0, Integer.class);
    }

    @Test
    void importCreatesTripsAndJob() {
        String file = "carnet-test-ok.csv";
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(csv("CARNET-TEST-1").getBytes(StandardCharsets.UTF_8))
                .when().post(URI + "?filename=" + file + "&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE"))
                .body("inserted", equalTo(1))
                .body("rejected", equalTo(0))
                .body("duplicate", equalTo(false));

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        org.junit.jupiter.api.Assertions.assertEquals(1, countJobs(file), "le job d'import doit être persisté");
        int trips = ctx.fetchOne("SELECT count(*) FROM trip WHERE name LIKE '%CARNET-TEST-1%'").get(0, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, trips, "la sortie doit être persistée");
    }

    @Test
    void reimportIsDetectedAsDuplicate() {
        String file = "carnet-test-dup.csv";
        byte[] bytes = csv("CARNET-TEST-DUP").getBytes(StandardCharsets.UTF_8);

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream").body(bytes)
                .when().post(URI + "?filename=" + file + "&mode=partial")
                .then().statusCode(200);

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream").body(bytes)
                .when().post(URI + "?filename=" + file + "&mode=partial")
                .then().statusCode(409)
                .body("duplicate", equalTo(true));

        org.junit.jupiter.api.Assertions.assertEquals(1, countJobs(file), "un seul job doit exister après le doublon");
    }

    @Test
    void lotWithoutBoundsIsRejected() {
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(csvLotSansBornes("CARNET-TEST-LOT").getBytes(StandardCharsets.UTF_8))
                .when().post(URI + "?filename=carnet-test-lot.csv&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE_WITH_ERRORS"))
                .body("inserted", equalTo(0))
                .body("rejected", equalTo(1))
                .body("errors.code", hasItem("METIER_LOT_BOUNDS"));
    }

    @Test
    void taggedCatchWithoutReferenceIsRejected() {
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(csvMarqueSansNumero("CARNET-TEST-TAG").getBytes(StandardCharsets.UTF_8))
                .when().post(URI + "?filename=carnet-test-tag.csv&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE_WITH_ERRORS"))
                .body("inserted", equalTo(0))
                .body("rejected", equalTo(1))
                .body("errors.code", hasItem("METIER_TAG_REF"));
    }
}
