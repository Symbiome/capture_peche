package fr.inrae.fishola.rest.editorial;

import fr.inrae.fishola.database.NewsFisholaDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Service in charge of sending courriel notification for interested fishola users.
 */
@Path("/api/v1/news-notifications/")
@Produces(MediaType.APPLICATION_JSON)
public class NewsCourrielNotificationService extends AbstractFisholaResource {

   @Inject
   protected NewsFisholaDao dao;


    @GET
    @Path("/next-check")
    public LocalDateTime getNextCheckDate(@Context HttpServletRequest request) {
      checkIsAdmin();
     return dao.getNextScheduledNotificationCheckDate();
    }

}
