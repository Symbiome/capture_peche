package fr.inrae.fishola.rest.imports.carnet;

/*-
 * #%L
 * Fishola :: Backend
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

import java.time.LocalTime;
import java.util.UUID;

/**
 * Une capture (ou un lot) saisie manuellement, format « carnet volontaire » (#143).
 * Champs marqués {@code TODO(#145)} : validés mais non persistés (pas de colonne dédiée).
 */
public class CarnetVolontaireCatchBean {

    public UUID speciesId;
    public Integer quantity;
    /** Taille individuelle (lot de 1). Ignorée si {@code quantity > 1} (cf. lotMinSize/lotMaxSize). */
    public Integer size;
    public Integer weight;
    public boolean kept;
    /** Bornes de taille d'un lot (quantity > 1) ; synthétisées dans {@code catch.size_class}. */
    public Integer lotMinSize;
    public Integer lotMaxSize;

    /** Technique propre à la capture ; à défaut, celle de la sortie s'applique. */
    public UUID techniqueId;
    public String baitOrLure; // TODO(#145)
    public LocalTime captureTime; // TODO(#145)
    /** naturelle / deversement / inconnue — pertinent seulement si espèce = TRF. */
    public String troutOrigin; // TODO(#145)
    public boolean tagged; // TODO(#145)
    public String tagReference; // TODO(#145)
}
