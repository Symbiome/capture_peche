package fr.inrae.fishola.rest.licences;

import fr.inrae.fishola.database.FishingLicencesDao;
import fr.inrae.fishola.entities.enums.LicenceType;
import fr.inrae.fishola.entities.tables.pojos.FisholaUserLicences;
import fr.inrae.fishola.rest.AbstractFisholaResource;

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

// TODO Add authentification.
//  Currently, ownership is not verified : any user can read/create/delete a licence.
@Path("/api/v1/licences")
public class LicenceResource extends AbstractFisholaResource {

    @Inject
    protected FishingLicencesDao fishingLicencesDao;

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLicences(@PathParam("userId") UUID userId) {
        List<LicenceResponseBean> licences = fishingLicencesDao.getLicencesByUser(userId);
        Response response = Response.ok(licences)
                                    .build();
        return response;
    }

    @GET
    @Path("/{userId}/{licenceId}")
    public Response getLicence(@PathParam("userId") UUID userId, @PathParam("licenceId") UUID licenceId) {
        Optional<FisholaUserLicences> optionalLicence = fishingLicencesDao.getLicence(licenceId);

        Response response;
        if (optionalLicence.isPresent()) {
            FisholaUserLicences licence = optionalLicence.get();
            LicenceType type = licence.getType();
            byte[] bytes = licence.getContent();
            String filename = licence.getName()
                                     .replaceAll("[ ]", "_");
            String mediaType = type == LicenceType.PDF ? "application/pdf" : "application/jpeg";
            String headerPayload = type == LicenceType.PDF ? "filename=\"%s.pdf\"" : "filename=\"%s.jpeg\"";
            response = Response.ok(this.wrapAsStreamingOutput(bytes))
                               .type(mediaType)
                               .header("Content-Disposition", String.format(headerPayload, filename))
                               .build();
        } else {
            response = Response.noContent().status(Response.Status.BAD_REQUEST).build();
        }

        return response;
    }

    @POST
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response postLicence(@PathParam("userId") UUID userId, LicenceFromClientBean licenceFromClientBean) {

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
        return Response.ok(licenceToSend).build();
    }

    @DELETE
    @Path("/{userId}/{licenceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLicence(@PathParam("userId") UUID userId, @PathParam("licenceId") UUID licenceId) {

        try {
            fishingLicencesDao.deleteLicence(licenceId);

            if (log.isDebugEnabled()) {
                log.debugf("Fishing licence deleted : id=%s; owner=%s", licenceId, userId);
            }

            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }
}
