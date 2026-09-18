package fr.inrae.fishola.gamification;

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

/**
 * Département (métropole) -> bassin hydrographique ("agence de l'eau"), pour le badge
 * « Explorateur » (#146, >= 3 bassins différents). Embarqué en dur comme {@link
 * fr.inrae.fishola.rest.department.Departments} plutôt qu'en table : référentiel national
 * fixe, pas de géométrie propre à ce projet (contrairement à {@code departement.geom}).
 *
 * <p><b>Approximation v1</b> : plusieurs départements sont à cheval sur deux bassins (ex.
 * Ardennes, Lozère, Côte-d'Or) -- un seul bassin "dominant" est retenu ici. À affiner si un
 * badge géographique plus précis est nécessaire un jour (croisement géométrique avec les
 * limites officielles des bassins plutôt qu'un département entier).
 */
public final class Basins {

    private Basins() {}

    /** Les 6 bassins mainland cités dans #146 (hors DOM, non couverts par l'UFBRMC). */
    public static final Map<String, String> DEPARTMENT_TO_BASIN = Map.ofEntries(
            Map.entry("01", "Rhône-Méditerranée & Corse"),
            Map.entry("02", "Artois-Picardie"),
            Map.entry("03", "Loire-Bretagne"),
            Map.entry("04", "Rhône-Méditerranée & Corse"),
            Map.entry("05", "Rhône-Méditerranée & Corse"),
            Map.entry("06", "Rhône-Méditerranée & Corse"),
            Map.entry("07", "Rhône-Méditerranée & Corse"),
            Map.entry("08", "Rhin-Meuse"),
            Map.entry("09", "Adour-Garonne"),
            Map.entry("10", "Seine-Normandie"),
            Map.entry("11", "Rhône-Méditerranée & Corse"),
            Map.entry("12", "Adour-Garonne"),
            Map.entry("13", "Rhône-Méditerranée & Corse"),
            Map.entry("14", "Seine-Normandie"),
            Map.entry("15", "Loire-Bretagne"),
            Map.entry("16", "Adour-Garonne"),
            Map.entry("17", "Adour-Garonne"),
            Map.entry("18", "Loire-Bretagne"),
            Map.entry("19", "Adour-Garonne"),
            Map.entry("2A", "Rhône-Méditerranée & Corse"),
            Map.entry("2B", "Rhône-Méditerranée & Corse"),
            Map.entry("21", "Seine-Normandie"),
            Map.entry("22", "Loire-Bretagne"),
            Map.entry("23", "Loire-Bretagne"),
            Map.entry("24", "Adour-Garonne"),
            Map.entry("25", "Rhône-Méditerranée & Corse"),
            Map.entry("26", "Rhône-Méditerranée & Corse"),
            Map.entry("27", "Seine-Normandie"),
            Map.entry("28", "Seine-Normandie"),
            Map.entry("29", "Loire-Bretagne"),
            Map.entry("30", "Rhône-Méditerranée & Corse"),
            Map.entry("31", "Adour-Garonne"),
            Map.entry("32", "Adour-Garonne"),
            Map.entry("33", "Adour-Garonne"),
            Map.entry("34", "Rhône-Méditerranée & Corse"),
            Map.entry("35", "Loire-Bretagne"),
            Map.entry("36", "Loire-Bretagne"),
            Map.entry("37", "Loire-Bretagne"),
            Map.entry("38", "Rhône-Méditerranée & Corse"),
            Map.entry("39", "Rhône-Méditerranée & Corse"),
            Map.entry("40", "Adour-Garonne"),
            Map.entry("41", "Loire-Bretagne"),
            Map.entry("42", "Loire-Bretagne"),
            Map.entry("43", "Loire-Bretagne"),
            Map.entry("44", "Loire-Bretagne"),
            Map.entry("45", "Loire-Bretagne"),
            Map.entry("46", "Adour-Garonne"),
            Map.entry("47", "Adour-Garonne"),
            Map.entry("48", "Rhône-Méditerranée & Corse"),
            Map.entry("49", "Loire-Bretagne"),
            Map.entry("50", "Seine-Normandie"),
            Map.entry("51", "Seine-Normandie"),
            Map.entry("52", "Seine-Normandie"),
            Map.entry("53", "Loire-Bretagne"),
            Map.entry("54", "Rhin-Meuse"),
            Map.entry("55", "Rhin-Meuse"),
            Map.entry("56", "Loire-Bretagne"),
            Map.entry("57", "Rhin-Meuse"),
            Map.entry("58", "Loire-Bretagne"),
            Map.entry("59", "Artois-Picardie"),
            Map.entry("60", "Seine-Normandie"),
            Map.entry("61", "Seine-Normandie"),
            Map.entry("62", "Artois-Picardie"),
            Map.entry("63", "Loire-Bretagne"),
            Map.entry("64", "Adour-Garonne"),
            Map.entry("65", "Adour-Garonne"),
            Map.entry("66", "Rhône-Méditerranée & Corse"),
            Map.entry("67", "Rhin-Meuse"),
            Map.entry("68", "Rhin-Meuse"),
            Map.entry("69", "Rhône-Méditerranée & Corse"),
            Map.entry("70", "Rhône-Méditerranée & Corse"),
            Map.entry("71", "Rhône-Méditerranée & Corse"),
            Map.entry("72", "Loire-Bretagne"),
            Map.entry("73", "Rhône-Méditerranée & Corse"),
            Map.entry("74", "Rhône-Méditerranée & Corse"),
            Map.entry("75", "Seine-Normandie"),
            Map.entry("76", "Seine-Normandie"),
            Map.entry("77", "Seine-Normandie"),
            Map.entry("78", "Seine-Normandie"),
            Map.entry("79", "Loire-Bretagne"),
            Map.entry("80", "Artois-Picardie"),
            Map.entry("81", "Adour-Garonne"),
            Map.entry("82", "Adour-Garonne"),
            Map.entry("83", "Rhône-Méditerranée & Corse"),
            Map.entry("84", "Rhône-Méditerranée & Corse"),
            Map.entry("85", "Loire-Bretagne"),
            Map.entry("86", "Loire-Bretagne"),
            Map.entry("87", "Loire-Bretagne"),
            Map.entry("88", "Rhin-Meuse"),
            Map.entry("89", "Seine-Normandie"),
            Map.entry("90", "Rhin-Meuse"),
            Map.entry("91", "Seine-Normandie"),
            Map.entry("92", "Seine-Normandie"),
            Map.entry("93", "Seine-Normandie"),
            Map.entry("94", "Seine-Normandie"),
            Map.entry("95", "Seine-Normandie"));

    /** Nom du bassin pour ce département, ou {@code null} si hors métropole/inconnu. */
    public static String basinOf(String departmentCode) {
        return DEPARTMENT_TO_BASIN.get(departmentCode);
    }
}
