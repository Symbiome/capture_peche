package fr.inra.fishola.rest.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Map;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDashboard.class)
public interface Dashboard {

    Map<UUID, Double> caughtSpeciesDistribution();

}
