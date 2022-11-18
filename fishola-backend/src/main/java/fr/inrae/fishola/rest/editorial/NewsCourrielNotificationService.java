package fr.inrae.fishola.rest.editorial;

import com.google.common.collect.Lists;
import fr.inrae.fishola.database.NewsFisholaDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import io.quarkus.scheduler.Scheduled;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
        if (newsById != null) {
            this.notifyUsersByCourrielAboutNews(Lists.newArrayList(newsById));
        }
        return Response.noContent().build();
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
            if (publicNewsThatHaveNotBeenNotifiedByMail.size() > 0) {
                log.info(publicNewsThatHaveNotBeenNotifiedByMail.size() + " News became public since last check, notify users by courriel");
            } else {
                log.info("No News became public since last check.");
            }

            this.notifyUsersByCourrielAboutNews(publicNewsThatHaveNotBeenNotifiedByMail);
        }
    }


    protected void notifyUsersByCourrielAboutNews(List<News> newsToNotifyByMail) {
        if( newsToNotifyByMail.size() > 0) {
            LocalDateTime now = LocalDateTime.now();
            String htmlContent = "";
            String subject = "Fishola - " + newsToNotifyByMail.get(0).getName();
            if (newsToNotifyByMail.size() > 1) {
                subject += " et autres actualités";
            }
            for (News news : newsToNotifyByMail) {
                if (newsToNotifyByMail.size() > 1) {
                    htmlContent += "<h1>" + news.getName() + "</h1>";
                }
                htmlContent += news.getContent();
                news.setDateNotificationSent(now);
                dao.update(news);
            }

            for (FisholaUser user : usersDao.findAllUsersAllowingCourriel()) {
                ImmutableFisholaMail mail = mailService.newMail(htmlContent)
                .subject(subject)
                .addTos(user.getEmail())
                .build();
                mailService.sendMail(mail);
            }
        }
    }
}
