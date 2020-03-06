package fr.inra.fishola.rest.trips;

import fr.inra.fishola.entities.enums.TripMode;
import fr.inra.fishola.entities.enums.TripType;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TripBean {

    public String id;
    public Optional<LocalDateTime> createdOn = Optional.empty();
    public TripMode mode;
    public TripType type;
    public String name;
    public UUID lakeId;
    public Set<UUID> speciesIds;
    public String otherSpecies;
    public Date date;
    public Date startedAt;
    public Date finishedAt;
    public UUID weatherId;
    public List<CatchBean> catchs;

    // Calculés
    public Optional<LocalDateTime> modifiableUntil;

    public void setDirty(boolean dirty) {
        // On ignore, c'est pour que le front réussisse l'appel
    }

    @Override
    public String toString() {
        return "TripBean{" +
                "id='" + id + '\'' +
                ", createdOn=" + createdOn +
                ", mode=" + mode +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", lakeId=" + lakeId +
                ", speciesIds=" + speciesIds +
                ", otherSpecies='" + otherSpecies + '\'' +
                ", date=" + date +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", weatherId=" + weatherId +
                ", catchs=" + catchs +
                ", modifiableUntil=" + modifiableUntil +
                '}';
    }
}
