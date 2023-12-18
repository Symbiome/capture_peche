package fr.inrae.fishola.rest.editorial;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2023 INRAE - UMR CARRTEL
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import fr.inrae.fishola.database.NewsFisholaDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import io.quarkus.scheduler.Scheduled;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.servlet.http.HttpServletRequest;
import org.jboss.logging.Logger;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

/**
 * Service in charge of sending courriel notification for interested fishola users.
 */
@Path("/api/v1/news-notifications/")
@Produces(MediaType.APPLICATION_JSON)
public class NewsCourrielNotificationService extends AbstractFisholaResource {

   @Inject
   protected NewsFisholaDao dao;

    @Inject
    protected Logger log;

    @Inject
    protected MailService mailService;

    /**
     * Returns the next time the back is going to send mail notifications about all public and not already sent news.
     */
    @GET
    @Path("/next-check")
    public LocalDateTime getNextScheduledNotificationCheckDate(@Context HttpServletRequest request) {
      checkIsAdmin();
     return dao.getNextScheduledNotificationCheck().getNextCheckDate();
    }

    /**
     * Sends immetialy a courriel to notify about the news with the given id (only if it is public).
     */
    @GET
    @Path("/send/{news-id}")
    public Response sendCourrielNotificationForNews(@Context HttpServletRequest request, @PathParam("news-id") UUID newsId) {
        checkIsAdmin();
        News newsById = dao.findById(newsId);
        if (newsById != null && newsById.getDateNotificationSent() == null) {
            this.notifyUsersByCourrielAboutNews(Lists.newArrayList(newsById));
        }
        return Response.noContent().build();
    }

    /**
     * Sends immetialy a courriel to notify about the news with the given id (only if it is public).
     */
    @GET
    @Path("/unsubscribe/{user-id}")
    @Produces(MediaType.TEXT_HTML)
    public Response unsubscribeForm(@Context HttpServletRequest request, @PathParam("user-id") UUID userId) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("emails/unsubscribe.html");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, ImmutableMap.of("userId", userId));
        String body = stringWriter.toString();
        body = body.replace("{userId}", userId.toString());
        return Response.ok(body).build();
    }

    @POST
    @Path("/unsubscribe/{user-id}")
    @Produces(MediaType.TEXT_HTML)
    public Response unsubscribe(@Context HttpServletRequest request, @PathParam("user-id") UUID userId) {
        Optional<FisholaUser> userById = usersDao.findById(userId);
        if (userById.isPresent()) {
            userById.get().setAcceptsMailNotifications(false);
            usersDao.updateUser(userById.get());
        }
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("emails/unsubscribed.html");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, ImmutableMap.of("userId", userId));
        String body = stringWriter.toString();
        body = body.replace("{userId}", userId.toString());
        return Response.ok(body).build();
    }

    @Scheduled(every="20m", concurrentExecution = SKIP, delayed = "5m")
    protected void checkForMailNotifications() {
        LocalDateTime nextScheduledNotificationCheck = dao.getNextScheduledNotificationCheck().getNextCheckDate();
        log.info("Checking for mail notifications, next sending date scheduled to " + nextScheduledNotificationCheck.toString());
        if (LocalDateTime.now().isAfter(nextScheduledNotificationCheck)) {
            // Set next check according to configuration
            dao.scheduleNestNotificationCheck(config.newsMailSendingDelayHours());
            // Notify user by mail about all public news that have not been notified yet
            List<News> publicNewsThatHaveNotBeenNotifiedByMail = dao.getNews(true).stream().filter(news -> news.getDateNotificationSent() == null).collect(Collectors.toList());
            if (!publicNewsThatHaveNotBeenNotifiedByMail.isEmpty()) {
                log.info(publicNewsThatHaveNotBeenNotifiedByMail.size() + " News became public since last check, notify users by courriel");
            } else {
                log.info("No News became public since last check.");
            }

            this.notifyUsersByCourrielAboutNews(publicNewsThatHaveNotBeenNotifiedByMail);
        }
    }


    protected void notifyUsersByCourrielAboutNews(List<News> newsToNotifyByMail) {
        if(!newsToNotifyByMail.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            StringBuilder htmlContent = new StringBuilder();
            String subject = "Fishola - " + newsToNotifyByMail.get(0).getName();
            if (newsToNotifyByMail.size() > 1) {
                subject += " et autres actualités";
            }
            for (News news : newsToNotifyByMail) {
                if (newsToNotifyByMail.size() > 1) {
                    htmlContent.append("<h1>").append(news.getName()).append("</h1>");
                }
                htmlContent.append(news.getContent());
                news.setDateNotificationSent(now);
                dao.update(news);
            }
            htmlContent.append(" <br/>Retrouvez toutes les actualités de Fishola sur notre <a href=\"https://fishola.fr/\"> site internet </a>.");

            for (FisholaUser user : usersDao.findAllUsersAllowingCourriel()) {
                String baseURL = "https://fishola.fr";
                if (config.backendBaseUrl().isPresent()) {
                    baseURL = config.backendBaseUrl().get();
                }
                String unsubscribeURL = baseURL + "/api/v1/news-notifications/unsubscribe/" + user.getId();
                String unsubscribeLink = "<br/><a href=\"" + unsubscribeURL + "\">Ne plus recevoir les communications Fishola par email</a>";
                ImmutableFisholaMail mail = mailService.newMail(htmlContent + unsubscribeLink)
                .subject(subject)
                .addTos(user.getEmail())
                .build();
                mailService.sendMail(mail);
            }
        }
    }
}
