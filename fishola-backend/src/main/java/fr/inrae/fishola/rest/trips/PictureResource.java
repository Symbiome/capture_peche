package fr.inrae.fishola.rest.trips;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import com.google.common.base.Preconditions;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.exceptions.AccessDeniedException;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import fr.inrae.fishola.exceptions.NotFoundException;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.ImageHelper;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/pictures")
public class PictureResource extends AbstractFisholaResource {

    @Inject
    protected Logger log;

    @Inject
    protected TripsDao tripsDao;

    @Inject
    protected CatchsDao catchsDao;

    @Inject
    protected TripResource tripResource;

    @PUT
    @Path("/{catchId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setPicture(@PathParam("catchId") UUID catchId,
                               String content) {

        if (log.isDebugEnabled()) {
            log.debugv("Réception d'une image pour la capture : %s", catchId);
        }

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        Catch existingCatch = catchsDao.getCatch(catchId);
        NotFoundException.check(existingCatch != null, "Pas de capture trouvée avec l'ID " + catchId);
        Trip existingTrip = tripsDao.getTrip(existingCatch.getTripId());
        Preconditions.checkState(existingTrip != null, "Pas de sortie trouvée pour la capture " + catchId);
        AccessDeniedException.check(existingTrip.getOwnerId().equals(userId));

        AccessDeniedException.check(tripResource.isStillModifiable(existingTrip), "Il n'est plus possible de modifier la sortie " + existingTrip.getId());

        // tokenize the data
        String[] contentSplitted = content.split(",");
        String base64Image = contentSplitted[1];

        byte[] bytes = Base64.getDecoder().decode(base64Image);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            BufferedImage image = ImageIO.read(bis);
            bis.close();

            image = ImageHelper.removeAlphaIfPresent(image);

//            Set<String> formats = ImmutableSet.of("jpeg");
//            for (String format : formats) {
//                for (float quality = 1f; quality >= 0.90f; quality -= 0.01f) {
//                    // write the image to a file
//                    byte[] testBytes = imageToBytes(image, format, quality);
//                    File parent = new File("/tmp/taiste-0.01");
//                    parent.mkdirs();
//                    File file = new File(parent, String.format("%s-%s-%.3f.%s", format, catchId, quality, format));
//                    Files.write(testBytes, file);
//
//                    if (testBytes.length > 0) {
//                        log.info(String.format("%s/%.3f=%dkb en %s", format, quality, testBytes.length / 1024, file.getAbsolutePath()));
//                    }
//                }
//            }

            byte[] jpegBytes = ImageHelper.imageToBytes(image, "jpeg", config.rawImageQuality());
            if (jpegBytes.length > 0) {
                log.infof("Pas de soucis pour: %s", image);
                log.infof("Taille: %dkb", jpegBytes.length / 1024);
            }

            Preconditions.checkState(jpegBytes.length > 0, "Contenu vide pour l'image : " + image);
            catchsDao.setPicture(catchId, jpegBytes);

            File file = getPreviewFile(catchId);
            if (file.exists() && !file.delete()) {
                log.errorf("Impossible de supprimer la preview: %s", file.getAbsolutePath());
            }

        } catch (IOException ioe) {
           throw new FisholaTechnicalException("Impossible de lire l'image", ioe);
        }

        Response response = noContent(userIdAndRenewal);
        return response;
    }

    @GET
    @Path("/{catchId}")
    @Produces("image/jpeg")
    public Response getPicture(@PathParam("catchId") UUID catchId) {

        // XXX AThimel 22/07/2020 Faut-il sécuriser l'accès aux images ?

        Optional<byte[]> bytes = catchsDao.getPicture(catchId);

        Response response = bytes.map(this::wrapAsStreamingOutput)
                .map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
        return response;
    }

    protected File getPreviewFile(UUID catchId) {
        File folder = config.getPicturesPreviewFolder();
        File subFolder = new File(folder, catchId.toString().substring(0, 2));
        if (subFolder.mkdirs() && log.isInfoEnabled()) {
            log.infof("Création du sous dossier : %s", subFolder.getAbsolutePath());
        }

        String fileName = String.format("%s.jpeg", catchId);
        File result = new File(subFolder, fileName);

        return result;
    }

    @GET
    @Path("/{catchId}/preview")
    @Produces("image/jpeg")
    public Response getPicturePreview(@PathParam("catchId") UUID catchId) {

        // XXX AThimel 22/07/2020 Faut-il sécuriser l'accès aux images ?

        Preconditions.checkArgument(catchId != null, "Identifiant de capture manquant");

        File file = getPreviewFile(catchId);

        if (!file.exists()) {

            Optional<byte[]> bytes = catchsDao.getPicture(catchId);

            NotFoundException.check(bytes.isPresent());

            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes.get());
                BufferedImage rawImage = ImageIO.read(inputStream);
                BufferedImage scaledImage;

                int width = rawImage.getWidth();
                int height = rawImage.getHeight();

                int percent = 100;

                int threshold = 285 * 2; // Double d'une largeur classique d'écran
                if (width > threshold) {
                    percent = threshold * 100 / width;
                }

                if (percent == 100) {
                    scaledImage = rawImage;
                } else {
                    int newWidth = width * percent / 100;
                    int newHeight = height * percent / 100;
                    if (log.isDebugEnabled()) {
                        log.debugf("On réduit à %d%% : %dx%d -> %dx%d", percent, width, height, newWidth, newHeight);
                    }
                    ResampleOp resizeOperation = new ResampleOp(newWidth, newHeight);
                    resizeOperation.setFilter(ResampleFilters.getLanczos3Filter());
                    scaledImage = resizeOperation.filter(rawImage, null);
                }

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                ImageHelper.imageToStream(scaledImage, "jpeg", .95f, fileOutputStream);
            } catch (IOException ioe) {
                throw new FisholaTechnicalException("Impossible de lire l'image", ioe);
            }

        }

        Response.ResponseBuilder builder = Response.ok(file);

        // TODO AThimel 12/02/2020 Cache !
//        CacheControl cc = new CacheControl();
//        cc.setMaxAge(86400);
//        cc.setPrivate(true);
//        builder.cacheControl(cc);

        Response result = builder.build();

        return result;
    }

}
