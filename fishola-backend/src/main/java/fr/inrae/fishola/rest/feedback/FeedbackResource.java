package fr.inrae.fishola.rest.feedback;

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

import com.google.common.collect.ImmutableMap;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import fr.inrae.fishola.mails.FisholaMail;
import fr.inrae.fishola.mails.FisholaMailAttachment;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.mails.ImmutableFisholaMailAttachment;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.ImageHelper;
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
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Path("/api/v1/feedback")
public class FeedbackResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(FeedbackResource.class);

    @Inject
    protected MailService mailService;

    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void newFeedback(Feedback bean) {

        log.info("Réception d'un feedback " + bean);

        Optional<byte[]> screenshotBytes = Optional.empty();

        if (bean.screenshot().isPresent()) {
            // tokenize the data
            String[] contentSplitted = bean.screenshot().get().split(",");
            String base64Image = contentSplitted[1];

            byte[] bytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            try {
                BufferedImage image = ImageIO.read(bis);
                bis.close();

                byte[] pngBytes = ImageHelper.imageToBytes(image, "png", .6f);

                screenshotBytes = Optional.of(pngBytes);

            } catch (IOException ioe) {
                throw new FisholaTechnicalException("Impossible de lire l'image", ioe);
            }
        }

        ImmutableFeedback feedback = ImmutableFeedback.builder()
                .from(bean)
                .backendVersion(config.getFullVersion())
                .build();

        FisholaMail fisholaMail = toFisholaMail(feedback, screenshotBytes);
        mailService.sendMail(fisholaMail);

    }

    protected FisholaMail toFisholaMail(Feedback feedback, Optional<byte[]> screenshotBytes) {
        String description = feedback.description().orElse("Pas de description");
        String screenshot = screenshotBytes.map(file -> "L'utilisateur a fournit une capture d'écran (cf PJ)")
                .orElse("L'utilisation n'a pas fournit de capture d'écran");

        ImmutableMap<String, Object> args = ImmutableMap.of(
                "feedback", feedback,
                "descriptionText", description,
                "screenshotText", screenshot
        );

        ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                "emails/new-feedback.html",
                args);
        builder.subject("Nouveau feedback : " + feedback.category());
        builder.addTos(config.getFeedbackMailTo());
        if (screenshotBytes.isPresent()) {
            byte[] bytes = screenshotBytes.get();
            FisholaMailAttachment attachment = ImmutableFisholaMailAttachment.builder()
                    .bytes(bytes)
                    .name(String.format("feedback-%s.png", feedback.id().toString()))
                    .type(com.google.common.net.MediaType.PNG)
                    .build();
            builder.addAttachments(attachment);
        }
        FisholaMail result = builder.build();
        return result;
    }

}
