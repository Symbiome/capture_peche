package fr.inrae.fishola.rest.trips;

import java.util.Optional;
import java.util.UUID;

public class CatchBean {

    public String id;
    public Optional<String> speciesId = Optional.empty();
    public Optional<String> otherSpecies = Optional.empty();
    public Optional<Integer> size = Optional.empty();
    public Optional<Integer> weight = Optional.empty();
    public boolean keep;
    public Optional<UUID> releasedStateId = Optional.empty();
    public UUID techniqueId;
    public Optional<String> description = Optional.empty();
    public Optional<String> caughtAt = Optional.empty();
    public Optional<String> sampleId = Optional.empty();
    public Optional<Double> latitude = Optional.empty();
    public Optional<Double> longitude = Optional.empty();

    public boolean hasPicture = false;
    public Optional<UUID> tripId = Optional.empty();

    @Override
    public String toString() {
        return "CatchBean{" +
                "id='" + id + '\'' +
                ", speciesId=" + speciesId +
                ", otherSpecies=" + otherSpecies +
                ", size=" + size +
                ", weight=" + weight +
                ", keep=" + keep +
                ", releasedStateId=" + releasedStateId +
                ", techniqueId=" + techniqueId +
                ", description=" + description +
                ", caughtAt=" + caughtAt +
                ", sampleId=" + sampleId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", hasPicture=" + hasPicture +
                ", tripId=" + tripId +
                '}';
    }

}
