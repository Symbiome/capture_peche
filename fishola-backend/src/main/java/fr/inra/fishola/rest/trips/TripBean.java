package fr.inra.fishola.rest.trips;

import fr.inra.fishola.entities.enums.TripMode;
import fr.inra.fishola.entities.enums.TripType;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public LocalDate date;
    public String startedAt;
    public String finishedAt;
    public UUID weatherId;
    public List<CatchBean> catchs;
    public Set<UUID> techniqueIds;

    public Optional<LocalDateTime> saveDelayMarker = Optional.empty();

    // Calculés
    public Optional<LocalDateTime> modifiableUntil;

//    public void setSaveDelayMarker( {
//        System.out.println(saveDelayMarker);
//        if (saveDelayMarker != null) {
//            System.out.println(saveDelayMarker.getClass().getName());
//        }
//    }

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
                ", techniqueIds=" + techniqueIds +
                ", modifiableUntil=" + modifiableUntil +
                '}';
    }
}
