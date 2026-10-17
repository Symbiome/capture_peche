package fr.inrae.fishola.rest.imports.survey;

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

import fr.inrae.fishola.entities.enums.DayPeriod;
import fr.inrae.fishola.entities.enums.FishingMode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Une sortie souvenir validée (#144) : une sortie passée du même pêcheur enquêté, sans
 * heures précises (seule la période de la journée est connue), devenue sa propre {@code
 * Trip} indépendante. Feuille « Session souvenir » : une ligne, une capture au plus, sans
 * code session (déduit de la session où le pêcheur a été enquêté). CSV plat (#242) : les
 * lignes souvenir d'un même (code session, code pêcheur), une capture chacune.
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

    /** Session où le pêcheur a été enquêté ; nul pour la feuille XLSX, qui ne la porte pas. */
    public String sessionCode;

    /** Captures de la sortie souvenir, vide si bredouille (#236). */
    public List<SurveyParsedCatch> catches = new ArrayList<>();
}
