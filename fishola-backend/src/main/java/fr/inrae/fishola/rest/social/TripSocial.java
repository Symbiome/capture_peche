package fr.inrae.fishola.rest.social;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.tables.pojos.TripSocialReaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ImmutableObject
@JsonSerialize(as = ImmutableTripSocial.class)
public interface TripSocial {
    UUID id();
    String tripName();
    String userName();
    String lakeName();
    LocalDate date();
    long durationInSeconds();
    List<TripSocialReaction> socialReactions();
    Map<String, Map<Maillage, Integer>> catchesCountPerMaillage();
}
