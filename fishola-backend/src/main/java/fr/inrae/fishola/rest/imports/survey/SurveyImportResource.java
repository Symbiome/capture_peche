package fr.inrae.fishola.rest.imports.survey;

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
import fr.inrae.fishola.rest.imports.ImportResultBean;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
 * Import de masse opérateur, format « enquête terrain » (#144) — classeur XLSX
 * multi-feuilles, ou CSV plat d'une ligne par capture si le nom du fichier finit par
 * « .csv » (#242), en corps brut, nom/mode en query params, staff uniquement, cloisonné
 * par périmètre, journalisé. Même contrat que {@link
 * fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireImportResource} (#143).
 */
@Path("/api/v1/admin/imports/survey")
@Produces(MediaType.APPLICATION_JSON)
public class SurveyImportResource extends AbstractFisholaResource {

    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CSV_MEDIA_TYPE = "text/csv; charset=UTF-8";

    @Inject
    protected SurveyImportService importService;

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Audited(value = "import.xlsx.survey", entityType = "import_job")
    public Response importFile(byte[] fileBytes,
                               @QueryParam("filename") @DefaultValue("enquete.xlsx") String filename,
                               @QueryParam("mode") @DefaultValue(SurveyImportService.MODE_PARTIAL) String mode) {
        FisholaAdmin admin = checkIsStaff();
        if (fileBytes == null || fileBytes.length == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Fichier vide").build();
        }
        String name = StringUtils.isBlank(filename) ? "enquete.xlsx" : filename;

        Set<UUID> allowedWaterEntities = admin.getIsNationalAdmin()
                ? null
                : adminDao.getAllowedWaterEntityIds(admin.getId());

        ImportResultBean result = importService.run(fileBytes, name, mode, admin.getId(), allowedWaterEntities);

        Response.Status httpStatus = result.duplicate ? Response.Status.CONFLICT : Response.Status.OK;
        return Response.status(httpStatus).entity(result).build();
    }

    /**
     * Gabarit à remplir : classeur XLSX à quatre feuilles avec une ligne d'exemple chacune
     * (#207), ou CSV plat avec des lignes d'exemple ({@code format=csv}, #242).
     *
     * @param format {@code xlsx} (défaut) ou {@code csv}
     * @return le gabarit en pièce jointe
     */
    @GET
    @Path("/template")
    @Produces({XLSX_MEDIA_TYPE, CSV_MEDIA_TYPE})
    public Response template(@QueryParam("format") @DefaultValue("xlsx") String format) {
        checkIsStaff();
        boolean csv = "csv".equalsIgnoreCase(format);
        String fileName = csv ? SurveyFlatTemplate.FILE_NAME : SurveyTemplate.FILE_NAME;
        return Response.ok(csv ? SurveyFlatTemplate.build() : SurveyTemplate.build())
                .type(csv ? CSV_MEDIA_TYPE : XLSX_MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }
}
