package fr.inrae.fishola.rest.imports.carnet;

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
import fr.inrae.fishola.rest.imports.ManualResultBean;
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
 * Saisie manuelle opérateur, format « carnet volontaire » (#143) : une sortie + ses
 * captures, contrôlée (mêmes règles que l'import dédié). Pendant de
 * {@link fr.inrae.fishola.rest.imports.ManualEntryResource} (#72).
 */
@Path("/api/v1/admin/manual-entries/carnet-volontaire")
@Produces(MediaType.APPLICATION_JSON)
public class CarnetVolontaireManualEntryResource extends AbstractFisholaResource {

    @Inject
    protected CarnetVolontaireManualEntryService manualEntryService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "manual_entry.create.carnet_volontaire", entityType = "trip")
    public Response create(CarnetVolontaireTripBean bean) {
        FisholaAdmin admin = checkIsStaff();

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
