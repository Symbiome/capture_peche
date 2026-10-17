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

import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.audit.Audited;
import fr.inrae.fishola.rest.security.RegistrationPolicy;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Import CSV des comptes des bêta-testeurs (#192), réservé à l'administrateur
 * national et au mode bêta-testeurs (production). Format : {@code email;prenom;nom;profil;perimetre;mot_de_passe}.
 */
@Path("/api/v1/admin/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class AccountImportResource extends AbstractCarnetResource {

    @Inject
    protected AccountImportService importService;

    @Inject
    protected RegistrationPolicy registrationPolicy;

    /**
     * Crée les comptes du fichier, sans dupliquer ceux qui existent déjà.
     *
     * @param fileBytes contenu du CSV
     * @return 200 et le rapport ligne par ligne, 400 si le fichier est vide ou
     * s'il manque des colonnes
     */
    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Audited(value = "accounts.import", entityType = "account")
    public Response importAccounts(byte[] fileBytes) {
        checkIsNationalAdmin();
        registrationPolicy.requireBetaTestersMode();
        AccountImportReport report = fileBytes == null || fileBytes.length == 0
                ? AccountImportReport.fileError("Fichier vide")
                : importService.run(fileBytes);
        Response.Status status = report.fileError == null ? Response.Status.OK : Response.Status.BAD_REQUEST;
        return Response.status(status).entity(report).build();
    }
}
