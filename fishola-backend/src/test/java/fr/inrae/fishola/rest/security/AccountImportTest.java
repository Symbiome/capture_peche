package fr.inrae.fishola.rest.security;

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
import fr.inrae.fishola.rest.AbstractCarnetTest;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Import CSV des comptes des bêta-testeurs (#192) : un compte par ligne, un même
 * e-mail pouvant être pêcheur et administrateur, import rejouable sans doublon.
 */
@QuarkusTest
@TestProfile(BetaTestersModeProfile.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountImportTest extends AbstractCarnetTest {

    private static final String DOMAIN = "@import-comptes.carnet-peche.test";
    private static final String ALICE = "alice" + DOMAIN;
    private static final String CSV = String.join("\n",
            "email;Prénom;nom;profil;Périmètre;Mot de passe",
            ALICE + ";Alice;Martin;pecheur;;alice-peche-1",
            ALICE.toUpperCase() + ";Alice;Martin;admin_regional;74,73;alice-admin-1",
            "bruno" + DOMAIN + ";Bruno;Durand;operateur;74;bruno-op-1",
            "chloe" + DOMAIN + ";Chloé;;Admin_National;;chloe-nat-1",
            "denis" + DOMAIN + ";Alice;Martel;pêcheur;;denis-peche-1",
            "profil" + DOMAIN + ";Paul;Pilote;pilote;;password1",
            "sans-perimetre" + DOMAIN + ";Olga;Op;operateur;;password1",
            "dept-inconnu" + DOMAIN + ";Olga;Op;operateur;999;password1",
            "court" + DOMAIN + ";Cléo;Court;pecheur;;123",
            "sans-prenom" + DOMAIN + ";;Anonyme;pecheur;;password1",
            "pas-un-email;Eve;Mail;pecheur;;password1",
            "perimetre-pecheur" + DOMAIN + ";Pia;Peche;pecheur;74;password1") + "\n";
    private static final List<String> EXPECTED_STATUSES = List.of(
            "CREATED", "CREATED", "CREATED", "CREATED", "CREATED",
            "REJECTED", "REJECTED", "REJECTED", "REJECTED", "REJECTED", "REJECTED", "REJECTED");

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID regionalAdminId = UUID.randomUUID();

    @BeforeAll
    @Transactional
    void seedRegionalAdmin() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), true, false, false)", regionalAdminId, "regional-importeur@carnet-peche.test");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", regionalAdminId);
    }

    @AfterAll
    @Transactional
    void deleteImportedAccounts() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM fishola_admin_departments WHERE fishola_admin_id IN "
                + "(SELECT id FROM fishola_admin WHERE email LIKE ?)", "%" + DOMAIN);
        ctx.execute("DELETE FROM fishola_admin WHERE email LIKE ?", "%" + DOMAIN);
        ctx.execute("DELETE FROM fishola_user WHERE email LIKE ?", "%" + DOMAIN);
    }

    @Test
    @Order(1)
    void importCreatesOneAccountPerLine() {
        JsonPath report = importCsv(CSV, loginAsAdmin(), 200);
        Assertions.assertEquals(12, report.getInt("total"));
        Assertions.assertEquals(5, report.getInt("created"));
        Assertions.assertEquals(7, report.getInt("rejected"));
        Assertions.assertEquals(EXPECTED_STATUSES, report.getList("lines.status"));
        Assertions.assertEquals(List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), report.getList("lines.line"));
    }

    @Test
    @Order(2)
    void sameEmailGivesWorkingAnglerAndAdminAccounts() {
        login(ALICE, "alice-peche-1");
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean(ALICE, "alice-admin-1"))
                .when().post("/api/v1/admin/login")
                .then().statusCode(204);
    }

    @Test
    @Order(3)
    void importedAccountsHaveTheirProfileAndHashedPassword() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        Assertions.assertEquals("Alice M. 2", ctx.fetchValue("SELECT pseudo FROM fishola_user WHERE email = ?", "denis" + DOMAIN));
        Assertions.assertTrue(ctx.fetchValue("SELECT password FROM fishola_user WHERE email = ?", ALICE).toString().startsWith("$2"));
        Assertions.assertEquals(Set.of("73", "74"), Set.copyOf(ctx.fetchValues(
                "SELECT d.department_code FROM fishola_admin_departments d JOIN fishola_admin a ON a.id = d.fishola_admin_id "
                        + "WHERE a.email = ?", ALICE)));
        Assertions.assertEquals(Boolean.TRUE, ctx.fetchValue("SELECT is_operator FROM fishola_admin WHERE email = ?", "bruno" + DOMAIN));
        Assertions.assertEquals(Boolean.TRUE, ctx.fetchValue(
                "SELECT is_national_admin AND can_create_admin FROM fishola_admin WHERE email = ?", "chloe" + DOMAIN));
    }

    @Test
    @Order(4)
    void replayingTheImportCreatesNoDuplicate() {
        JsonPath report = importCsv(CSV, loginAsAdmin(), 200);
        Assertions.assertEquals(0, report.getInt("created"));
        Assertions.assertEquals(5, report.getInt("ignored"));
        Assertions.assertEquals(7, report.getInt("rejected"));
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        Assertions.assertEquals(2, ctx.fetchCount(DSL.table("fishola_user"), DSL.field("email").like("%" + DOMAIN)));
        Assertions.assertEquals(3, ctx.fetchCount(DSL.table("fishola_admin"), DSL.field("email").like("%" + DOMAIN)));
    }

    @Test
    @Order(5)
    void missingColumnRejectsTheWholeFile() {
        String withoutPassword = "email;prenom;nom;profil;perimetre\n" + "eve" + DOMAIN + ";Eve;Lyn;pecheur;\n";
        JsonPath report = importCsv(withoutPassword, loginAsAdmin(), 400);
        Assertions.assertTrue(report.getString("fileError").contains("mot_de_passe"));
        Assertions.assertEquals(0, report.getInt("total"));
    }

    @Test
    @Order(6)
    void onlyNationalAdminImportsAccounts() {
        given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, jwtHelper.createAdminToken(regionalAdminId))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(CSV.getBytes(StandardCharsets.UTF_8))
                .when().post("/api/v1/admin/accounts/import")
                .then().statusCode(403);
    }

    private JsonPath importCsv(String csv, String adminToken, int expectedStatus) {
        return given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv.getBytes(StandardCharsets.UTF_8))
                .when().post("/api/v1/admin/accounts/import")
                .then().statusCode(expectedStatus)
                .extract().jsonPath();
    }
}
