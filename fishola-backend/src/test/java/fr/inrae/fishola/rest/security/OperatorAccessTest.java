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
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * EXEMPLE DE RÉFÉRENCE — cloisonnement du rôle opérateur (#69, correctif « default-deny »).
 *
 * <p>Verrouille la règle de sécurité clé : un opérateur authentifié ({@code is_operator = true})
 * est cantonné à la saisie/import et ne doit atteindre AUCUN endpoint d'administration. La garde
 * {@link AbstractFisholaResource#checkIsAdmin()} rejette les opérateurs (403) ; seuls les endpoints
 * explicitement ouverts au staff via {@link AbstractFisholaResource#checkIsStaff()} leur restent
 * accessibles.
 *
 * <p><b>Auto-suffisant</b> (patron repris de {@code AuditLogTest}) : le test provisionne ses propres
 * comptes en base et forge leurs jetons — il ne dépend donc PAS du seed de recette ({@code amorel},
 * cf. #67), et tourne au vert sur base propre. C'est le gabarit à suivre pour les nouveaux tests des
 * endpoints staff (import CSV #71, saisie manuelle #72, changement de mot de passe #55).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperatorAccessTest {

    /** Un endpoint d'administration quelconque, gardé par {@code checkIsAdmin()}. */
    private static final String ADMIN_ONLY_ENDPOINT = "/api/v1/admin/";
    /** Un endpoint ouvert à tout le staff, gardé par {@code checkIsStaff()}. */
    private static final String STAFF_ENDPOINT = "/api/v1/admin/check";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID nationalAdminId = UUID.randomUUID();
    private final UUID operatorId = UUID.randomUUID();
    private String nationalAdminToken;
    private String operatorToken;

    @BeforeAll
    @Transactional
    void seedAccounts() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        // Admin national : témoin « autorisé » sur l'endpoint d'administration.
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalAdminId, "operator-test-admin@fishola.test", "x");
        // Opérateur : is_operator = true, sans droit d'administration ni périmètre national.
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, false, true)", operatorId, "operator-test-op@fishola.test", "x");

        // JwtHelper est @RequestScoped : on active un contexte de requête le temps de forger
        // les jetons (hors flux HTTP réel dans @BeforeAll). Idem AuditLogTest.
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalAdminToken = jwtHelper.createAdminToken(nationalAdminId);
            operatorToken = jwtHelper.createAdminToken(operatorId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM fishola_admin_water_entities WHERE fishola_admin_id IN (?, ?)", nationalAdminId, operatorId);
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?)", nationalAdminId, operatorId);
    }

    @Test
    void operatorIsForbiddenOnAdminEndpoint() {
        // #69 : cœur du correctif — l'opérateur est rejeté (403) par checkIsAdmin().
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(ADMIN_ONLY_ENDPOINT)
                .then().statusCode(403);
    }

    @Test
    void nationalAdminIsAllowedOnAdminEndpoint() {
        // Témoin : le même endpoint répond bien 200 pour un administrateur — le 403 ci-dessus
        // tient au rôle, pas à un endpoint cassé.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalAdminToken)
                .when().get(ADMIN_ONLY_ENDPOINT)
                .then().statusCode(200);
    }

    @Test
    void operatorIsAllowedOnStaffEndpoint() {
        // L'opérateur garde l'accès aux endpoints ouverts au staff (checkIsStaff) : /admin/check
        // renvoie son profil, avec isOperator = true.
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .when().get(STAFF_ENDPOINT)
                .then().statusCode(200)
                .body("isOperator", equalTo(true));
    }

    @Test
    void anonymousIsUnauthorizedOnAdminEndpoint() {
        // Sans cookie : 401 (non authentifié) — à distinguer du 403 (authentifié mais non autorisé).
        given()
                .when().get(ADMIN_ONLY_ENDPOINT)
                .then().statusCode(401);
    }
}
