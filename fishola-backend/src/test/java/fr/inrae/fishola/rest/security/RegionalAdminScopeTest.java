package fr.inrae.fishola.rest.security;

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

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Cloisonnement de l'administrateur RÉGIONAL (CdC §3.1.5.1, volet A).
 *
 * <p>Avant ce correctif, la distinction national/régional n'existait que dans le menu du
 * back-office : {@code checkIsAdmin()} ne rejetant que les opérateurs, un régional pouvait
 * atteindre par l'API les portées non cloisonnables (comptes pêcheurs, référentiels
 * nationaux, documentation, sorties, journal d'audit). Ces portées sont désormais gardées
 * par {@code checkIsNationalAdmin()}.
 *
 * <p>Auto-suffisant (patron de {@code OperatorAccessTest}) : provisionne ses comptes et
 * forge ses jetons, sans dépendre d'un seed externe.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionalAdminScopeTest {

    /** Portées réservées au national (non cloisonnables par périmètre). */
    private static final String USERS_ENDPOINT = "/api/v1/security/users";
    private static final String AUDIT_ENDPOINT = "/api/v1/admin/audit-log";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID nationalId = UUID.randomUUID();
    private final UUID regionalId = UUID.randomUUID();
    private String nationalToken;
    private String regionalToken;

    @BeforeAll
    @Transactional
    void seedAccounts() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalId, "scope-test-national@fishola.test", "x");
        // Régional : peut gérer des comptes, mais n'est PAS national.
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, false, false)", regionalId, "scope-test-regional@fishola.test", "x");

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalId);
            regionalToken = jwtHelper.createAdminToken(regionalId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM fishola_admin_water_entities WHERE fishola_admin_id IN (?, ?)", nationalId, regionalId);
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?)", nationalId, regionalId);
    }

    private io.restassured.specification.RequestSpecification as(String token) {
        return given().cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token);
    }

    @Test
    void regionalIsForbiddenOnUsers() {
        // Comptes pêcheurs : écran « Utilisateurs », réservé au national.
        as(regionalToken).when().get(USERS_ENDPOINT).then().statusCode(403);
    }

    @Test
    void regionalIsForbiddenOnAuditLog() {
        as(regionalToken).when().get(AUDIT_ENDPOINT).then().statusCode(403);
    }

    @Test
    void regionalIsForbiddenOnNationalReferential() {
        // Mutation d'un référentiel national (météo) : réservée au national.
        as(regionalToken)
                .contentType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("name", "Ensoleillé (test)", "exportAs", "Ensoleillé (test)"))
                .when().post("/api/v1/referential/weathers")
                .then().statusCode(403);
    }

    @Test
    void nationalIsAllowedOnUsers() {
        // Témoin : le même endpoint répond pour un national — le 403 tient au rôle.
        as(nationalToken).when().get(USERS_ENDPOINT).then().statusCode(200);
    }

    @Test
    void nationalIsAllowedOnAuditLog() {
        as(nationalToken).when().get(AUDIT_ENDPOINT).then().statusCode(200);
    }
}
