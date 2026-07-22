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
import fr.inrae.fishola.entities.tables.daos.FisholaAdminDao;
import fr.inrae.fishola.entities.tables.daos.FisholaAdminWaterEntitiesDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdminWaterEntities;
import fr.inrae.fishola.entities.tables.records.FisholaAdminRecord;
import fr.inrae.fishola.rest.security.AdminProfileForAdmin;
import fr.inrae.fishola.rest.security.ImmutableAdminProfileForAdmin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.inrae.fishola.entities.Tables.FISHOLA_ADMIN;
import static fr.inrae.fishola.entities.Tables.FISHOLA_ADMIN_WATER_ENTITIES;

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
            // Authentification NOMINATIVE d'abord : hash bcrypt propre au compte,
            // pour TOUS les rôles (national inclus) — imputabilité RGPD (#55).
            if (hashed != null && !hashed.isBlank()) {
                BCrypt.Result result = BCrypt.verifyer().verify(plain.toCharArray(), hashed);
                if (result.verified) {
                    return true;
                }
            }
            // Repli DÉPRÉCIÉ, non-breaking : mot de passe national PARTAGÉ
            // (application.properties). À retirer une fois tous les comptes nationaux
            // provisionnés avec un mot de passe nominatif (#55).
            if (isNationalAdmin && config.adminPassword() != null && config.adminPassword().equals(plain)) {
                log.warn("Auth admin national via mot de passe PARTAGE (deprecie, #55) - provisionner un mot de passe nominatif.");
                return true;
            }
            return false;
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

    public void create(String rawEmail, String passwordHashed, boolean canCreateAdmin, boolean isNationalAdmin, UUID[] waterEntityIds) {
        String email = rawEmail.toLowerCase();
        FisholaAdminRecord inserted = withContext(context -> context.insertInto(FISHOLA_ADMIN,
                        FISHOLA_ADMIN.EMAIL, FISHOLA_ADMIN.PASSWORD, FISHOLA_ADMIN.CREATED_ON, FISHOLA_ADMIN.CAN_CREATE_ADMIN, FISHOLA_ADMIN.IS_NATIONAL_ADMIN)
                .values(email, passwordHashed, LocalDateTime.now(), canCreateAdmin, isNationalAdmin)
                .returning(FISHOLA_ADMIN.ID)
                .fetchOne());
        UUID insertedAdminId = inserted.getId();
        withDaoNoResult(FisholaAdminWaterEntitiesDao.class, dao -> {
            for (UUID waterEntityId: waterEntityIds) {
                dao.insert(new FisholaAdminWaterEntities(insertedAdminId, waterEntityId));
            }
        });
    }


    public Set<UUID> getAllowedWaterEntities(UUID adminID) {
        return withDao(FisholaAdminWaterEntitiesDao.class, dao -> dao.fetchByFisholaAdminId(adminID).stream().map(FisholaAdminWaterEntities::getWaterEntityId).collect(Collectors.toSet()));
    }

    public AdminProfileForAdmin toUserProfileForAdmin(FisholaAdmin input) {
        ImmutableAdminProfileForAdmin result = ImmutableAdminProfileForAdmin.builder()
                .id(input.getId())
                .email(input.getEmail())
                .canCreateAdmin(input.getCanCreateAdmin())
                .isNationalAdmin(input.getIsNationalAdmin())
                .waterEntityIds(this.getAllowedWaterEntities(input.getId()))
                .build();
        return result;
    }

    public void updateAdmin(UUID adminId, Boolean canCreateAdmin, Set<UUID> waterEntityIds) {
        DSLContext context = newContext();
        context.update(FISHOLA_ADMIN)
                .set(FISHOLA_ADMIN.CAN_CREATE_ADMIN, canCreateAdmin)
                .where(FISHOLA_ADMIN.ID.equal(adminId))
                .returning(FISHOLA_ADMIN.ID)
                .fetchOne();

        context.deleteFrom(FISHOLA_ADMIN_WATER_ENTITIES)
        .where(FISHOLA_ADMIN_WATER_ENTITIES.FISHOLA_ADMIN_ID.equal(adminId))
        .execute();

        withDaoNoResult(FisholaAdminWaterEntitiesDao.class, dao -> {
            for (UUID waterEntityId : waterEntityIds) {
                dao.insert(new FisholaAdminWaterEntities(adminId, waterEntityId));
            }
        });
    }


    public void deleteAdmin(FisholaAdmin existingUser) {
        withDaoNoResult(FisholaAdminDao.class, dao -> dao.delete(existingUser));
    }
}
