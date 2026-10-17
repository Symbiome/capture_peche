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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une capture « preuve » d'un badge sur la carte souvenir (#275). {@code id} sert aussi à
 * charger sa photo ({@code /v1/pictures/{id}/preview}) quand {@code hasPicture}.
 */
public class SouvenirCatchBean {

    public UUID id;
    public UUID tripId;
    public String speciesName;
    public Integer size;
    public Integer weight;
    public Integer quantity;

    /** Heure de capture si renseignée ; sinon le mobile retombe sur la date de la sortie. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    public LocalDateTime caughtAt;

    public boolean hasPicture;
    public Double latitude;
    public Double longitude;
}
