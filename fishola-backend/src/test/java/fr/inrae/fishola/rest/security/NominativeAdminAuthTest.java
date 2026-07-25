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

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Non-régression sécurité (#55) : l'authentification admin est <b>nominative uniquement</b>.
 *
 * <p>Le repli « mot de passe national partagé » (ancienne propriété {@code fishola.admin-password},
 * lue par {@code AdminDao.verifySharedNationalPassword}) a été retiré. Il ne doit plus authentifier
 * personne : seul le hash bcrypt propre au compte fait foi, pour tous les rôles — condition de
 * l'imputabilité nominative exigée par la note Q8 (archivage nominatif) et le journal d'audit (#11).
 *
 * <p>Auto-suffisant (gabarit {@code OperatorAccessTest}) : provisionne son propre admin national
 * avec un hash bcrypt propre, sans dépendre du seed de recette.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NominativeAdminAuthTest {

    private static final String EMAIL = "nominative-auth-test@fishola.test";
    private static final String NOMINATIVE_PASSWORD = "N0minative-r3cette!";
    /** Anciennes valeurs du secret partagé (application.properties %dev / %test) : ne doivent plus authentifier. */
    private static final String[] FORMER_SHARED_SECRETS = {"whatever", "azerty"};

    @Inject
    AgroalDataSource dataSource;

    private final UUID nationalAdminId = UUID.randomUUID();

    @BeforeAll
    @Transactional
    void seedNationalAdmin() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        String hash = BCrypt.withDefaults().hashToString(10, NOMINATIVE_PASSWORD.toCharArray());
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, ?, now(), true, true, false)", nationalAdminId, EMAIL, hash);
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", nationalAdminId);
    }

    /** Le mot de passe nominatif (bcrypt du compte) authentifie bien (204). */
    @Test
    void nominativePasswordAuthenticates() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginBean(EMAIL, NOMINATIVE_PASSWORD))
                .when().post("/api/v1/admin/login")
                .then().statusCode(204);
    }

    /** L'ancien secret partagé ne doit plus authentifier un admin national (401). */
    @Test
    void formerSharedSecretIsRejected() {
        for (String shared : FORMER_SHARED_SECRETS) {
            given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginBean(EMAIL, shared))
                    .when().post("/api/v1/admin/login")
                    .then().statusCode(401);
        }
    }
}
