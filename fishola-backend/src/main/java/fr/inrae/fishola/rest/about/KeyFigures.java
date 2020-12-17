package fr.inrae.fishola.rest.about;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import org.immutables.value.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableKeyFigures.class)
public interface KeyFigures {

    int tripsCount();

    int catchsCount();

    int picturesCount();

    List<Lake> lakes();

    Map<UUID, Integer> catchsCountPerLakeId();

    String titleText();

    String contributeText();

    LocalDateTime computedOn();

}
