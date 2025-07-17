package fr.inrae.fishola.rest.referential;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpeciesAliasesPerLakeBean {
    public List<UUID> targetLakes;
    public Map<UUID, Map<UUID, String>> speciesPerLakeAliases;
}
