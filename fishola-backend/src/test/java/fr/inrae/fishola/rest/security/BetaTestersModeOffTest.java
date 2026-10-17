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
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Hors mode bêta-testeurs (recette, #192) : l'inscription reste ouverte quel que
 * soit le réglage stocké, et ni le réglage ni l'import de comptes ne sont accessibles.
 */
@QuarkusTest
class BetaTestersModeOffTest extends AbstractCarnetTest {

    @Test
    void registrationStaysOpen() {
        given().when().get("/api/v1/security/registration")
                .then().statusCode(200).body("enabled", equalTo(true));
    }

    @Test
    void adminProfileReportsInactiveMode() {
        given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, loginAsAdmin())
                .when().get("/api/v1/admin/check")
                .then().statusCode(200).body("betaTestersMode", equalTo(false));
    }

    @Test
    void nationalAdminCannotCloseRegistration() {
        given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, loginAsAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegistrationStatus(false))
                .when().put("/api/v1/admin/registration")
                .then().statusCode(403);
    }

    @Test
    void nationalAdminCannotImportAccounts() {
        String csv = "email;prenom;nom;profil;perimetre;mot_de_passe\nhors-beta@carnet-peche.test;Eve;Lyn;pecheur;;password1\n";
        given()
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, loginAsAdmin())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv.getBytes(StandardCharsets.UTF_8))
                .when().post("/api/v1/admin/accounts/import")
                .then().statusCode(403);
    }
}
