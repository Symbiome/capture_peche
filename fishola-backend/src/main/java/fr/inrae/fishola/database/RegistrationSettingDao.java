package fr.inrae.fishola.database;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.entities.Tables;
import jakarta.inject.Singleton;

/**
 * Ouverture de l'inscription libre dans l'app pêcheur (#192), stockée dans la
 * ligne unique de {@code registration_setting} (migration V3.4.0).
 */
@Singleton
public class RegistrationSettingDao extends AbstractCarnetDao {

    /**
     * Indique si un pêcheur peut créer son compte depuis l'app.
     *
     * @return {@code true} si l'inscription libre est ouverte
     */
    public boolean isRegistrationEnabled() {
        return withContext(context -> context
                .select(Tables.REGISTRATION_SETTING.ENABLED)
                .from(Tables.REGISTRATION_SETTING)
                .fetchOne(Tables.REGISTRATION_SETTING.ENABLED));
    }

    /**
     * Ouvre ou ferme l'inscription libre.
     *
     * @param enabled {@code true} pour l'ouvrir, {@code false} pour la fermer
     */
    public void setRegistrationEnabled(boolean enabled) {
        withContext(context -> context.update(Tables.REGISTRATION_SETTING)
                .set(Tables.REGISTRATION_SETTING.ENABLED, enabled)
                .execute());
    }
}
