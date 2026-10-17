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

import fr.inrae.fishola.rest.imports.CsvSupport;
import fr.inrae.fishola.rest.imports.ImportError;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Correspondance entre les colonnes du CSV plat (#242) et celles des feuilles du classeur
 * XLSX (#144) : une ligne plate est découpée en enregistrements de feuille, validés par
 * les mêmes règles ; les erreurs sont ensuite rapportées sous le nom de colonne du CSV.
 */
final class SurveyFlatColumns {

    private SurveyFlatColumns() {}

    private static final String ESPECE_RECHERCHEE = "Espèce recherchée";

    static final Map<String, String> SESSION = columns(
            "Code session", "code_session",
            "Secteur", "secteur",
            "Date", "date_session",
            SurveySchema.COL_SESSION_START, "heure_debut_session",
            SurveySchema.COL_SESSION_END, "heure_fin_session",
            SurveySchema.COL_OPERATOR, "nom_operateur",
            "Nb pêcheurs carnassiers du bord observés non-enquêtés", "nb_bord_non_enquetes",
            "Nb pêcheurs carnassiers en bateau observés non-enquêtés", "nb_bateau_non_enquetes");

    static final Map<String, String> SORTIE = columns(
            "Code session", "code_session",
            "Code sortie", "code_sortie",
            "Heure du contrôle", "heure_controle",
            "Heure de début de pêche", "heure_debut",
            "Heure de fin de pêche prévue", "heure_fin_prevue",
            "Date de fin de pêche prévue", "date_fin_prevue");

    private static final Map<String, String> TRIP_AND_CATCH = columns(
            ESPECE_RECHERCHEE, "espece_recherchee",
            "Mode de pêche", "mode_peche",
            "Technique", "technique",
            "Nombre de lignes", "nombre_lignes",
            "Appât / type de leurre", "appat_leurre",
            "Bredouille", "bredouille",
            "Espèce (capture)", "capture_espece",
            "Taille", "capture_taille",
            "Nombre", "capture_nombre",
            "Poisson conservé", "capture_conservee",
            "Taille min", "capture_taille_min",
            "Taille max", "capture_taille_max");

    static final Map<String, String> CAPTURE = withTripAndCatch(columns(
            "Code sortie", "code_sortie",
            "Code pêcheur", "code_pecheur",
            "Département origine", "departement_origine"));

    static final Map<String, String> SOUVENIR = withTripAndCatch(columns(
            "Code pêcheur", "code_pecheur",
            "Site pêché", "site_peche",
            "Date", "date_souvenir",
            "Période de la journée", "periode_journee"));

    private static final Map<String, Map<String, String>> BY_SHEET = Map.of(
            SurveySchema.SHEET_SESSION, SESSION,
            SurveySchema.SHEET_SORTIE, SORTIE,
            SurveySchema.SHEET_CAPTURE, CAPTURE,
            SurveySchema.SHEET_SOUVENIR, SOUVENIR);

    private static Map<String, String> columns(String... labelThenFlatColumn) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < labelThenFlatColumn.length; i += 2) {
            result.put(labelThenFlatColumn[i], labelThenFlatColumn[i + 1]);
        }
        return result;
    }

    private static Map<String, String> withTripAndCatch(Map<String, String> head) {
        Map<String, String> result = new LinkedHashMap<>(head);
        result.putAll(TRIP_AND_CATCH);
        return result;
    }

    /**
     * Enregistrement d'une feuille, nommé par ses libellés de colonne, tiré d'une ligne plate.
     * Une espèce recherchée vide vaut « Toutes espèces » (#236).
     *
     * @param sheetColumns correspondance libellé de feuille -> colonne du CSV
     * @param flat ligne du CSV, par colonne
     * @return enregistrement de feuille
     */
    static Map<String, String> toSheetRecord(Map<String, String> sheetColumns, Map<String, String> flat) {
        Map<String, String> result = new LinkedHashMap<>();
        sheetColumns.forEach((label, column) -> result.put(label, flat.getOrDefault(column, "")));
        if (result.containsKey(ESPECE_RECHERCHEE) && CsvSupport.isBlank(result.get(ESPECE_RECHERCHEE))) {
            result.put(ESPECE_RECHERCHEE, "Toutes espèces");
        }
        return result;
    }

    /**
     * Erreur de validation d'une feuille, rapportée sous le nom de colonne du CSV plat ; les
     * renvois à une feuille du classeur deviennent des renvois au fichier.
     *
     * @param error erreur produite par la validation d'une feuille
     * @return erreur exprimée dans les termes du CSV plat
     */
    static ImportError toFlatError(ImportError error) {
        String column = error.column();
        if (column != null) {
            int separator = column.indexOf(" / ");
            if (separator > 0) {
                Map<String, String> sheetColumns = BY_SHEET.get(column.substring(0, separator));
                String label = column.substring(separator + 3);
                column = sheetColumns == null ? label : sheetColumns.getOrDefault(label, label);
            }
        }
        String message = error.message().replaceAll("inconnu de la feuille « [^»]+ »", "invalide ou absent du fichier")
                .replaceAll("absent de la feuille « [^»]+ »", "absent du fichier");
        return new ImportError(error.line(), column, error.stage(), error.code(), message);
    }
}
