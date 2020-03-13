package fr.inra.fishola.rest.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inra.fishola.rest.trips.CatchBean;
import org.immutables.value.Value;

import java.util.List;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDashboardTopCatchs.class)
public interface DashboardTopCatchs {

    UUID speciesId();

    List<CatchBean> elements();

}
