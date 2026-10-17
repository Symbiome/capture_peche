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

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Inscription libre ouverte ou fermée par l'administrateur national, en mode
 * bêta-testeurs (#192) : fermée par défaut, l'API refuse alors l'inscription
 * et la validation d'un lien reçu avant la fermeture.
 */
@QuarkusTest
@TestProfile(BetaTestersModeProfile.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationSettingTest {

    private static final String PENDING_EMAIL = "lien-en-attente@inscription.carnet-peche.test";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID nationalAdminId = UUID.randomUUID();
    private final UUID regionalAdminId = UUID.randomUUID();
    private String nationalToken;
    private String regionalToken;

    @BeforeAll
    @Transactional
    void seedStaff() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), true, true, false)", nationalAdminId, "national@inscription.carnet-peche.test");
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), true, false, false)", regionalAdminId, "regional@inscription.carnet-peche.test");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", regionalAdminId);
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalAdminId);
            regionalToken = jwtHelper.createAdminToken(regionalAdminId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterEach
    void restoreClosedRegistration() {
        setRegistration(nationalToken, false).then().statusCode(200);
    }

    @AfterAll
    @Transactional
    void deleteValidatedUser() {
        DSL.using(dataSource, SQLDialect.POSTGRES).execute("DELETE FROM fishola_user WHERE email = ?", PENDING_EMAIL);
    }

    @Test
    void registrationIsClosedByDefault() {
        given().when().get("/api/v1/security/registration")
                .then().statusCode(200).body("enabled", equalTo(false));
    }

    @Test
    void nationalAdminOpensRegistration() {
        setRegistration(nationalToken, true).then().statusCode(200).body("enabled", equalTo(true));
        given().when().get("/api/v1/security/registration")
                .then().statusCode(200).body("enabled", equalTo(true));
    }

    @Test
    void closedRegistrationRejectsSignUp() {
        setRegistration(nationalToken, false).then().statusCode(200).body("enabled", equalTo(false));

        given().when().get("/api/v1/security/registration")
                .then().statusCode(200).body("enabled", equalTo(false));
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRegistration())
                .when().put("/api/v1/security/register")
                .then().statusCode(403)
                .body("registration", equalTo(SecurityResource.REGISTRATION_CLOSED_MESSAGE));
    }

    @Test
    void closedRegistrationRejectsPendingValidationLink() {
        String token = jwtHelper.createCustomToken("register", 1, Map.of(
                "email", PENDING_EMAIL,
                "firstName", "Lien",
                "pseudo", "lien-en-attente",
                "receive_mail_notifications", "false",
                "passwordHashed", BCrypt.withDefaults().hashToString(4, "secret1".toCharArray()),
                "postalCode", "74000",
                "birthYear", "1980"));

        setRegistration(nationalToken, false).then().statusCode(200);
        given().queryParam("t", token).when().get("/api/v1/security/verify-app").then().statusCode(400);
        Assertions.assertEquals(0, countUsers(PENDING_EMAIL));

        setRegistration(nationalToken, true).then().statusCode(200);
        given().queryParam("t", token).when().get("/api/v1/security/verify-app").then().statusCode(200);
        Assertions.assertEquals(1, countUsers(PENDING_EMAIL));
    }

    @Test
    void onlyNationalAdminChangesRegistration() {
        setRegistration(regionalToken, true).then().statusCode(403);
        given().when().get("/api/v1/security/registration")
                .then().statusCode(200).body("enabled", equalTo(false));
    }

    private io.restassured.response.Response setRegistration(String adminToken, boolean enabled) {
        return given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegistrationStatus(enabled))
                .when().put("/api/v1/admin/registration");
    }

    private static RegisterBean validRegistration() {
        RegisterBean bean = new RegisterBean();
        bean.firstName = "Inscription";
        bean.pseudo = "inscription-fermee";
        bean.email = "inscription-fermee@inscription.carnet-peche.test";
        bean.password = "secret1";
        bean.postalCode = "74000";
        bean.birthYear = 1980;
        return bean;
    }

    private int countUsers(String email) {
        return DSL.using(dataSource, SQLDialect.POSTGRES).fetchCount(DSL.table("fishola_user"), DSL.field("email").eq(email));
    }
}
