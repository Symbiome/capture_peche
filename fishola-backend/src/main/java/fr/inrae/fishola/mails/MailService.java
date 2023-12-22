package fr.inrae.fishola.mails;

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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import fr.inrae.fishola.FisholaConfiguration;
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
    protected FisholaConfiguration config;
    @Inject
    protected Mailer mailer;

    protected static final ConcurrentLinkedQueue<FisholaMail> PENDING_EMAILS = new ConcurrentLinkedQueue<>();

    public ImmutableFisholaMail.Builder newMail(String body) {
        ImmutableFisholaMail.Builder builder = ImmutableFisholaMail.builder()
                                                                   .from(config.mailFrom())
                                                                   .body(body);
        return builder;
    }

    public ImmutableFisholaMail.Builder newMailFromTemplate(String templatePath,
                                                            Map<String, Object> templateParameters) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(templatePath);
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, templateParameters);
        String body = stringWriter.toString();
        ImmutableFisholaMail.Builder builder = newMail(body);
        return builder;
    }

    public ImmutableFisholaMail.Builder newMailFromTemplate(String templatePath, String arg1Name, Object arg1,
                                                            String arg2Name, Object arg2) {
        ImmutableMap<String, Object> args = ImmutableMap.of(arg1Name, arg1, arg2Name, arg2);
        ImmutableFisholaMail.Builder builder = newMailFromTemplate(templatePath, args);
        return builder;
    }

    public void sendMail(FisholaMail mail) {

        if (config.asyncEmails()) {
            sendMailAsync(mail);
        } else {
            sendMail0(mail);
        }

    }

    public void sendMailAsync(FisholaMail mail) {

        if (log.isInfoEnabled()) {
            log.infof("Saving email to send to '%s': « %s »", mail.getTos(), mail.getSubject());
        }

        FisholaMail safeMail = mail.pendingSince().isPresent() ? mail : ImmutableFisholaMail.builder()
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

        Iterator<FisholaMail> iterator = PENDING_EMAILS.iterator();
        while (iterator.hasNext()) {
            FisholaMail fisholaMail = iterator.next();
            try {
                sendMail0(fisholaMail);
                // Envoi de mail OK, on le supprime de la liste
                iterator.remove();
            } catch (Exception eee) {
                Preconditions.checkState(fisholaMail.pendingSince().isPresent(),
                        "FisholaMail sans pendingSince: " + fisholaMail);
                Duration pendingDuration = Duration.between(fisholaMail.pendingSince().get(), LocalDateTime.now());
                log.warnf("Unable to send mail for the last %d seconds", pendingDuration.toSeconds(), eee);
                int retentionMinutes = config.asyncEmailsRetentionMinutes();
                if (pendingDuration.toMinutes() > retentionMinutes) {
                    log.errorf("Email could be send for more than %d minutes, now stop trying: %s", retentionMinutes,
                            fisholaMail);
                    iterator.remove();
                }
            }
        }
    }

    protected void sendMail0(FisholaMail fisholaMail) {

        if (log.isInfoEnabled()) {
            log.infof("Trying to send an email to '%s': « %s »", fisholaMail.getTos(), fisholaMail.getSubject());
        }

        for (String recipient : fisholaMail.getTos()) {
            Mail mail = Mail.withText(recipient, fisholaMail.getSubject(), fisholaMail.getBody());
            for (FisholaMailAttachment attachment : fisholaMail.getAttachments()) {
                mail.addAttachment(attachment.getName(), attachment.getBytes(), attachment.getType().toString());
            }
            mailer.send(mail);
        }

        if (log.isInfoEnabled()) {
            log.infof("Email sent to '%s': « %s »", fisholaMail.getTos(), fisholaMail.getSubject());
        }
    }

}
