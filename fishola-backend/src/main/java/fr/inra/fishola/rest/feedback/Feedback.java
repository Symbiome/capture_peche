package fr.inra.fishola.rest.feedback;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableFeedback.class)
public interface Feedback {

    @Value.Derived
    default UUID id() {
        return UUID.randomUUID();
    };

    String category();

    Optional<String> userId();

    Optional<String> email();

    Optional<String> description();

    @Value.Auxiliary // exclu des equals/hashCode/toString
    Optional<String> screenshot();

    Optional<String> browser();

    Optional<String> os();

    Optional<String> platform();

    Optional<String> screenResolution();

    Optional<String> displaySize();

    Optional<String> locale();

    Optional<String> frontendVersion();

    Optional<String> backendVersion();

    Optional<LocalDateTime> date();

    Optional<String> location();

    Optional<String> locationTitle();

}
