package fr.inrae.fishola.rest.editorial;

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

import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.EditorialAndDocumentationDao;
import fr.inrae.fishola.entities.tables.pojos.Documentation;
import fr.inrae.fishola.entities.tables.pojos.Editorial;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.Weather;
import fr.inrae.fishola.exceptions.NotFoundException;
import fr.inrae.fishola.rest.AbstractFisholaResource;

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
import javax.ws.rs.core.StreamingOutput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentationResource extends AbstractFisholaResource {

    @Inject
    protected EditorialAndDocumentationDao dao;

    @GET
    @Path("/documentations")
    public List<DocumentationLight> getDocumentation(@Context HttpServletRequest request) {
        LinkedHashMap<UUID, Pair<String,String>> docs = dao.listDocumentations();
        List<DocumentationLight> result = docs.entrySet()
                .stream()
                .map(entry -> toDocumentationLight(entry, request))
                .collect(Collectors.toList());
        return result;
    }

    @DELETE
    @Path("/documentations/{documentId}")
    public Response deleteDocumentation(@PathParam("documentId") UUID documentId) {
        dao.deleteDocumentation(documentId);
        return Response.noContent().build();
    }

    protected DocumentationLight toDocumentationLight(Map.Entry<UUID, Pair<String,String>> entry, HttpServletRequest request) {
        String url = config.getApiUrl("/api/v1/documentation/" + entry.getKey(), request);
        DocumentationLight result = ImmutableDocumentationLight.builder()
                .id(entry.getKey())
                .natural_id(entry.getValue().getLeft())
                .name(entry.getValue().getRight())
                .url(url)
                .build();
        return result;
    }

    @GET
    @Path("/documentation/{docId}")
    @Produces("application/pdf")
    public Response downloadDocumentation(@PathParam("docId") UUID docId) {
        Optional<Documentation> optional = dao.getDocumentation(docId);
        if (optional.isEmpty()) {
            return Response.status(404).build();
        }

        Documentation documentation = optional.get();
        String filename = documentation.getName()
                .replaceAll("[ ]", "_");
        StreamingOutput output = this.wrapAsStreamingOutput(documentation.getContent());
        Response response = Response.ok(output)
                .header("Content-Disposition", String.format("filename=\"%s.pdf\"", filename))
                .build();
        return response;
    }

    @Deprecated
    protected Response downloadDocumentationByNaturalId(String naturalId) {
        LinkedHashMap<UUID, Pair<String, String>> docs = dao.listDocumentations();

        // TODO AThimel 09/04/2020 Améliorer ça pour éviter les problèmes en cas de renommage inopiné
        Optional<UUID> docId = docs.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getLeft().equalsIgnoreCase(naturalId))
                .map(Map.Entry::getKey)
                .findAny();

        NotFoundException.check(docId.isPresent(), String.format("Impossible de trouver le document « %s »", naturalId));

        Response response = downloadDocumentation(docId.get());
        return response;
    }

    @GET
    @Path("/documentation/fixed/cgu")
    @Produces("application/pdf")
    public Response downloadGCU() {
        // TODO AThimel 09/04/2020 Améliorer ça pour éviter les problèmes en cas de renommage inopiné
        Response response = downloadDocumentationByNaturalId("cgu");
        return response;
    }

    @GET
    @Path("/documentation/fixed/samples")
    @Produces("application/pdf")
    public Response downloadSamples() {
        Response response = downloadDocumentationByNaturalId("prélèvements");
        return response;
    }

    @GET
    @Path("/editorial")
    public Response getEditorials() {
        List<Editorial> editorials = dao.getEditorials();
        Response response = Response.ok(editorials).build();
        return response;
    }

    @PUT
    @Path("/editorial/{editorialId}")
    public Response updateEditorial(@PathParam("editorialId") UUID editorialId, Editorial editorial) {
        Preconditions.checkArgument(editorialId != null, "Identifiant de page éditoriale obligatoire");
        Preconditions.checkArgument(editorialId.equals(editorial.getId()), "L'identifiant ne correspond pas");
        // TODO AThimel 06/07/2020 Vérifier le droit d'admin
        dao.updateEditorial(editorial);
        return Response.noContent().build();
    }

    @GET
    @Path("/editorial/{name}")
    public Response getEditorial(@PathParam("name") String name) {
        Optional<Editorial> editorial = dao.findEditorial(name);
        Response response = editorial.map(Response::ok)
                .orElseGet(() -> Response.status(404))
                .build();
        return response;
    }

}
