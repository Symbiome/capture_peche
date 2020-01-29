package fr.inra.fishola.rest.trips;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class CatchBean {

    public String id;
    public UUID speciesId;
    public Optional<Integer> size;
    public Optional<Integer> weight;
    public boolean keep;
    public Optional<UUID> releasedStateId;
    public UUID techniqueId;
    public Optional<String> description;
    public Date caughtAt;
    public boolean withSample;

}
