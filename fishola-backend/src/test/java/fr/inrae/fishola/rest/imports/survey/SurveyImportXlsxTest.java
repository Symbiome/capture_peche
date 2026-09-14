package fr.inrae.fishola.rest.imports.survey;

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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Import XLSX opérateur « enquête terrain » de bout en bout (#144), pendant de {@code
 * CarnetVolontaireImportCsvTest} (#143) pour ce pipeline multi-feuilles : le classeur doit
 * réellement créer une session, un pêcheur enquêté et une sortie en base, un ré-import doit
 * être détecté comme doublon, une clé de liaison orpheline et une règle métier fautive
 * doivent rejeter les lignes concernées, et une session souvenir doit produire sa propre
 * sortie indépendante.
 *
 * <p>Auto-suffisant : provisionne son opérateur et son périmètre à partir des entités de la
 * fixture de test (mêmes plans d'eau qu'{@code CarnetVolontaireImportCsvTest}).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SurveyImportXlsxTest {

    private static final String URI = "/api/v1/admin/imports/survey";

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
        var rec = ctx.fetchOne("SELECT id, name, department FROM water_entity WHERE name = 'Annecy' LIMIT 1");
        waterEntityName = rec.get("name", String.class);
        String department = rec.get("department", String.class);

        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "survey-test-op@fishola.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                operatorId, department);

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
        // Un trip « souvenir » n'a pas de survey_session_id (cf. #144) : le repère aussi par
        // surveyed_angler_id, sans quoi il échappe au nettoyage par nom de session/sortie.
        String tripFilter = "(survey_session_id IN (SELECT id FROM survey_session WHERE code LIKE 'SURVEY-TEST%') "
                + "OR surveyed_angler_id IN (SELECT id FROM surveyed_angler WHERE code = 'P1'))";
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE " + tripFilter + ")");
        ctx.execute("DELETE FROM trip WHERE " + tripFilter);
        ctx.execute("DELETE FROM import_row_error WHERE import_id IN (SELECT id FROM import_job WHERE file_name LIKE 'survey-test%')");
        ctx.execute("DELETE FROM import_job WHERE file_name LIKE 'survey-test%'");
        ctx.execute("DELETE FROM surveyed_angler WHERE code = 'P1'");
        ctx.execute("DELETE FROM survey_session WHERE code LIKE 'SURVEY-TEST%'");
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
    }

    /** Un classeur avec une session, une sortie, une capture individuelle valide, sans session souvenir. */
    private byte[] workbook(String sessionCode, String sortieCode, String tailleNombre, String tailleMin,
                            String tailleMax, List<String[]> souvenirRows) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeSheet(wb, SurveySchema.SHEET_SESSION, SurveySchema.HEADER_SESSION, List.<String[]>of(
                    new String[] {sessionCode, waterEntityName, "01/07/2026", "", ""}));
            writeSheet(wb, SurveySchema.SHEET_SORTIE, SurveySchema.HEADER_SORTIE, List.<String[]>of(
                    new String[] {sessionCode, sortieCode, "09:00", "08:00", "11:00"}));
            writeSheet(wb, SurveySchema.SHEET_CAPTURE, SurveySchema.HEADER_CAPTURE, List.<String[]>of(
                    new String[] {sortieCode, "P1", "74", "Aucune", "bord statique", "Pêche au coup", "1", "",
                            "non", "Perche", tailleNombre.equals("1") ? "25" : "", tailleNombre, "oui",
                            tailleMin, tailleMax}));
            if (souvenirRows != null && !souvenirRows.isEmpty()) {
                writeSheet(wb, SurveySchema.SHEET_SOUVENIR, SurveySchema.HEADER_SOUVENIR, souvenirRows);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] simpleWorkbook(String sessionCode, String sortieCode) {
        return workbook(sessionCode, sortieCode, "1", "", "", null);
    }

    private void writeSheet(XSSFWorkbook wb, String name, List<String> header, List<String[]> rows) {
        Sheet sheet = wb.createSheet(name);
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < header.size(); c++) {
            headerRow.createCell(c).setCellValue(header.get(c));
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            String[] values = rows.get(r);
            for (int c = 0; c < values.length; c++) {
                row.createCell(c).setCellValue(values[c]);
            }
        }
    }

    private int countJobs(String fileName) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchOne("SELECT count(*) FROM import_job WHERE file_name = ?", fileName).get(0, Integer.class);
    }

    @Test
    void importCreatesSessionAnglerAndTrip() {
        String file = "survey-test-ok.xlsx";
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(simpleWorkbook("SURVEY-TEST-S1", "SURVEY-TEST-SO1"))
                .when().post(URI + "?filename=" + file + "&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE"))
                .body("inserted", equalTo(1))
                .body("rejected", equalTo(0))
                .body("duplicate", equalTo(false));

        Assertions.assertEquals(1, countJobs(file), "le job d'import doit être persisté");
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        int sessions = ctx.fetchOne("SELECT count(*) FROM survey_session WHERE code = 'SURVEY-TEST-S1'").get(0, Integer.class);
        Assertions.assertEquals(1, sessions, "la session d'enquête doit être persistée");
        int anglerLinkedToTrip = ctx.fetchOne("SELECT count(*) FROM trip t JOIN surveyed_angler sa ON sa.id = t.surveyed_angler_id "
                + "WHERE t.external_ref = 'SURVEY-TEST-SO1/P1' AND sa.code = 'P1'").get(0, Integer.class);
        Assertions.assertEquals(1, anglerLinkedToTrip, "le pêcheur enquêté (code P1) doit être persisté et rattaché à la sortie");
        int trips = ctx.fetchOne("SELECT count(*) FROM trip WHERE external_ref = 'SURVEY-TEST-SO1/P1'").get(0, Integer.class);
        Assertions.assertEquals(1, trips, "la sortie de l'enquêté doit être persistée");
    }

    @Test
    void reimportIsDetectedAsDuplicate() {
        String file = "survey-test-dup.xlsx";
        byte[] bytes = simpleWorkbook("SURVEY-TEST-DUP", "SURVEY-TEST-SODUP");

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream").body(bytes)
                .when().post(URI + "?filename=" + file + "&mode=partial")
                .then().statusCode(200);

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream").body(bytes)
                .when().post(URI + "?filename=" + file + "&mode=partial")
                .then().statusCode(409)
                .body("duplicate", equalTo(true));

        Assertions.assertEquals(1, countJobs(file), "un seul job doit exister après le doublon");
    }

    @Test
    void sortieWithUnknownSessionIsRejected() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeSheet(wb, SurveySchema.SHEET_SESSION, SurveySchema.HEADER_SESSION, List.of());
            writeSheet(wb, SurveySchema.SHEET_SORTIE, SurveySchema.HEADER_SORTIE, List.<String[]>of(
                    new String[] {"SURVEY-TEST-GHOST", "SURVEY-TEST-SOGHOST", "09:00", "08:00", "11:00"}));
            writeSheet(wb, SurveySchema.SHEET_CAPTURE, SurveySchema.HEADER_CAPTURE, List.of());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                    .contentType("application/octet-stream").body(out.toByteArray())
                    .when().post(URI + "?filename=survey-test-orphan.xlsx&mode=partial")
                    .then().statusCode(200)
                    .body("status", equalTo("DONE_WITH_ERRORS"))
                    .body("errors.code", hasItem("REF_SESSION"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void lotWithoutBoundsIsRejected() {
        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(workbook("SURVEY-TEST-LOT", "SURVEY-TEST-SOLOT", "2", "", "", null))
                .when().post(URI + "?filename=survey-test-lot.xlsx&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE_WITH_ERRORS"))
                .body("inserted", equalTo(0))
                .body("rejected", equalTo(1))
                .body("errors.code", hasItem("METIER_LOT_BOUNDS"));
    }

    @Test
    void souvenirForKnownAnglerCreatesItsOwnTrip() {
        String file = "survey-test-souvenir.xlsx";
        List<String[]> souvenir = List.<String[]>of(new String[] {
                "P1", waterEntityName, "15/06/2026", "matin", "Aucune", "bord statique", "Pêche au coup", "1",
                "", "non", "Perche", "22", "1", "oui", "", ""});

        given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(workbook("SURVEY-TEST-SOUV", "SURVEY-TEST-SOSOUV", "1", "", "", souvenir))
                .when().post(URI + "?filename=" + file + "&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE"))
                .body("inserted", equalTo(2))
                .body("rejected", equalTo(0));

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        int souvenirTrips = ctx.fetchOne(
                "SELECT count(*) FROM trip WHERE collection_method = 'enquete_souvenir' AND external_ref = 'P1'")
                .get(0, Integer.class);
        Assertions.assertEquals(1, souvenirTrips, "la session souvenir doit créer sa propre sortie");
    }
}
