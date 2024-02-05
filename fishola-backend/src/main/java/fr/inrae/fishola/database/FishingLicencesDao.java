package fr.inrae.fishola.database;

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
        List<LicenceResponseBean> licenceBeans = licences.stream().map(this::convertToBean).toList();
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
        Optional<FisholaUserLicences> result;
        try {
            result = Optional.ofNullable(withDao(FisholaUserLicencesDao.class, dao -> dao.fetchOneById(licenceId)));
        } catch (NullPointerException e) {
            result = Optional.empty();
        }
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

    public void deleteLicence(UUID licenceId) {
        withDaoNoResult(FisholaUserLicencesDao.class, dao -> dao.deleteById(licenceId));
    }
}
