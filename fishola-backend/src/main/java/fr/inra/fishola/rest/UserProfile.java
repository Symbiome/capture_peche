package fr.inra.fishola.rest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import fr.inra.fishola.entities.enums.Gender;
import org.immutables.value.Value;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value.Immutable
@JsonSerialize(as = ImmutableUserProfile.class)
public interface UserProfile {

    String firstName();

    Optional<String> lastName();

    String email();

    Optional<Gender> gender();

    Optional<Integer> birthYear();

    @Value.Derived
    default String initials() {
        List<Character> initials = new LinkedList<>(splitToInitials(firstName()));
        lastName().map(this::splitToInitials)
                .ifPresent(initials::addAll);
        String joined = Joiner.on("").join(initials).toUpperCase();
        return joined;
    }

    default List<Character> splitToInitials(String input) {
        List<String> parts = Splitter.on(Pattern.compile("[ -]"))
                .trimResults()
                .omitEmptyStrings()
                .splitToList(input.trim());
        List<Character> result = parts.stream()
                .map(part -> part.charAt(0))
                .collect(Collectors.toList());
        return result;
    }

}
