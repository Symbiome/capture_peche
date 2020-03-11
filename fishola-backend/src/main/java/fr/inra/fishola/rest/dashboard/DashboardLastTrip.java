package fr.inra.fishola.rest.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Date;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDashboardLastTrip.class)
public interface DashboardLastTrip {

    UUID tripId();

    Date day();

    int catchsCount();

}
