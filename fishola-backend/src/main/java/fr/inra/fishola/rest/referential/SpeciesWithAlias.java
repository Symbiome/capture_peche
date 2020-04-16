package fr.inra.fishola.rest.referential;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inra.fishola.entities.tables.pojos.Species;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableSpeciesWithAlias.class)
public interface SpeciesWithAlias {

    UUID id();
    String  name();
    boolean builtIn();
    boolean mandatorySize();

    Optional<String> alias();

    static SpeciesWithAlias of(Species source, String alias) {
        ImmutableSpeciesWithAlias result = ImmutableSpeciesWithAlias.builder()
                .id(source.getId())
                .name(source.getName())
                .builtIn(source.getBuiltIn())
                .mandatorySize(source.getMandatorySize())
                .alias(Optional.ofNullable(alias))
                .build();
        return result;
    }

}
