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
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.exceptions.AccessDeniedException;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import fr.inrae.fishola.exceptions.NotFoundException;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.ImageHelper;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

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

    @GET
    @Path("/for-lake/{lakeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, List<PicturePerTripBean>> allPicturesForLake(@PathParam("lakeId") String lakeId) {
        Optional<List<UUID>> lakesFilter = Optional.empty();
        if (lakeId != null && !lakeId.isEmpty()) {
            List<UUID> lakeIds = new ArrayList<>();
            lakeIds.add(UUID.fromString(lakeId));
            lakesFilter = Optional.of(lakeIds);
        }
        return this.doGetAllPicturesForLake(lakesFilter);
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, List<PicturePerTripBean>> allPictures() {
        return this.doGetAllPicturesForLake(Optional.empty());
    }

    public Map<Integer, List<PicturePerTripBean>> doGetAllPicturesForLake(Optional<List<UUID>> lakesFilter) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<FisholaUser> user = usersDao.findById(userId);
        Preconditions.checkArgument(user.isPresent(), "No user found");
        LocalDateTime year = user.get().getCreatedOn().minusYears(1);
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, List<PicturePerTripBean>> picturesPerYear = new LinkedHashMap<>();
        while (year.getYear() <= now.getYear()) {
            List<PicturePerTripBean> picturesPerTripForYear = tripsDao.getPicturesPerTripForYearAndLakes(userId, year.getYear(), lakesFilter);
            if (!picturesPerTripForYear.isEmpty()) {
                picturesPerYear.put(year.getYear(), picturesPerTripForYear);
            }
            year = year.plusYears(1);
        }
        return picturesPerYear;
    }


    @PUT
    @Path("/{catchId}/{order}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setPictureWithOrder(@PathParam("catchId") UUID catchId,
                                        @PathParam("order") int order,
                                        String content) {

        if (log.isDebugEnabled()) {
            log.debugf("Réception d'une image pour la capture : %s (order=%d)", catchId, order);
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

        byte[] jpegBytes = ImageHelper.base64ImageToJpegBytes(base64Image, config.rawImageQuality());
        catchsDao.setPicture(catchId, order, jpegBytes);

        deletePreview(catchId);
        deletePreview(catchId, Optional.of(order).map(String::valueOf));

        Response response = noContent(userIdAndRenewal);
        return response;
    }

    protected void deletePreview(UUID catchId) {
        deletePreview(catchId, Optional.empty());
    }

    protected void deletePreview(UUID catchId, Optional<String> order) {
        File file = getPreviewFile(catchId, order);
        if (file.exists() && !file.delete()) {
            log.errorf("Impossible de supprimer la preview: %s (%s)", file.getAbsolutePath(), order);
        }
    }

    @PUT
    @Path("/{catchId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated // Conservé pour la rétrocompatibilité avec le mode galerie
    public Response setPicture(@PathParam("catchId") UUID catchId,
                               String content) {

        if (log.isDebugEnabled()) {
            log.infof("Réception d'une image pour la capture '%s' sur le endpoint de rétrocompatibilité", catchId);
        }

        Response response = setPictureWithOrder(catchId, 0, content);
        return response;
    }

    @PUT
    @Path("/measure/{catchId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMeasurementPicture(@PathParam("catchId") UUID catchId,
                                          String content) {

        if (log.isDebugEnabled()) {
            log.infof("Réception d'une image de mesure pour la capture '%s'", catchId);
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

        byte[] jpegBytes = ImageHelper.base64ImageToJpegBytes(base64Image, config.rawImageQuality());
        catchsDao.setMeasurementPicture(catchId, jpegBytes);

        deletePreview(catchId);
        deletePreview(catchId, Optional.of("measure"));

        Response response = noContent(userIdAndRenewal);
        return response;
    }

    @GET
    @Path("/{catchId}/{order}")
    @Produces("image/jpeg")
    public Response getPictureWithOrder(@PathParam("catchId") UUID catchId,
                                        @PathParam("order") int order) {

        // XXX AThimel 22/07/2020 Faut-il sécuriser l'accès aux images ?

        Optional<byte[]> bytes = catchsDao.getPicture(catchId, order);

        Response response = bytes.map(this::wrapAsStreamingOutput)
                .map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
        return response;
    }

    @GET
    @Path("/{catchId}")
    @Produces("image/jpeg")
    @Deprecated // Conservé pour rétro compatibilité
    public Response getPicture(@PathParam("catchId") UUID catchId) {
        Response response = getPictureWithOrder(catchId, 0);
        return response;
    }

    @GET
    @Path("/measure/{catchId}")
    @Produces("image/jpeg")
    public Response getMeasurementPicture(@PathParam("catchId") UUID catchId) {

        // XXX AThimel 22/07/2020 Faut-il sécuriser l'accès aux images ?

        Optional<byte[]> bytes = catchsDao.getMeasurementPicture(catchId);

        Response response = bytes.map(this::wrapAsStreamingOutput)
                .map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
        return response;
    }

    protected File getPreviewFile(UUID catchId, Optional<String> addition) {
        File folder = config.getPicturesPreviewFolder();
        File subFolder = new File(folder, catchId.toString().substring(0, 2));
        if (subFolder.mkdirs() && log.isInfoEnabled()) {
            log.infof("Création du sous dossier : %s", subFolder.getAbsolutePath());
        }

        String fileNameWithoutExtension = catchId.toString();
        if (addition.isPresent()) {
            fileNameWithoutExtension += "-" + addition.get();
        }
        String fileName = String.format("%s.jpeg", fileNameWithoutExtension);
        File result = new File(subFolder, fileName);

        if (log.isDebugEnabled()) {
            log.debugf("Miniature (%s,%s) =>%s", catchId, addition, result.getAbsolutePath());
        }

        return result;
    }

    @GET
    @Path("/{catchId}/preview")
    @Produces("image/jpeg")
    public Response getPicturePreview(@PathParam("catchId") UUID catchId) {

        // XXX AThimel 22/07/2020 Faut-il sécuriser l'accès aux images ?

        Preconditions.checkArgument(catchId != null, "Identifiant de capture manquant");

        File file = getPreviewFile(catchId, Optional.empty());

        if (!file.exists()) {

            Optional<byte[]> bytes = catchsDao.getLastPicture(catchId);

            NotFoundException.check(bytes.isPresent());

            writeImageToFile(file, bytes.get());

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

    @GET
    @Path("/{catchId}/preview/{order}")
    @Produces("image/jpeg")
    public Response getPicturePreview(@PathParam("catchId") UUID catchId,
                                      @PathParam("order") int order) {

        // XXX AThimel 22/07/2020 Faut-il sécuriser l'accès aux images ?

        Preconditions.checkArgument(catchId != null, "Identifiant de capture manquant");

        File file = getPreviewFile(catchId, Optional.of(order).map(String::valueOf));

        if (!file.exists()) {

            Optional<byte[]> bytes = catchsDao.getPicture(catchId, order);

            NotFoundException.check(bytes.isPresent());

            writeImageToFile(file, bytes.get());

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

    @GET
    @Path("/measure/{catchId}/preview")
    @Produces("image/jpeg")
    public Response getMeasurementPicturePreview(@PathParam("catchId") UUID catchId) {

        Preconditions.checkArgument(catchId != null, "Identifiant de capture manquant");

        File file = getPreviewFile(catchId, Optional.of("measure"));

        if (!file.exists()) {

            Optional<byte[]> bytes = catchsDao.getMeasurementPicture(catchId);

            NotFoundException.check(bytes.isPresent());

            writeImageToFile(file, bytes.get());

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

    protected void writeImageToFile(File file, byte[] bytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
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
                    log.debugf("On réduit à %d%s : %dx%d -> %dx%d", percent, "%", width, height, newWidth, newHeight);
                }
                ResampleOp resizeOperation = new ResampleOp(newWidth, newHeight);
                resizeOperation.setFilter(ResampleFilters.getLanczos3Filter());
                scaledImage = resizeOperation.filter(rawImage, null);
            }

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ImageHelper.imageToStream(scaledImage, "jpeg", .95f, fileOutputStream);
        } catch (IOException ioe) {
            throw new FisholaTechnicalException("Impossible de d'écrire l'image vers un fichier", ioe);
        }
    }

    @DELETE
    @Path("/{catchId}/{order}")
    public void deletePicture(@PathParam("catchId") UUID catchId,
                              @PathParam("order") int order) {

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        Catch existingCatch = catchsDao.getCatch(catchId);
        NotFoundException.check(existingCatch != null, "Pas de capture trouvée avec l'ID " + catchId);
        Trip existingTrip = tripsDao.getTrip(existingCatch.getTripId());
        Preconditions.checkState(existingTrip != null, "Pas de sortie trouvée pour la capture " + catchId);
        AccessDeniedException.check(existingTrip.getOwnerId().equals(userId));

        AccessDeniedException.check(tripResource.isStillModifiable(existingTrip), "Il n'est plus possible de modifier la sortie " + existingTrip.getId());

        catchsDao.deletePicture(catchId, order);

        // Par sécurité, on supprime la miniature aussi
        deletePreview(catchId);
        deletePreview(catchId, Optional.of(order).map(String::valueOf));
    }

}
