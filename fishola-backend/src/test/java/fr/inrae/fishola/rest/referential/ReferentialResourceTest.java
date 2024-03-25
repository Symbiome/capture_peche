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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class ReferentialResourceTest {

    @Test
    void testGetLakes() {
        // [{"id":"7440e0bd-9dab-4997-bb11-8062c0ec5ec1","name":"Léman","exportAs":"Léman","latitude":46.426,"longitude":6.5499},{"id":"75fd08fe-b377-4b58-982a-88640da8e33a","name":"Lac du Bourget","exportAs":"Bourget","latitude":45.7249,"longitude":5.8684},{"id":"5e9e93ff-f759-4ab8-a8ce-b2041d749bb1","name":"Lac d'Aiguebelette","exportAs":"Aiguebelette","latitude":45.5508,"longitude":5.8015},{"id":"c5b98dc6-3045-4aa8-86c0-18318159ef03","name":"Lac d'Annecy","exportAs":"Annecy","latitude":45.859756,"longitude":6.169396}]
        given()
                .when()
                    .get("/api/v1/referential/lakes")
                .then()
                    .statusCode(200)
                    .body("size()", equalTo(4))
                    .body("name", hasItems("Lac d'Annecy", "Léman", "Lac du Bourget", "Lac d'Aiguebelette"))
                    .body("exportAs", hasItems("Annecy", "Léman", "Bourget", "Aiguebelette"))
                    .body("[0].id", notNullValue())
                    .body("[1].id", notNullValue())
                    .body("[2].id", notNullValue())
                    .body("[3].id", notNullValue())
                    .body("[0].latitude", notNullValue())
                    .body("[1].latitude", notNullValue())
                    .body("[2].latitude", notNullValue())
                    .body("[3].latitude", notNullValue())
                    .body("[0].longitude", notNullValue())
                    .body("[1].longitude", notNullValue())
                    .body("[2].longitude", notNullValue())
                    .body("[3].longitude", notNullValue());
    }

}
