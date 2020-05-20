package fr.inrae.fishola.rest.referential;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.entities.tables.pojos.Species;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableSpeciesWithAlias.class)
public interface SpeciesWithAlias {

    // Informations liées uniquement à l'espèce
    UUID id();
    String  name();
    boolean builtIn();
    boolean mandatorySize();

    // Informations dépendante du lac
    Optional<String> alias();
    boolean authorizedSample();

    static SpeciesWithAlias of(Species source) {
        SpeciesWithAlias result = of(source, null, false);
        return result;
    }

    static SpeciesWithAlias of(Species source, String alias, boolean authorizedSample) {
        ImmutableSpeciesWithAlias result = ImmutableSpeciesWithAlias.builder()
                .id(source.getId())
                .name(source.getName())
                .builtIn(source.getBuiltIn())
                .mandatorySize(source.getMandatorySize())
                .alias(Optional.ofNullable(alias))
                .authorizedSample(authorizedSample)
                .build();
        return result;
    }

}
