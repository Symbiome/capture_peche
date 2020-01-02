package fr.inra.fishola.rest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableTripLight.class)
public interface TripLight {

    int catchsCount();

    Optional<Date> modifiableUntil();

    UUID id();

    String name();

    UUID lakeId();

    LocalDate date();

    Optional<LocalTime> startedAt();

    Optional<LocalTime> finishedAt();

}
