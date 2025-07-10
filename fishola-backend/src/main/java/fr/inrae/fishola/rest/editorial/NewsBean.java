package fr.inrae.fishola.rest.editorial;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class NewsBean {

    public UUID id;
    public String name;
    public String content;
    public LocalDateTime datePublicationDebut;
    public LocalDateTime datePublicationFin;
    public LocalDateTime dateNotificationSent;
    public UUID miniatureId;
    public Boolean isNational;
    public Set<UUID> lakeIds;

    public NewsBean(UUID id, String name, String content, LocalDateTime datePublicationDebut, LocalDateTime datePublicationFin, LocalDateTime dateNotificationSent, UUID miniatureId, Boolean isNational, Set<UUID> lakeIds) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.datePublicationDebut = datePublicationDebut;
        this.datePublicationFin = datePublicationFin;
        this.dateNotificationSent = dateNotificationSent;
        this.miniatureId = miniatureId;
        this.isNational = isNational;
        this.lakeIds = lakeIds;
    }
}
