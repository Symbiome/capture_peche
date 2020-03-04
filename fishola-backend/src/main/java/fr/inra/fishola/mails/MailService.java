package fr.inra.fishola.mails;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.exceptions.FisholaTechnicalException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

@RequestScoped
public class MailService {

    private static final Log log = LogFactory.getLog(MailService.class);

    @Inject
    protected FisholaConfiguration config;

    public ImmutableFisholaMail.Builder newMail(String body) {
        ImmutableFisholaMail.Builder builder = ImmutableFisholaMail.builder()
                .from(config.getMailFrom())
                .body(body);
        return builder;
    }

    public ImmutableFisholaMail.Builder newMailFromTemplate(String templatePath, Map<String, Object> templateParameters) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(templatePath);
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, templateParameters);
        String body = stringWriter.toString();
        ImmutableFisholaMail.Builder builder = newMail(body);
        return builder;
    }

    public ImmutableFisholaMail.Builder newMailFromTemplate(String templatePath, String arg1Name, Object arg1, String arg2Name, Object arg2) {
        ImmutableMap<String, Object> args = ImmutableMap.of(arg1Name, arg1, arg2Name, arg2);
        ImmutableFisholaMail.Builder builder = newMailFromTemplate(templatePath, args);
        return builder;
    }

    private MimeMessage buildMimeMessage(FisholaMail fisholaMail) {

        Properties prop = config.getMailProperties();

        Session session = Session.getInstance(prop /*, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("username", "password");
            }
        }*/);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(fisholaMail.getFrom());
            for (String to : fisholaMail.getTos()) {
                message.addRecipients(Message.RecipientType.TO, to);
            }
            message.setSubject(fisholaMail.getSubject());

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(fisholaMail.getBody(), "text/html;charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            for (FisholaMailAttachment attachment : fisholaMail.getAttachments()) {
                MimeBodyPart attachmentPart = toAttachmentPart(attachment);
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);

        } catch (MessagingException me) {
            throw new FisholaTechnicalException("Unable to build MimeMessage", me);
        }
        return message;
    }

    protected MimeBodyPart toAttachmentPart(FisholaMailAttachment attachment) throws MessagingException {

        byte[] attachmentBytes = attachment.getBytes();
        MediaType attachmentType = attachment.getType();
        String attachmentName = attachment.getName();

        DataSource attachmentDataSource = new ByteArrayDataSource(attachmentBytes, attachmentType.toString());

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setDisposition("attachment");
        attachmentPart.setFileName(attachmentName);
        attachmentPart.setDataHandler(new DataHandler(attachmentDataSource));

        return attachmentPart;
    }

    public void sendMail(FisholaMail mail) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Will send an email to '%s': « %s »", mail.getTos(), mail.getSubject()));
        }

        MimeMessage message = buildMimeMessage(mail);
        try {
            Transport.send(message);
        } catch (MessagingException e) {
            throw new FisholaTechnicalException("Peut pas envoyer le mail", e);
        }
    }

}
