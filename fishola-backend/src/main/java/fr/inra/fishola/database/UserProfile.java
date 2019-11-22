package fr.inra.fishola.database;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inra.fishola.entities.enums.Gender;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableUserProfile.class)
public interface UserProfile {

    String firstName();

    Optional<String> lastName();

    String email();

    Optional<Gender> gender();

    Optional<Integer> birthYear();

}
