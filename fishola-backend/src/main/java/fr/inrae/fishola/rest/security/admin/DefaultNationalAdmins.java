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

import fr.inrae.fishola.database.AdminDao;
import fr.inrae.fishola.rest.security.AccountRules;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Crée au démarrage les administrateurs nationaux par défaut (#192), déclarés
 * dans {@code fishola.default-national-admins} (« email:motdepasse »). Les mots
 * de passe viennent du vault Ansible de l'environnement. Un compte existant
 * n'est jamais modifié : son titulaire a pu changer de mot de passe.
 */
@Singleton
public class DefaultNationalAdmins {

    @Inject
    protected AdminDao adminDao;

    @Inject
    protected Logger log;

    /**
     * Crée les administrateurs nationaux absents de la base.
     *
     * @param entries comptes « email:motdepasse »
     * @return les e-mails des comptes créés
     */
    @Transactional
    public List<String> createMissing(List<String> entries) {
        List<String> created = new ArrayList<>();
        for (String entry : entries) {
            String email = StringUtils.trimToEmpty(StringUtils.substringBefore(entry, ":")).toLowerCase();
            String password = StringUtils.substringAfter(entry, ":");
            if (!AccountRules.isValidEmail(email) || AccountRules.passwordError(password).isPresent()) {
                log.errorf("Administrateur national par défaut ignoré, e-mail ou mot de passe invalide : '%s'", email);
            } else if (adminDao.findByEmail(email).isEmpty()) {
                adminDao.create(email, adminDao.hashPassword(password), true, true, false, new String[0]);
                log.infof("Administrateur national par défaut créé : %s", email);
                created.add(email);
            }
        }
        return created;
    }
}
