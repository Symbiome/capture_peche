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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.exception.DataAccessException;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pipeline d'import CSV opérateur (#71), portage fidèle de
 * {@code fishola-backoffice/backoffice/imports/service.py}.
 *
 * Trois étages : structurel (colonnes/types/cohérence), référentiel (résolution
 * espèce/technique/entité hydro), métier (bornes de taille, lots, bredouille). Les
 * sorties sont regroupées par {@code session_ref} ; chaque ligne de capture devient
 * un {@code catch}. Suivi et idempotence (SHA-256) dans {@code import_job}.
 */
@Singleton
public class ImportService {

    public static final String MODE_ALL_OR_NOTHING = "all_or_nothing";
    public static final String MODE_PARTIAL = "partial";

    @Inject
    protected ImportDao importDao;

    private record RowValidation(List<ImportError> errors, ParsedRow parsed) {}

    /** Règle métier Q8 partagée (import CSV + saisie manuelle) : taille hors bornes de l'espèce. */
    public static boolean sizeOutOfBounds(ImportDao.Bounds bounds, Integer size) {
        if (bounds == null || size == null) {
            return false;
        }
        Integer lo = bounds.min();
        Integer hi = bounds.max();
        return (lo != null && size < lo) || (hi != null && size > hi);
    }

    private static String get(Map<String, String> rec, String key) {
        String v = rec.get(key);
        return v == null ? "" : v;
    }

    private static String emptyToNull(String v) {
        return (v == null || v.isEmpty()) ? null : v;
    }

    // --- Validation d'une ligne ---------------------------------------------

