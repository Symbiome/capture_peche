package fr.inra.fishola.rest.trips;

import com.google.common.base.Preconditions;
import fr.inra.fishola.database.CatchsDao;
import fr.inra.fishola.database.TripsDao;
import fr.inra.fishola.entities.tables.pojos.Catch;
import fr.inra.fishola.entities.tables.pojos.Trip;
import fr.inra.fishola.exceptions.AccessDeniedException;
import fr.inra.fishola.rest.AbstractFisholaResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/pictures")
public class PictureResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(PictureResource.class);

    @Inject
    protected TripsDao tripsDao;

    @Inject
    protected CatchsDao catchsDao;

    @Inject
    protected TripResource tripResource;

    @PUT
    @Path("/{catchId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response setPicture(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("catchId") UUID catchId, String content) {

        UUID userId = getUserId(cookie);

        Catch existingCatch = catchsDao.getCatch(catchId);
        Trip existingTrip = tripsDao.getTrip(existingCatch.getTripId());
        Preconditions.checkState(existingTrip != null);
        AccessDeniedException.check(existingTrip.getOwnerId().equals(userId));

        AccessDeniedException.check(tripResource.isStillModifiable(existingTrip), "Il n'est plus possible de modifier la sortie");

        if (log.isDebugEnabled()) {
            log.debug("Réception d'une image pour la capture : " + catchId);
        }

        // tokenize the data
        String[] contentSplitted = content.split(",");
        String base64Image = contentSplitted[1];

        byte[] bytes = Base64.getDecoder().decode(base64Image);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            BufferedImage image = ImageIO.read(bis);
            bis.close();

            // write the image to a file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", outputStream);

            byte[] jpegBytes = outputStream.toByteArray();
            catchsDao.setPicture(catchId, jpegBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Response response = Response.ok().build();
        return response;
    }

    @GET
    @Path("/{catchId}")
    @Produces("image/jpeg")
    public Response getPicture(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("catchId") UUID catchId) {

        Optional<byte[]> bytes = catchsDao.getPicture(catchId);

        Response response = bytes.map(this::wrapAsStreamingOutput)
                .map(Response::ok)
                .orElseGet(() -> Response.status(404))
                .build();
        return response;
    }

    protected StreamingOutput wrapAsStreamingOutput(byte[] array) {
        return output -> {
            output.write(array);
            output.flush();
        };
    }

}
