package fr.inrae.fishola.rest.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;
import fr.inrae.fishola.entities.enums.Maillage;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Month;
import java.util.Map;
import java.util.UUID;

@ImmutableObject
@JsonSerialize(as= ImmutableEvolutionMetricsForLake.class)
public interface EvolutionMetricsForLake {
    Map<Integer, Map<Month, Map<UUID, Map<Boolean, Long>>>> catchCountPerMonthAndSpecies();
    Map<Integer, Map<Month, Map<UUID, Long>>> tripCountPerMonthAndSpecies();

}
