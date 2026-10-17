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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Un enregistrement à valider, nommé par les colonnes du classeur XLSX (#144), et le numéro
 * de ligne du fichier où signaler ses erreurs : ligne de la feuille, ou ligne du CSV plat
 * dont il est issu (#242).
 *
 * @param line numéro de ligne dans le fichier (en-tête = 1)
 * @param values valeur par libellé de colonne du classeur
 */
public record SurveyRow(int line, Map<String, String> values) {

    /**
     * Enregistrements d'une feuille, numérotés à partir de la ligne 2 (sous l'en-tête).
     *
     * @param records enregistrements de la feuille, dans l'ordre
     * @return lignes numérotées
     */
    public static List<SurveyRow> ofSheet(List<Map<String, String>> records) {
        List<SurveyRow> rows = new ArrayList<>();
        int line = 1;
        for (Map<String, String> rec : records) {
            line++;
            rows.add(new SurveyRow(line, rec));
        }
        return rows;
    }
}
