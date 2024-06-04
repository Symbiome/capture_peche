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

import fr.inrae.fishola.entities.tables.daos.FisholaUserLicencesDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaUserLicences;
import fr.inrae.fishola.rest.licences.LicenceResponseBean;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class FishingLicencesDao extends AbstractFisholaDao {

    public List<LicenceResponseBean> getLicencesByUser(UUID userId) {
        List<FisholaUserLicences> licences = withDao(FisholaUserLicencesDao.class, dao -> dao.fetchByUserId(userId));
        List<LicenceResponseBean> licenceBeans = licences.stream()
                .map(this::convertToBean)
                .sorted((a,b) -> b.expirationDate.compareTo(a.expirationDate))
                .toList();
        return licenceBeans;
    }

    private LicenceResponseBean convertToBean(FisholaUserLicences licence) {
        LicenceResponseBean licenceBean = new LicenceResponseBean();
        licenceBean.id = licence.getId();
        licenceBean.name = licence.getName();
        licenceBean.userId = licence.getUserId();
        licenceBean.type = licence.getType();
        licenceBean.expirationDate = licence.getExpirationDate();
        return licenceBean;
    }

    public Optional<FisholaUserLicences> getLicence(UUID licenceId) {
        Optional<FisholaUserLicences> result = withDao(FisholaUserLicencesDao.class, dao -> dao.fetchOptionalById(licenceId));
        return result;
    }

    public void createLicence(FisholaUserLicences licence) {
        List<FisholaUserLicences> licences = withDao(FisholaUserLicencesDao.class, dao -> dao.fetchByUserId(licence.getUserId()));
        List<String> licencesNames = licences.stream().map(FisholaUserLicences::getName).toList();
        if (licencesNames.contains(licence.getName())) {
            throw new IllegalArgumentException("Une carte de pêche porte déjà ce nom.");
        }
        withDaoNoResult(FisholaUserLicencesDao.class, dao -> dao.insert(licence));
    }

    public void update(FisholaUserLicences modifiedLicence) {
        withDaoNoResult(FisholaUserLicencesDao.class, dao -> dao.update(modifiedLicence));
    }

    public void deleteLicence(UUID licenceId) {
        withDaoNoResult(FisholaUserLicencesDao.class, dao -> dao.deleteById(licenceId));
    }
}
