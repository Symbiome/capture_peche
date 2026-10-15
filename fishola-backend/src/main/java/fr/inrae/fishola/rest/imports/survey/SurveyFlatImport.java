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
import fr.inrae.fishola.rest.imports.ImportResultBean;
import fr.inrae.fishola.rest.imports.survey.SurveyFlatConsistency.FlatRow;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Import « enquête terrain » au format CSV plat (#242) : une ligne par capture (ou une
 * ligne « bredouille »), informations de session, de sortie et de pêcheur répétées. Après
 * contrôle de cohérence des valeurs répétées ({@link SurveyFlatConsistency}), les lignes
 * sont regroupées en sessions, sorties, sorties de pêcheur et sorties souvenir, validées
 * par les règles du classeur XLSX ({@link SurveyImportService}) et persistées par le même
 * chemin d'écriture.
 */
final class SurveyFlatImport {

    private final SurveyImportService service;

    SurveyFlatImport(SurveyImportService service) {
        this.service = service;
    }

    /**
     * Lit, valide et persiste un CSV plat.
     *
     * @param fileBytes contenu du fichier, UTF-8
     * @param file fichier importé, mode et périmètre
     * @return résultat de l'import, erreurs exprimées dans les colonnes du CSV
     */
    ImportResultBean run(byte[] fileBytes, SurveyImportService.ImportFile file) {
        String text = new String(fileBytes, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        CsvSupport.Parsed parsed = CsvSupport.readRows(text);
        if (!SurveySchema.FLAT_HEADER.equals(parsed.header())) {
            return service.persistFailed(file, new ImportError(1, null, SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_HEADER, "en-tête non conforme au modèle CSV plat attendu ("
                            + SurveySchema.FLAT_HEADER.size() + " colonnes séparées par « ; »)"));
        }
        List<FlatRow> rows = new ArrayList<>();
        for (int i = 0; i < parsed.records().size(); i++) {
            rows.add(new FlatRow(i + 2, parsed.records().get(i)));
        }

        List<ImportError> errors = new ArrayList<>();
        List<FlatRow> usable = checkTypesAndCodes(rows, errors);
        List<ImportError> consistencyErrors = SurveyFlatConsistency.check(usable);
        errors.addAll(consistencyErrors);
        Set<Integer> inconsistentLines = new HashSet<>();
        consistencyErrors.forEach(e -> inconsistentLines.add(e.line()));
        usable = usable.stream().filter(r -> !inconsistentLines.contains(r.line())).toList();

        SurveyImportService.Validated validated = validate(usable, file, errors);
        return service.persist(file, rows.size(), validated);
    }

    /** Lignes de type connu dont les codes obligatoires sont renseignés ; les autres sont signalées. */
    private static List<FlatRow> checkTypesAndCodes(List<FlatRow> rows, List<ImportError> errors) {
        List<FlatRow> usable = new ArrayList<>();
        for (FlatRow row : rows) {
            String type = SurveyImportService.normalize(row.get("type_sortie")).replace(' ', '_');
            if (!SurveySchema.TYPE_EN_COURS.equals(type) && !SurveySchema.TYPE_SOUVENIR.equals(type)) {
                errors.add(new ImportError(row.line(), "type_sortie", SurveySchema.STRUCTUREL,
                        SurveySchema.STRUCT_TYPE_SORTIE, "type de sortie « " + row.get("type_sortie")
                                + " » inconnu (en_cours ou souvenir attendu)"));
            } else if (row.sessionKey().isEmpty()) {
                errors.add(missingCode(row, "code_session", "code session obligatoire"));
            } else if (!row.isSouvenir() && row.sortieKey().isEmpty()) {
                errors.add(missingCode(row, "code_sortie", "code sortie obligatoire pour une sortie en cours"));
            } else {
                usable.add(row);
            }
        }
        return usable;
    }

    private static ImportError missingCode(FlatRow row, String column, String message) {
        return new ImportError(row.line(), column, SurveySchema.STRUCTUREL, SurveySchema.STRUCT_CODE_MISSING, message);
    }

    /** Sessions, sorties, captures puis souvenirs, validés dans l'ordre de leurs dépendances. */
    private SurveyImportService.Validated validate(List<FlatRow> rows, SurveyImportService.ImportFile file,
                                                   List<ImportError> errors) {
        List<FlatRow> enCours = rows.stream().filter(r -> !r.isSouvenir()).toList();
        List<FlatRow> souvenirRows = rows.stream().filter(FlatRow::isSouvenir).toList();

        SurveyImportService.SessionsResult sessions = service.validateSessions(
                firstOfEachGroup(rows, FlatRow::sessionKey, SurveyFlatColumns.SESSION), file.allowedWaterEntities());
        addFlatErrors(errors, sessions.errors());
        SurveyImportService.SortiesResult sorties = service.validateSorties(
                firstOfEachGroup(enCours, FlatRow::sortieKey, SurveyFlatColumns.SORTIE), sessions.sessions());
        addFlatErrors(errors, sorties.errors());
        SurveyImportService.CapturesResult captures = service.validateCaptures(
                enCours.stream().map(r -> sheetRow(r, SurveyFlatColumns.CAPTURE)).toList(), sorties.sorties());
        addFlatErrors(errors, captures.errors());

        List<SurveyParsedSouvenir> souvenirs = validateSouvenirs(souvenirRows, sessions, sorties, captures, file,
                errors);
        return new SurveyImportService.Validated(errors, sessions.sessions(), sorties.sorties(), captures.byTripKey(),
                captures.anglerOrigins(), souvenirs);
    }

    /**
     * Une sortie souvenir par (code session, code pêcheur), une capture par ligne (#236). Le
     * pêcheur doit avoir été enquêté dans la session, sur une ligne en cours valide.
     */
    private List<SurveyParsedSouvenir> validateSouvenirs(List<FlatRow> rows,
                                                         SurveyImportService.SessionsResult sessions,
                                                         SurveyImportService.SortiesResult sorties,
                                                         SurveyImportService.CapturesResult captures,
                                                         SurveyImportService.ImportFile file,
                                                         List<ImportError> errors) {
        Set<String> surveyedInSession = surveyedAnglersBySession(sorties, captures);
        List<SurveyParsedSouvenir> souvenirs = new ArrayList<>();
        for (List<FlatRow> group : SurveyFlatConsistency.groupBy(rows, FlatRow::souvenirKey).values()) {
            FlatRow first = group.get(0);
            String angler = first.get("code_pecheur");
            if (!sessions.sessions().containsKey(first.sessionKey())
                    || !surveyedInSession.contains(first.souvenirKey())) {
                group.forEach(row -> errors.add(new ImportError(row.line(), "code_pecheur", SurveySchema.REFERENTIEL,
                        SurveySchema.REF_SESSION, "pêcheur « " + angler + " » sans ligne en_cours valide dans la session « "
                                + first.sessionKey() + " »")));
                continue;
            }
            SurveyParsedSouvenir merged = mergeSouvenirRows(group, Set.of(angler), file, errors);
            if (merged != null) {
                merged.sessionCode = first.sessionKey();
                souvenirs.add(merged);
            }
        }
        return souvenirs;
    }

    /** Valide chaque ligne du souvenir ; la première ligne valide porte la sortie, chacune sa capture. */
    private SurveyParsedSouvenir mergeSouvenirRows(List<FlatRow> group, Set<String> knownAngler,
                                                   SurveyImportService.ImportFile file, List<ImportError> errors) {
        SurveyParsedSouvenir merged = null;
        for (FlatRow row : group) {
            SurveyImportService.SouvenirRowResult rv = service.validateSouvenirRow(row.line(),
                    SurveyFlatColumns.toSheetRecord(SurveyFlatColumns.SOUVENIR, row.values()), knownAngler,
                    file.allowedWaterEntities());
            if (!rv.errors().isEmpty()) {
                addFlatErrors(errors, rv.errors());
            } else if (merged == null) {
                merged = rv.parsed();
            } else {
                merged.catches.addAll(rv.parsed().catches);
            }
        }
        return merged;
    }

    /** Couples « code session + code pêcheur » des pêcheurs enquêtés sur une ligne en cours valide. */
    private static Set<String> surveyedAnglersBySession(SurveyImportService.SortiesResult sorties,
                                                        SurveyImportService.CapturesResult captures) {
        Set<String> result = new LinkedHashSet<>();
        for (List<SurveyParsedCapture> trip : captures.byTripKey().values()) {
            SurveyParsedCapture first = trip.get(0);
            SurveyParsedSortie sortie = sorties.sorties().get(first.sortieCode);
            if (sortie != null) {
                result.add(sortie.sessionCode + " " + first.anglerCode);
            }
        }
        return result;
    }

    /** Première ligne de chaque groupe, sous forme d'enregistrement de feuille : elle décrit le groupe. */
    private static List<SurveyRow> firstOfEachGroup(List<FlatRow> rows, Function<FlatRow, String> key,
                                                    Map<String, String> sheetColumns) {
        return SurveyFlatConsistency.groupBy(rows, key).values().stream()
                .map(group -> sheetRow(group.get(0), sheetColumns))
                .toList();
    }

    private static SurveyRow sheetRow(FlatRow row, Map<String, String> sheetColumns) {
        return new SurveyRow(row.line(), SurveyFlatColumns.toSheetRecord(sheetColumns, row.values()));
    }

    private static void addFlatErrors(List<ImportError> errors, List<ImportError> sheetErrors) {
        sheetErrors.stream().map(SurveyFlatColumns::toFlatError).forEach(errors::add);
    }
}
