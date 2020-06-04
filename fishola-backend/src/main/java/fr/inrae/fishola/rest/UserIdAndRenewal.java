package fr.inrae.fishola.rest;

import org.immutables.value.Value;

import java.util.Optional;
import java.util.UUID;

@Value.Immutable
public interface UserIdAndRenewal {

    UUID userId();

    Optional<String> renewalToken();

    static UserIdAndRenewal of(UUID userId) {
        return ImmutableUserIdAndRenewal.builder().userId(userId).build();
    }

    static UserIdAndRenewal of(UUID userId, String token) {
        return ImmutableUserIdAndRenewal.builder().userId(userId).renewalToken(token).build();
    }

}
