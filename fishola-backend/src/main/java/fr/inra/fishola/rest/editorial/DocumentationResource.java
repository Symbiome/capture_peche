package fr.inra.fishola.rest.editorial;

import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.database.EditorialAndDocumentationDao;
import fr.inra.fishola.entities.tables.pojos.Documentation;
import fr.inra.fishola.entities.tables.pojos.Editorial;
import fr.inra.fishola.exceptions.NotFoundException;
import fr.inra.fishola.rest.AbstractFisholaResource;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
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

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentationResource extends AbstractFisholaResource {

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected EditorialAndDocumentationDao dao;

    @GET
    @Path("/documentations")
    public List<DocumentationLight> getDocumentation(@Context HttpServletRequest request) {
        LinkedHashMap<UUID, String> docs = dao.listDocumentations();
        List<DocumentationLight> result = docs.entrySet()
                .stream()
                .map(entry -> toDocumentationLight(entry, request))
                .collect(Collectors.toList());
        return result;
    }

    protected DocumentationLight toDocumentationLight(Map.Entry<UUID, String> entry, HttpServletRequest request) {
        String url = config.getApiUrl("/api/v1/documentation/" + entry.getKey(), request);
        DocumentationLight result = ImmutableDocumentationLight.builder()
                .id(entry.getKey())
                .name(entry.getValue())
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
    protected Response downloadDocumentationByName(String name) {
        LinkedHashMap<UUID, String> docs = dao.listDocumentations();

        // TODO AThimel 09/04/2020 Améliorer ça pour éviter les problèmes en cas de renommage inopiné
        Optional<UUID> docId = docs.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findAny();

        NotFoundException.check(docId.isPresent(), String.format("Impossible de trouver le document « %s »", name));

        Response response = downloadDocumentation(docId.get());
        return response;
    }

    @GET
    @Path("/documentation/fixed/cgu")
    @Produces("application/pdf")
    public Response downloadGCU() {
        // TODO AThimel 09/04/2020 Améliorer ça pour éviter les problèmes en cas de renommage inopiné
        Response response = downloadDocumentationByName("Conditions Générales d'Utilisation");
        return response;
    }

    @GET
    @Path("/documentation/fixed/samples")
    @Produces("application/pdf")
    public Response downloadSamples() {
        // TODO AThimel 09/04/2020 Améliorer ça pour éviter les problèmes en cas de renommage inopiné
        Response response = downloadDocumentationByName("Documentation sur les prélèvements");
        return response;
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
