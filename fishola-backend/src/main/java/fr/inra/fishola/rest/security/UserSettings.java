package fr.inra.fishola.rest.security;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableUserSettings.class)
public interface UserSettings {

    boolean promptWeight();

    boolean promptSamples();

}
