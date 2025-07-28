package fr.inrae.fishola.database;

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
        Optional<FisholaUser> optionalUser = this.usersDao.findByEmail("chloe.goulon@inrae.fr");
        Assertions.assertTrue(optionalUser.isPresent());
        FisholaUser user = optionalUser.get();

        // Insert a fishing licence
        String fileName = "fishing-licence-document.pdf";
        URL fishingLicenceUrl = getClass().getResource("/%s".formatted(fileName));
        if (fishingLicenceUrl == null) {
            throw new RuntimeException("Resource not found : %s".formatted(fileName));
        }
        File file = new File(fishingLicenceUrl.getFile());
        byte[] pdfFishingLicenceAsBytes = Files.readAllBytes(Path.of(file.getAbsolutePath()));
        FisholaUserLicences licence = new FisholaUserLicences();
        licence.setUserId(user.getId());
        licence.setName("licence");
        licence.setType(LicenceType.PDF);
        licence.setExpirationDate(LocalDate.now().plusYears(2));
        licence.setContent(pdfFishingLicenceAsBytes);
        fishingLicencesDao.createLicence(licence);

        // Delete user
        usersDao.deleteUser(user);

        // Check that licence is also deleted
        Assertions.assertTrue(fishingLicencesDao.getLicence(licence.getId()).isEmpty());

        // User is needed for other tests, so we add it again.
        usersDao.create(user.getFirstName(), user.getLastName(), user.getEmail(), user.getPassword(), user.getAcceptsMailNotifications(), user.getAcceptsShareTrips());
    }
}
