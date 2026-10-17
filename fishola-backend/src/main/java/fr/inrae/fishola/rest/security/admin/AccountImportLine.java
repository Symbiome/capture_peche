package fr.inrae.fishola.rest.security.admin;

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

/**
 * Résultat de l'import d'une ligne du CSV de comptes (#192).
 */
public class AccountImportLine {

    public static final String CREATED = "CREATED";
    public static final String IGNORED = "IGNORED";
    public static final String REJECTED = "REJECTED";

    public int line;
    public String email;
    public String profile;
    public String status;
    public String message;

    public AccountImportLine(int line, String email, String profile, String status, String message) {
        this.line = line;
        this.email = email;
        this.profile = profile;
        this.status = status;
        this.message = message;
    }
}
