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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Saisie manuelle d'une sortie « carnet volontaire » (#143) + ses captures. Pendant de
 * {@link fr.inrae.fishola.rest.imports.ManualTripBean} (#72), dédié à ce format.
 */
public class CarnetVolontaireTripBean {

    public LocalDate day;
    public LocalTime startTime;
    public LocalTime endTime;

    // Localisation : id prioritaire (clic-carte), repli par nom.
    public UUID waterEntityId;
    public String eauNom;
    public String commune;

    public String fishingMode;
    public UUID techniqueId;
    public UUID secondaryTechniqueId;
    public Integer rodCount;
    public String baitOrLure; // TODO(#145) : validé, non persisté

    /** Espèce recherchée ; ignoré si {@code noExpectedSpecies}. */
    public UUID expectedSpeciesId; // TODO(#145) : validé, non persisté
    public boolean noExpectedSpecies; // modalité « Aucune »
    public String observations; // TODO(#145) : validé, non persisté

    public boolean bredouille;
    public List<CarnetVolontaireCatchBean> captures;
}
