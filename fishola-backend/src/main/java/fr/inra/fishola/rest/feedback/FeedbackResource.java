package fr.inra.fishola.rest.feedback;

import fr.inra.fishola.exceptions.FisholaTechnicalException;
import fr.inra.fishola.rest.ImageHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

@Path("/api/v1/feedback")
public class FeedbackResource {

    private static final Log log = LogFactory.getLog(FeedbackResource.class);

    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void newFeedback(FeedbackBean bean) {

        log.info("Réception d'un feedback " + bean);

        if (bean.withPicture && bean.picture.isPresent()) {
            // tokenize the data
            String[] contentSplitted = bean.picture.get().split(",");
            String base64Image = contentSplitted[1];

            byte[] bytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            try {
                BufferedImage image = ImageIO.read(bis);
                bis.close();

                File tempFile = File.createTempFile("feedback-", ".png");
                FileOutputStream fileOutputStream = new FileOutputStream(tempFile);

                ImageHelper.imageToStream(image, "png", .6f, fileOutputStream);

                log.info("Capture créée dans " + tempFile.getAbsolutePath());

            } catch (IOException ioe) {
                throw new FisholaTechnicalException("Impossible de lire l'image", ioe);
            }
        }

    }

}
