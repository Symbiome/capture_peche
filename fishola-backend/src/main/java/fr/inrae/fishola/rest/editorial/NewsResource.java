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
import fr.inrae.fishola.database.EditorialAndDocumentationDao;
import fr.inrae.fishola.entities.tables.pojos.Documentation;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import fr.inrae.fishola.exceptions.NotFoundException;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import org.apache.commons.lang3.tuple.Pair;

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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class NewsResource extends AbstractFisholaResource {

    public static final boolean IS_NEWS = true;

    @Inject
    protected EditorialAndDocumentationDao dao;

    @GET
    @Path("/news")
    public List<DocumentationWithBase64ContentBean> getDocumentations(@Context HttpServletRequest request) {
        LinkedHashMap<UUID, Pair<String,String>> docs = dao.listDocumentations(IS_NEWS);
        List<DocumentationWithBase64ContentBean> result = docs.entrySet()
                .stream()
                .map(entry -> toDocumentationWithBase64Content(entry, request))
                .collect(Collectors.toList());
        return result;
    }

    @DELETE
    @Path("/news/{documentId}")
    public Response deleteDocumentation(@PathParam("documentId") UUID documentId) {
        checkIsAdmin();
        dao.deleteDocumentation(documentId);
        return Response.noContent().build();
    }

    protected DocumentationWithBase64ContentBean toDocumentationWithBase64Content(Map.Entry<UUID, Pair<String,String>> entry, HttpServletRequest request) {
        String url = config.getApiUrl("/api/v1/documentation/" + entry.getKey(), request);
        DocumentationWithBase64ContentBean result = new DocumentationWithBase64ContentBean();
        result.setId(entry.getKey());
        result.setNaturalId(entry.getValue().getLeft());
        result.setName(entry.getValue().getRight());
        result.setUrl(url);
        result.setBase64Content("");
        return result;
    }

    @PUT
    @Path("/news/{docId}")
    public Response updateDocumentation(@PathParam("docId") UUID docId, DocumentationWithBase64ContentBean documentationBase64Content) {
        checkIsAdmin();
        Preconditions.checkArgument(docId != null, "Identifiant de document obligatoire");
        Preconditions.checkArgument(docId.equals(documentationBase64Content.id()), "L'identifiant ne correspond pas");
        try {
            Documentation documentation = documentationFromBase64Content(Optional.of(docId), documentationBase64Content);
            dao.updateDocumentation(documentation);
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de mettre à jour la documentation : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }

    @POST
    @Path("/news")
    public Response createDocumentation(DocumentationWithBase64ContentBean documentationBase64Content) {
        checkIsAdmin();
        try {
            Documentation documentation = documentationFromBase64Content(Optional.empty(), documentationBase64Content);
            dao.createDocumentation(documentation);
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de créer la documentation : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }

    protected Documentation documentationFromBase64Content(Optional<UUID> docId, DocumentationWithBase64ContentBean documentationBase64Content) throws FisholaTechnicalException {
        Documentation documentation = new Documentation();
        docId.ifPresent(documentation::setId);
        documentation.setNaturalId(documentationBase64Content.naturalId());
        documentation.setName(documentationBase64Content.name());
        documentation.setNews(IS_NEWS);
        // If new documentation was sent in base64
        if (documentationBase64Content.base64Content() != null && documentationBase64Content.base64Content().length() > 10) {
            String[] contentSplitted = documentationBase64Content.base64Content().split(",");
            String base64PDF = contentSplitted[1];
            byte[] bytes = Base64.getDecoder().decode(base64PDF);
            documentation.setContent(bytes);
        } else {
            Preconditions.checkArgument(docId.isPresent(), "Pas de contenu base64 spécifié, il faut avoir donné un identifiant de documentation");
            // Reuse existing content if none sent
            Optional<Documentation> existingDoc = dao.getDocumentation(docId.get());
            NotFoundException.check(existingDoc.isPresent(), "Missing documentation " + docId.get());
            documentation.setContent(existingDoc.get().getContent());
        }
        return documentation;
    }

}
