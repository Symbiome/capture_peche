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

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * Profil d'un compte créé par import CSV (#192), colonne « profil ». Un même
 * e-mail peut avoir un compte pêcheur et un compte staff : ce sont deux
 * comptes distincts ({@code fishola_user} et {@code fishola_admin}).
 */
public enum AccountProfile {

    PECHEUR("pecheur", false, false, false),
    OPERATEUR("operateur", true, true, false),
    ADMIN_REGIONAL("admin_regional", true, true, false),
    ADMIN_NATIONAL("admin_national", true, false, true);

    private final String csvValue;
    private final boolean staff;
    private final boolean perimeterRequired;
    private final boolean nationalAdmin;

    AccountProfile(String csvValue, boolean staff, boolean perimeterRequired, boolean nationalAdmin) {
        this.csvValue = csvValue;
        this.staff = staff;
        this.perimeterRequired = perimeterRequired;
        this.nationalAdmin = nationalAdmin;
    }

    /**
     * Retrouve un profil depuis sa valeur CSV, sans tenir compte de la casse
     * ni des accents (« Pêcheur » vaut « pecheur »).
     *
     * @param value valeur lue dans le fichier
     * @return le profil, vide si la valeur est inconnue
     */
    public static Optional<AccountProfile> fromCsv(String value) {
        String normalized = StringUtils.stripAccents(StringUtils.trimToEmpty(value)).toLowerCase();
        return Arrays.stream(values()).filter(profile -> profile.csvValue.equals(normalized)).findFirst();
    }

    /** @return la valeur attendue dans la colonne « profil » */
    public String csvValue() {
        return csvValue;
    }

    /** @return {@code true} pour un compte du back-office ({@code fishola_admin}) */
    public boolean isStaff() {
        return staff;
    }

    /** @return {@code true} si le compte doit avoir au moins un département */
    public boolean isPerimeterRequired() {
        return perimeterRequired;
    }

    /** @return {@code true} pour un administrateur national */
    public boolean isNationalAdmin() {
        return nationalAdmin;
    }

    /** @return {@code true} pour un opérateur */
    public boolean isOperator() {
        return this == OPERATEUR;
    }
}
