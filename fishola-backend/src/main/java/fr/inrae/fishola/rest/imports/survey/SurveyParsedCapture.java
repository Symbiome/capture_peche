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

import fr.inrae.fishola.entities.enums.FishingMode;

import java.util.UUID;

/**
 * Une ligne validée de la feuille « Capture pêcheur » (#144) : décrit à la fois la sortie
 * de l'enquêté (champs répétés sur chaque ligne d'un même {@code (sortieCode, anglerCode)},
 * même principe que {@code session_ref} côté carnet volontaire) et, le cas échéant, une
 * capture.
 */
public class SurveyParsedCapture {

    public int line;
    public String sortieCode;
    public String anglerCode;

    // Pêcheur enquêté (non nominatif) : première occurrence du code retenue pour tout le fichier.
    public String originDepartment;
    public String originCountry;

    // Niveau sortie de l'enquêté (répété sur chaque ligne du même sortieCode/anglerCode).
    public FishingMode fishingMode;
    public UUID techniqueId;
    public Integer rodCount;
    public String baitOrLure;
    public UUID expectedSpeciesId;
    public boolean bredouille;

    // Niveau capture.
    public boolean hasCapture;
    public UUID speciesId;
    public Integer size;
    public Integer quantity;
    public boolean kept;
    public Integer lotMinSize;
    public Integer lotMaxSize;
}
