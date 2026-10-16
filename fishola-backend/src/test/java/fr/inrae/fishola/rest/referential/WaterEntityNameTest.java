package fr.inrae.fishola.rest.referential;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
 * Nom d'une seule entité hydrographique (#204), utilisé par la fiche d'une prise
 * de l'admin à la place du référentiel complet des noms.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WaterEntityNameTest {

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID adminId = UUID.randomUUID();
    private String adminToken;
    private UUID annecyId;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), false, true, false)", adminId, "water-entity-name@fishola.test", "x");
        annecyId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Annecy'").get("id", UUID.class);

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
        DSL.using(dataSource, SQLDialect.POSTGRES).execute("DELETE FROM fishola_admin WHERE id = ?", adminId);
    }

    @Test
    void returnsTheNameOfOneWaterEntity() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/referential/waterEntities/names/" + annecyId)
                .then().statusCode(200)
                .body("id", equalTo(annecyId.toString()))
                .body("name", equalTo("Annecy"));
    }

    @Test
    void unknownWaterEntityIsNotFound() {
        given()
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/referential/waterEntities/names/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void requiresStaffAuthentication() {
        given()
                .when().get("/api/v1/referential/waterEntities/names/" + annecyId)
                .then().statusCode(401);
    }
}
