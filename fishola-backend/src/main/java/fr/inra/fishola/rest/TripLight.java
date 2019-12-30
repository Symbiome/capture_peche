package fr.inra.fishola.rest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableTripLight.class)
public interface TripLight {

    int catchsCount();

    boolean canBeModified();

    UUID id();

    String name();

    UUID lakeId();

    Date date();

    Optional<Date> startedAt();

    Optional<Date> finishedAt();

}
