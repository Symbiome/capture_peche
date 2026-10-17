package fr.inrae.fishola.rest.imports.survey;

/*-
 * #%L
 * Carnet de pêche :: Backend
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

import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Import « enquête terrain » au format CSV plat de bout en bout (#242) : une ligne par
 * capture, hiérarchie session → sortie → pêcheur → captures reconstruite en regroupant sur
 * les codes, valeurs répétées divergentes signalées ligne par ligne, mêmes règles et même
 * persistance que le classeur XLSX ({@link SurveyImportXlsxTest}).
 *
 * <p>Auto-suffisant : provisionne son opérateur (périmètre d'Annecy) et nettoie les
 * sessions, sorties et pêcheurs de codes {@code FLAT-TEST*} / {@code FLAT-P*}.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SurveyImportFlatCsvTest {

    private static final String URI = "/api/v1/admin/imports/survey";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID operatorId = UUID.randomUUID();
    private String operatorToken;
    private String annecy;

    @BeforeAll
    @Transactional
    void seedOperator() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var rec = ctx.fetchOne("SELECT name, department FROM water_entity WHERE name = 'Annecy' LIMIT 1");
        annecy = rec.get("name", String.class);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, "
                + "is_operator) VALUES (?, ?, ?, now(), false, false, true)", operatorId, "flat-test-op@carnet-peche.test", "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                operatorId, rec.get("department", String.class));

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
        String trips = "SELECT id FROM trip WHERE session_code LIKE 'FLAT-TEST%'";
        ctx.execute("DELETE FROM catch WHERE trip_id IN (" + trips + ")");
        ctx.execute("DELETE FROM trip WHERE session_code LIKE 'FLAT-TEST%'");
        ctx.execute("DELETE FROM import_row_error WHERE import_id IN (SELECT id FROM import_job WHERE file_name LIKE 'flat-test%')");
        ctx.execute("DELETE FROM import_job WHERE file_name LIKE 'flat-test%'");
        ctx.execute("DELETE FROM surveyed_angler WHERE code LIKE 'FLAT-P%'");
        ctx.execute("DELETE FROM survey_session WHERE code LIKE 'FLAT-TEST%'");
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", operatorId);
    }

    // --- Construction des lignes -------------------------------------------------

    /** Ligne « en cours » valide : capture d'une perche de 25 cm, conservée. */
    private Map<String, String> enCours(String session, String sortie, String angler, String... overrides) {
        Map<String, String> row = sessionColumns(session);
        row.putAll(Map.of("type_sortie", "en_cours", "code_sortie", sortie, "heure_controle", "09:00",
                "heure_debut", "07:30", "heure_fin_prevue", "11:00", "code_pecheur", angler,
                "departement_origine", "74", "espece_recherchee", "Perche"));
        row.putAll(tripAndCatchColumns());
        return withOverrides(row, overrides);
    }

    /** Ligne « souvenir » valide du pêcheur : perche de 22 cm le 15/06 au matin. */
    private Map<String, String> souvenir(String session, String angler, String... overrides) {
        Map<String, String> row = sessionColumns(session);
        row.putAll(Map.of("type_sortie", "souvenir", "code_pecheur", angler, "departement_origine", "74",
                "site_peche", annecy, "date_souvenir", "15/06/2026", "periode_journee", "matin"));
        row.putAll(tripAndCatchColumns());
        row.put("capture_taille", "22");
        return withOverrides(row, overrides);
    }

    private Map<String, String> sessionColumns(String session) {
        Map<String, String> row = new LinkedHashMap<>();
        SurveySchema.FLAT_HEADER.forEach(column -> row.put(column, ""));
        row.putAll(Map.of("code_session", session, "secteur", annecy, "date_session", "01/07/2026",
                "heure_debut_session", "08:00", "heure_fin_session", "12:00", "nom_operateur", "J. Dupont",
                "nb_bord_non_enquetes", "2", "nb_bateau_non_enquetes", "0"));
        return row;
    }

    private static Map<String, String> tripAndCatchColumns() {
        return Map.of("mode_peche", "bord statique", "technique", "Pêche au coup", "nombre_lignes", "1",
                "bredouille", "non", "capture_espece", "Perche", "capture_taille", "25", "capture_nombre", "1",
                "capture_conservee", "oui");
    }

    private static Map<String, String> withOverrides(Map<String, String> row, String... overrides) {
        for (int i = 0; i < overrides.length; i += 2) {
            Assertions.assertTrue(row.containsKey(overrides[i]), overrides[i]);
            row.put(overrides[i], overrides[i + 1]);
        }
        return row;
    }

    @SafeVarargs
    private static byte[] csv(Map<String, String>... rows) {
        StringBuilder text = new StringBuilder(String.join(";", SurveySchema.FLAT_HEADER)).append('\n');
        for (Map<String, String> row : rows) {
            text.append(SurveySchema.FLAT_HEADER.stream().map(row::get).collect(Collectors.joining(";"))).append('\n');
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ValidatableResponse post(byte[] body, String fileName, String mode) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(body)
                .when().post(URI + "?filename=" + fileName + "&mode=" + mode)
                .then();
    }

    private ValidatableResponse post(byte[] body, String fileName) {
        return post(body, fileName, "partial").statusCode(200);
    }

    private int count(String sql, Object... binds) {
        return DSL.using(dataSource, SQLDialect.POSTGRES).fetchOne(sql, binds).get(0, Integer.class);
    }

    // --- Fichier valide ----------------------------------------------------------

    /** Exemple de l'issue : capture, lot, carpiste sur 3 jours bredouille, souvenir à deux captures. */
    @Test
    void validFileRebuildsSessionSortiesAnglersAndSouvenir() {
        String s = "FLAT-TEST-OK";
        byte[] body = csv(
                enCours(s, "FLAT-TEST-OK-01", "FLAT-P001"),
                enCours(s, "FLAT-TEST-OK-01", "FLAT-P001", "capture_espece", "Gardon", "capture_taille", "",
                        "capture_nombre", "6", "capture_conservee", "non", "capture_taille_min", "10",
                        "capture_taille_max", "15"),
                enCours(s, "FLAT-TEST-OK-02", "FLAT-P002", "heure_controle", "10:30", "heure_debut", "06:00",
                        "heure_fin_prevue", "10:00", "date_fin_prevue", "03/07/2026", "departement_origine", "73",
                        "espece_recherchee", "Carpe commune", "technique", "Pêche au feeder", "nombre_lignes", "2",
                        "appat_leurre", "bouillette", "bredouille", "oui", "capture_espece", "", "capture_taille", "",
                        "capture_nombre", "", "capture_conservee", ""),
                souvenir(s, "FLAT-P001"),
                souvenir(s, "FLAT-P001", "capture_espece", "Brochet", "capture_taille", "60",
                        "capture_conservee", "non"));

        post(body, "flat-test-ok.csv")
                .body("status", equalTo("DONE"))
                .body("total", equalTo(5))
                .body("inserted", equalTo(3))
                .body("rejected", equalTo(0));

        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var session = ctx.fetchOne("SELECT start_time, end_time, operator_name, unsurveyed_shore_anglers "
                + "FROM survey_session WHERE code = ?", s);
        Assertions.assertEquals(LocalTime.of(8, 0), session.get("start_time", LocalTime.class));
        Assertions.assertEquals(LocalTime.of(12, 0), session.get("end_time", LocalTime.class));
        Assertions.assertEquals("J. Dupont", session.get("operator_name", String.class));
        Assertions.assertEquals(2, session.get("unsurveyed_shore_anglers", Integer.class));

        Assertions.assertEquals(7, count("SELECT coalesce(sum(c.quantity), 0) FROM catch c JOIN trip t ON t.id = c.trip_id "
                + "WHERE t.external_ref = 'FLAT-TEST-OK-01/FLAT-P001'"), "perche + lot de 6 gardons");
        var carp = ctx.fetchOne("SELECT id, end_timestamp FROM trip WHERE external_ref = 'FLAT-TEST-OK-02/FLAT-P002'");
        Assertions.assertEquals(LocalDateTime.of(2026, 7, 3, 10, 0), carp.get("end_timestamp", LocalDateTime.class));
        Assertions.assertEquals(0, count("SELECT count(*) FROM catch WHERE trip_id = ?", carp.get("id", UUID.class)));
        Assertions.assertEquals(2, count("SELECT count(*) FROM catch c JOIN trip t ON t.id = c.trip_id "
                + "WHERE t.collection_method = 'enquete_souvenir' AND t.session_code = ?", s), "souvenir à deux captures");
    }

    @Test
    void emptyExpectedSpeciesMeansAllSpecies() {
        String s = "FLAT-TEST-ALL";
        post(csv(enCours(s, "FLAT-TEST-ALL-01", "FLAT-P-ALL", "espece_recherchee", ""),
                enCours(s, "FLAT-TEST-ALL-01", "FLAT-P-ALL", "espece_recherchee", "Toutes espèces")),
                "flat-test-all-species.csv")
                .body("status", equalTo("DONE"))
                .body("inserted", equalTo(1));

        Assertions.assertEquals(1, count("SELECT count(*) FROM trip WHERE external_ref = 'FLAT-TEST-ALL-01/FLAT-P-ALL' "
                + "AND expected_species_id IS NULL"));
    }

    // --- Cohérence des valeurs répétées ------------------------------------------

    @Test
    void divergentSessionValueIsRejectedOnItsLine() {
        String s = "FLAT-TEST-INCSESS";
        post(csv(enCours(s, "FLAT-TEST-INCSESS-01", "FLAT-P-IS1"),
                enCours(s, "FLAT-TEST-INCSESS-02", "FLAT-P-IS2", "nom_operateur", "M. Martin")),
                "flat-test-inconsistent-session.csv")
                .body("status", equalTo("DONE_WITH_ERRORS"))
                .body("inserted", equalTo(1))
                .body("rejected", equalTo(1))
                .body("errors[0].line", equalTo(3))
                .body("errors[0].column", equalTo("nom_operateur"))
                .body("errors[0].code", equalTo(SurveySchema.STRUCT_INCONSISTENT_SESSION));
    }

    @Test
    void divergentSortieValueOrSortieInTwoSessionsIsRejected() {
        post(csv(enCours("FLAT-TEST-INCSO", "FLAT-TEST-INCSO-01", "FLAT-P-ISO1"),
                enCours("FLAT-TEST-INCSO", "FLAT-TEST-INCSO-01", "FLAT-P-ISO2", "heure_debut", "08:00"),
                enCours("FLAT-TEST-INCSO-B", "FLAT-TEST-INCSO-01", "FLAT-P-ISO3")),
                "flat-test-inconsistent-sortie.csv")
                .body("rejected", equalTo(2))
                .body("errors.code", hasItem(SurveySchema.STRUCT_INCONSISTENT_SORTIE))
                .body("errors.column", hasItem("heure_debut"))
                .body("errors.column", hasItem("code_session"));
    }

    @Test
    void divergentTripValueIsRejected() {
        String s = "FLAT-TEST-INCTRIP";
        post(csv(enCours(s, "FLAT-TEST-INCTRIP-01", "FLAT-P-IT"),
                enCours(s, "FLAT-TEST-INCTRIP-01", "FLAT-P-IT", "mode_peche", "bateau")),
                "flat-test-inconsistent-trip.csv")
                .body("errors.code", hasItem(SurveySchema.STRUCT_INCONSISTENT_TRIP))
                .body("errors.column", hasItem("mode_peche"));
    }

    @Test
    void divergentAnglerOriginIsRejected() {
        String s = "FLAT-TEST-INCANG";
        post(csv(enCours(s, "FLAT-TEST-INCANG-01", "FLAT-P-IA"),
                enCours(s, "FLAT-TEST-INCANG-02", "FLAT-P-IA", "departement_origine", "69")),
                "flat-test-inconsistent-angler.csv")
                .body("errors.code", hasItem(SurveySchema.STRUCT_INCONSISTENT_ANGLER))
                .body("errors.column", hasItem("departement_origine"));
    }

    @Test
    void bredouilleOnSeveralLinesOrWithCaptureIsRejected() {
        String s = "FLAT-TEST-BRED";
        post(csv(enCours(s, "FLAT-TEST-BRED-01", "FLAT-P-B1", "bredouille", "oui", "capture_espece", ""),
                enCours(s, "FLAT-TEST-BRED-01", "FLAT-P-B1", "bredouille", "oui", "capture_espece", ""),
                enCours(s, "FLAT-TEST-BRED-02", "FLAT-P-B2", "bredouille", "oui")),
                "flat-test-bredouille.csv")
                .body("rejected", equalTo(2))
                .body("errors.code", hasItem(SurveySchema.METIER_BREDOUILLE))
                .body("errors.line", hasItem(3))
                .body("errors.line", hasItem(4));
    }

    // --- Règles de référentiel et métier -----------------------------------------

    @Test
    void souvenirWithoutEnCoursLineInItsSessionIsRejected() {
        post(csv(enCours("FLAT-TEST-SOUVA", "FLAT-TEST-SOUVA-01", "FLAT-P-SV"),
                souvenir("FLAT-TEST-SOUVB", "FLAT-P-SV")),
                "flat-test-orphan-souvenir.csv")
                .body("inserted", equalTo(1))
                .body("errors[0].line", equalTo(3))
                .body("errors[0].code", equalTo(SurveySchema.REF_SESSION));
    }

    @Test
    void controlTimeOutsideSortieIsRejected() {
        String s = "FLAT-TEST-CTRL";
        post(csv(enCours(s, "FLAT-TEST-CTRL-01", "FLAT-P-C1", "heure_controle", "11:30"),
                enCours(s, "FLAT-TEST-CTRL-02", "FLAT-P-C2", "heure_controle", "07:00")),
                "flat-test-control-time.csv")
                .body("rejected", equalTo(2))
                .body("errors.code", hasItem(SurveySchema.STRUCT_TIME_ORDER))
                .body("errors.column", hasItem("heure_controle"));
    }

    @Test
    void unknownSortieTypeIsRejected() {
        post(csv(enCours("FLAT-TEST-TYPE", "FLAT-TEST-TYPE-01", "FLAT-P-T", "type_sortie", "passee")),
                "flat-test-type.csv")
                .body("errors[0].code", equalTo(SurveySchema.STRUCT_TYPE_SORTIE));
    }

    // --- Modes, doublons, en-tête et gabarit -------------------------------------

    @Test
    void allOrNothingInsertsNothingWhenOneLineFails() {
        String s = "FLAT-TEST-AON";
        post(csv(enCours(s, "FLAT-TEST-AON-01", "FLAT-P-AON1"),
                enCours(s, "FLAT-TEST-AON-02", "FLAT-P-AON2", "capture_espece", "Espèce inconnue")),
                "flat-test-all-or-nothing.csv", SurveyImportService.MODE_ALL_OR_NOTHING).statusCode(200)
                .body("status", equalTo("DONE_WITH_ERRORS"))
                .body("inserted", equalTo(0))
                .body("errors.column", hasItem("capture_espece"));
        Assertions.assertEquals(0, count("SELECT count(*) FROM survey_session WHERE code = ?", s));
    }

    @Test
    void reimportIsDetectedAsDuplicate() {
        byte[] body = csv(enCours("FLAT-TEST-DUP", "FLAT-TEST-DUP-01", "FLAT-P-DUP"));
        post(body, "flat-test-dup.csv").body("duplicate", equalTo(false));
        post(body, "flat-test-dup.csv", "partial").statusCode(409).body("duplicate", equalTo(true));
    }

    @Test
    void wrongHeaderFailsTheWholeFile() {
        post("type_sortie;code_session\nen_cours;FLAT-TEST-HDR\n".getBytes(StandardCharsets.UTF_8),
                "flat-test-header.csv")
                .body("status", equalTo("FAILED"))
                .body("errors[0].code", equalTo(SurveySchema.STRUCT_HEADER));
    }

    @Test
    void csvTemplateFilledWithItsExampleRowsImportsWithoutError() {
        String template = given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(URI + "/template?format=csv")
                .then().statusCode(200)
                .contentType("text/csv")
                .extract().asString();
        Assertions.assertTrue(template.contains(String.join(";", SurveySchema.FLAT_HEADER)));

        // Seules les valeurs propres à la base de test changent : secteur, technique, codes.
        String filled = template.replace("ENQ-001", "FLAT-TEST-TPL").replace("P001", "FLAT-P-TPL1")
                .replace("P002", "FLAT-P-TPL2").replace("Lac d'Annecy", annecy)
                .replace("Pêche de la carpe de nuit", "Pêche au feeder");
        post(filled.getBytes(StandardCharsets.UTF_8), "flat-test-template.csv")
                .body("status", equalTo("DONE"))
                .body("inserted", equalTo(3))
                .body("rejected", equalTo(0));
    }

    // --- Archive des gabarits ----------------------------------------------------

    private Map<String, byte[]> downloadTemplateBundle() throws IOException {
        byte[] zip = given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(URI + "/template?format=zip")
                .then().statusCode(200)
                .contentType("application/zip")
                .extract().asByteArray();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                entries.put(entry.getName(), in.readAllBytes());
            }
        }
        return entries;
    }

    @Test
    void templateBundleHoldsBothFormatsTheExampleAndAReadmePresentingThem() throws IOException {
        Map<String, byte[]> entries = downloadTemplateBundle();

        Assertions.assertEquals(Set.of(SurveyTemplateBundle.README_NAME, SurveyTemplate.FILE_NAME,
                SurveyFlatTemplate.FILE_NAME, SurveyTemplateBundle.EXAMPLE_NAME), entries.keySet());
        Assertions.assertArrayEquals(SurveyFlatTemplate.build(), entries.get(SurveyFlatTemplate.FILE_NAME));
        String readme = new String(entries.get(SurveyTemplateBundle.README_NAME), StandardCharsets.UTF_8);
        Assertions.assertTrue(readme.contains(SurveyTemplate.FILE_NAME), readme);
        Assertions.assertTrue(readme.contains(SurveyFlatTemplate.FILE_NAME), readme);
        Assertions.assertTrue(readme.contains(SurveyTemplateBundle.EXAMPLE_NAME), readme);
    }

    /** Exemple du ZIP : 2 sessions, 18 lignes, 10 sorties (dont 3 souvenirs, 2 bredouilles, 3 lots). */
    @Test
    void exampleOfTheTemplateBundleImportsWithoutError() throws IOException {
        String example = new String(downloadTemplateBundle().get(SurveyTemplateBundle.EXAMPLE_NAME),
                StandardCharsets.UTF_8);
        Assertions.assertTrue(example.contains(String.join(";", SurveySchema.FLAT_HEADER)));

        // Seules les valeurs propres à la base de test changent : secteurs et codes.
        String filled = example.replace("ENQ-", "FLAT-TEST-EX-").replace("P-ANN-", "FLAT-P-EX-ANN-")
                .replace("P-LEM-", "FLAT-P-EX-LEM-").replace("Lac d'Annecy", annecy)
                .replace("Lac Léman", "Léman");
        post(filled.getBytes(StandardCharsets.UTF_8), "flat-test-example.csv")
                .body("status", equalTo("DONE"))
                .body("total", equalTo(18))
                .body("inserted", equalTo(10))
                .body("rejected", equalTo(0));
    }
}
