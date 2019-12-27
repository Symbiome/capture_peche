package fr.inra.fishola.rest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Date;

@Value.Immutable
@JsonSerialize(as = ImmutableTripLight.class)
public interface TripLight {

    int catchs();

    boolean canBeModified();

    String id();

    String name();

    String lakeName();

    Date date();

}
