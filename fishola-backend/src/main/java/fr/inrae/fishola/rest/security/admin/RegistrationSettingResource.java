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

import fr.inrae.fishola.database.RegistrationSettingDao;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.audit.Audited;
import fr.inrae.fishola.rest.security.RegistrationPolicy;
import fr.inrae.fishola.rest.security.RegistrationStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Ouverture et fermeture de l'inscription libre (#192), réservées à
 * l'administrateur national et au mode bêta-testeurs (production). La
 * lecture est publique :
 * {@code GET /api/v1/security/registration}.
 */
@Path("/api/v1/admin/registration")
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationSettingResource extends AbstractCarnetResource {

    @Inject
    protected RegistrationSettingDao registrationSettingDao;

    @Inject
    protected RegistrationPolicy registrationPolicy;

    /**
     * Ouvre ou ferme l'inscription libre.
     *
     * @param status état souhaité
     * @return l'état enregistré
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "registration.update", entityType = "registration_setting")
    public RegistrationStatus update(RegistrationStatus status) {
        checkIsNationalAdmin();
        registrationPolicy.requireBetaTestersMode();
        if (status == null) {
            throw new BadRequestException("État d'inscription manquant");
        }
        registrationSettingDao.setRegistrationEnabled(status.enabled);
        return new RegistrationStatus(registrationPolicy.isRegistrationOpen());
    }
}
