package fr.inrae.fishola.rest.licences;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.database.FishingLicencesDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.LicenceType;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.FisholaUserLicences;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class LicenceResourceTest extends AbstractFisholaTest {

    @Inject
    protected FishingLicencesDao fishingLicencesDao;
    @Inject
    protected UsersDao usersDao;
    private UUID userId;
    private FisholaUserLicences pdfLicence;
    private FisholaUserLicences jpegLicence;
    private String token;
    private UUID badUserId;
    private String badToken;

    @BeforeEach
    @Transactional
    public void loginAndInit() throws IOException {
        Optional<FisholaUser> fisholaUser = this.usersDao.findByEmail("thimel@codelutin.com");
        Assertions.assertTrue(fisholaUser.isPresent());
        this.userId = fisholaUser.get().getId();
        this.token = login("thimel@codelutin.com", "sispea");

        Optional<FisholaUser> badFisholaUser = this.usersDao.findByEmail("chloe.goulon@inrae.fr");
        Assertions.assertTrue(badFisholaUser.isPresent());
        this.badUserId = badFisholaUser.get().getId();
        this.badToken = "0000".concat(token.substring(4));

        LocalDate expirationDate = LocalDate.now().plusYears(2);

        // Insert a licence of type pdf
        byte[] pdfFishingLicenceAsBytes = readFishingLicenceFile("fishing-licence-document.pdf");
        pdfLicence = new FisholaUserLicences();
        pdfLicence.setUserId(userId);
        pdfLicence.setName("pdfLicence");
        pdfLicence.setType(LicenceType.PDF);
        pdfLicence.setExpirationDate(expirationDate);
        pdfLicence.setContent(pdfFishingLicenceAsBytes);
        fishingLicencesDao.createLicence(pdfLicence);

        // Insert a licence of type jpeg
        byte[] jpegFishingLicenceAsBytes = readFishingLicenceFile("fishing-licence-picture.jpeg");
        jpegLicence = new FisholaUserLicences();
        jpegLicence.setUserId(userId);
        jpegLicence.setName("jpegLicence");
        jpegLicence.setType(LicenceType.JPEG);
        jpegLicence.setExpirationDate(expirationDate);
        jpegLicence.setContent(jpegFishingLicenceAsBytes);
        fishingLicencesDao.createLicence(jpegLicence);
    }

    private byte[] readFishingLicenceFile(String fileName) throws IOException {
        URL fishingLicenceUrl = getClass().getResource("/%s".formatted(fileName));
        if (fishingLicenceUrl == null) {
            throw new RuntimeException("Resource not found : %s".formatted(fileName));
        }
        File file = new File(fishingLicenceUrl.getFile());
        byte[] content = Files.readAllBytes(Path.of(file.getAbsolutePath()));
        return content;
    }

    @AfterEach
    @Transactional
    public void tearDown() {
        List<LicenceResponseBean> licences = fishingLicencesDao.getLicencesByUser(userId);
        licences.forEach(licence -> fishingLicencesDao.deleteLicence(licence.id));
    }

    @Test
    @Transactional
    void testGetLicence() throws IOException {
        checkGetLicence(pdfLicence);
        checkGetLicence(jpegLicence);
    }

    private void checkGetLicence(FisholaUserLicences licence) throws IOException {
        // We first check that returned status code is ´OK´
        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/licences/%s".formatted(licence.getId()))
                .then()
                .statusCode(200);

        // Then we check that the content matches
        InputStream responseStream = given()
                .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/licences/%s".formatted(licence.getId()))
                .asInputStream();
        byte[] responseBytes = IOUtils.toByteArray(responseStream);
        Assertions.assertArrayEquals(licence.getContent(), responseBytes);
    }

    @Test
    @Transactional
    void testGetLicenceForbidden() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, badToken)
                .get("/api/v1/licences/%s".formatted(pdfLicence.getId()))
                .then()
                // badToken is not a valid token for userId
                .statusCode(401);
    }

    @Test
    @Transactional
    void testGetNotOwnedLicence() throws IOException {
        byte[] jpegFishingLicenceAsBytes = readFishingLicenceFile("fishing-licence-picture.jpeg");
        FisholaUserLicences notOwnedLicence = new FisholaUserLicences();
        notOwnedLicence.setUserId(badUserId);
        notOwnedLicence.setName("notOwnedLicence");
        notOwnedLicence.setType(LicenceType.JPEG);
        notOwnedLicence.setExpirationDate(LocalDate.now());
        notOwnedLicence.setContent(jpegFishingLicenceAsBytes);
        fishingLicencesDao.createLicence(notOwnedLicence);

        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/licences/%s".formatted(notOwnedLicence.getId()))
                .then()
                .log().all()
                // user cannot access a licence that it does not own
                .statusCode(400);
    }


    @Test
    @Transactional
    void testGetAllLicences() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/licences")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].id", equalTo(pdfLicence.getId().toString()))
                .body("[0].name", equalTo(pdfLicence.getName()))
                .body("[0].userId", equalTo(pdfLicence.getUserId().toString()))
                .body("[0].type", equalTo(pdfLicence.getType().toString()))
                .body("[1].id", equalTo(jpegLicence.getId().toString()))
                .body("[1].name", equalTo(jpegLicence.getName()))
                .body("[1].userId", equalTo(jpegLicence.getUserId().toString()))
                .body("[1].type", equalTo(jpegLicence.getType().toString()));
    }

    @Test
    @Transactional
    void testGetAllLicencesForbidden() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, badToken)
                .get("/api/v1/licences")
                .then()
                .statusCode(401);
    }


    @Test
    @Transactional
    void testGetAllLicencesForNonExistingUser() {
        String badUserId = "1234";

        given()
                .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/licences/%s".formatted(badUserId))
                .then()
                .statusCode(404);
    }

    @Test
    @Transactional
    void testPostLicence() {
        byte[] content = pdfLicence.getContent();

        LicenceFromClientBean licenceSentByClient = new LicenceFromClientBean();
        licenceSentByClient.name = "createdLicence";
        licenceSentByClient.type = LicenceType.PDF;
        licenceSentByClient.expirationDate = LocalDate.now();
        licenceSentByClient.content = Base64.getEncoder().encodeToString(content);

        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(licenceSentByClient)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .post("api/v1/licences")
                .then()
                .statusCode(200);

        List<LicenceResponseBean> licences = fishingLicencesDao.getLicencesByUser(userId);
        Optional<LicenceResponseBean> matchingLicence = licences.stream()
                                                                .filter(licenceBean -> Objects.equals(licenceBean.name,
                                                                        licenceSentByClient.name))
                                                                .findFirst();

        if (matchingLicence.isPresent()) {
            UUID retrievedLicenceId = matchingLicence.get().id;
            Optional<FisholaUserLicences> retrievedLicence = fishingLicencesDao.getLicence(retrievedLicenceId);
            Assertions.assertTrue(retrievedLicence.isPresent());
            Assertions.assertEquals(licenceSentByClient.name, retrievedLicence.get().getName());
            Assertions.assertEquals(licenceSentByClient.type, retrievedLicence.get().getType());
            Assertions.assertEquals(licenceSentByClient.expirationDate, retrievedLicence.get().getExpirationDate());
            Assertions.assertArrayEquals(content, retrievedLicence.get().getContent());
        } else {
            Assertions.fail();
        }
    }

    @Test
    @Transactional
    void testPostLicenceForbidden() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, badToken)
                .post("api/v1/licences")
                .then()
                .statusCode(401);
    }

    @Test
    @Transactional
    void testDeleteLicence() {
        List<LicenceResponseBean> licences = fishingLicencesDao.getLicencesByUser(userId);
        Assertions.assertEquals(2, licences.size());

        given()
                .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .delete("api/v1/licences/%s".formatted(pdfLicence.getId()))
                .then()
                .statusCode(204);

        given()
                .when()
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .delete("api/v1/licences/%s".formatted(jpegLicence.getId()))
                .then()
                .statusCode(204);

        List<LicenceResponseBean> retrievedLicences = fishingLicencesDao.getLicencesByUser(userId);
        Assertions.assertTrue(retrievedLicences.isEmpty());
    }

    @Test
    @Transactional
    void testDeleteLicenceForbidden() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, badToken)
                .delete("/api/v1/licences/%s".formatted(pdfLicence.getId()))
                .then()
                // badToken is not a valid token for userId
                .statusCode(401);
    }
}
