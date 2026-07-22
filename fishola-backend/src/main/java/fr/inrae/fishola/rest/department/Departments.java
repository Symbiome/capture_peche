package fr.inrae.fishola.rest.department;

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

import java.util.Map;
import java.util.Optional;

/**
 * French department reference (code → name), for the offline "download a
 * department" packs (#54). Kept as an embedded constant rather than a DB table:
 * the list of departments is a fixed national referential, so a static map
 * avoids a migration and a data load. The department <em>code</em> itself is
 * derived at query time from the commune INSEE code (see {@code DepartmentDao}):
 * this map only enriches those codes with a human-readable name.
 */
public final class Departments {

    private Departments() {
    }

    /** Metropolitan (with Corsica 2A/2B) + overseas departments (DOM). */
    public static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("01", "Ain"),
            Map.entry("02", "Aisne"),
            Map.entry("03", "Allier"),
            Map.entry("04", "Alpes-de-Haute-Provence"),
            Map.entry("05", "Hautes-Alpes"),
            Map.entry("06", "Alpes-Maritimes"),
            Map.entry("07", "Ardèche"),
            Map.entry("08", "Ardennes"),
            Map.entry("09", "Ariège"),
            Map.entry("10", "Aube"),
            Map.entry("11", "Aude"),
            Map.entry("12", "Aveyron"),
            Map.entry("13", "Bouches-du-Rhône"),
            Map.entry("14", "Calvados"),
            Map.entry("15", "Cantal"),
            Map.entry("16", "Charente"),
            Map.entry("17", "Charente-Maritime"),
            Map.entry("18", "Cher"),
            Map.entry("19", "Corrèze"),
            Map.entry("2A", "Corse-du-Sud"),
            Map.entry("2B", "Haute-Corse"),
            Map.entry("21", "Côte-d'Or"),
            Map.entry("22", "Côtes-d'Armor"),
            Map.entry("23", "Creuse"),
            Map.entry("24", "Dordogne"),
            Map.entry("25", "Doubs"),
            Map.entry("26", "Drôme"),
            Map.entry("27", "Eure"),
            Map.entry("28", "Eure-et-Loir"),
            Map.entry("29", "Finistère"),
            Map.entry("30", "Gard"),
            Map.entry("31", "Haute-Garonne"),
            Map.entry("32", "Gers"),
            Map.entry("33", "Gironde"),
            Map.entry("34", "Hérault"),
            Map.entry("35", "Ille-et-Vilaine"),
            Map.entry("36", "Indre"),
            Map.entry("37", "Indre-et-Loire"),
            Map.entry("38", "Isère"),
            Map.entry("39", "Jura"),
            Map.entry("40", "Landes"),
            Map.entry("41", "Loir-et-Cher"),
            Map.entry("42", "Loire"),
            Map.entry("43", "Haute-Loire"),
            Map.entry("44", "Loire-Atlantique"),
            Map.entry("45", "Loiret"),
            Map.entry("46", "Lot"),
            Map.entry("47", "Lot-et-Garonne"),
            Map.entry("48", "Lozère"),
            Map.entry("49", "Maine-et-Loire"),
            Map.entry("50", "Manche"),
            Map.entry("51", "Marne"),
            Map.entry("52", "Haute-Marne"),
            Map.entry("53", "Mayenne"),
            Map.entry("54", "Meurthe-et-Moselle"),
            Map.entry("55", "Meuse"),
            Map.entry("56", "Morbihan"),
            Map.entry("57", "Moselle"),
            Map.entry("58", "Nièvre"),
            Map.entry("59", "Nord"),
            Map.entry("60", "Oise"),
            Map.entry("61", "Orne"),
            Map.entry("62", "Pas-de-Calais"),
            Map.entry("63", "Puy-de-Dôme"),
            Map.entry("64", "Pyrénées-Atlantiques"),
            Map.entry("65", "Hautes-Pyrénées"),
            Map.entry("66", "Pyrénées-Orientales"),
            Map.entry("67", "Bas-Rhin"),
            Map.entry("68", "Haut-Rhin"),
            Map.entry("69", "Rhône"),
            Map.entry("70", "Haute-Saône"),
            Map.entry("71", "Saône-et-Loire"),
            Map.entry("72", "Sarthe"),
            Map.entry("73", "Savoie"),
            Map.entry("74", "Haute-Savoie"),
            Map.entry("75", "Paris"),
            Map.entry("76", "Seine-Maritime"),
            Map.entry("77", "Seine-et-Marne"),
            Map.entry("78", "Yvelines"),
            Map.entry("79", "Deux-Sèvres"),
            Map.entry("80", "Somme"),
            Map.entry("81", "Tarn"),
            Map.entry("82", "Tarn-et-Garonne"),
            Map.entry("83", "Var"),
            Map.entry("84", "Vaucluse"),
            Map.entry("85", "Vendée"),
            Map.entry("86", "Vienne"),
            Map.entry("87", "Haute-Vienne"),
            Map.entry("88", "Vosges"),
            Map.entry("89", "Yonne"),
            Map.entry("90", "Territoire de Belfort"),
            Map.entry("91", "Essonne"),
            Map.entry("92", "Hauts-de-Seine"),
            Map.entry("93", "Seine-Saint-Denis"),
            Map.entry("94", "Val-de-Marne"),
            Map.entry("95", "Val-d'Oise"),
            Map.entry("971", "Guadeloupe"),
            Map.entry("972", "Martinique"),
            Map.entry("973", "Guyane"),
            Map.entry("974", "La Réunion"),
            Map.entry("975", "Saint-Pierre-et-Miquelon"),
            Map.entry("976", "Mayotte"));

    /** Human-readable name for a department code, or the code itself if unknown. */
    public static String nameOf(String code) {
        return NAMES.getOrDefault(code, code);
    }

    /** Whether the code is a known department code (validates path params). */
    public static boolean isValidCode(String code) {
        return code != null && NAMES.containsKey(code);
    }

    /** Optional name lookup, empty when the code is unknown. */
    public static Optional<String> lookup(String code) {
        return Optional.ofNullable(NAMES.get(code));
    }
}
