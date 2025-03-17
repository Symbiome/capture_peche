package fr.inrae.fishola.rest.trips;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;
import fr.inrae.fishola.entities.enums.Maillage;

import java.time.LocalDate;
import java.util.UUID;

@ImmutableObject
@JsonSerialize(as = ImmutableCatchMarker.class)
public interface CatchMarker {

    UUID id();
    LocalDate date();
    UUID tripId();
    String tripName();
    String specieName();
    String lakeName();
    Double latitude();
    Double longitude();
    Double size();
    Double weight();
    Maillage maillage();
    boolean hasValidCoordinates();
}
