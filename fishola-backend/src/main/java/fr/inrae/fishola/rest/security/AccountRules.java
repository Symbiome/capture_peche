package fr.inrae.fishola.rest.security;

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

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Règles communes aux comptes pêcheur et staff : format de l'e-mail et
 * politique de mot de passe, partagées par l'inscription, la création d'un
 * administrateur et l'import de comptes (#192).
 */
public final class AccountRules {

    private static final int PASSWORD_MIN_LENGTH = 6;

    private AccountRules() {}

    /**
     * Contrôle un mot de passe en clair.
     *
     * @param password mot de passe saisi
     * @return le message d'erreur, vide si le mot de passe est acceptable
     */
    public static Optional<String> passwordError(String password) {
        if (StringUtils.isEmpty(password)) {
            return Optional.of("Le mot de passe est obligatoire");
        } else if (password.length() < PASSWORD_MIN_LENGTH) {
            return Optional.of("Le mot de passe doit comporter au moins %d caractères".formatted(PASSWORD_MIN_LENGTH));
        }
        return Optional.empty();
    }

    /**
     * Indique si l'adresse est un e-mail syntaxiquement valide.
     *
     * @param email adresse à contrôler
     * @return {@code true} si l'adresse est valide
     */
    public static boolean isValidEmail(String email) {
        try {
            new InternetAddress(email).validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }
}
