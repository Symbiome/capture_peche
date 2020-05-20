package fr.inrae.fishola.rest.editorial;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDocumentationLight.class)
public interface DocumentationLight {

    UUID id();
    String  name();
    String url();

}
