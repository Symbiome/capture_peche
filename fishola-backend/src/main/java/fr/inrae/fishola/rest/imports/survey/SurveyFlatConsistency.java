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

import fr.inrae.fishola.rest.imports.CsvSupport;
import fr.inrae.fishola.rest.imports.ImportError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Cohérence des valeurs répétées du CSV plat (#242) : au sein d'une session, d'une sortie,
 * d'une sortie de pêcheur ou d'un pêcheur, chaque valeur répétée doit être identique à
 * celle de la première ligne du groupe, après normalisation (espaces, casse, accents ;
 * espèce recherchée vide = « Toutes espèces »). Chaque ligne divergente est signalée.
 */
final class SurveyFlatConsistency {

    private SurveyFlatConsistency() {}

    /** Une ligne du CSV plat et son numéro dans le fichier (en-tête = 1). */
    record FlatRow(int line, Map<String, String> values) {

        String get(String column) {
            String value = values.get(column);
            return value == null ? "" : value.strip();
        }

        boolean isSouvenir() {
            return SurveySchema.TYPE_SOUVENIR.equals(SurveyImportService.normalize(get("type_sortie")).replace(' ', '_'));
        }

        String sessionKey() {
            return get("code_session");
        }

        String sortieKey() {
            return get("code_sortie");
        }

        String tripKey() {
            return blankIfAnyBlank(get("code_sortie"), get("code_pecheur"));
        }

        String souvenirKey() {
            return blankIfAnyBlank(get("code_session"), get("code_pecheur"));
        }

        private static String blankIfAnyBlank(String first, String second) {
            return first.isEmpty() || second.isEmpty() ? "" : first + " " + second;
        }
    }

    private static final List<String> SESSION_FIELDS = List.of("secteur", "date_session", "heure_debut_session",
            "heure_fin_session", "nom_operateur", "nb_bord_non_enquetes", "nb_bateau_non_enquetes");
    private static final List<String> SORTIE_FIELDS = List.of("code_session", "heure_controle", "heure_debut",
            "heure_fin_prevue", "date_fin_prevue");
    private static final List<String> TRIP_FIELDS = List.of("espece_recherchee", "mode_peche", "technique",
            "nombre_lignes", "appat_leurre", "bredouille");
    private static final List<String> SOUVENIR_FIELDS = Stream.concat(
            Stream.of("site_peche", "date_souvenir", "periode_journee"), TRIP_FIELDS.stream()).toList();
    private static final List<String> ANGLER_FIELDS = List.of("departement_origine");

    /**
     * Erreurs de cohérence de toutes les lignes : valeurs répétées divergentes et sorties
     * bredouille décrites sur plusieurs lignes.
     *
     * @param rows lignes du CSV de type connu, codes renseignés
     * @return erreurs, une par ligne et par colonne divergente
     */
    static List<ImportError> check(List<FlatRow> rows) {
        List<FlatRow> enCours = rows.stream().filter(r -> !r.isSouvenir()).toList();
        List<FlatRow> souvenirs = rows.stream().filter(FlatRow::isSouvenir).toList();
        List<ImportError> errors = new ArrayList<>();
        errors.addAll(checkGroups(rows, FlatRow::sessionKey, SESSION_FIELDS,
                SurveySchema.STRUCT_INCONSISTENT_SESSION, r -> "la session « " + r.sessionKey() + " »"));
        errors.addAll(checkGroups(enCours, FlatRow::sortieKey, SORTIE_FIELDS,
                SurveySchema.STRUCT_INCONSISTENT_SORTIE, r -> "la sortie « " + r.sortieKey() + " »"));
        errors.addAll(checkGroups(enCours, FlatRow::tripKey, TRIP_FIELDS, SurveySchema.STRUCT_INCONSISTENT_TRIP,
                r -> "la sortie « " + r.sortieKey() + " » du pêcheur « " + r.get("code_pecheur") + " »"));
        errors.addAll(checkGroups(souvenirs, FlatRow::souvenirKey, SOUVENIR_FIELDS,
                SurveySchema.STRUCT_INCONSISTENT_TRIP,
                r -> "la sortie souvenir du pêcheur « " + r.get("code_pecheur") + " » (session « " + r.sessionKey()
                        + " »)"));
        errors.addAll(checkGroups(rows, r -> r.get("code_pecheur"), ANGLER_FIELDS,
                SurveySchema.STRUCT_INCONSISTENT_ANGLER, r -> "le pêcheur « " + r.get("code_pecheur") + " »"));
        errors.addAll(checkBredouille(enCours, FlatRow::tripKey));
        errors.addAll(checkBredouille(souvenirs, FlatRow::souvenirKey));
        return errors;
    }

    /** Lignes regroupées par clé, dans l'ordre du fichier ; clé vide ignorée. */
    static Map<String, List<FlatRow>> groupBy(List<FlatRow> rows, Function<FlatRow, String> key) {
        Map<String, List<FlatRow>> groups = new LinkedHashMap<>();
        for (FlatRow row : rows) {
            String k = key.apply(row);
            if (!k.isEmpty()) {
                groups.computeIfAbsent(k, x -> new ArrayList<>()).add(row);
            }
        }
        return groups;
    }

    private static List<ImportError> checkGroups(List<FlatRow> rows, Function<FlatRow, String> key,
                                                 List<String> fields, String code,
                                                 Function<FlatRow, String> groupLabel) {
        List<ImportError> errors = new ArrayList<>();
        for (List<FlatRow> group : groupBy(rows, key).values()) {
            FlatRow reference = group.get(0);
            for (FlatRow row : group.subList(1, group.size())) {
                for (String field : fields) {
                    if (!canonical(field, row.get(field)).equals(canonical(field, reference.get(field)))) {
                        errors.add(new ImportError(row.line(), field, SurveySchema.STRUCTUREL, code,
                                "« " + row.get(field) + " » diffère de la ligne " + reference.line() + " (valeur attendue « "
                                        + reference.get(field) + " ») pour " + groupLabel.apply(row)));
                    }
                }
            }
        }
        return errors;
    }

    private static List<ImportError> checkBredouille(List<FlatRow> rows, Function<FlatRow, String> key) {
        List<ImportError> errors = new ArrayList<>();
        for (List<FlatRow> group : groupBy(rows, key).values()) {
            FlatRow reference = group.get(0);
            if (!isOui(reference.get("bredouille"))) {
                continue;
            }
            for (FlatRow row : group.subList(1, group.size())) {
                errors.add(new ImportError(row.line(), "bredouille", SurveySchema.METIER, SurveySchema.METIER_BREDOUILLE,
                        "sortie déclarée bredouille à la ligne " + reference.line()
                                + " : une seule ligne attendue, sans capture"));
            }
        }
        return errors;
    }

    private static boolean isOui(String value) {
        try {
            return CsvSupport.parseBoolOuiNon(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Valeur comparée : normalisée, espèce recherchée vide équivalente à « Toutes espèces ». */
    private static String canonical(String field, String value) {
        String normalized = SurveyImportService.normalize(value);
        if ("espece_recherchee".equals(field) && normalized.isEmpty()) {
            return SurveySchema.ESPECE_RECHERCHEE_TOUTES;
        }
        return normalized;
    }
}
