package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.daos.FisholaAdminDao;
import fr.inrae.fishola.entities.tables.daos.FisholaAdminLakesDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdminLakes;
import fr.inrae.fishola.entities.tables.records.FisholaAdminRecord;
import fr.inrae.fishola.rest.security.AdminProfileForAdmin;
import fr.inrae.fishola.rest.security.ImmutableAdminProfileForAdmin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.inrae.fishola.entities.Tables.FISHOLA_ADMIN;
import static fr.inrae.fishola.entities.Tables.FISHOLA_ADMIN_LAKES;

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

    public void create(String rawEmail, String passwordHashed, boolean canCreateAdmin, boolean isNationalAdmin, UUID[] lakeIds) {
        String email = rawEmail.toLowerCase();
        FisholaAdminRecord inserted = withContext(context -> context.insertInto(FISHOLA_ADMIN,
                        FISHOLA_ADMIN.EMAIL, FISHOLA_ADMIN.PASSWORD, FISHOLA_ADMIN.CREATED_ON, FISHOLA_ADMIN.CAN_CREATE_ADMIN, FISHOLA_ADMIN.IS_NATIONAL_ADMIN)
                .values(email, passwordHashed, LocalDateTime.now(), canCreateAdmin, isNationalAdmin)
                .returning(FISHOLA_ADMIN.ID)
                .fetchOne());
        UUID insertedAdminId = inserted.getId();
        withDaoNoResult(FisholaAdminLakesDao.class, dao -> {
            for (UUID lakeId: lakeIds) {
                dao.insert(new FisholaAdminLakes(insertedAdminId, lakeId));
            }
        });
    }


    public Set<UUID> getAllowedLakes(UUID adminID) {
        return withDao(FisholaAdminLakesDao.class, dao -> dao.fetchByFisholaAdminId(adminID).stream().map(FisholaAdminLakes::getLakeId).collect(Collectors.toSet()));
    }

    public AdminProfileForAdmin toUserProfileForAdmin(FisholaAdmin input) {
        ImmutableAdminProfileForAdmin result = ImmutableAdminProfileForAdmin.builder()
                .id(input.getId())
                .email(input.getEmail())
                .canCreateAdmin(input.getCanCreateAdmin())
                .isNationalAdmin(input.getIsNationalAdmin())
                .lakeIds(this.getAllowedLakes(input.getId()))
                .build();
        return result;
    }

    public void updateAdmin(UUID adminId, Boolean canCreateAdmin, Set<UUID> lakeIds) {
        DSLContext context = newContext();
        context.update(FISHOLA_ADMIN)
                .set(FISHOLA_ADMIN.CAN_CREATE_ADMIN, canCreateAdmin)
                .where(FISHOLA_ADMIN.ID.equal(adminId))
                .returning(FISHOLA_ADMIN.ID)
                .fetchOne();

        context.deleteFrom(FISHOLA_ADMIN_LAKES)
        .where(FISHOLA_ADMIN_LAKES.FISHOLA_ADMIN_ID.equal(adminId))
        .execute();

        withDaoNoResult(FisholaAdminLakesDao.class, dao -> {
            for (UUID lakeId : lakeIds) {
                dao.insert(new FisholaAdminLakes(adminId, lakeId));
            }
        });
    }


    public void deleteAdmin(FisholaAdmin existingUser) {
        withDaoNoResult(FisholaAdminDao.class, dao -> dao.delete(existingUser));
    }
}
