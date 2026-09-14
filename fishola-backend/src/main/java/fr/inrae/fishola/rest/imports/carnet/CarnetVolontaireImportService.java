package fr.inrae.fishola.rest.imports.carnet;

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
import fr.inrae.fishola.rest.imports.ImportDao;
import fr.inrae.fishola.rest.imports.ImportError;
import fr.inrae.fishola.rest.imports.ImportResultBean;
import fr.inrae.fishola.rest.imports.ImportService;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pipeline d'import CSV opérateur « carnet volontaire » (#143), dédié — format
 * distinct du pipeline générique ({@link ImportService}, #71).
 *
 * Trois étages : structurel (colonnes/types/cohérence), référentiel (résolution
 * espèce/technique/entité hydro), métier (bornes de taille, lots, bredouille, marquage).
 * Les sorties sont regroupées par {@code session_ref} ; chaque ligne de capture devient
 * un {@code catch}. Suivi et idempotence (SHA-256) dans {@code import_job}.
 */
@Singleton
public class CarnetVolontaireImportService {

    public static final String MODE_ALL_OR_NOTHING = "all_or_nothing";
    public static final String MODE_PARTIAL = "partial";

    @Inject
    protected ImportDao importDao;

    private record RowValidation(List<ImportError> errors, CarnetVolontaireParsedRow parsed) {}

    private static String get(Map<String, String> rec, String key) {
        String v = rec.get(key);
        return v == null ? "" : v;
    }

    private static String emptyToNull(String v) {
        return (v == null || v.isEmpty()) ? null : v;
    }

    /** Minuscule, sans accents : sert à comparer les valeurs figées (mode de pêche, origine TRF). */
    private static String normalize(String v) {
        String s = Normalizer.normalize(v.strip().toLowerCase(), Normalizer.Form.NFD);
        return s.replaceAll("\\p{M}", "");
    }

    // --- Validation d'une ligne ---------------------------------------------

    private RowValidation validateRow(int line, Map<String, String> rec, Set<UUID> allowedWaterEntities) {
        List<ImportError> errors = new ArrayList<>();
        CarnetVolontaireParsedRow parsed = new CarnetVolontaireParsedRow();
        parsed.line = line;

        // --- Étage 1 : structurel ---
        try {
            parsed.day = CsvSupport.parseDate(get(rec, "date"));
        } catch (RuntimeException e) {
            errors.add(new ImportError(line, "date", CarnetVolontaireSchema.STRUCTUREL,
                    CarnetVolontaireSchema.STRUCT_DATE, "date invalide (JJ/MM/AAAA) : « " + get(rec, "date") + " »"));
        }

        boolean timesOk = true;
        try {
            parsed.start = CsvSupport.parseTime(get(rec, "heure_debut"));
        } catch (RuntimeException e) {
            timesOk = false;
            errors.add(new ImportError(line, "heure_debut", CarnetVolontaireSchema.STRUCTUREL,
                    CarnetVolontaireSchema.STRUCT_TIME, "heure invalide (HH:MM) : « " + get(rec, "heure_debut") + " »"));
        }
        try {
            parsed.end = CsvSupport.parseTime(get(rec, "heure_fin"));
        } catch (RuntimeException e) {
            timesOk = false;
            errors.add(new ImportError(line, "heure_fin", CarnetVolontaireSchema.STRUCTUREL,
                    CarnetVolontaireSchema.STRUCT_TIME, "heure invalide (HH:MM) : « " + get(rec, "heure_fin") + " »"));
        }
        if (timesOk && !parsed.end.isAfter(parsed.start)) {
            errors.add(new ImportError(line, "heure_fin", CarnetVolontaireSchema.STRUCTUREL,
                    CarnetVolontaireSchema.STRUCT_TIME_ORDER, "l'heure de fin doit être postérieure à l'heure de début"));
        }

        String heureCapture = get(rec, "capture_heure");
        if (!CsvSupport.isBlank(heureCapture)) {
            try {
                parsed.captureTime = CsvSupport.parseTime(heureCapture);
            } catch (RuntimeException e) {
                errors.add(new ImportError(line, "capture_heure", CarnetVolontaireSchema.STRUCTUREL,
                        CarnetVolontaireSchema.STRUCT_TIME, "heure invalide (HH:MM) : « " + heureCapture + " »"));
            }
        }

        String mode = normalize(get(rec, "mode_peche"));
        if (!CarnetVolontaireSchema.MODES_PECHE.contains(mode)) {
            errors.add(new ImportError(line, "mode_peche", CarnetVolontaireSchema.STRUCTUREL,
                    CarnetVolontaireSchema.STRUCT_MODE, "mode de pêche inconnu : « " + get(rec, "mode_peche") + " »"));
        }

        boolean bredouille = false;
        try {
            bredouille = CsvSupport.parseBoolOuiNon(get(rec, "bredouille"));
        } catch (RuntimeException e) {
            errors.add(new ImportError(line, "bredouille", CarnetVolontaireSchema.STRUCTUREL,
                    CarnetVolontaireSchema.STRUCT_BREDOUILLE, "valeur oui/non attendue"));
        }
        parsed.bredouille = bredouille;

        String origineTrf = get(rec, "capture_origine_trf");
        if (!CsvSupport.isBlank(origineTrf)) {
            String normalized = normalize(origineTrf);
            if (!CarnetVolontaireSchema.TROUT_ORIGINS.contains(normalized)) {
                errors.add(new ImportError(line, "capture_origine_trf", CarnetVolontaireSchema.STRUCTUREL,
                        CarnetVolontaireSchema.STRUCT_TROUT_ORIGIN, "origine TRF inconnue : « " + origineTrf + " »"));
            } else {
                parsed.troutOrigin = normalized;
            }
        }

        if (!errors.isEmpty()) {
            return new RowValidation(errors, null); // on n'enchaîne pas les étages suivants
        }

        boolean hasCapture = !bredouille && !CsvSupport.isBlank(get(rec, "capture_espece"));
        parsed.hasCapture = hasCapture;

        // --- Étage 2 : référentiel ---
        parsed.waterEntityId = importDao.resolveWaterEntity(get(rec, "eau_nom"), get(rec, "commune"));
        if (parsed.waterEntityId == null) {
            errors.add(new ImportError(line, "eau_nom", CarnetVolontaireSchema.REFERENTIEL,
                    CarnetVolontaireSchema.REF_WATER_ENTITY,
                    "entité hydrographique non résolue : « " + get(rec, "eau_nom") + " » / commune « "
                            + get(rec, "commune") + " »"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(parsed.waterEntityId)) {
            errors.add(new ImportError(line, "eau_nom", CarnetVolontaireSchema.REFERENTIEL,
                    CarnetVolontaireSchema.REF_WATER_ENTITY_SCOPE,
                    "entité hydrographique « " + get(rec, "eau_nom") + " » hors de votre périmètre"));
        }

        parsed.techniqueId = importDao.resolveTechnique(get(rec, "technique_principale"));
        if (parsed.techniqueId == null) {
            errors.add(new ImportError(line, "technique_principale", CarnetVolontaireSchema.REFERENTIEL,
                    CarnetVolontaireSchema.REF_TECHNIQUE,
                    "technique non résolue : « " + get(rec, "technique_principale") + " »"));
        }

        String techniqueSecondaire = get(rec, "technique_secondaire");
        if (!CsvSupport.isBlank(techniqueSecondaire)) {
            parsed.secondaryTechniqueId = importDao.resolveTechnique(techniqueSecondaire);
            if (parsed.secondaryTechniqueId == null) {
                errors.add(new ImportError(line, "technique_secondaire", CarnetVolontaireSchema.REFERENTIEL,
                        CarnetVolontaireSchema.REF_TECHNIQUE_SECONDARY,
                        "technique secondaire non résolue : « " + techniqueSecondaire + " »"));
            }
        }
        parsed.baitOrLure = emptyToNull(get(rec, "appat_leurre"));
        parsed.observations = emptyToNull(get(rec, "observations_diverses"));

        String especeRecherchee = get(rec, "espece_recherchee");
        if (CsvSupport.isBlank(especeRecherchee)) {
            errors.add(new ImportError(line, "espece_recherchee", CarnetVolontaireSchema.REFERENTIEL,
                    CarnetVolontaireSchema.REF_EXPECTED_SPECIES, "espèce recherchée obligatoire (ou « Aucune »)"));
        } else if (!normalize(especeRecherchee).equals(CarnetVolontaireSchema.ESPECE_RECHERCHEE_AUCUNE)) {
            parsed.expectedSpeciesId = importDao.resolveSpecies(especeRecherchee);
            if (parsed.expectedSpeciesId == null) {
                errors.add(new ImportError(line, "espece_recherchee", CarnetVolontaireSchema.REFERENTIEL,
                        CarnetVolontaireSchema.REF_EXPECTED_SPECIES,
                        "espèce recherchée non résolue : « " + especeRecherchee + " »"));
            }
        }

        String captureTechnique = get(rec, "capture_technique");
        if (!CsvSupport.isBlank(captureTechnique)) {
            parsed.captureTechniqueId = importDao.resolveTechnique(captureTechnique);
            if (parsed.captureTechniqueId == null) {
                errors.add(new ImportError(line, "capture_technique", CarnetVolontaireSchema.REFERENTIEL,
                        CarnetVolontaireSchema.REF_TECHNIQUE,
                        "technique de capture non résolue : « " + captureTechnique + " »"));
            }
        }
        parsed.captureBaitOrLure = emptyToNull(get(rec, "capture_appat_leurre"));

        if (hasCapture) {
            parsed.speciesId = importDao.resolveSpecies(get(rec, "capture_espece"));
            if (parsed.speciesId == null) {
                errors.add(new ImportError(line, "capture_espece", CarnetVolontaireSchema.REFERENTIEL,
                        CarnetVolontaireSchema.REF_SPECIES,
                        "espèce non résolue : « " + get(rec, "capture_espece") + " »"));
            }
        }

        if (!errors.isEmpty()) {
            return new RowValidation(errors, null);
        }

        // Repli : technique/appât de capture héritent de la sortie si non renseignés.
        if (parsed.captureTechniqueId == null) {
            parsed.captureTechniqueId = parsed.techniqueId;
        }
        if (parsed.captureBaitOrLure == null) {
            parsed.captureBaitOrLure = parsed.baitOrLure;
        }

        // --- Étage 3 : métier ---
        int nbLignes = 0;
        try {
            nbLignes = CsvSupport.parseInt(get(rec, "nombre_lignes"));
        } catch (RuntimeException e) {
            nbLignes = 0;
        }
        if (nbLignes < 1) {
            errors.add(new ImportError(line, "nombre_lignes", CarnetVolontaireSchema.METIER,
                    CarnetVolontaireSchema.METIER_QUANTITY, "le nombre de lignes doit être ≥ 1"));
        }

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
                errors.add(new ImportError(line, "capture_nombre", CarnetVolontaireSchema.METIER,
                        CarnetVolontaireSchema.METIER_QUANTITY, "le nombre d'individus d'un lot doit être ≥ 1"));
            }
            parsed.quantity = quantity;

            boolean isLot = quantity > 1;
            if (isLot) {
                Integer min = tryParseInt(get(rec, "capture_taille_min"));
                Integer max = tryParseInt(get(rec, "capture_taille_max"));
                if (min == null || max == null) {
                    errors.add(new ImportError(line, "capture_taille_min", CarnetVolontaireSchema.METIER,
                            CarnetVolontaireSchema.METIER_LOT_BOUNDS,
                            "taille min et taille max obligatoires pour un lot (nombre > 1)"));
                } else if (min > max) {
                    errors.add(new ImportError(line, "capture_taille_min", CarnetVolontaireSchema.METIER,
                            CarnetVolontaireSchema.METIER_LOT_BOUNDS, "la taille min doit être ≤ à la taille max"));
                } else {
                    parsed.lotMinSize = min;
                    parsed.lotMaxSize = max;
                }
            } else {
                Integer size = tryParseInt(get(rec, "capture_taille"));
                if (size == null && importDao.isSizeMandatory(parsed.speciesId)) {
                    errors.add(new ImportError(line, "capture_taille", CarnetVolontaireSchema.METIER,
                            CarnetVolontaireSchema.METIER_SIZE_REQUIRED,
                            "taille obligatoire pour cette espèce"));
                }
                if (size != null) {
                    ImportDao.Bounds bounds = importDao.sizeBounds(parsed.speciesId);
                    if (ImportService.sizeOutOfBounds(bounds, size)) {
                        errors.add(new ImportError(line, "capture_taille", CarnetVolontaireSchema.METIER,
                                CarnetVolontaireSchema.METIER_SIZE_ABERRANT,
                                "taille " + size + " cm hors bornes [" + bounds.min() + "-" + bounds.max()
                                        + "] pour l'espèce"));
                    }
                    parsed.size = size;
                }
            }

            parsed.weight = tryParseInt(get(rec, "capture_poids"));

            boolean kept;
            try {
                String raw = get(rec, "capture_conservee");
                kept = CsvSupport.parseBoolOuiNon(raw.isEmpty() ? "non" : raw);
            } catch (RuntimeException e) {
                kept = false;
            }
            parsed.kept = kept;

            boolean tagged = false;
            String rawTagged = get(rec, "capture_marque");
            if (!CsvSupport.isBlank(rawTagged)) {
                try {
                    tagged = CsvSupport.parseBoolOuiNon(rawTagged);
                } catch (RuntimeException e) {
                    tagged = false;
                }
            }
            parsed.tagged = tagged;
            String tagReference = get(rec, "capture_marque_numero");
            if (tagged && CsvSupport.isBlank(tagReference)) {
                errors.add(new ImportError(line, "capture_marque_numero", CarnetVolontaireSchema.METIER,
                        CarnetVolontaireSchema.METIER_TAG_REF,
                        "numéro de marquage obligatoire quand le poisson est marqué/bagué"));
            }
            parsed.tagReference = emptyToNull(tagReference);
        } else if (bredouille && !CsvSupport.isBlank(get(rec, "capture_espece"))) {
            errors.add(new ImportError(line, "bredouille", CarnetVolontaireSchema.METIER,
                    CarnetVolontaireSchema.METIER_BREDOUILLE,
                    "sortie déclarée bredouille mais une espèce capturée est renseignée"));
        }

        if (!errors.isEmpty()) {
            return new RowValidation(errors, null);
        }
        return new RowValidation(List.of(), parsed);
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

    // --- Validation de tout le fichier (pure hors résolution référentielle) ---

    /** Regroupe les lignes valides par session_ref (ordre d'apparition préservé). */
    Result validateAll(List<Map<String, String>> records, Set<UUID> allowedWaterEntities) {
        List<ImportError> allErrors = new ArrayList<>();
        LinkedHashMap<String, List<CarnetVolontaireParsedRow>> sessions = new LinkedHashMap<>();
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

    private record Result(List<ImportError> errors, LinkedHashMap<String, List<CarnetVolontaireParsedRow>> sessions) {}

    // --- Exécution complète --------------------------------------------------

    public ImportResultBean run(byte[] fileBytes, String filename, String mode, UUID createdBy,
                                Set<UUID> allowedWaterEntities) {
        String fileHash = sha256Hex(fileBytes);

        var existing = importDao.findByHash(fileHash);
        if (existing.isPresent()) {
            ImportDao.JobSummary j = existing.get();
            return new ImportResultBean(j.id(), j.status(), j.total(), j.inserted(), j.rejected(), true, List.of());
        }

        String text = decode(fileBytes);
        CsvSupport.Parsed parsed = CsvSupport.readRows(text);
        int total = parsed.records().size();

        // Étage 0 : en-tête conforme
        if (!parsed.header().equals(CarnetVolontaireSchema.EXPECTED_HEADER)) {
            ImportError headerErr = new ImportError(1, null, CarnetVolontaireSchema.STRUCTUREL,
                    CarnetVolontaireSchema.STRUCT_HEADER,
                    "en-tête non conforme au modèle « carnet volontaire » attendu ("
                            + CarnetVolontaireSchema.EXPECTED_HEADER.size() + " colonnes)");
            try {
                // sessions vide : rien à persister, on réutilise persist() (générique sur le type
                // de ParsedRow, ici inféré vide) plutôt que de dupliquer l'insertion du job échoué.
                ImportDao.Persisted p = importDao.persist(filename, fileHash, "FAILED", total, 0,
                        createdBy, List.of(headerErr), false, Map.of());
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
            ImportDao.Persisted p = importDao.persistCarnetVolontaire(filename, fileHash, status, total, rejected,
                    createdBy, allErrors, doInsert, validation.sessions());
            return new ImportResultBean(p.jobId(), status, total, p.inserted(), rejected, false, allErrors);
        } catch (DataAccessException e) {
            return concurrentDuplicate(fileHash, e);
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

    private static String decode(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
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
