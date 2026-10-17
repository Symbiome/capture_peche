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

import fr.inrae.fishola.rest.security.admin.DefaultNationalAdmins;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Administrateurs nationaux par défaut (#192) : créés s'ils manquent, jamais
 * modifiés ensuite, les entrées invalides étant ignorées.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultNationalAdminsTest {

    private static final String DOMAIN = "@admins-defaut.carnet-peche.test";
    private static final String FIRST = "premier" + DOMAIN;
    private static final String SECOND = "second" + DOMAIN;

    @Inject
    DefaultNationalAdmins defaultNationalAdmins;

    @Inject
    AgroalDataSource dataSource;

    @AfterAll
    @Transactional
    void deleteDefaultAdmins() {
        DSL.using(dataSource, SQLDialect.POSTGRES).execute("DELETE FROM fishola_admin WHERE email LIKE ?", "%" + DOMAIN);
    }

    @Test
    void missingAdminsAreCreatedOnceAndNeverModified() {
        List<String> created = defaultNationalAdmins.createMissing(List.of(
                FIRST + ":Premier-mdp-1", SECOND.toUpperCase() + ":Second-mdp-2",
                "pas-un-email:Invalide-1", "court" + DOMAIN + ":123"));
        Assertions.assertEquals(List.of(FIRST, SECOND), created);
        Assertions.assertEquals(Boolean.TRUE, DSL.using(dataSource, SQLDialect.POSTGRES).fetchValue(
                "SELECT bool_and(is_national_admin AND can_create_admin AND NOT is_operator) FROM fishola_admin WHERE email LIKE ?",
                "%" + DOMAIN));
        assertAdminLogin(FIRST, "Premier-mdp-1", 204);

        Assertions.assertEquals(List.of(), defaultNationalAdmins.createMissing(List.of(FIRST + ":Autre-mdp-3")));
        assertAdminLogin(FIRST, "Premier-mdp-1", 204);
        assertAdminLogin(FIRST, "Autre-mdp-3", 401);
    }

    private static void assertAdminLogin(String email, String password, int expectedStatus) {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean(email, password))
                .when().post("/api/v1/admin/login")
                .then().statusCode(expectedStatus);
    }
}
