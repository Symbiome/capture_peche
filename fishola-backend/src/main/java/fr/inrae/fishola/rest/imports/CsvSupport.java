package fr.inrae.fishola.rest.imports;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture du CSV (séparateur « ; », UTF-8) et coercition des valeurs.
 */
public final class CsvSupport {

    private CsvSupport() {}

    private static final char DELIMITER = ';';
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Résultat de lecture : en-tête (strippé) + enregistrements colonne -> valeur (strippée). */
    public record Parsed(List<String> header, List<Map<String, String>> records) {}

    public static Parsed readRows(String text) {
        List<List<String>> rows = tokenize(text);
        if (rows.isEmpty()) {
            return new Parsed(List.of(), List.of());
        }
        List<String> header = new ArrayList<>();
        for (String h : rows.get(0)) {
            header.add(h.strip());
        }
        List<Map<String, String>> records = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> raw = rows.get(r);
            boolean allBlank = raw.stream().allMatch(c -> c == null || c.strip().isEmpty());
            if (allBlank) {
                continue; // ligne entièrement vide
            }
            Map<String, String> record = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
                String cell = i < raw.size() ? raw.get(i).strip() : "";
                record.put(header.get(i), cell);
            }
            records.add(record);
        }
        return new Parsed(header, records);
    }

    /** Découpe un texte CSV en lignes de champs, en gérant les champs entre guillemets. */
    private static List<List<String>> tokenize(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == DELIMITER) {
                current.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                current.add(field.toString());
                field.setLength(0);
                rows.add(current);
                current = new ArrayList<>();
            } else if (c == '\r') {
                // ignoré (CRLF) : le \n suivant clôt la ligne
            } else {
                field.append(c);
            }
        }
        // dernier champ / dernière ligne (fichier sans retour final)
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current);
        }
        return rows;
    }

    public static LocalDate parseDate(String value) {
        return LocalDate.parse(value.strip(), DATE_FMT);
    }

    public static LocalTime parseTime(String value) {
        return LocalTime.parse(value.strip(), TIME_FMT);
    }

    public static int parseInt(String value) {
        return Integer.parseInt(value.strip());
    }

    /** oui/o/true/1 -> true ; non/n/false/0 -> false ; sinon exception. */
    public static boolean parseBoolOuiNon(String value) {
        String v = (value == null ? "" : value).strip().toLowerCase();
        if (v.equals("oui") || v.equals("o") || v.equals("true") || v.equals("1")) {
            return true;
        }
        if (v.equals("non") || v.equals("n") || v.equals("false") || v.equals("0")) {
            return false;
        }
        throw new IllegalArgumentException("valeur oui/non attendue, reçu « " + value + " »");
    }

    public static boolean isBlank(String value) {
        return value == null || value.strip().isEmpty();
    }
}
