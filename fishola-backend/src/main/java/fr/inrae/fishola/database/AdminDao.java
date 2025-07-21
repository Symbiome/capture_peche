package fr.inrae.fishola.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.inrae.fishola.entities.tables.daos.FisholaAdminDao;
import fr.inrae.fishola.entities.tables.daos.FisholaAdminLakesDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdminLakes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.inrae.fishola.entities.Tables.FISHOLA_ADMIN;

@Singleton
public class AdminDao extends AbstractFisholaDao {

    @Inject
    protected Logger log;

    public String hashPassword(String password) {
        int cost = config.passwordHashCost();
        String result = BCrypt.withDefaults().hashToString(cost, password.toCharArray());
        return result;
    }

    protected boolean verifyPassword(boolean isNationalAdmin, String plain, String hashed) {
        try {
            // For national admins, use password defined in application.properties
            if (isNationalAdmin) {
               return config.adminPassword().equals(plain);
            } else {
                BCrypt.Result result = BCrypt.verifyer().verify(plain.toCharArray(), hashed);
                return result.verified;
            }
        } catch (Exception eee) {
            log.error(eee);
            return false;
        }
    }

    public Optional<Boolean> authenticate(String rawEmail, String password) {
        String email = rawEmail.toLowerCase();
        Optional<FisholaAdmin> user = findByEmail(email);
        Optional<Boolean> result = user
                .map(FisholaAdmin::getPassword)
                .map(userPassword -> verifyPassword(user.get().getIsNationalAdmin(), password, userPassword));
        return result;
    }

    public Optional<FisholaAdmin> findById(UUID userId) {
        FisholaAdmin user = withDao(FisholaAdminDao.class, dao -> dao.findById(userId));
        Optional<FisholaAdmin> result = Optional.ofNullable(user);
        return result;
    }

    public List<FisholaAdmin> findAll() {
        List<FisholaAdmin> result = withDao(FisholaAdminDao.class, FisholaAdminDao::findAll);
        return result;
    }

    public boolean isValidUserId(UUID userId) {
        boolean result = withDao(FisholaAdminDao.class, dao -> dao.existsById(userId));
        return result;
    }

    public Optional<FisholaAdmin> findByEmail(String rawEmail) {
        String email = rawEmail.toLowerCase();
        FisholaAdmin user = withDao(FisholaAdminDao.class, dao -> dao.fetchOneByEmail(email));
        Optional<FisholaAdmin> result = Optional.ofNullable(user);
        return result;
    }

    public void create(String rawEmail, String passwordHashed, boolean canCreateAdmin, boolean isNationalAdmin) {
        String email = rawEmail.toLowerCase();
        withContext(context -> context.insertInto(FISHOLA_ADMIN,
                        FISHOLA_ADMIN.EMAIL, FISHOLA_ADMIN.PASSWORD, FISHOLA_ADMIN.CREATED_ON, FISHOLA_ADMIN.CAN_CREATE_ADMIN, FISHOLA_ADMIN.IS_NATIONAL_ADMIN)
                .values(email, passwordHashed, LocalDateTime.now(), canCreateAdmin, isNationalAdmin)
                .execute());
    }

    public void updateUser(FisholaAdmin existingUser) {
        withDaoNoResult(FisholaAdminDao.class, dao -> dao.update(existingUser));
    }

    public void deleteUser(FisholaAdmin existingUser) {
        withDaoNoResult(FisholaAdminDao.class, dao -> dao.delete(existingUser));
    }

    public Set<UUID> getAllowedLakes(UUID adminID) {
        return withDao(FisholaAdminLakesDao.class, dao -> dao.fetchByFisholaAdminId(adminID).stream().map(FisholaAdminLakes::getLakeId).collect(Collectors.toSet()));
    }
}
