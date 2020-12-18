package fr.inrae.fishola.rest.trips;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import org.apache.commons.collections4.CollectionUtils;

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
    public Optional<UUID> weatherId = Optional.empty();
    public List<CatchBean> catchs;
    public Set<UUID> techniqueIds;
    public Optional<Double> beginLatitude = Optional.empty();
    public Optional<Double> beginLongitude = Optional.empty();
    public Optional<Double> endLatitude = Optional.empty();
    public Optional<Double> endLongitude = Optional.empty();
    public DeviceType source;

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
                ", catchs=" + CollectionUtils.size(catchs) +
                ", techniqueIds=" + techniqueIds +
                ", beginLatitude=" + beginLatitude +
                ", beginLongitude=" + beginLongitude +
                ", endLatitude=" + endLatitude +
                ", endLongitude=" + endLongitude +
                ", source=" + source +
                ", saveDelayMarker=" + saveDelayMarker +
                ", modifiableUntil=" + modifiableUntil +
                '}';
    }
}
