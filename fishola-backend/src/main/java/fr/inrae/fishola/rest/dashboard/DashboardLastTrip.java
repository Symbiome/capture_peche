package fr.inrae.fishola.rest.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDashboardLastTrip.class)
public interface DashboardLastTrip {

    UUID tripId();

    LocalDate day();

    int catchsCount();

}