    private RowValidation validateRow(int line, Map<String, String> rec, Set<UUID> allowedWaterEntities) {
        List<ImportError> errors = new ArrayList<>();
        ParsedRow parsed = new ParsedRow();
        parsed.line = line;

        // --- Étage 1 : structurel ---
        String cm = get(rec, "collection_method");
        if (!ImportSchema.COLLECTION_METHODS.contains(cm)) {
            errors.add(new ImportError(line, "collection_method", ImportSchema.STRUCTUREL,
                    ImportSchema.STRUCT_COLLECTION_METHOD, "méthode de collecte inconnue : « " + cm + " »"));
        }
        parsed.collectionMethod = cm;

        try {
            parsed.day = CsvSupport.parseDate(get(rec, "date"));
        } catch (RuntimeException e) {
            errors.add(new ImportError(line, "date", ImportSchema.STRUCTUREL, ImportSchema.STRUCT_DATE,
                    "date invalide (JJ/MM/AAAA) : « " + get(rec, "date") + " »"));
        }

        boolean timesOk = true;
        try {
            parsed.start = CsvSupport.parseTime(get(rec, "heure_debut"));
        } catch (RuntimeException e) {
            timesOk = false;
            errors.add(new ImportError(line, "heure_debut", ImportSchema.STRUCTUREL, ImportSchema.STRUCT_TIME,
                    "heure invalide (HH:MM) : « " + get(rec, "heure_debut") + " »"));
        }
        try {
            parsed.end = CsvSupport.parseTime(get(rec, "heure_fin"));
        } catch (RuntimeException e) {
            timesOk = false;
            errors.add(new ImportError(line, "heure_fin", ImportSchema.STRUCTUREL, ImportSchema.STRUCT_TIME,
                    "heure invalide (HH:MM) : « " + get(rec, "heure_fin") + " »"));
        }
        if (timesOk && !parsed.end.isAfter(parsed.start)) {
            errors.add(new ImportError(line, "heure_fin", ImportSchema.STRUCTUREL, ImportSchema.STRUCT_TIME_ORDER,
                    "l'heure de fin doit être postérieure à l'heure de début"));
        }

        String mode = get(rec, "mode_peche");
        if (!CsvSupport.isBlank(mode) && !ImportSchema.FISHING_MODES.contains(mode.toLowerCase())) {
            errors.add(new ImportError(line, "mode_peche", ImportSchema.STRUCTUREL, ImportSchema.STRUCT_MODE,
                    "mode de pêche inconnu : « " + mode + " »"));
        }

        boolean bredouille = false;
        try {
            // Aligné sur le portage Django : une valeur vide n'est pas « non » implicite, c'est une erreur.
            bredouille = CsvSupport.parseBoolOuiNon(get(rec, "bredouille"));
        } catch (RuntimeException e) {
            errors.add(new ImportError(line, "bredouille", ImportSchema.STRUCTUREL, ImportSchema.STRUCT_BREDOUILLE,
                    "valeur oui/non attendue"));
        }
        parsed.bredouille = bredouille;

        if (!errors.isEmpty()) {
            return new RowValidation(errors, null); // on n'enchaîne pas les étages suivants
        }

        boolean hasCapture = !bredouille && !CsvSupport.isBlank(get(rec, "capture_espece"));
        parsed.hasCapture = hasCapture;

        // --- Étage 2 : référentiel ---
        parsed.waterEntityId = importDao.resolveWaterEntity(get(rec, "eau_nom"), get(rec, "commune"));
        if (parsed.waterEntityId == null) {
            errors.add(new ImportError(line, "eau_nom", ImportSchema.REFERENTIEL, ImportSchema.REF_WATER_ENTITY,
                    "entité hydrographique non résolue : « " + get(rec, "eau_nom") + " » / commune « "
                            + get(rec, "commune") + " »"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(parsed.waterEntityId)) {
            // Cloisonnement (#63) : entité résolue mais hors du périmètre de l'opérateur.
            errors.add(new ImportError(line, "eau_nom", ImportSchema.REFERENTIEL, ImportSchema.REF_WATER_ENTITY_SCOPE,
                    "entité hydrographique « " + get(rec, "eau_nom") + " » hors de votre périmètre"));
        }

        parsed.techniqueId = importDao.resolveTechnique(get(rec, "technique"));
        if (parsed.techniqueId == null) {
            errors.add(new ImportError(line, "technique", ImportSchema.REFERENTIEL, ImportSchema.REF_TECHNIQUE,
                    "technique non résolue : « " + get(rec, "technique") + " »"));
        }

        if (hasCapture) {
            parsed.speciesId = importDao.resolveSpecies(get(rec, "capture_espece"));
            if (parsed.speciesId == null) {
                errors.add(new ImportError(line, "capture_espece", ImportSchema.REFERENTIEL, ImportSchema.REF_SPECIES,
                        "espèce non résolue : « " + get(rec, "capture_espece") + " »"));
            }
        }

        if (!errors.isEmpty()) {
            return new RowValidation(errors, null);
        }

        // --- Étage 3 : métier ---
        if (hasCapture) {
            int quantity = 1;
            String rawNb = get(rec, "capture_nombre");
            if (!CsvSupport.isBlank(rawNb)) {
                try {
                    quantity = CsvSupport.parseInt(rawNb);
                } catch (RuntimeException e) {
                    quantity = 0;
                }
            }
            if (quantity < 1) {
                errors.add(new ImportError(line, "capture_nombre", ImportSchema.METIER, ImportSchema.METIER_QUANTITY,
                        "le nombre d'individus d'un lot doit être ≥ 1"));
            }
            parsed.quantity = quantity;

            Integer longueur = null;
            if (!CsvSupport.isBlank(get(rec, "capture_longueur_cm"))) {
                try {
                    longueur = CsvSupport.parseInt(get(rec, "capture_longueur_cm"));
                } catch (RuntimeException e) {
                    errors.add(new ImportError(line, "capture_longueur_cm", ImportSchema.METIER,
                            ImportSchema.METIER_SIZE_ABERRANT, "longueur non numérique"));
                }
            }
            if (longueur != null) {
                ImportDao.Bounds bounds = importDao.sizeBounds(parsed.speciesId);
                if (sizeOutOfBounds(bounds, longueur)) {
                    errors.add(new ImportError(line, "capture_longueur_cm", ImportSchema.METIER,
                            ImportSchema.METIER_SIZE_ABERRANT,
                            "taille " + longueur + " cm hors bornes [" + bounds.min() + "-" + bounds.max()
                                    + "] pour l'espèce"));
                }
            }
            parsed.longueur = longueur;

            Integer weight = null;
            if (!CsvSupport.isBlank(get(rec, "capture_poids_g"))) {
                try {
                    weight = CsvSupport.parseInt(get(rec, "capture_poids_g"));
                } catch (RuntimeException e) {
                    weight = null;
                }
            }
            parsed.weight = weight;

            boolean kept = false;
            try {
                String raw = get(rec, "capture_conservation");
                kept = CsvSupport.parseBoolOuiNon(raw.isEmpty() ? "non" : raw);
            } catch (RuntimeException e) {
                kept = false;
            }
            parsed.kept = kept;
            parsed.sizeClass = emptyToNull(get(rec, "capture_classe_taille"));
            parsed.description = emptyToNull(get(rec, "capture_pathologies"));
        } else if (bredouille && !CsvSupport.isBlank(get(rec, "capture_espece"))) {
            errors.add(new ImportError(line, "bredouille", ImportSchema.METIER, ImportSchema.METIER_BREDOUILLE,
                    "sortie déclarée bredouille mais une espèce capturée est renseignée"));
        }

        if (!errors.isEmpty()) {
            return new RowValidation(errors, null);
        }
        return new RowValidation(List.of(), parsed);
    }

    // --- Validation de tout le fichier (pure hors résolution référentielle) ---

    /** Regroupe les lignes valides par session_ref (ordre d'apparition préservé). */
    Result validateAll(List<Map<String, String>> records, Set<UUID> allowedWaterEntities) {
        List<ImportError> allErrors = new ArrayList<>();
        LinkedHashMap<String, List<ParsedRow>> sessions = new LinkedHashMap<>();
        int line = 1; // l'en-tête est la ligne 1
        for (Map<String, String> rec : records) {
            line++;
            RowValidation rv = validateRow(line, rec, allowedWaterEntities);
            if (!rv.errors().isEmpty()) {
                allErrors.addAll(rv.errors());
                continue;
            }
            String sref = get(rec, "session_ref");
            if (sref.isEmpty()) {
                sref = "L" + line;
            }
            sessions.computeIfAbsent(sref, k -> new ArrayList<>()).add(rv.parsed());
        }
        return new Result(allErrors, sessions);
    }

    private record Result(List<ImportError> errors, LinkedHashMap<String, List<ParsedRow>> sessions) {}

    // --- Exécution complète --------------------------------------------------

    public ImportResultBean run(byte[] fileBytes, String filename, String mode, UUID createdBy,
                                Set<UUID> allowedWaterEntities) {
        String fileHash = sha256Hex(fileBytes);

        var existing = importDao.findByHash(fileHash);
        if (existing.isPresent()) {
            ImportDao.JobSummary j = existing.get();
            ImportResultBean dup = new ImportResultBean(j.id(), j.status(), j.total(), j.inserted(), j.rejected(),
                    true, List.of());
            return dup;
        }

        String text = decode(fileBytes);
        CsvSupport.Parsed parsed = CsvSupport.readRows(text);
        int total = parsed.records().size();

        // Étage 0 : en-tête conforme
        if (!parsed.header().equals(ImportSchema.EXPECTED_HEADER)) {
            ImportError headerErr = new ImportError(1, null, ImportSchema.STRUCTUREL, ImportSchema.STRUCT_HEADER,
                    "en-tête non conforme au modèle attendu (28 colonnes)");
            try {
                ImportDao.Persisted p = importDao.persist(filename, fileHash, "FAILED", total, 0, createdBy,
                        List.of(headerErr), false, Map.of());
                return new ImportResultBean(p.jobId(), "FAILED", total, 0, 0, false, List.of(headerErr));
            } catch (DataAccessException e) {
                return concurrentDuplicate(fileHash, e);
            }
        }

        Result validation = validateAll(parsed.records(), allowedWaterEntities);
        List<ImportError> allErrors = validation.errors();
        Set<Integer> rejectedLines = new LinkedHashSet<>();
        for (ImportError e : allErrors) {
            rejectedLines.add(e.line());
        }
        int rejected = rejectedLines.size();
        boolean doInsert = !(MODE_ALL_OR_NOTHING.equals(mode) && !allErrors.isEmpty());
        String status = allErrors.isEmpty() ? "DONE" : "DONE_WITH_ERRORS";

        try {
            ImportDao.Persisted p = importDao.persist(filename, fileHash, status, total, rejected, createdBy,
                    allErrors, doInsert, validation.sessions());
            return new ImportResultBean(p.jobId(), status, total, p.inserted(), rejected, false, allErrors);
        } catch (DataAccessException e) {
            return concurrentDuplicate(fileHash, e);
        }
    }

    /**
     * Course TOCTOU : entre le contrôle d'unicité et l'insert, un import concurrent a posé le
     * même {@code file_hash} → la contrainte unique lève. On renvoie alors le doublon proprement
     * (comme si le contrôle initial l'avait vu) ; si le hash reste introuvable, l'erreur n'est pas
     * un doublon et on la relaie.
     */
    private ImportResultBean concurrentDuplicate(String fileHash, DataAccessException original) {
        // On ne conclut au doublon QUE sur une violation de contrainte d'unicité (SQLState 23505).
        // Toute autre erreur de persistance doit remonter : sinon un échec d'insertion serait
        // présenté à l'opérateur comme « fichier déjà importé », alors que rien n'a été écrit.
        if (!isUniqueViolation(original)) {
            throw original;
        }
        return importDao.findByHash(fileHash)
                .map(j -> new ImportResultBean(j.id(), j.status(), j.total(), j.inserted(), j.rejected(),
                        true, List.of()))
                .orElseThrow(() -> original);
    }

    /** SQLState 23505 = unique_violation (PostgreSQL), y compris à travers les causes chaînées. */
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

    /**
     * Décode le CSV en tolérant les encodages réels du terrain : BOM UTF-8 (utf-8-sig),
     * UTF-8 strict, et à défaut Windows-1252 (export Excel FR par défaut) — sinon les accents
     * des libellés (« Brochet »…) seraient corrompus et casseraient la résolution référentielle.
     */
    private static String decode(byte[] bytes) {
        // BOM UTF-8 explicite → on le retire et on décode en UTF-8.
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        // UTF-8 strict ; si les octets ne sont pas de l'UTF-8 valide, repli Windows-1252.
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
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
