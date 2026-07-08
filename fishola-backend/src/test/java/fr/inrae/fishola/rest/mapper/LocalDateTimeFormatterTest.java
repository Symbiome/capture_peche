package fr.inrae.fishola.rest.mapper;

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

import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Ce test permet de valider qu'il n'y a pas de régression sur le format des dates envoyées au front
 * Le format attendu pour la sérialisation des LocalDateTime est un tableau tel que : [année,mois,jour,heure,minute,seconde,...]
 * Ce test doit rester tel quel à moins de changer le code du front !
 */
@QuarkusTest
class LocalDateTimeFormatterTest extends AbstractFisholaTest {

    @Inject
    ReferentialDao referentialDao;

    @Test
    @Transactional
    void testOnGlobalDashboard() {
        String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy,M,d,H,m"));
        UUID waterEntityId = this.referentialDao.listWaterEntities().iterator().next().getId();
        Integer year = 2024;
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/global-dashboard?year="+year + "&waterEntity=" + waterEntityId)
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"computedOn\":[" + formatted));
    }

    @Test
    void testOnKeyFigures() {
        String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy,M,d,H,m"));
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/about/key-figures")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("\"computedOn\":[" + formatted));
    }

}
