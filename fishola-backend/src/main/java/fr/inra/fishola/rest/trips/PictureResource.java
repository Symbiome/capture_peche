package fr.inra.fishola.rest.trips;

import com.google.common.base.Preconditions;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.database.CatchsDao;
import fr.inra.fishola.database.TripsDao;
import fr.inra.fishola.entities.tables.pojos.Catch;
import fr.inra.fishola.entities.tables.pojos.Trip;
import fr.inra.fishola.exceptions.AccessDeniedException;
import fr.inra.fishola.exceptions.FisholaTechnicalException;
import fr.inra.fishola.rest.AbstractFisholaResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/pictures")
public class PictureResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(PictureResource.class);

    @Inject
    protected FisholaConfiguration config;

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

            image = removeAlphaIfPresent(image);

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

            byte[] jpegBytes = imageToBytes(image, "jpeg", config.getRawImageQuality());
            if (jpegBytes.length > 0) {
                log.info("Pas de soucis pour: " + image);
                log.info("Taille: " + (jpegBytes.length / 1024) + "kb");
            }

            Preconditions.checkState(jpegBytes.length > 0, "Contenu vide pour l'image : " + image);
            catchsDao.setPicture(catchId, jpegBytes);

            File file = getPreviewFile(catchId);
            if (file.exists() && !file.delete() && log.isErrorEnabled()) {
                log.error("Impossible de supprimer la preview: " + file.getAbsolutePath());
            }

        } catch (IOException ioe) {
           throw new FisholaTechnicalException("Impossible de lire l'image", ioe);
        }

        Response response = Response.noContent().build();
        return response;
    }

    protected BufferedImage removeAlphaIfPresent(BufferedImage image) {
        // On gère le cas de photos avec un canal alpha -> on le remplace par du noir
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = copy.createGraphics();
            g2d.setColor(Color.BLACK); // Or what ever fill color you want...
            g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            return copy;
        }
        return image;
    }

    protected byte[] imageToBytes(BufferedImage image, String format, float quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        imageToStream(image, format, quality, outputStream);

        byte[] result = outputStream.toByteArray();
        return result;
    }

    protected void imageToStream(BufferedImage image, String format, float quality, OutputStream outputStream) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Nécessaire pour pouvoir impacter la qualité
        param.setCompressionQuality(quality);
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(imageOutputStream);

        writer.write(null, new IIOImage(image, null, null), param);
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

    protected File getPreviewFile(UUID catchId) {
        File folder = config.getPicturesPreviewFolder();
        File subFolder = new File(folder, catchId.toString().substring(0, 2));
        if (subFolder.mkdirs() && log.isInfoEnabled()) {
            log.info("Création du sous dossier : " + subFolder.getAbsolutePath());
        }

        String fileName = String.format("%s.jpeg", catchId);
        File result = new File(subFolder, fileName);

        return result;
    }

    @GET
    @Path("/{catchId}/preview")
    @Produces("image/jpeg")
    public Response getPicturePreview(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, @PathParam("catchId") UUID catchId) {

        Preconditions.checkArgument(catchId != null);

        File file = getPreviewFile(catchId);

        if (!file.exists()) {

            Optional<byte[]> bytes = catchsDao.getPicture(catchId);

            if (bytes.isEmpty()) {
                return Response.status(404).build();
            }

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
                        log.debug(String.format("On réduit à %d%% : %dx%d -> %dx%d", percent, width, height, newWidth, newHeight));
                    }
                    ResampleOp resizeOperation = new ResampleOp(newWidth, newHeight);
                    resizeOperation.setFilter(ResampleFilters.getLanczos3Filter());
                    scaledImage = resizeOperation.filter(rawImage, null);
                }

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                imageToStream(scaledImage, "jpeg", .95f, fileOutputStream);
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

    protected StreamingOutput wrapAsStreamingOutput(byte[] array) {
        return output -> {
            output.write(array);
            output.flush();
        };
    }

}
