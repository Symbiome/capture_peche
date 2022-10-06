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
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class NewsResource extends AbstractFisholaResource {

    @Inject
    protected NewsFisholaDao dao;

    @GET
    @Path("/news")
    public List<News> getPublishedNews(@Context HttpServletRequest request) {
        // Return all news for which publication date is active
        return dao.getNews(true);
    }

    @GET
    @Path("/news-all")
    public List<News> getAllNews(@Context HttpServletRequest request) {
        // Return all news
        checkIsAdmin();
        return dao.getNews(false);
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
    public Response updateNews(@PathParam("newsId") UUID newsId, News news) {
        checkIsAdmin();
        Preconditions.checkArgument(newsId != null, "Identifiant de news obligatoire");
        Preconditions.checkArgument(newsId.equals(news.getId()), "L'identifiant ne correspond pas");
        try {
            dao.update(news);
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de mettre à jour la news : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }

    @POST
    @Path("/news-all")
    public Response createNews(News news) {
        checkIsAdmin();
        try {
            dao.insert(news);
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de créer la news : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }
}
