package fr.inrae.fishola.rest.gamification;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

import com.fasterxml.jackson.annotation.JsonFormat;
import fr.inrae.fishola.rest.hydro.GeoPoint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Une sortie « preuve » d'un badge sur la carte souvenir (#275) : date, milieu, commune et
 * département, positions de début/fin et des captures. Les positions précises ne servent
 * qu'à situer la zone : la carte partagée n'affiche par défaut qu'une emprise approximative.
 */
public class SouvenirTripBean {

    public UUID id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    public LocalDateTime beginTimestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    public LocalDateTime endTimestamp;

    public String waterEntityName;
    public String waterEntityKind;
    public Double waterEntityAltitude;
    public Double waterEntityLatitude;
    public Double waterEntityLongitude;

    /** Commune du point de début (à défaut du centroïde du milieu) ; nulle hors référentiel communal. */
    public String commune;
    public String department;
    public String departmentName;

    public Double beginLatitude;
    public Double beginLongitude;
    public Double endLatitude;
    public Double endLongitude;
    public List<GeoPoint> catchPositions = new ArrayList<>();
}
