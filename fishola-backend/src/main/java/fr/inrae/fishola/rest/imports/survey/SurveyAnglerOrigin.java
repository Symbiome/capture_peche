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

import fr.inrae.fishola.rest.department.Departments;
import fr.inrae.fishola.rest.imports.CsvSupport;

import java.text.Normalizer;
import java.util.Map;

/**
 * Origine résolue d'un pêcheur enquêté (#144) : code département FR, ou pays en repli
 * pour un non-résident. Les deux sont {@code null} si non renseignée (champ facultatif).
 */
public record SurveyAnglerOrigin(String department, String country) {

    public static final SurveyAnglerOrigin NONE = new SurveyAnglerOrigin(null, null);

    /** « liste départements FR, ou pays si non-résident » (#144) : code, nom, ou pays en repli. */
    public static SurveyAnglerOrigin resolve(String raw) {
        if (CsvSupport.isBlank(raw)) {
            return NONE;
        }
        String trimmed = raw.strip();
        if (Departments.isValidCode(trimmed)) {
            return new SurveyAnglerOrigin(trimmed, null);
        }
        String normalized = normalize(trimmed);
        for (Map.Entry<String, String> e : Departments.NAMES.entrySet()) {
            if (normalize(e.getValue()).equals(normalized)) {
                return new SurveyAnglerOrigin(e.getKey(), null);
            }
        }
        return new SurveyAnglerOrigin(null, trimmed);
    }

    private static String normalize(String v) {
        String s = Normalizer.normalize(v.strip().toLowerCase(), Normalizer.Form.NFD);
        return s.replaceAll("\\p{M}", "").replace('-', ' ').replaceAll("\\s+", " ").strip();
    }
}
