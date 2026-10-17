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

import fr.inrae.fishola.CarnetConfiguration;
import fr.inrae.fishola.database.RegistrationSettingDao;
import fr.inrae.fishola.exceptions.AccessDeniedException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Règle d'ouverture de l'inscription libre (#192). Hors mode bêta-testeurs
 * (recette), l'inscription est toujours ouverte ; en mode bêta-testeurs
 * (production), elle suit le réglage de l'administrateur national, fermée par
 * défaut.
 */
@Singleton
public class RegistrationPolicy {

    @Inject
    protected CarnetConfiguration config;

    @Inject
    protected RegistrationSettingDao registrationSettingDao;

    /** @return {@code true} si le mode bêta-testeurs est actif sur cet environnement */
    public boolean isBetaTestersMode() {
        return config.betaTestersMode();
    }

    /** @return {@code true} si un pêcheur peut créer son compte depuis l'app */
    public boolean isRegistrationOpen() {
        return !config.betaTestersMode() || registrationSettingDao.isRegistrationEnabled();
    }

    /**
     * Refuse une action propre au mode bêta-testeurs (réglage de l'inscription,
     * import de comptes) sur un environnement où il est inactif.
     */
    public void requireBetaTestersMode() {
        if (!config.betaTestersMode()) {
            AccessDeniedException.throwNew("Mode bêta-testeurs inactif sur cet environnement");
        }
    }
}
