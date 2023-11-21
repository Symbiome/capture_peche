package fr.inrae.fishola.rest.trips;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CatchBean {

    public String id;
    public Optional<String> speciesId = Optional.empty();
    public Optional<String> otherSpecies = Optional.empty();
    public Optional<Integer> size = Optional.empty();
    public Optional<Integer> automaticMeasure = Optional.empty();
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
    public List<Integer> pictureOrders = new LinkedList<>();
    public boolean hasMeasurementPicture = false;
    public Optional<UUID> tripId = Optional.empty();
    public Optional<UUID> editedSpeciesId = Optional.empty();
    public Optional<Integer> editedSize = Optional.empty();
    public Optional<Integer> editedWeight = Optional.empty();
    public boolean excludeFromExport = false;

    @Override
    public String toString() {
        return "CatchBean{" +
                "id='" + id + '\'' +
                ", speciesId=" + speciesId +
                ", otherSpecies=" + otherSpecies +
                ", size=" + size +
                ", automaticMeasure" + automaticMeasure +
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
                ", pictureOrders=" + pictureOrders +
                ", hasMeasurementPicture=" + hasMeasurementPicture +
                ", tripId=" + tripId +
                '}';
    }

}
