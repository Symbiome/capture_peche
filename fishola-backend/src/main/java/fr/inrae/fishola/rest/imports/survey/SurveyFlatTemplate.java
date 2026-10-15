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

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gabarit CSV plat téléchargeable de l'import « enquête terrain » (#242) : l'en-tête de
 * {@link SurveySchema#FLAT_HEADER} et des lignes d'exemple cohérentes (capture
 * individuelle, lot, carpiste sur plusieurs jours bredouille, souvenir à deux captures).
 * Construit depuis les constantes du schéma, il ne peut pas dériver du format attendu.
 */
public final class SurveyFlatTemplate {

    private SurveyFlatTemplate() {}

    public static final String FILE_NAME = "gabarit-enquete-terrain.csv";

    private static final String SESSION = "ENQ-001;Lac d'Annecy;01/07/2026;08:00;12:00;J. Dupont;2;0";

    static final List<String> EXAMPLE_ROWS = List.of(
            "en_cours;" + SESSION + ";ENQ-001-01;09:00;07:30;11:00;;P001;74;;;;Perche;bord statique;Pêche au coup;1;;"
                    + "non;Perche;25;1;oui;;",
            "en_cours;" + SESSION + ";ENQ-001-01;09:00;07:30;11:00;;P001;74;;;;Perche;bord statique;Pêche au coup;1;;"
                    + "non;Gardon;;6;non;10;15",
            "en_cours;" + SESSION + ";ENQ-001-02;10:30;06:00;10:00;03/07/2026;P002;73;;;;Carpe commune;bord statique;"
                    + "Pêche de la carpe de nuit;2;bouillette;oui;;;;;;",
            "souvenir;" + SESSION + ";;;;;;P001;74;Lac d'Annecy;15/06/2026;matin;;bord statique;Pêche au coup;1;;"
                    + "non;Perche;22;1;oui;;",
            "souvenir;" + SESSION + ";;;;;;P001;74;Lac d'Annecy;15/06/2026;matin;;bord statique;Pêche au coup;1;;"
                    + "non;Brochet;60;1;non;;");

    /** Gabarit CSV (UTF-8 avec BOM, pour qu'Excel affiche correctement les accents). */
    public static byte[] build() {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(String.join(";", SurveySchema.FLAT_HEADER)).append("\r\n");
        EXAMPLE_ROWS.forEach(row -> csv.append(row).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
