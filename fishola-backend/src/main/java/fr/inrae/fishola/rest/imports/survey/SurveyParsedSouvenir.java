package fr.inrae.fishola.rest.imports.survey;

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

import fr.inrae.fishola.entities.enums.DayPeriod;
import fr.inrae.fishola.entities.enums.FishingMode;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Une ligne validée de la feuille facultative « Session souvenir » (#144) : une sortie
 * passée du même pêcheur enquêté, sans heures précises (seule la période de la journée est
 * connue). La feuille ne porte ni code session ni code sortie : chaque ligne devient sa
 * propre {@code Trip} indépendante, rattachée au seul {@code anglerCode} (déjà connu d'une
 * ligne de la feuille « Capture pêcheur », sans quoi la ligne est rejetée).
 */
public class SurveyParsedSouvenir {

    public int line;
    public String anglerCode;
    public UUID waterEntityId;
    public LocalDate day;
    public DayPeriod dayPeriod;

    public FishingMode fishingMode;
    public UUID techniqueId;
    public Integer rodCount;
    public String baitOrLure;
    public UUID expectedSpeciesId;
    public boolean bredouille;

    public boolean hasCapture;
    public UUID speciesId;
    public Integer size;
    public Integer quantity;
    public boolean kept;
    public Integer lotMinSize;
    public Integer lotMaxSize;
}
