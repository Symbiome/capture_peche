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

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Gabarit XLSX téléchargeable de l'import « enquête terrain » (#207) : les quatre
 * feuilles de {@link SurveySchema}, en-têtes compris, chacune avec une ligne d'exemple
 * cohérente (la sortie et la capture pointent sur la session, la session souvenir sur le
 * pêcheur enquêté). Construit depuis les constantes du schéma, il ne peut pas dériver
 * du format attendu par {@link SurveyImportService}.
 */
public final class SurveyTemplate {

    private SurveyTemplate() {}

    public static final String FILE_NAME = "gabarit-enquete-terrain.xlsx";

    private static final int MIN_COLUMN_CHARS = 12;

    static final List<String> EXAMPLE_SESSION = List.of(
            "ENQ-001", "Lac d'Annecy", "01/07/2026", "2", "0");

    static final List<String> EXAMPLE_SORTIE = List.of(
            "ENQ-001", "ENQ-001-01", "09:00", "07:30", "11:00");

    static final List<String> EXAMPLE_CAPTURE = List.of(
            "ENQ-001-01", "P001", "74", "Aucune", "bord statique", "Pêche au coup", "1", "",
            "non", "Perche", "25", "1", "oui", "", "");

    static final List<String> EXAMPLE_SOUVENIR = List.of(
            "P001", "Lac d'Annecy", "15/06/2026", "matin", "Aucune", "bord statique",
            "Pêche au coup", "1", "", "non", "Perche", "22", "1", "oui", "", "");

    /** Classeur du gabarit, sérialisé en XLSX. */
    public static byte[] build() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            writeSheet(workbook, headerStyle, SurveySchema.SHEET_SESSION, SurveySchema.HEADER_SESSION, EXAMPLE_SESSION);
            writeSheet(workbook, headerStyle, SurveySchema.SHEET_SORTIE, SurveySchema.HEADER_SORTIE, EXAMPLE_SORTIE);
            writeSheet(workbook, headerStyle, SurveySchema.SHEET_CAPTURE, SurveySchema.HEADER_CAPTURE, EXAMPLE_CAPTURE);
            writeSheet(workbook, headerStyle, SurveySchema.SHEET_SOUVENIR, SurveySchema.HEADER_SOUVENIR,
                    EXAMPLE_SOUVENIR);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }

    // Valeurs saisies en texte (dates JJ/MM/AAAA, heures HH:mm) : lues telles quelles
    // par XlsxSupport, sans dépendre du format de cellule d'Excel.
    private static void writeSheet(XSSFWorkbook workbook, CellStyle headerStyle, String name,
                                   List<String> header, List<String> example) {
        Sheet sheet = workbook.createSheet(name);
        Row headerRow = sheet.createRow(0);
        Row exampleRow = sheet.createRow(1);
        for (int c = 0; c < header.size(); c++) {
            headerRow.createCell(c).setCellValue(header.get(c));
            headerRow.getCell(c).setCellStyle(headerStyle);
            exampleRow.createCell(c).setCellValue(example.get(c));
            int chars = Math.max(MIN_COLUMN_CHARS, header.get(c).length() + 2);
            sheet.setColumnWidth(c, chars * 256);
        }
        sheet.createFreezePane(0, 1);
    }
}
