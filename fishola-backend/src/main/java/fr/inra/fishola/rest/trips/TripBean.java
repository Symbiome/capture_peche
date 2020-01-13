package fr.inra.fishola.rest.trips;

import fr.inra.fishola.entities.enums.TripMode;
import fr.inra.fishola.entities.enums.TripType;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TripBean {

    public String id;
    public Optional<Date> createdOn = Optional.empty();
    public TripMode mode;
    public TripType type;
    public String name;
    public UUID lakeId;
    public List<UUID> speciesIds;
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

    public class CatchBean {
    }

    @Override
    public String toString() {
        return "TripBean{" +
                "id='" + id + '\'' +
                ", createdOn='" + createdOn + '\'' +
                ", mode='" + mode + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", lakeId=" + lakeId +
                ", speciesIds=" + speciesIds +
                ", date=" + date +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", catchs=" + catchs +
                '}';
    }
}
