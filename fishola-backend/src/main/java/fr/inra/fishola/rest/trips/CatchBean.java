package fr.inra.fishola.rest.trips;

import java.util.Optional;
import java.util.UUID;

public class CatchBean {

    public String id;
    public Optional<UUID> speciesId = Optional.empty();
    public Optional<String> otherSpecies = Optional.empty();
    public int size;
    public Optional<Integer> weight = Optional.empty();
    public boolean keep;
    public Optional<UUID> releasedStateId = Optional.empty();
    public UUID techniqueId;
    public Optional<String> description = Optional.empty();
    public Optional<String> caughtAt = Optional.empty();
    public boolean withSample;
    public Optional<Double> latitude = Optional.empty();
    public Optional<Double> longitude = Optional.empty();

    public boolean hasPicture = false;
    public Optional<UUID> tripId = Optional.empty();

}
