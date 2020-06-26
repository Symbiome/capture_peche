package fr.inrae.fishola.mails;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import io.quarkus.scheduler.Scheduled;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequestScoped
public class MailService {

    private static final Log log = LogFactory.getLog(MailService.class);

    @Inject
    protected FisholaConfiguration config;

    protected static final ConcurrentLinkedQueue<FisholaMail> PENDING_EMAILS = new ConcurrentLinkedQueue<>();

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
            log.info(String.format("Saving email to send to '%s': « %s »", mail.getTos(), mail.getSubject()));
        }

        FisholaMail safeMail = mail.pendingSince().isPresent() ? mail : ImmutableFisholaMail.builder()
                .fromInstance(mail)
                .pendingSince(LocalDateTime.now())
                .build();

        PENDING_EMAILS.add(safeMail);

    }

    @Scheduled(every="30s")
    protected void sendPendingEmails() {

        int pendingEmailsCount = PENDING_EMAILS.size();
        if (log.isInfoEnabled() && pendingEmailsCount > 0) {
            log.info(String.format("Trying to send %d pending emails", pendingEmailsCount));
        }

        Iterator<FisholaMail> iterator = PENDING_EMAILS.iterator();
        while (iterator.hasNext()) {
            FisholaMail fisholaMail = iterator.next();
            try {
                sendMail0(fisholaMail);
                // Envoi de mail OK, on le supprime de la liste
                iterator.remove();
            } catch (Exception eee) {
                Preconditions.checkState(fisholaMail.pendingSince().isPresent(), "FisholaMail sans pendingSince: " + fisholaMail);
                Duration pendingDuration = Duration.between(fisholaMail.pendingSince().get(), LocalDateTime.now());
                log.warn(String.format("Unable to send mail for the last %d seconds", pendingDuration.toSeconds()), eee);
                if (pendingDuration.toMinutes() > 1) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Email could never been send, now stop trying: %s", fisholaMail));
                    }
                    iterator.remove();
                }
            }
        }
    }

    protected void sendMail0(FisholaMail mail) throws MessagingException {

        if (log.isInfoEnabled()) {
            log.info(String.format("Trying to send an email to '%s': « %s »", mail.getTos(), mail.getSubject()));
        }

        MimeMessage message = buildMimeMessage(mail);

        if (config.getSmtpUsername().isPresent() && config.getSmtpPassword().isPresent()) {
            Transport.send(message, config.getSmtpUsername().get(), config.getSmtpPassword().get());
        } else {
            Transport.send(message);
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("Email sent to '%s': « %s »", mail.getTos(), mail.getSubject()));
        }
    }

}
