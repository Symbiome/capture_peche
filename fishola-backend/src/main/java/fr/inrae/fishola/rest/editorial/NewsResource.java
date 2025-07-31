package fr.inrae.fishola.rest.editorial;

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

import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.NewsFisholaDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.entities.tables.pojos.NewsPicture;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.nuiton.util.ResourceNotFoundException;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class NewsResource extends AbstractFisholaResource {

    @Inject
    protected NewsFisholaDao dao;

    @GET
    @Path("/news")
    public List<News> getPublishedNews(@Context HttpServletRequest request) {
        // Return all news for which publication date is active
        return dao.getNews(true, Optional.empty());
    }

    @GET
    @Path("/news/lake/{lakeId}")
    public List<News> getPublishedNews(@PathParam("lakeId") UUID lakeId, @Context HttpServletRequest request) {
        // Return all news for which publication date is active
        return dao.getPublishedNewsForLake(lakeId);
    }

    @GET
    @Path("/news-all")
    public List<NewsBean> getAllNews(@Context HttpServletRequest request) {
        // Return all news
        FisholaAdmin fisholaAdmin = checkIsAdmin();
        return dao.getNews(false, Optional.of(fisholaAdmin)).stream().map(this::newsToNewsBean).toList();
    }
    @GET
    @Path("/news/{newsId}")
    public News getPublishedNews(@Context HttpServletRequest request, @PathParam("newsId") UUID newsId) {
       News news = dao.findById(newsId);
        LocalDateTime now = LocalDateTime.now();
       if (news == null || news.getDatePublicationDebut() == null || now.isBefore(news.getDatePublicationDebut()) ||
                news.getDatePublicationFin() == null || now.isAfter(news.getDatePublicationFin())) {
           throw new ResourceNotFoundException("News not found or not published yet");
       }
       return news;
    }


    @DELETE
    @Path("/news-all/{newsId}")
    public Response deleteNews(@PathParam("newsId") UUID newsId) {
        checkIsAdmin();
        dao.deleteById(newsId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/news-all/{newsId}")
    public Response updateNews(@PathParam("newsId") UUID newsId, NewsBean news) {
        FisholaAdmin fisholaAdmin = checkIsAdmin();
        boolean newsIsNationalAndAdminIsRegional = news.isNational && !fisholaAdmin.getIsNationalAdmin();
        boolean newsIsRegionalAndAdminHasNoRightsOnLake = !news.isNational && !fisholaAdmin.getIsNationalAdmin() && !getAllowedAdminLakes().containsAll(news.lakeIds);
        if (newsIsNationalAndAdminIsRegional || newsIsRegionalAndAdminHasNoRightsOnLake) {
            throw new ForbiddenException("L'administrateur " + fisholaAdmin.getEmail() + " n'a pas accès aux lacs " + news.lakeIds);
        }
        Preconditions.checkArgument(newsId != null, "Identifiant de news obligatoire");
        Preconditions.checkArgument(newsId.equals(news.id), "L'identifiant ne correspond pas");
        try {
            dao.update(this.newsBeanToNews(news), news.lakeIds);
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de mettre à jour la news : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }


    @POST
    @Path("/news-all")
    public Response createNews(NewsBean news) {
        FisholaAdmin fisholaAdmin = checkIsAdmin();
        boolean newsIsNationalAndAdminIsRegional = news.isNational && !fisholaAdmin.getIsNationalAdmin();
        boolean newsIsRegionalAndAdminHasNoRightsOnLake = !news.isNational && !fisholaAdmin.getIsNationalAdmin() && !getAllowedAdminLakes().containsAll(news.lakeIds);
        if (newsIsNationalAndAdminIsRegional || newsIsRegionalAndAdminHasNoRightsOnLake) {
            throw new ForbiddenException("L'administrateur " + fisholaAdmin.getEmail() + " n'a pas accès aux lacs " + news.lakeIds);
        }
        try {
            News inserted = dao.insert(this.newsBeanToNews(news), news.lakeIds);
            // Update all news pictures uploaded with temp id
            dao.updateTempNewsPictureIds(inserted.getId());
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de créer la news : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }

    @POST
    @Path("/news-picture/{newsId}")
    public NewsPicture postNewsPicture(@PathParam("newsId") String newsId, String content) {
        checkIsAdmin();
        try {
            return dao.insertNewsPicture(newsId, content, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de créer l'image : " + e.getMessage());
        }
    }

    @POST
    @Path("/news-miniature/{newsId}")
    public NewsPicture postNewsMiniature(@PathParam("newsId") String newsId, String content) {
        checkIsAdmin();
        try {
            return dao.insertNewsPicture(newsId, content, true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de créer l'image : " + e.getMessage());
        }
    }

    @GET
    @Path("/news-picture/{picId}")
    @Produces("image/jpeg")
    public Response getNewsPicture(@PathParam("picId") UUID picId) {
         Optional<byte[]> bytes = dao.getNewsPicture(picId);

        Response response = bytes.map(this::wrapAsStreamingOutput)
                .map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
        return response;
    }

    private NewsBean newsToNewsBean(News n) {
        Set<UUID> lakeIds = this.dao.getLakeIds(n.getId());
        return new NewsBean(n.getId(), n.getName(), n.getContent(), n.getDatePublicationDebut(), n.getDatePublicationFin(), n.getDateNotificationSent(), n.getMiniatureId(), n.getIsNational(), lakeIds);
    }
    private News newsBeanToNews(NewsBean news) {
        return new News(news.id, news.name, news.content, news.datePublicationDebut, news.datePublicationFin, news.dateNotificationSent, news.miniatureId, news.isNational);
    }
}
