package fr.inrae.fishola.rest.trips;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.rest.editorial.DocumentationLight;
import fr.inrae.fishola.rest.editorial.ImmutableDocumentationLight;
import java.util.UUID;

@JsonSerialize(as = DocumentationWithBase64ContentBean.class)
public class DocumentationWithBase64ContentBean implements DocumentationLight {
    public UUID id;
    public String naturalId;
    public String name;
    public String url;
    public String base64Content;

    public UUID id() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String naturalId() {
        return naturalId;
    }

    public void setNaturalId(String naturalId) {
        this.naturalId = naturalId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String url() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String base64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }
}
