package fr.inrae.fishola.rest.licences;

import fr.inrae.fishola.database.FishingLicencesDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.LicenceType;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.FisholaUserLicences;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;
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

    @BeforeEach
    @Transactional
    public void loginAndInit() throws IOException {
        FisholaUser fisholaUser = this.usersDao.findByEmail("thimel@codelutin.com").get();
        this.userId = fisholaUser.getId();

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
                .get("/api/v1/licences/" + licence.getUserId() + "/" + licence.getId())
                .then()
                .statusCode(200);

        // Then we check that the content matches
        InputStream responseStream = given()
                .when()
                .get("/api/v1/licences/" + licence.getUserId() + "/" + licence.getId())
                .asInputStream();
        byte[] responseBytes = IOUtils.toByteArray(responseStream);
        Assertions.assertArrayEquals(licence.getContent(), responseBytes);
    }

    @Test
    @Transactional
    void testGetLicenceForBadUser() {
        String badUserId = "1234";
        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .get("/api/v1/licences/" + badUserId + "/" + pdfLicence.getId())
                .then()
                .statusCode(404);
    }


    @Test
    @Transactional
    void testGetAllLicences() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/licences/" + userId)
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
    void testGetAllLicencesForNonExistingUser() {
        String badUserId = "1234";

        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/licences/" + badUserId)
                .then()
                .statusCode(404);
    }

    @Test
    @Transactional
    void testPostLicence() throws IOException {
        URL fishingLicenceUrl = getClass().getResource("/fishing-licence-document.pdf");
        if (fishingLicenceUrl == null) {
            throw new RuntimeException("Resource not found.");
        }
        File pdfFile = new File(fishingLicenceUrl.getFile());
        byte[] content = Files.readAllBytes(Path.of(pdfFile.getAbsolutePath()));

        LicenceFromClientBean licenceSentByClient = new LicenceFromClientBean();
        licenceSentByClient.name = "createdLicence";
        licenceSentByClient.type = LicenceType.PDF;
        licenceSentByClient.expirationDate = LocalDate.now();
        licenceSentByClient.content = Base64.getEncoder().encodeToString(content);

        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(licenceSentByClient)
                .post("api/v1/licences/" + userId)
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
            Assertions.assertArrayEquals(pdfLicence.getContent(), retrievedLicence.get().getContent());
        } else {
            Assertions.fail();
        }
    }

    @Test
    @Transactional
    void testDeleteLicence() {
        List<LicenceResponseBean> licences = fishingLicencesDao.getLicencesByUser(userId);
        Assertions.assertEquals(2, licences.size());

        given()
                .when()
                .delete("api/v1/licences/" + userId + "/" + pdfLicence.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .delete("api/v1/licences/" + userId + "/" + jpegLicence.getId())
                .then()
                .statusCode(204);

        List<LicenceResponseBean> retrievedLicences = fishingLicencesDao.getLicencesByUser(userId);
        Assertions.assertTrue(retrievedLicences.isEmpty());
    }
}