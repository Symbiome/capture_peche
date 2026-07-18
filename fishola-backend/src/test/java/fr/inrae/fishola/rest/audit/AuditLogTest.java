package fr.inrae.fishola.rest.audit;

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
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Tests d'intégration du journal d'audit (#11 §4.4). Auto-suffisant : provisionne
 * son propre admin national et son token (pas de dépendance à un seed externe).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditLogTest {

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID adminId = UUID.randomUUID();
    private String adminToken;

    @BeforeAll
    @Transactional
    void seedAdmin() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin) "
                + "VALUES (?, ?, ?, now(), false, true)", adminId, "audit-test@fishola.test", "x");
        // JwtHelper est @RequestScoped : on active un contexte de requête le temps
        // de forger le token (hors flux HTTP réel dans @BeforeAll).
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            adminToken = jwtHelper.createAdminToken(adminId);
        } finally {
            requestContext.deactivate();
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM fishola_admin_water_entities WHERE fishola_admin_id = ?", adminId);
        ctx.execute("DELETE FROM audit_log WHERE actor_id = ?", adminId);
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", adminId);
    }

    private int auditCount(String action) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchOne("SELECT count(*) FROM audit_log WHERE action = ? AND actor_id = ?", action, adminId)
                .get(0, Integer.class);
    }

    @Test
    void mutationSuccessWritesAuditRow() {
        // #11 §4.4 cas 1 : un PUT /admin/{id} réussi journalise une ligne
        // admin.update, actor='admin' (appelant), entity = admin cible.
        Map<String, Object> bean = Map.of(
                "email", "audit-test@fishola.test",
                "canCreateAdmin", false,
                "waterEntityIds", new HashSet<>());

        int before = auditCount("admin.update");
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .body(bean)
                .when().put("/api/v1/admin/" + adminId)
                .then().statusCode(204);

        int after = auditCount("admin.update");
        Assertions.assertEquals(before + 1, after, "une ligne d'audit attendue après admin.update");

        // La ligne porte bien l'acteur, le type et l'entité visée.
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var rec = ctx.fetchOne("SELECT actor_type, entity_type, entity_id FROM audit_log "
                + "WHERE action = ? AND actor_id = ? ORDER BY at DESC LIMIT 1", "admin.update", adminId);
        Assertions.assertEquals("admin", rec.get("actor_type", String.class));
        Assertions.assertEquals("fishola_admin", rec.get("entity_type", String.class));
        Assertions.assertEquals(adminId, rec.get("entity_id", UUID.class));
    }

    @Test
    void consultationEndpointReturnsRows() {
        // Génère au moins une ligne, puis la consulte via l'endpoint admin.
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .body(Map.of("email", "audit-test@fishola.test", "canCreateAdmin", false, "waterEntityIds", new HashSet<>()))
                .when().put("/api/v1/admin/" + adminId)
                .then().statusCode(204);

        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .queryParam("action", "admin.update")
                .when().get("/api/v1/admin/audit-log")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].action", equalTo("admin.update"))
                .body("[0].actorType", equalTo("admin"));
    }

    @Test
    void consultationRequiresAdmin() {
        // Sans cookie admin : accès refusé (401).
        given()
                .when().get("/api/v1/admin/audit-log")
                .then().statusCode(401);
    }
}
