package fr.inrae.fishola.rest.licences;

import fr.inrae.fishola.database.FishingLicencesDao;
import fr.inrae.fishola.entities.enums.LicenceType;
import fr.inrae.fishola.entities.tables.pojos.FisholaUserLicences;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

        Response response = wrapEntity(licenceToSend, userIdAndRenewal);
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
            entity.put("error", e.getMessage());
            Response.ResponseBuilder responseBuilder = Response.noContent()
                                                               .status(Response.Status.BAD_REQUEST);
            Response response = buildResponse(responseBuilder, userIdAndRenewal);
            return response;
        }
    }
}
