package fr.inra.fishola.rest.trips;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class CatchBean {

    public String id;
    public UUID speciesId;
    public int size;
    public Optional<Integer> weight = Optional.empty();
    public boolean keep;
    public Optional<UUID> releasedStateId = Optional.empty();
    public UUID techniqueId;
    public Optional<String> description = Optional.empty();
    public Optional<Date> caughtAt = Optional.empty();
    public boolean withSample;

    public boolean hasPicture = false;

}
