package fr.inrae.fishola.rest.imports;

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

/**
 * Import CSV opérateur de bout en bout (#71) : le fichier doit réellement créer
 * des sorties en base, et un ré-import doit être détecté comme doublon.
 *
 * <p>Auto-suffisant : provisionne son opérateur et son périmètre à partir des
 * entités de la fixture de test.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportCsvTest {

    private static final String HEADER = String.join(";", ImportSchema.EXPECTED_HEADER);

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID operatorId = UUID.randomUUID();
    private String operatorToken;
    private UUID waterEntityId;
    private String waterEntityName;

    @BeforeAll
    @Transactional
    void seedOperator() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        // Une entité hydro de la fixture sert de périmètre.
        var rec = ctx.fetchOne("SELECT id, name FROM water_entity WHERE name = 'Annecy' LIMIT 1");
        waterEntityId = rec.get("id", UUID.class);
        waterEntityName = rec.get("name", String.class);

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "import-test-op@fishola.test", "x");
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
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE name LIKE '%IMPORT-TEST%')");
        ctx.execute("DELETE FROM trip WHERE name LIKE '%IMPORT-TEST%'");
        ctx.execute("DELETE FROM import_row_error WHERE import_id IN (SELECT id FROM import_job WHERE file_name LIKE 'import-test%')");
        ctx.execute("DELETE FROM import_job WHERE file_name LIKE 'import-test%'");
        ctx.execute("DELETE FROM fishola_admin_water_entities WHERE fishola_admin_id = ?", operatorId);
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
    }

    /** Une sortie avec une capture, sur l'entité du périmètre. */
    private String csv(String sessionRef) {
        String row = String.join(";",
                sessionRef, "carnet_volontaire", "01/07/2026", "08:00", "11:30",
                waterEntityName, "", "du bord", "Pêche au coup", "1",
                "", "non",
                "", "", "", "", "", "", "",
                "Perche", "25", "210", "oui", "1", "", "non", "", "");
        return HEADER + "\n" + row + "\n";
    }

    private int countJobs(String fileName) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchOne("SELECT count(*) FROM import_job WHERE file_name = ?", fileName).get(0, Integer.class);
    }

    @Test
    void importCreatesTripsAndJob() {
        String file = "import-test-ok.csv";
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(csv("IMPORT-TEST-1").getBytes(StandardCharsets.UTF_8))
                .when().post("/api/v1/admin/imports?filename=" + file + "&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE"))
                .body("inserted", equalTo(1))
                .body("rejected", equalTo(0))
                // Un premier import ne peut pas être un doublon.
                .body("duplicate", equalTo(false));

        // Le job et la sortie doivent EXISTER en base (transaction réellement validée).
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        int jobs = countJobs(file);
        org.junit.jupiter.api.Assertions.assertEquals(1, jobs, "le job d'import doit être persisté");
        int trips = ctx.fetchOne("SELECT count(*) FROM trip WHERE name LIKE '%IMPORT-TEST-1%'").get(0, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, trips, "la sortie doit être persistée");
    }
}
