package fr.inrae.fishola.rest.imports;

/*-
 * #%L
 * Fishola :: Backend
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

import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Saisie manuelle opérateur (#72, CdC §3.1.5.3) : une sortie + ses captures, contrôlée
 * (mêmes règles que l'import). Ouverte au staff via {@code checkIsAdmin}, cloisonnée par
 * périmètre (#63), et journalisée ({@code @Audited}).
 */
@Path("/api/v1/admin/manual-entries")
@Produces(MediaType.APPLICATION_JSON)
public class ManualEntryResource extends AbstractFisholaResource {

    @Inject
    protected ManualEntryService manualEntryService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "manual_entry.create", entityType = "trip")
    public Response create(ManualTripBean bean) {
        FisholaAdmin admin = checkIsStaff();

        // Cloisonnement (#63) : opérateur/admin régional borné à son périmètre ; national non restreint.
        Set<UUID> allowedWaterEntities = admin.getIsNationalAdmin()
                ? null
                : adminDao.getAllowedWaterEntities(admin.getId());

        ManualResultBean result = manualEntryService.submit(bean, allowedWaterEntities, LocalDate.now());

        Response.Status status = result.errors.isEmpty()
                ? Response.Status.CREATED
                : Response.Status.BAD_REQUEST;
        return Response.status(status).entity(result).build();
    }
}
