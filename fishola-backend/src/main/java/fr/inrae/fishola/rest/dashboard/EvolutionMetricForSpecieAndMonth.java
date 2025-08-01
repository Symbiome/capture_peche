package fr.inrae.fishola.rest.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;

@ImmutableObject
@JsonSerialize(as = ImmutableEvolutionMetricForSpecieAndMonth.class)
public interface EvolutionMetricForSpecieAndMonth {
    Long tripsCount();
    Long keptCatchesCount();
    Long totalCatchesCount();

}
