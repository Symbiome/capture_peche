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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Bloc facultatif « Session souvenir » saisi manuellement pour un pêcheur enquêté (#144) :
 * sa dernière sortie passée, sans heures précises (période de la journée seulement). Une
 * seule capture (ou bredouille), comme sur la feuille XLSX du même nom — pas de notion de
 * lot de sorties, la « session souvenir » désigne un seul souvenir de sortie passée.
 */
public class SurveySouvenirBean {

    public LocalDate day;
    public String dayPeriod;

    // Localisation : id prioritaire (clic-carte), repli par nom.
    public UUID waterEntityId;
    public String sitePeche;

    public String fishingMode;
    public UUID techniqueId;
    public Integer rodCount;
    public String baitOrLure;

    public UUID expectedSpeciesId;
    public boolean noExpectedSpecies;

    public boolean bredouille;
    public SurveyCatchBean capture;
}
