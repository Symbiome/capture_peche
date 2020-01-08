package fr.inra.fishola.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.inra.fishola.entities.tables.daos.FisholaUserDao;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

import static fr.inra.fishola.entities.Tables.FISHOLA_USER;

@Singleton
public class UsersDao extends AbstractFisholaDao {

    public String hashPassword(String password) {
        int cost = config.getPasswordHashCost();
        String result = BCrypt.withDefaults().hashToString(cost, password.toCharArray());
        return result;
    }

    protected boolean verifyPassword(String plain, String hashed) {
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(plain.toCharArray(), hashed);
            boolean verified = result.verified;
            return verified;
        } catch (Exception eee) {
            eee.printStackTrace();
            return false;
        }
    }

    public Optional<Boolean> authenticate(String email, String password) {
        Optional<FisholaUser> user = findByEmail(email);
        Optional<Boolean> result = user
                .map(FisholaUser::getPassword)
                .map(userPassword -> verifyPassword(password, userPassword));
        return result;
    }

    public Optional<FisholaUser> findById(UUID userId) {
        FisholaUser user = withDao(FisholaUserDao.class, dao -> dao.findById(userId));
        Optional<FisholaUser> result = Optional.ofNullable(user);
        return result;
    }

    public boolean isValidUserId(UUID userId) {
        boolean result = withDao(FisholaUserDao.class, dao -> dao.existsById(userId));
        return result;
    }

    public Optional<FisholaUser> findByEmail(String email) {
        FisholaUser user = withDao(FisholaUserDao.class, dao -> dao.fetchOneByEmail(email));
        Optional<FisholaUser> result = Optional.ofNullable(user);
        return result;
    }

    public void create(String firstName, String lastName, String email, String passwordHashed) {
        withContext(context -> context.insertInto(FISHOLA_USER,
                FISHOLA_USER.FIRST_NAME, FISHOLA_USER.LAST_NAME, FISHOLA_USER.EMAIL, FISHOLA_USER.PASSWORD)
                .values(firstName, lastName, email, passwordHashed)
                .execute());
    }

}
