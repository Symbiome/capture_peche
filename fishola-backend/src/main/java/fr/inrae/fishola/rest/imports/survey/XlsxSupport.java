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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lecture d'un classeur XLSX multi-feuilles (import « enquête terrain », #144) : chaque
 * feuille devient un {@link SheetData} colonne (en-tête) -> valeur, sur le même principe
 * que {@code CsvSupport.readRows} pour le CSV, afin de réutiliser la même coercition de
 * valeurs ({@code CsvSupport.parseDate}, {@code parseTime}, {@code parseInt}...).
 *
 * <p>Les cellules date/heure Excel réelles (numériques, format daté) sont reconverties en
 * chaînes {@code JJ/MM/AAAA} / {@code HH:mm} selon que leur format contient une année —
 * seule distinction fiable entre une cellule "date" et une cellule "heure" côté Excel,
 * qui stocke les deux comme un simple nombre de jours depuis une époque.
 */
public final class XlsxSupport {

    private XlsxSupport() {}

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Résultat de lecture d'une feuille : en-tête (strippé) + enregistrements colonne -> valeur (strippée). */
    public record SheetData(List<String> header, List<Map<String, String>> records) {}

    /** Lit toutes les feuilles d'un classeur, indexées par nom de feuille (strippé). */
    public static Map<String, SheetData> readWorkbook(byte[] bytes) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Map<String, SheetData> result = new LinkedHashMap<>();
            DataFormatter formatter = new DataFormatter(Locale.FRENCH);
            for (Sheet sheet : workbook) {
                result.put(sheet.getSheetName().strip(), readSheet(sheet, formatter));
            }
            return result;
        } catch (IOException | org.apache.poi.EmptyFileException | org.apache.poi.ooxml.POIXMLException e) {
            throw new IllegalArgumentException("Classeur XLSX illisible ou corrompu", e);
        } catch (UncheckedIOException e) {
            throw new IllegalArgumentException("Classeur XLSX illisible ou corrompu", e);
        }
    }

    private static SheetData readSheet(Sheet sheet, DataFormatter formatter) {
        Iterator<Row> rows = sheet.iterator();
        if (!rows.hasNext()) {
            return new SheetData(List.of(), List.of());
        }
        Row headerRow = rows.next();
        List<String> header = new ArrayList<>();
        int lastCol = headerRow.getLastCellNum();
        for (int c = 0; c < lastCol; c++) {
            header.add(cellText(headerRow.getCell(c), formatter).strip());
        }

        List<Map<String, String>> records = new ArrayList<>();
        while (rows.hasNext()) {
            Row row = rows.next();
            Map<String, String> record = new LinkedHashMap<>();
            boolean allBlank = true;
            for (int c = 0; c < header.size(); c++) {
                String value = cellText(row.getCell(c), formatter).strip();
                if (!value.isEmpty()) {
                    allBlank = false;
                }
                record.put(header.get(c), value);
            }
            if (!allBlank) {
                records.add(record);
            }
        }
        return new SheetData(header, records);
    }

    private static String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            String pattern = cell.getCellStyle().getDataFormatString();
            LocalDateTime value = cell.getLocalDateTimeCellValue();
            boolean isDate = pattern != null && pattern.toLowerCase(Locale.ROOT).contains("y");
            return isDate ? value.toLocalDate().format(DAY_FMT) : value.toLocalTime().format(TIME_FMT);
        }
        return formatter.formatCellValue(cell);
    }
}
