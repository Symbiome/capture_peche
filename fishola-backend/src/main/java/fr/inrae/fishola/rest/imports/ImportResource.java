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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.UUID;

/**
 * Import CSV de masse opérateur (#71, CdC §3.1.5.3). Le fichier est envoyé en corps brut
 * ({@code application/octet-stream}) ; le nom et le mode sont passés en query params.
 * Ouvert aux comptes staff (admin comme opérateur) via {@code checkIsAdmin} ; l'acteur est
 * journalisé ({@code @Audited}) et enregistré dans {@code import_job.created_by}.
 */
@Path("/api/v1/admin/imports")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource extends AbstractFisholaResource {

    @Inject
    protected ImportService importService;

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Audited(value = "import.csv", entityType = "import_job")
    public Response importCsv(byte[] fileBytes,
                              @QueryParam("filename") @DefaultValue("import.csv") String filename,
                              @QueryParam("mode") @DefaultValue(ImportService.MODE_PARTIAL) String mode) {
        FisholaAdmin admin = checkIsStaff();
        if (fileBytes == null || fileBytes.length == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Fichier vide").build();
        }
        String name = StringUtils.isBlank(filename) ? "import.csv" : filename;

        // Cloisonnement (#63) : un opérateur/admin régional est borné à son périmètre ;
        // un administrateur national n'est pas restreint (null = pas de filtre).
        Set<UUID> allowedWaterEntities = admin.getIsNationalAdmin()
                ? null
                : adminDao.getAllowedWaterEntities(admin.getId());

        ImportResultBean result = importService.run(fileBytes, name, mode, admin.getId(), allowedWaterEntities);

        // Idempotence : un fichier déjà importé est signalé sans réécriture.
        Response.Status httpStatus = result.duplicate ? Response.Status.CONFLICT : Response.Status.OK;
        return Response.status(httpStatus).entity(result).build();
    }
}
