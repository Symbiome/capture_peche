package fr.inrae.fishola.rest.security;

/*-
 * #%L
 * Carnet de pêche :: Backend
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

import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import jakarta.inject.Inject;

import java.util.Optional;

public class AbstractSecurityCarnetResource  extends AbstractCarnetResource {
    protected static final String CLAIM_EMAIL = "email";
    protected static final String CLAIM_PASSWORD_HASHED = "passwordHashed";

    @Inject
    protected MailService mailService;

    protected Optional<String> validatePassword(String password) {
        return AccountRules.passwordError(password);
    }

    protected boolean isEmailInValidFormat(String email) {
        boolean valid = AccountRules.isValidEmail(email);
        if (!valid && log.isInfoEnabled()) {
            log.infof("'%s' does not seem to be a valid email address", email);
        }
        return valid;
    }
}
