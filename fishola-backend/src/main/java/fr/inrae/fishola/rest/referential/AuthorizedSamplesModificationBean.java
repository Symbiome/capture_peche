package fr.inrae.fishola.rest.referential;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuthorizedSamplesModificationBean {
    public List<UUID> targetLakes;
    public Map<UUID, Map<UUID, Object>> authorizations;
    public Map<UUID, Map<UUID, Object>> minSizes;
    public Map<UUID, Map<UUID, Object>> maxSizes;
}
