package fr.inrae.fishola.mails;

/*-
 * #%L
 * Carnet de pêche :: Backend
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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import fr.inrae.fishola.CarnetConfiguration;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

@RequestScoped
public class MailService {

    @Inject
    protected Logger log;

    @Inject
    protected CarnetConfiguration config;
    @Inject
    protected Mailer mailer;

    protected static final ConcurrentLinkedQueue<CarnetMail> PENDING_EMAILS = new ConcurrentLinkedQueue<>();

    public ImmutableCarnetMail.Builder newMail(String body) {
        ImmutableCarnetMail.Builder builder = ImmutableCarnetMail.builder().body(body);
        return builder;
    }

    public ImmutableCarnetMail.Builder newMailFromTemplate(String templatePath,
                                                            Map<String, Object> templateParameters) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(templatePath);
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, templateParameters);
        String body = stringWriter.toString();
        ImmutableCarnetMail.Builder builder = newMail(body);
        return builder;
    }

    public ImmutableCarnetMail.Builder newMailFromTemplate(String templatePath, String arg1Name, Object arg1,
                                                            String arg2Name, Object arg2) {
        ImmutableMap<String, Object> args = ImmutableMap.of(arg1Name, arg1, arg2Name, arg2);
        ImmutableCarnetMail.Builder builder = newMailFromTemplate(templatePath, args);
        return builder;
    }

    public void sendMail(CarnetMail mail) {

        if (config.asyncEmails()) {
            sendMailAsync(mail);
        } else {
            sendMail0(mail);
        }

    }

    public void sendMailAsync(CarnetMail mail) {

        if (log.isInfoEnabled()) {
            log.infof("Saving email to send to '%s': « %s »", mail.getTos(), mail.getSubject());
        }

        CarnetMail safeMail = mail.pendingSince().isPresent() ? mail : ImmutableCarnetMail.builder()
                                                                                            .fromInstance(mail)
                                                                                            .pendingSince(LocalDateTime.now())
                                                                                            .build();

        PENDING_EMAILS.add(safeMail);

    }

    @Scheduled(every = "{fishola.async-emails-every}", concurrentExecution = SKIP)
    protected void sendPendingEmails() {

        int pendingEmailsCount = PENDING_EMAILS.size();
        if (log.isInfoEnabled() && pendingEmailsCount > 0) {
            log.infof("Trying to send %d pending emails", pendingEmailsCount);
        }

        Preconditions.checkState(
                pendingEmailsCount == 0 || config.asyncEmails(),
                "On ne devrait pas avoir de mails pending vu qu'on est pas en async");

        Iterator<CarnetMail> iterator = PENDING_EMAILS.iterator();
        while (iterator.hasNext()) {
            CarnetMail carnetMail = iterator.next();
            try {
                sendMail0(carnetMail);
                // Envoi de mail OK, on le supprime de la liste
                iterator.remove();
            } catch (Exception eee) {
                Preconditions.checkState(carnetMail.pendingSince().isPresent(),
                        "CarnetMail sans pendingSince: " + carnetMail);
                Duration pendingDuration = Duration.between(carnetMail.pendingSince().get(), LocalDateTime.now());
                log.warnf("Unable to send mail for the last %d seconds", pendingDuration.toSeconds(), eee);
                log.warnf("Mail exception was " + eee.getClass().getName() +" : "  + eee.getMessage());
                int retentionMinutes = config.asyncEmailsRetentionMinutes();
                if (pendingDuration.toMinutes() > retentionMinutes) {
                    log.errorf("Email could be send for more than %d minutes, now stop trying: %s", retentionMinutes,
                            carnetMail);
                    iterator.remove();
                }
            }
        }
    }

    protected void sendMail0(CarnetMail carnetMail) {

        if (log.isInfoEnabled()) {
            log.infof("Trying to send an email to '%s': « %s »", carnetMail.getTos(), carnetMail.getSubject());
        }

        for (String recipient : carnetMail.getTos()) {
            Mail mail = Mail
                    .withHtml(recipient, carnetMail.getSubject(), carnetMail.getBody());
            for (CarnetMailAttachment attachment : carnetMail.getAttachments()) {
                mail.addAttachment(attachment.getName(), attachment.getBytes(), attachment.getType().toString());
            }
            mailer.send(mail);
        }

        if (log.isInfoEnabled()) {
            log.infof("Email sent to '%s': « %s »", carnetMail.getTos(), carnetMail.getSubject());
        }
    }

}
