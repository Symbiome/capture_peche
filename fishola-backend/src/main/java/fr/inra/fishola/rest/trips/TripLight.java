package fr.inra.fishola.rest.trips;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableTripLight.class)
public interface TripLight {

    UUID id();

    String name();

    UUID lakeId();

    LocalDate date();

    long durationInSeconds();

    int catchsCount();

    boolean modifiable();

}
