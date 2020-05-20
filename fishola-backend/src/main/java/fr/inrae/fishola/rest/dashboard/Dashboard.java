package fr.inrae.fishola.rest.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.rest.trips.CatchBean;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDashboard.class)
public interface Dashboard {

    Map<UUID, Double> caughtSpeciesDistribution();

    List<DashboardLastTrip> latestTripsCatchs();

    Optional<Double> averageCatchsPerTrip();

    Map<UUID, List<CatchBean>> topBySize();

    Map<UUID, List<CatchBean>> topByWeight();

}
