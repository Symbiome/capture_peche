package fr.inrae.fishola.rest.editorial;

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

import fr.inrae.fishola.entities.tables.daos.DocumentationDao;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class NewsResourceTest {

    @Inject
    protected DocumentationDao newsDao;

    @Test
    public void testNewsUpdateImpossibleForNonAdmin() {
        UUID newsId = newsDao.findAll().get(0).getId();
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .put("/api/v1/news/" + newsId)
                .then()
                .statusCode(401);
    }

    @Test
    public void testNewsUpdate() {
   /*     News news = newsDao.findAll().get(0);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(trip)
                .put("/api/v1/news/" + newsId)
                .then()
                .statusCode(401);*/
    }
}
