package fr.inrae.fishola.rest.security.admin;

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

import java.util.Set;
import java.util.UUID;

public class LoggedAdminBean {
    public String email;
    public Boolean isNationalAdmin;
    public final Boolean canCreateAdmins;
    public final Boolean isOperator;
    /** Périmètre du staff (plans d'eau) ; vide pour un administrateur national. */
    public final Set<UUID> waterEntityIds;

    public LoggedAdminBean(String email, Boolean isNationalAdmin, Boolean canCreateAdmins, Boolean isOperator, Set<UUID> waterEntityIds) {
        this.email = email;
        this.isNationalAdmin = isNationalAdmin;
        this.canCreateAdmins = canCreateAdmins;
        this.isOperator = isOperator;
        this.waterEntityIds = waterEntityIds;
    }
}
