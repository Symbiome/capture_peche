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

import fr.inrae.fishola.entities.enums.DayPeriod;
import fr.inrae.fishola.entities.enums.FishingMode;
import fr.inrae.fishola.rest.imports.CsvSupport;
import fr.inrae.fishola.rest.imports.ImportDao;
import fr.inrae.fishola.rest.imports.ImportError;
import fr.inrae.fishola.rest.imports.ImportResultBean;
import fr.inrae.fishola.rest.imports.ImportService;
import fr.inrae.fishola.rest.trips.TripTimestamps;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.exception.DataAccessException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pipeline d'import opérateur « enquête terrain » (#144) : classeur XLSX multi-feuilles
 * (session d'enquête, sortie, capture pêcheur, session souvenir facultative), distinct du
 * pipeline générique ({@link ImportService}, #71) et du carnet volontaire ({@link
 * fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireImportService}, #143), dont il
 * réutilise le vocabulaire {@code mode_peche} et les mêmes trois étages de validation
 * (structurel / référentiel / métier, codes d'erreur stables).
 *
 * <p>Contrairement au carnet volontaire (un fichier plat, un {@code session_ref} par
 * sortie), ce format est relationnel : chaque feuille est validée dans l'ordre de ses
 * dépendances (session -> sortie -> capture), avec résolution des clés de liaison avant
 * résolution des référentiels métier. Chaque {@code (Code sortie, Code pêcheur)} devient
 * une {@code Trip} distincte ; chaque {@code Session souvenir} devient sa propre
 * {@code Trip} indépendante ({@code collection_method = 'enquete_souvenir'}).
 */
@Singleton
public class SurveyImportService {

    public static final String MODE_ALL_OR_NOTHING = "all_or_nothing";
    public static final String MODE_PARTIAL = "partial";

    private static final Map<String, FishingMode> FISHING_MODE_BY_LABEL = Map.of(
            "bateau", FishingMode.bateau,
            "float tube/canoe", FishingMode.float_tube_canoe,
            "bord itinerant", FishingMode.bord_itinerant,
            "bord statique", FishingMode.bord_statique);

    private static final Map<String, DayPeriod> DAY_PERIOD_BY_LABEL = Map.of(
            "matin", DayPeriod.matin,
            "apres midi", DayPeriod.apres_midi,
            "journee entiere", DayPeriod.journee_entiere,
            "soiree", DayPeriod.soiree);

    @Inject
    protected ImportDao importDao;

    private static String get(Map<String, String> rec, String key) {
        String v = rec.get(key);
        return v == null ? "" : v;
    }

    private static String emptyToNull(String v) {
        return (v == null || v.isEmpty()) ? null : v;
    }

    /** Minuscule, sans accents, tirets assimilés à des espaces : sert aux valeurs figées. */
    static String normalize(String v) {
        String s = Normalizer.normalize(v.strip().toLowerCase(), Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");
        return s.replace('-', ' ').replaceAll("\\s+", " ").strip();
    }

    private static ImportError err(String sheet, int line, String column, String stage, String code, String message) {
        return new ImportError(line, sheet + " / " + column, stage, code, message);
    }

    private static Integer tryParseInt(String v) {
        if (CsvSupport.isBlank(v)) {
            return null;
        }
        try {
            return CsvSupport.parseInt(v);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Short tryParseShort(String v) {
        Integer i = tryParseInt(v);
        return i == null ? null : i.shortValue();
    }

    // --- Étage « Session d'enquête » ------------------------------------------

    private record SessionRowResult(List<ImportError> errors, SurveyParsedSession parsed) {}

    private SessionRowResult validateSessionRow(int line, Map<String, String> rec, Set<UUID> allowedWaterEntities) {
        List<ImportError> errors = new ArrayList<>();
        SurveyParsedSession s = new SurveyParsedSession();
        s.code = get(rec, "Code session").strip();
        if (CsvSupport.isBlank(s.code)) {
            errors.add(err(SurveySchema.SHEET_SESSION, line, "Code session", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_CODE_MISSING, "code session obligatoire"));
        }
        try {
            s.day = CsvSupport.parseDate(get(rec, "Date"));
        } catch (RuntimeException e) {
            errors.add(err(SurveySchema.SHEET_SESSION, line, "Date", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_DATE, "date invalide (JJ/MM/AAAA) : « " + get(rec, "Date") + " »"));
        }

        String secteur = get(rec, "Secteur");
        s.waterEntityId = importDao.resolveWaterEntity(secteur, null);
        if (s.waterEntityId == null) {
            errors.add(err(SurveySchema.SHEET_SESSION, line, "Secteur", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_WATER_ENTITY, "secteur non résolu : « " + secteur + " »"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(s.waterEntityId)) {
            errors.add(err(SurveySchema.SHEET_SESSION, line, "Secteur", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_WATER_ENTITY_SCOPE, "secteur « " + secteur + " » hors de votre périmètre"));
        }

        s.unsurveyedShoreAnglers = tryParseShort(get(rec, "Nb pêcheurs carnassiers du bord observés non-enquêtés"));
        s.unsurveyedBoatAnglers = tryParseShort(get(rec, "Nb pêcheurs carnassiers en bateau observés non-enquêtés"));
        if (rec.containsKey(SurveySchema.COL_SESSION_START)) {
            validateSessionHoursAndOperator(line, rec, s, errors);
        }

        return new SessionRowResult(errors, errors.isEmpty() ? s : null);
    }

    /** Heures de session et opérateur (#236, #242) : début et opérateur obligatoires, fin facultative. */
    private static void validateSessionHoursAndOperator(int line, Map<String, String> rec, SurveyParsedSession s,
                                                        List<ImportError> errors) {
        String rawStart = get(rec, SurveySchema.COL_SESSION_START);
        try {
            s.startTime = CsvSupport.parseTime(rawStart);
        } catch (RuntimeException e) {
            errors.add(err(SurveySchema.SHEET_SESSION, line, SurveySchema.COL_SESSION_START, SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME, "heure invalide (HH:MM) : « " + rawStart + " »"));
        }
        String rawEnd = get(rec, SurveySchema.COL_SESSION_END);
        if (!CsvSupport.isBlank(rawEnd)) {
            try {
                s.endTime = CsvSupport.parseTime(rawEnd);
            } catch (RuntimeException e) {
                errors.add(err(SurveySchema.SHEET_SESSION, line, SurveySchema.COL_SESSION_END, SurveySchema.STRUCTUREL,
                        SurveySchema.STRUCT_TIME, "heure invalide (HH:MM) : « " + rawEnd + " »"));
            }
        }
        if (s.startTime != null && s.endTime != null && !s.endTime.isAfter(s.startTime)) {
            errors.add(err(SurveySchema.SHEET_SESSION, line, SurveySchema.COL_SESSION_END, SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME_ORDER, "l'heure de fin de session doit être postérieure à son heure de début"));
        }
        s.operatorName = emptyToNull(get(rec, SurveySchema.COL_OPERATOR).strip());
        if (s.operatorName == null) {
            errors.add(err(SurveySchema.SHEET_SESSION, line, SurveySchema.COL_OPERATOR, SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_CODE_MISSING, "nom de l'opérateur obligatoire"));
        }
    }

    record SessionsResult(List<ImportError> errors, LinkedHashMap<String, SurveyParsedSession> sessions) {}

    SessionsResult validateSessions(List<SurveyRow> rows, Set<UUID> allowedWaterEntities) {
        List<ImportError> errors = new ArrayList<>();
        LinkedHashMap<String, SurveyParsedSession> sessions = new LinkedHashMap<>();
        for (SurveyRow row : rows) {
            int line = row.line();
            SessionRowResult rv = validateSessionRow(line, row.values(), allowedWaterEntities);
            if (!rv.errors().isEmpty()) {
                errors.addAll(rv.errors());
                continue;
            }
            SurveyParsedSession s = rv.parsed();
            if (sessions.containsKey(s.code)) {
                errors.add(err(SurveySchema.SHEET_SESSION, line, "Code session", SurveySchema.STRUCTUREL,
                        SurveySchema.STRUCT_CODE_DUPLICATE, "code session « " + s.code + " » déjà utilisé dans le fichier"));
                continue;
            }
            sessions.put(s.code, s);
        }
        return new SessionsResult(errors, sessions);
    }

    // --- Étage « Sortie de pêche » --------------------------------------------

    private record SortieRowResult(List<ImportError> errors, SurveyParsedSortie parsed) {}

    private SortieRowResult validateSortieRow(int line, Map<String, String> rec,
                                              Map<String, SurveyParsedSession> sessions) {
        List<ImportError> errors = new ArrayList<>();
        SurveyParsedSortie s = new SurveyParsedSortie();
        s.sessionCode = get(rec, "Code session").strip();
        s.sortieCode = get(rec, "Code sortie").strip();

        if (CsvSupport.isBlank(s.sortieCode)) {
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Code sortie", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_CODE_MISSING, "code sortie obligatoire"));
        }
        if (CsvSupport.isBlank(s.sessionCode) || !sessions.containsKey(s.sessionCode)) {
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Code session", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_SESSION,
                    "code session « " + s.sessionCode + " » inconnu de la feuille « "
                            + SurveySchema.SHEET_SESSION + " »"));
        }

        boolean timesOk = true;
        try {
            s.controlTime = CsvSupport.parseTime(get(rec, "Heure du contrôle"));
        } catch (RuntimeException e) {
            timesOk = false;
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Heure du contrôle", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME, "heure invalide (HH:MM) : « " + get(rec, "Heure du contrôle") + " »"));
        }
        try {
            s.startTime = CsvSupport.parseTime(get(rec, "Heure de début de pêche"));
        } catch (RuntimeException e) {
            timesOk = false;
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Heure de début de pêche", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME,
                    "heure invalide (HH:MM) : « " + get(rec, "Heure de début de pêche") + " »"));
        }
        try {
            s.endTime = CsvSupport.parseTime(get(rec, "Heure de fin de pêche prévue"));
        } catch (RuntimeException e) {
            timesOk = false;
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Heure de fin de pêche prévue", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME,
                    "heure invalide (HH:MM) : « " + get(rec, "Heure de fin de pêche prévue") + " »"));
        }
        String rawEndDay = get(rec, "Date de fin de pêche prévue");
        if (!CsvSupport.isBlank(rawEndDay)) {
            try {
                s.endDay = CsvSupport.parseDate(rawEndDay);
            } catch (RuntimeException e) {
                errors.add(err(SurveySchema.SHEET_SORTIE, line, "Date de fin de pêche prévue", SurveySchema.STRUCTUREL,
                        SurveySchema.STRUCT_DATE, "date invalide (JJ/MM/AAAA) : « " + rawEndDay + " »"));
            }
        }
        SurveyParsedSession session = sessions.get(s.sessionCode);
        if (timesOk && s.endDay == null && CsvSupport.isBlank(rawEndDay) && !s.endTime.isAfter(s.startTime)) {
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Heure de fin de pêche prévue", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME_ORDER,
                    "l'heure de fin prévue doit être postérieure à l'heure de début"));
        }
        if (timesOk && s.endDay != null && session != null && session.day != null
                && !LocalDateTime.of(s.endDay, s.endTime).isAfter(LocalDateTime.of(session.day, s.startTime))) {
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Date de fin de pêche prévue", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME_ORDER,
                    "la fin de pêche prévue doit être postérieure au début de pêche (date de la session "
                            + session.day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")"));
        }
        if (timesOk && s.controlTime.isBefore(s.startTime)) {
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Heure du contrôle", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME_ORDER,
                    "l'heure du contrôle (" + s.controlTime + ") ne peut pas être antérieure à l'heure de début de pêche ("
                            + s.startTime + ")"));
        } else if (timesOk && errors.isEmpty() && session != null && session.day != null
                && LocalDateTime.of(session.day, s.controlTime)
                        .isAfter(TripTimestamps.endTimestamp(session.day, s.startTime, s.endDay, s.endTime))) {
            errors.add(err(SurveySchema.SHEET_SORTIE, line, "Heure du contrôle", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_TIME_ORDER,
                    "l'heure du contrôle (" + s.controlTime + ") ne peut pas être postérieure à la fin de pêche prévue"));
        }

        return new SortieRowResult(errors, errors.isEmpty() ? s : null);
    }

    record SortiesResult(List<ImportError> errors, LinkedHashMap<String, SurveyParsedSortie> sorties) {}

    SortiesResult validateSorties(List<SurveyRow> rows, Map<String, SurveyParsedSession> sessions) {
        List<ImportError> errors = new ArrayList<>();
        LinkedHashMap<String, SurveyParsedSortie> sorties = new LinkedHashMap<>();
        for (SurveyRow row : rows) {
            int line = row.line();
            SortieRowResult rv = validateSortieRow(line, row.values(), sessions);
            if (!rv.errors().isEmpty()) {
                errors.addAll(rv.errors());
                continue;
            }
            SurveyParsedSortie s = rv.parsed();
            if (sorties.containsKey(s.sortieCode)) {
                errors.add(err(SurveySchema.SHEET_SORTIE, line, "Code sortie", SurveySchema.STRUCTUREL,
                        SurveySchema.STRUCT_CODE_DUPLICATE, "code sortie « " + s.sortieCode + " » déjà utilisé dans le fichier"));
                continue;
            }
            sorties.put(s.sortieCode, s);
        }
        return new SortiesResult(errors, sorties);
    }

    // --- Étage « Capture pêcheur » --------------------------------------------

    /** Résout mode de pêche / technique / espèce recherchée / bredouille : partagé capture + souvenir. */
    private void validateTripLevelFields(String sheet, int line, Map<String, String> rec, List<ImportError> errors,
                                         java.util.function.Consumer<FishingMode> setMode,
                                         java.util.function.Consumer<UUID> setTechnique,
                                         java.util.function.Consumer<Integer> setRodCount,
                                         java.util.function.Consumer<String> setBaitOrLure,
                                         java.util.function.Consumer<UUID> setExpectedSpecies,
                                         java.util.function.Consumer<Boolean> setBredouille) {
        String modeRaw = get(rec, "Mode de pêche");
        FishingMode mode = FISHING_MODE_BY_LABEL.get(normalize(modeRaw));
        if (mode == null) {
            errors.add(err(sheet, line, "Mode de pêche", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_MODE, "mode de pêche inconnu : « " + modeRaw + " »"));
        } else {
            setMode.accept(mode);
        }

        String technique = get(rec, "Technique");
        UUID techniqueId = importDao.resolveTechnique(technique);
        if (techniqueId == null) {
            errors.add(err(sheet, line, "Technique", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_TECHNIQUE, "technique non résolue : « " + technique + " »"));
        } else {
            setTechnique.accept(techniqueId);
        }

        Integer rodCount = tryParseInt(get(rec, "Nombre de lignes"));
        if (rodCount == null || rodCount < 1) {
            errors.add(err(sheet, line, "Nombre de lignes", SurveySchema.METIER,
                    SurveySchema.METIER_QUANTITY, "le nombre de lignes doit être ≥ 1"));
        } else {
            setRodCount.accept(rodCount);
        }
        setBaitOrLure.accept(emptyToNull(get(rec, "Appât / type de leurre")));

        String especeRecherchee = get(rec, "Espèce recherchée");
        String normalizedEspece = normalize(especeRecherchee);
        if (CsvSupport.isBlank(especeRecherchee)) {
            errors.add(err(sheet, line, "Espèce recherchée", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_EXPECTED_SPECIES, "espèce recherchée obligatoire (ou « Aucune »/« Toutes espèces »)"));
        } else if (!normalizedEspece.equals(SurveySchema.ESPECE_RECHERCHEE_AUCUNE)
                && !normalizedEspece.equals(SurveySchema.ESPECE_RECHERCHEE_TOUTES)) {
            UUID expectedSpeciesId = importDao.resolveSpecies(especeRecherchee);
            if (expectedSpeciesId == null) {
                errors.add(err(sheet, line, "Espèce recherchée", SurveySchema.REFERENTIEL,
                        SurveySchema.REF_EXPECTED_SPECIES, "espèce recherchée non résolue : « " + especeRecherchee + " »"));
            } else {
                setExpectedSpecies.accept(expectedSpeciesId);
            }
        }

        try {
            setBredouille.accept(CsvSupport.parseBoolOuiNon(get(rec, "Bredouille")));
        } catch (RuntimeException e) {
            errors.add(err(sheet, line, "Bredouille", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_BREDOUILLE, "valeur oui/non attendue"));
        }
    }

    /** Résout espèce / taille / lot / conservation d'une capture : partagé capture + souvenir. */
    private void validateCatchFields(String sheet, int line, Map<String, String> rec, boolean bredouille,
                                     boolean hasCapture, List<ImportError> errors,
                                     java.util.function.Consumer<UUID> setSpecies,
                                     java.util.function.Consumer<Integer> setQuantity,
                                     java.util.function.Consumer<Integer> setSize,
                                     java.util.function.Consumer<Integer> setLotMinSize,
                                     java.util.function.Consumer<Integer> setLotMaxSize,
                                     java.util.function.Consumer<Boolean> setKept) {
        if (!hasCapture) {
            if (bredouille && !CsvSupport.isBlank(get(rec, "Espèce (capture)"))) {
                errors.add(err(sheet, line, "Bredouille", SurveySchema.METIER,
                        SurveySchema.METIER_BREDOUILLE,
                        "sortie déclarée bredouille mais une espèce capturée est renseignée"));
            }
            return;
        }
        UUID speciesId = importDao.resolveSpecies(get(rec, "Espèce (capture)"));
        if (speciesId == null) {
            errors.add(err(sheet, line, "Espèce (capture)", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_SPECIES, "espèce non résolue : « " + get(rec, "Espèce (capture)") + " »"));
            return;
        }
        setSpecies.accept(speciesId);

        int quantity = 1;
        String rawNb = get(rec, "Nombre");
        if (!CsvSupport.isBlank(rawNb)) {
            Integer parsed = tryParseInt(rawNb);
            quantity = parsed == null ? 0 : parsed;
        }
        if (quantity < 1) {
            errors.add(err(sheet, line, "Nombre", SurveySchema.METIER,
                    SurveySchema.METIER_QUANTITY, "le nombre d'individus d'un lot doit être ≥ 1"));
            return;
        }
        setQuantity.accept(quantity);

        boolean isLot = quantity > 1;
        if (isLot) {
            Integer min = tryParseInt(get(rec, "Taille min"));
            Integer max = tryParseInt(get(rec, "Taille max"));
            if (min == null || max == null) {
                errors.add(err(sheet, line, "Taille min", SurveySchema.METIER,
                        SurveySchema.METIER_LOT_BOUNDS,
                        "taille min et taille max obligatoires pour un lot (nombre > 1)"));
            } else if (min > max) {
                errors.add(err(sheet, line, "Taille min", SurveySchema.METIER,
                        SurveySchema.METIER_LOT_BOUNDS, "la taille min doit être ≤ à la taille max"));
            } else {
                setLotMinSize.accept(min);
                setLotMaxSize.accept(max);
            }
        } else {
            Integer size = tryParseInt(get(rec, "Taille"));
            if (size == null && importDao.isSizeMandatory(speciesId)) {
                errors.add(err(sheet, line, "Taille", SurveySchema.METIER,
                        SurveySchema.METIER_SIZE_REQUIRED, "taille obligatoire pour cette espèce"));
            }
            if (size != null) {
                ImportDao.Bounds bounds = importDao.sizeBounds(speciesId);
                if (ImportService.sizeOutOfBounds(bounds, size)) {
                    errors.add(err(sheet, line, "Taille", SurveySchema.METIER,
                            SurveySchema.METIER_SIZE_ABERRANT,
                            "taille " + size + " cm hors bornes [" + bounds.min() + "-" + bounds.max()
                                    + "] pour l'espèce"));
                }
                setSize.accept(size);
            }
        }

        try {
            String raw = get(rec, "Poisson conservé");
            setKept.accept(CsvSupport.parseBoolOuiNon(raw.isEmpty() ? "non" : raw));
        } catch (RuntimeException e) {
            errors.add(err(sheet, line, "Poisson conservé", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_KEPT, "valeur oui/non attendue"));
        }
    }

    private record CaptureRowResult(List<ImportError> errors, SurveyParsedCapture parsed) {}

    private CaptureRowResult validateCaptureRow(int line, Map<String, String> rec,
                                                Map<String, SurveyParsedSortie> sorties) {
        List<ImportError> errors = new ArrayList<>();
        SurveyParsedCapture c = new SurveyParsedCapture();
        c.line = line;
        c.sortieCode = get(rec, "Code sortie").strip();
        c.anglerCode = get(rec, "Code pêcheur").strip();

        if (CsvSupport.isBlank(c.anglerCode)) {
            errors.add(err(SurveySchema.SHEET_CAPTURE, line, "Code pêcheur", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_CODE_MISSING, "code pêcheur obligatoire"));
        }
        if (CsvSupport.isBlank(c.sortieCode) || !sorties.containsKey(c.sortieCode)) {
            errors.add(err(SurveySchema.SHEET_CAPTURE, line, "Code sortie", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_SORTIE,
                    "code sortie « " + c.sortieCode + " » inconnu de la feuille « " + SurveySchema.SHEET_SORTIE + " »"));
        }
        if (!errors.isEmpty()) {
            return new CaptureRowResult(errors, null);
        }

        SurveyAnglerOrigin origin = SurveyAnglerOrigin.resolve(get(rec, "Département origine"));
        c.originDepartment = origin.department();
        c.originCountry = origin.country();

        validateTripLevelFields(SurveySchema.SHEET_CAPTURE, line, rec, errors,
                mode -> c.fishingMode = mode, tech -> c.techniqueId = tech, rc -> c.rodCount = rc,
                bl -> c.baitOrLure = bl, es -> c.expectedSpeciesId = es, b -> c.bredouille = b);
        if (!errors.isEmpty()) {
            return new CaptureRowResult(errors, null);
        }

        c.hasCapture = !c.bredouille && !CsvSupport.isBlank(get(rec, "Espèce (capture)"));
        validateCatchFields(SurveySchema.SHEET_CAPTURE, line, rec, c.bredouille, c.hasCapture, errors,
                sp -> c.speciesId = sp, q -> c.quantity = q, sz -> c.size = sz,
                min -> c.lotMinSize = min, max -> c.lotMaxSize = max, k -> c.kept = k);

        if (!errors.isEmpty()) {
            return new CaptureRowResult(errors, null);
        }
        return new CaptureRowResult(List.of(), c);
    }

    record CapturesResult(List<ImportError> errors,
                                  LinkedHashMap<String, List<SurveyParsedCapture>> byTripKey,
                                  LinkedHashMap<String, SurveyAnglerOrigin> anglerOrigins) {}

    static String tripKey(String sortieCode, String anglerCode) {
        return sortieCode + " " + anglerCode;
    }

    CapturesResult validateCaptures(List<SurveyRow> rows, Map<String, SurveyParsedSortie> sorties) {
        List<ImportError> errors = new ArrayList<>();
        LinkedHashMap<String, List<SurveyParsedCapture>> byTripKey = new LinkedHashMap<>();
        LinkedHashMap<String, SurveyAnglerOrigin> anglerOrigins = new LinkedHashMap<>();
        for (SurveyRow row : rows) {
            CaptureRowResult rv = validateCaptureRow(row.line(), row.values(), sorties);
            if (!rv.errors().isEmpty()) {
                errors.addAll(rv.errors());
                continue;
            }
            SurveyParsedCapture c = rv.parsed();
            byTripKey.computeIfAbsent(tripKey(c.sortieCode, c.anglerCode), k -> new ArrayList<>()).add(c);
            // Première occurrence du pêcheur dans le fichier : conserve son origine (#144).
            anglerOrigins.computeIfAbsent(c.anglerCode, k -> new SurveyAnglerOrigin(c.originDepartment, c.originCountry));
        }
        return new CapturesResult(errors, byTripKey, anglerOrigins);
    }

    // --- Étage « Session souvenir » (facultatif) ------------------------------

    record SouvenirRowResult(List<ImportError> errors, SurveyParsedSouvenir parsed) {}

    SouvenirRowResult validateSouvenirRow(int line, Map<String, String> rec, Set<String> knownAnglerCodes,
                                                   Set<UUID> allowedWaterEntities) {
        List<ImportError> errors = new ArrayList<>();
        SurveyParsedSouvenir s = new SurveyParsedSouvenir();
        s.line = line;
        s.anglerCode = get(rec, "Code pêcheur").strip();

        if (CsvSupport.isBlank(s.anglerCode)) {
            errors.add(err(SurveySchema.SHEET_SOUVENIR, line, "Code pêcheur", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_CODE_MISSING, "code pêcheur obligatoire"));
        } else if (!knownAnglerCodes.contains(s.anglerCode)) {
            errors.add(err(SurveySchema.SHEET_SOUVENIR, line, "Code pêcheur", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_SESSION,
                    "code pêcheur « " + s.anglerCode + " » absent de la feuille « " + SurveySchema.SHEET_CAPTURE + " »"));
        }

        try {
            s.day = CsvSupport.parseDate(get(rec, "Date"));
        } catch (RuntimeException e) {
            errors.add(err(SurveySchema.SHEET_SOUVENIR, line, "Date", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_DATE, "date invalide (JJ/MM/AAAA) : « " + get(rec, "Date") + " »"));
        }

        String periode = get(rec, "Période de la journée");
        DayPeriod dayPeriod = DAY_PERIOD_BY_LABEL.get(normalize(periode));
        if (dayPeriod == null) {
            errors.add(err(SurveySchema.SHEET_SOUVENIR, line, "Période de la journée", SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_PERIOD, "période de la journée inconnue : « " + periode + " »"));
        } else {
            s.dayPeriod = dayPeriod;
        }

        String sitePeche = get(rec, "Site pêché");
        s.waterEntityId = importDao.resolveWaterEntity(sitePeche, null);
        if (s.waterEntityId == null) {
            errors.add(err(SurveySchema.SHEET_SOUVENIR, line, "Site pêché", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_WATER_ENTITY, "site pêché non résolu : « " + sitePeche + " »"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(s.waterEntityId)) {
            errors.add(err(SurveySchema.SHEET_SOUVENIR, line, "Site pêché", SurveySchema.REFERENTIEL,
                    SurveySchema.REF_WATER_ENTITY_SCOPE, "site pêché « " + sitePeche + " » hors de votre périmètre"));
        }

        if (!errors.isEmpty()) {
            return new SouvenirRowResult(errors, null);
        }

        validateTripLevelFields(SurveySchema.SHEET_SOUVENIR, line, rec, errors,
                mode -> s.fishingMode = mode, tech -> s.techniqueId = tech, rc -> s.rodCount = rc,
                bl -> s.baitOrLure = bl, es -> s.expectedSpeciesId = es, b -> s.bredouille = b);
        if (!errors.isEmpty()) {
            return new SouvenirRowResult(errors, null);
        }

        boolean hasCapture = !s.bredouille && !CsvSupport.isBlank(get(rec, "Espèce (capture)"));
        SurveyParsedCatch c = new SurveyParsedCatch();
        validateCatchFields(SurveySchema.SHEET_SOUVENIR, line, rec, s.bredouille, hasCapture, errors,
                sp -> c.speciesId = sp, q -> c.quantity = q, sz -> c.size = sz,
                min -> c.lotMinSize = min, max -> c.lotMaxSize = max, k -> c.kept = k);
        if (hasCapture) {
            s.catches.add(c);
        }

        if (!errors.isEmpty()) {
            return new SouvenirRowResult(errors, null);
        }
        return new SouvenirRowResult(List.of(), s);
    }

    record SouvenirsResult(List<ImportError> errors, List<SurveyParsedSouvenir> souvenirs) {}

    private SouvenirsResult validateSouvenirs(List<SurveyRow> rows, Set<String> knownAnglerCodes,
                                              Set<UUID> allowedWaterEntities) {
        List<ImportError> errors = new ArrayList<>();
        List<SurveyParsedSouvenir> souvenirs = new ArrayList<>();
        for (SurveyRow row : rows) {
            SouvenirRowResult rv = validateSouvenirRow(row.line(), row.values(), knownAnglerCodes, allowedWaterEntities);
            if (!rv.errors().isEmpty()) {
                errors.addAll(rv.errors());
            } else {
                souvenirs.add(rv.parsed());
            }
        }
        return new SouvenirsResult(errors, souvenirs);
    }

    // --- Exécution complète ----------------------------------------------------

    /**
     * Importe un fichier d'enquête terrain : CSV plat (#242) si son nom finit par « .csv »,
     * classeur XLSX multi-feuilles (#144) sinon. Un fichier déjà importé (même empreinte)
     * est signalé comme doublon sans être relu.
     *
     * @param fileBytes contenu du fichier
     * @param filename nom du fichier, qui détermine le format
     * @param mode {@code partial} (lignes valides importées) ou {@code all_or_nothing}
     * @param createdBy compte staff à l'origine de l'import
     * @param allowedWaterEntities secteurs du périmètre, null pour un national
     * @return résultat de l'import, erreurs comprises
     */
    public ImportResultBean run(byte[] fileBytes, String filename, String mode, UUID createdBy,
                                Set<UUID> allowedWaterEntities) {
        String fileHash = sha256Hex(fileBytes);
        var existing = importDao.findByHash(fileHash);
        if (existing.isPresent()) {
            ImportDao.JobSummary j = existing.get();
            return new ImportResultBean(j.id(), j.status(), j.total(), j.inserted(), j.rejected(), true, List.of());
        }
        ImportFile file = new ImportFile(filename, fileHash, mode, createdBy, allowedWaterEntities);
        if (filename.toLowerCase().endsWith(".csv")) {
            return new SurveyFlatImport(this).run(fileBytes, file);
        }
        return runXlsx(fileBytes, file);
    }

    /** Fichier en cours d'import : identité, mode et périmètre de l'opérateur. */
    record ImportFile(String filename, String fileHash, String mode, UUID createdBy, Set<UUID> allowedWaterEntities) {}

    /** Données validées, prêtes à persister, et erreurs de toutes les lignes. */
    record Validated(List<ImportError> errors, Map<String, SurveyParsedSession> sessions,
                     Map<String, SurveyParsedSortie> sorties, Map<String, List<SurveyParsedCapture>> tripsByKey,
                     Map<String, SurveyAnglerOrigin> anglerOrigins, List<SurveyParsedSouvenir> souvenirs) {}

    private ImportResultBean runXlsx(byte[] fileBytes, ImportFile file) {
        Map<String, XlsxSupport.SheetData> sheets;
        try {
            sheets = XlsxSupport.readWorkbook(fileBytes);
        } catch (IllegalArgumentException e) {
            return persistFailed(file, new ImportError(1, null, SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_HEADER, "classeur XLSX illisible ou corrompu"));
        }

        List<ImportError> headerErrors = new ArrayList<>();
        checkSheet(sheets, SurveySchema.SHEET_SESSION,
                List.of(SurveySchema.HEADER_SESSION, SurveySchema.LEGACY_HEADER_SESSION), true, headerErrors);
        checkSheet(sheets, SurveySchema.SHEET_SORTIE,
                List.of(SurveySchema.HEADER_SORTIE, SurveySchema.LEGACY_HEADER_SORTIE), true, headerErrors);
        checkSheet(sheets, SurveySchema.SHEET_CAPTURE, List.of(SurveySchema.HEADER_CAPTURE), true, headerErrors);
        checkSheet(sheets, SurveySchema.SHEET_SOUVENIR, List.of(SurveySchema.HEADER_SOUVENIR), false, headerErrors);
        if (!headerErrors.isEmpty()) {
            return persistFailed(file, headerErrors);
        }

        List<SurveyRow> sessionRows = SurveyRow.ofSheet(sheets.get(SurveySchema.SHEET_SESSION).records());
        List<SurveyRow> sortieRows = SurveyRow.ofSheet(sheets.get(SurveySchema.SHEET_SORTIE).records());
        List<SurveyRow> captureRows = SurveyRow.ofSheet(sheets.get(SurveySchema.SHEET_CAPTURE).records());
        List<SurveyRow> souvenirRows = sheets.containsKey(SurveySchema.SHEET_SOUVENIR)
                ? SurveyRow.ofSheet(sheets.get(SurveySchema.SHEET_SOUVENIR).records()) : List.of();
        int total = sessionRows.size() + sortieRows.size() + captureRows.size() + souvenirRows.size();

        List<ImportError> allErrors = new ArrayList<>();
        SessionsResult sessionsResult = validateSessions(sessionRows, file.allowedWaterEntities());
        allErrors.addAll(sessionsResult.errors());
        SortiesResult sortiesResult = validateSorties(sortieRows, sessionsResult.sessions());
        allErrors.addAll(sortiesResult.errors());
        CapturesResult capturesResult = validateCaptures(captureRows, sortiesResult.sorties());
        allErrors.addAll(capturesResult.errors());
        SouvenirsResult souvenirsResult = validateSouvenirs(
                souvenirRows, capturesResult.anglerOrigins().keySet(), file.allowedWaterEntities());
        allErrors.addAll(souvenirsResult.errors());

        return persist(file, total, new Validated(allErrors, sessionsResult.sessions(), sortiesResult.sorties(),
                capturesResult.byTripKey(), capturesResult.anglerOrigins(), souvenirsResult.souvenirs()));
    }

    /**
     * Persiste le job d'import, ses erreurs et, sauf rejet global en mode {@code
     * all_or_nothing}, les sessions, sorties et captures valides.
     *
     * @param file fichier importé
     * @param total nombre de lignes lues
     * @param validated données validées et erreurs
     * @return résultat de l'import
     */
    ImportResultBean persist(ImportFile file, int total, Validated validated) {
        List<ImportError> allErrors = validated.errors();
        Set<Integer> rejectedLines = new LinkedHashSet<>();
        for (ImportError e : allErrors) {
            rejectedLines.add(e.line());
        }
        int rejected = rejectedLines.size();
        boolean doInsert = !(MODE_ALL_OR_NOTHING.equals(file.mode()) && !allErrors.isEmpty());
        String status = allErrors.isEmpty() ? "DONE" : "DONE_WITH_ERRORS";
        try {
            ImportDao.SurveyPersisted p = importDao.persistSurvey(file.filename(), file.fileHash(), status, total,
                    rejected, file.createdBy(), allErrors, doInsert, validated.sessions(), validated.sorties(),
                    validated.tripsByKey(), validated.anglerOrigins(), validated.souvenirs());
            return new ImportResultBean(p.jobId(), status, total, p.inserted(), rejected, false, allErrors);
        } catch (DataAccessException e) {
            return concurrentDuplicate(file.fileHash(), e);
        }
    }

    /** {@code acceptedHeaders} : en-tête attendu en premier, puis éventuels en-têtes antérieurs encore acceptés. */
    private void checkSheet(Map<String, XlsxSupport.SheetData> sheets, String sheetName,
                            List<List<String>> acceptedHeaders, boolean mandatory, List<ImportError> errors) {
        List<String> expectedHeader = acceptedHeaders.get(0);
        XlsxSupport.SheetData data = sheets.get(sheetName);
        if (data == null) {
            if (mandatory) {
                errors.add(new ImportError(1, sheetName, SurveySchema.STRUCTUREL,
                        SurveySchema.STRUCT_SHEET_MISSING, "feuille obligatoire absente : « " + sheetName + " »"));
            }
            return;
        }
        if (data.records().isEmpty() && data.header().isEmpty()) {
            // feuille présente mais totalement vide : tolérée pour la feuille facultative,
            // rejetée comme en-tête non conforme pour une feuille obligatoire.
            if (mandatory) {
                errors.add(new ImportError(1, sheetName, SurveySchema.STRUCTUREL,
                        SurveySchema.STRUCT_HEADER, "feuille « " + sheetName + " » vide"));
            }
            return;
        }
        if (!acceptedHeaders.contains(data.header())) {
            errors.add(new ImportError(1, sheetName, SurveySchema.STRUCTUREL,
                    SurveySchema.STRUCT_HEADER,
                    "en-tête non conforme au modèle attendu pour « " + sheetName + " » ("
                            + expectedHeader.size() + " colonnes)"));
        }
    }

    ImportResultBean persistFailed(ImportFile file, ImportError error) {
        return persistFailed(file, List.of(error));
    }

    ImportResultBean persistFailed(ImportFile file, List<ImportError> errors) {
        try {
            // Sessions/sorties/captures/souvenirs vides : rien à persister, on réutilise
            // persistSurvey() plutôt que de dupliquer l'insertion du job échoué.
            ImportDao.SurveyPersisted p = importDao.persistSurvey(file.filename(), file.fileHash(), "FAILED",
                    errors.size(), errors.size(), file.createdBy(), errors, false, Map.of(), Map.of(), Map.of(),
                    Map.of(), List.of());
            return new ImportResultBean(p.jobId(), "FAILED", errors.size(), 0, 0, false, errors);
        } catch (DataAccessException e) {
            return concurrentDuplicate(file.fileHash(), e);
        }
    }

    private ImportResultBean concurrentDuplicate(String fileHash, DataAccessException original) {
        if (!isUniqueViolation(original)) {
            throw original;
        }
        return importDao.findByHash(fileHash)
                .map(j -> new ImportResultBean(j.id(), j.status(), j.total(), j.inserted(), j.rejected(), true, List.of()))
                .orElseThrow(() -> original);
    }

    private boolean isUniqueViolation(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
