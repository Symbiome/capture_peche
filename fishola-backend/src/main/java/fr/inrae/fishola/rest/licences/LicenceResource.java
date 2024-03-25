package fr.inrae.fishola.rest.licences;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.database.FishingLicencesDao;
import fr.inrae.fishola.entities.enums.LicenceType;
import fr.inrae.fishola.entities.tables.pojos.FisholaUserLicences;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/licences")
public class LicenceResource extends AbstractFisholaResource {

    @Inject
    protected FishingLicencesDao fishingLicencesDao;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLicences() {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        Response response;
        List<LicenceResponseBean> licences = fishingLicencesDao.getLicencesByUser(userId);
        response = wrapEntity(licences, userIdAndRenewal);
        return response;
    }

    @GET
    @Path("/{licenceId}")
    public Response getLicence(@PathParam("licenceId") UUID licenceId) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();

        Optional<FisholaUserLicences> optionalLicence = fishingLicencesDao.getLicence(licenceId);
        Response.ResponseBuilder responseBuilder;
        if (optionalLicence.isPresent()) {
            FisholaUserLicences licence = optionalLicence.get();
            LicenceType type = licence.getType();
            byte[] bytes = licence.getContent();
            String filename = licence.getName()
                                     .replaceAll("[ ]", "_");
            String mediaType = type == LicenceType.PDF ? "application/pdf" : "application/jpeg";
            String headerPayload = type == LicenceType.PDF ? "filename=\"%s.pdf\"" : "filename=\"%s.jpeg\"";

            responseBuilder = Response.ok(this.wrapAsStreamingOutput(bytes))
                                      .type(mediaType)
                                      .header("Content-Disposition", String.format(headerPayload, filename));

        } else {
            responseBuilder = Response.noContent()
                                      .status(Response.Status.BAD_REQUEST);
        }

        Response response = buildResponse(responseBuilder, userIdAndRenewal);
        return response;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response postLicence(LicenceFromClientBean licenceFromClientBean) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        FisholaUserLicences licence = new FisholaUserLicences();
        byte[] contentAsBytes = Base64.getDecoder().decode(licenceFromClientBean.content);
        licence.setUserId(userId);
        licence.setName(licenceFromClientBean.name);
        licence.setContent(contentAsBytes);
        licence.setType(licenceFromClientBean.type);
        licence.setExpirationDate(licenceFromClientBean.expirationDate);

        Response response;
        try {
            fishingLicencesDao.createLicence(licence);
            if (log.isDebugEnabled()) {
                log.debugf("New fishing licence saved : id=%s; owner=%s", licence.getId(), userId);
            }

            LicenceResponseBean licenceToSend = new LicenceResponseBean();
            licenceToSend.id = licence.getId();
            licenceToSend.type = licence.getType();
            licenceToSend.name = licence.getName();
            licenceToSend.userId = licence.getUserId();
            licenceToSend.expirationDate = licence.getExpirationDate();

            response = wrapEntity(licenceToSend, userIdAndRenewal);
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            if (StringUtils.isNotEmpty(e.getMessage())) {
                entity.put("error", e.getMessage());
            } else {
                entity.put("error", "Unexpected error");
            }
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);
            responseBuilder.entity(entity);
            response = buildResponse(responseBuilder, userIdAndRenewal);
        }
        return response;
    }

    @DELETE
    @Path("/{licenceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLicence(@PathParam("licenceId") UUID licenceId) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        try {
            fishingLicencesDao.deleteLicence(licenceId);

            if (log.isDebugEnabled()) {
                log.debugf("Fishing licence deleted : id=%s; owner=%s", licenceId, userId);
            }

            return noContent(userIdAndRenewal);
        } catch (IllegalArgumentException e) {
            Map<String, String> entity = new LinkedHashMap<>();
            if (StringUtils.isNotEmpty(e.getMessage())) {
                entity.put("error", e.getMessage());
            } else {
                entity.put("error", "Illegal argument exception.");
            }
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);
            responseBuilder.entity(entity);
            Response response = buildResponse(responseBuilder, userIdAndRenewal);
            return response;
        }
    }
}
