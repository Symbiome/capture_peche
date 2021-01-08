package fr.inrae.fishola.database;

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

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.inrae.fishola.entities.Sequences;
import fr.inrae.fishola.entities.tables.daos.FisholaUserDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;

import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.inrae.fishola.entities.Tables.FISHOLA_USER;

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

    public Optional<Boolean> authenticate(String rawEmail, String password) {
        String email = rawEmail.toLowerCase();
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

    public List<FisholaUser> findAll() {
        List<FisholaUser> result = withDao(FisholaUserDao.class, FisholaUserDao::findAll);
        return result;
    }

    public boolean isValidUserId(UUID userId) {
        boolean result = withDao(FisholaUserDao.class, dao -> dao.existsById(userId));
        return result;
    }

    public Optional<FisholaUser> findByEmail(String rawEmail) {
        String email = rawEmail.toLowerCase();
        FisholaUser user = withDao(FisholaUserDao.class, dao -> dao.fetchOneByEmail(email));
        Optional<FisholaUser> result = Optional.ofNullable(user);
        return result;
    }

    public void create(String firstName, String lastName, String rawEmail, String passwordHashed) {
        String email = rawEmail.toLowerCase();
        withContext(context -> context.insertInto(FISHOLA_USER,
                FISHOLA_USER.FIRST_NAME, FISHOLA_USER.LAST_NAME, FISHOLA_USER.EMAIL, FISHOLA_USER.PASSWORD, FISHOLA_USER.CREATED_ON)
                .values(firstName, lastName, email, passwordHashed, LocalDateTime.now())
                .execute());
    }

    public void updateUser(FisholaUser existingUser) {
        withDaoNoResult(FisholaUserDao.class, dao -> dao.update(existingUser));
    }

    public void deleteUser(FisholaUser existingUser) {
        withDaoNoResult(FisholaUserDao.class, dao -> dao.delete(existingUser));
    }

    public void increaseSampleBaseId(UUID userId) {
        withContextNoResult(context -> {
            int nextSampleBaseId = context.nextval(Sequences.SAMPLE_BASE_ID_SEQUENCE).intValue();
            context.update(FISHOLA_USER)
                    .set(FISHOLA_USER.SAMPLE_BASE_ID, nextSampleBaseId)
                    .where(FISHOLA_USER.ID.eq(userId))
                    .execute();
        });
    }

}
