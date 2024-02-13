package fr.inrae.fishola.database;

import fr.inrae.fishola.entities.enums.LicenceType;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.FisholaUserLicences;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


@QuarkusTest
public class LicenceDaoTest extends AbstractFisholaTest {

    @Inject
    protected FishingLicencesDao fishingLicencesDao;
    @Inject
    protected UsersDao usersDao;

    @Test
    @Transactional
    void testRemovingUserDeletesLicences() throws IOException {

        // User information
        Optional<FisholaUser> user = this.usersDao.findByEmail("thimel@codelutin.com");
        Assertions.assertTrue(user.isPresent());
        UUID userId = user.get().getId();
        String token = login("thimel@codelutin.com", "sispea");

        // Insert a fishing licence
        String fileName = "fishing-licence-document.pdf";
        URL fishingLicenceUrl = getClass().getResource("/%s".formatted(fileName));
        if (fishingLicenceUrl == null) {
            throw new RuntimeException("Resource not found : %s".formatted(fileName));
        }
        File file = new File(fishingLicenceUrl.getFile());
        byte[] pdfFishingLicenceAsBytes = Files.readAllBytes(Path.of(file.getAbsolutePath()));
        FisholaUserLicences licence = new FisholaUserLicences();
        licence.setUserId(userId);
        licence.setName("licence");
        licence.setType(LicenceType.PDF);
        licence.setExpirationDate(LocalDate.now().plusYears(2));
        licence.setContent(pdfFishingLicenceAsBytes);
        fishingLicencesDao.createLicence(licence);

        // Delete user
        usersDao.deleteUser(user.get());

        // Check that licence is also deleted
        Assertions.assertTrue(fishingLicencesDao.getLicence(licence.getId()).isEmpty());
    }
}
