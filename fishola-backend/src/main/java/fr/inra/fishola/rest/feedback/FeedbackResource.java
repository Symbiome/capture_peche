package fr.inra.fishola.rest.feedback;

import com.google.common.collect.ImmutableMap;
import fr.inra.fishola.exceptions.FisholaTechnicalException;
import fr.inra.fishola.mails.FisholaMail;
import fr.inra.fishola.mails.ImmutableFisholaMail;
import fr.inra.fishola.mails.MailService;
import fr.inra.fishola.rest.ImageHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import javax.inject.Inject;
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
import java.util.Optional;

@Path("/api/v1/feedback")
public class FeedbackResource {

    private static final Log log = LogFactory.getLog(FeedbackResource.class);

    @Inject
    protected MailService mailService;

    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void newFeedback(Feedback bean) {

        log.info("Réception d'un feedback immuable " + bean);

        Optional<File> screenshotFile = Optional.empty();

        if (bean.screenshot().isPresent()) {
            // tokenize the data
            String[] contentSplitted = bean.screenshot().get().split(",");
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

                screenshotFile = Optional.of(tempFile);

            } catch (IOException ioe) {
                throw new FisholaTechnicalException("Impossible de lire l'image", ioe);
            }
        }

        FisholaMail fisholaMail = toFisholaMail(bean, screenshotFile);
        mailService.sendMail(fisholaMail);

    }

    protected FisholaMail toFisholaMail(Feedback feedback, Optional<File> screenshotFile) {
        ImmutableMap<String, Object> args = ImmutableMap.of(
                "feedback", feedback,
                "description", feedback.description().orElse("Pas de description"),
                "screenshotFilePath", screenshotFile.map(File::getAbsolutePath).orElse("L'utilisation n'a pas fournit de capture d'écran")
        );
        ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                "emails/new-feedback.html",
                args);
        builder.subject("Nouveau feedback : " + feedback.category());
        builder.addTos("thimel@codelutin.com");
        FisholaMail result = builder.build();
        return result;
    }

}
