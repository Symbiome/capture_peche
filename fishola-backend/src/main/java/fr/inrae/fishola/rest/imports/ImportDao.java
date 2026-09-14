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

import fr.inrae.fishola.database.AbstractFisholaDao;
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.enums.CollectionMethod;
import fr.inrae.fishola.entities.enums.DayPeriod;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.FishingMode;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.enums.TroutOrigin;
import fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireParsedRow;
import fr.inrae.fishola.rest.imports.survey.SurveyAnglerOrigin;
import fr.inrae.fishola.rest.imports.survey.SurveyParsedCapture;
import fr.inrae.fishola.rest.imports.survey.SurveyParsedSession;
import fr.inrae.fishola.rest.imports.survey.SurveyParsedSortie;
import fr.inrae.fishola.rest.imports.survey.SurveyParsedSouvenir;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import jakarta.transaction.Transactional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static fr.inrae.fishola.entities.Tables.CATCH;
import static fr.inrae.fishola.entities.Tables.IMPORT_JOB;
import static fr.inrae.fishola.entities.Tables.IMPORT_ROW_ERROR;
import static fr.inrae.fishola.entities.Tables.SPECIES;
import static fr.inrae.fishola.entities.Tables.SPECIES_SIZE_BOUNDS;
import static fr.inrae.fishola.entities.Tables.SURVEY_SESSION;
import static fr.inrae.fishola.entities.Tables.SURVEYED_ANGLER;
import static fr.inrae.fishola.entities.Tables.TECHNIQUE;
import static fr.inrae.fishola.entities.Tables.TRIP;
import static fr.inrae.fishola.entities.Tables.WATER_ENTITY;

/**
 * Accès base pour l'import CSV opérateur (#71) : idempotence, résolution référentielle
 * (accent-insensible via {@code f_unaccent}) et persistance transactionnelle (job,
 * erreurs, sorties, captures). Écrit dans les tables du socle {@code V1.2.0}.
 */
@Singleton
public class ImportDao extends AbstractFisholaDao {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record JobSummary(UUID id, String status, int total, int inserted, int rejected) {}

    public record Bounds(Integer min, Integer max) {}

    public record Persisted(UUID jobId, int inserted) {}

    /**
     * Champs {@code trip} additionnels des formats carnet volontaire (#143) / enquête (#144,
     * cf. #145). {@link #NONE} pour le pipeline générique (#71), qui n'en a pas.
     */
    public record TripExtras(UUID expectedSpeciesId, UUID secondaryTechniqueId, String baitOrLure,
                             Short rodCount, FishingMode fishingMode, String[] observations,
                             DayPeriod dayPeriod, String externalRef, UUID surveySessionId,
                             UUID surveyedAnglerId) {
        public static final TripExtras NONE =
                new TripExtras(null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Champs {@code catch} additionnels des formats carnet volontaire (#143) / enquête (#144,
     * cf. #145). {@link #NONE} pour le pipeline générique (#71), qui n'en a pas.
     */
    public record CatchExtras(TroutOrigin troutOrigin, Short lotMinSize, Short lotMaxSize,
                              Boolean tagged, String tagReference, String baitOrLure,
                              LocalDateTime catchTimestamp) {
        public static final CatchExtras NONE =
                new CatchExtras(null, null, null, null, null, null, null);
    }

    // --- Idempotence ---------------------------------------------------------

    public Optional<JobSummary> findByHash(String fileHash) {
        Record r = withContext(ctx -> ctx
                .select(IMPORT_JOB.ID, IMPORT_JOB.STATUS, IMPORT_JOB.TOTAL, IMPORT_JOB.INSERTED, IMPORT_JOB.REJECTED)
                .from(IMPORT_JOB)
                .where(IMPORT_JOB.FILE_HASH.eq(fileHash))
                .fetchOne());
        if (r == null) {
            return Optional.empty();
        }
        return Optional.of(new JobSummary(
                r.get(IMPORT_JOB.ID), r.get(IMPORT_JOB.STATUS),
                r.get(IMPORT_JOB.TOTAL), r.get(IMPORT_JOB.INSERTED), r.get(IMPORT_JOB.REJECTED)));
    }

    // --- Résolution référentielle -------------------------------------------

    private static Condition unaccentEq(Field<String> field, String value) {
        return DSL.condition("f_unaccent(lower({0})) = f_unaccent(lower({1}))", field, DSL.val(value));
    }

    private static Condition unaccentContains(Field<String> field, String value) {
        return DSL.condition("f_unaccent(lower({0})) ILIKE '%' || f_unaccent(lower({1})) || '%'", field, DSL.val(value));
    }

    public UUID resolveSpecies(String name) {
        if (CsvSupport.isBlank(name)) {
            return null;
        }
        return withContext(ctx -> ctx.select(SPECIES.ID).from(SPECIES)
                .where(unaccentEq(SPECIES.NAME, name).or(unaccentEq(SPECIES.EXPORT_AS, name)))
                .limit(1).fetchOne(SPECIES.ID));
    }

    public UUID resolveTechnique(String name) {
        if (CsvSupport.isBlank(name)) {
            return null;
        }
        UUID exact = withContext(ctx -> ctx.select(TECHNIQUE.ID).from(TECHNIQUE)
                .where(unaccentEq(TECHNIQUE.NAME, name).or(unaccentEq(TECHNIQUE.EXPORT_AS, name)))
                .limit(1).fetchOne(TECHNIQUE.ID));
        if (exact != null) {
            return exact;
        }
        // Repli : le libellé court du template (« Leurre ») est contenu dans le nom du
        // référentiel (« Pêche aux leurres »). Accepté seulement si non ambigu.
        List<UUID> matches = withContext(ctx -> ctx.select(TECHNIQUE.ID).from(TECHNIQUE)
                .where(unaccentContains(TECHNIQUE.NAME, name))
                .limit(2).fetch(TECHNIQUE.ID));
        return matches.size() == 1 ? matches.get(0) : null;
    }

    public UUID resolveWaterEntity(String eauNom, String commune) {
        // Résolution par nom (accent-insensible) ; la désambiguïsation spatiale par
        // commune (homonymes) est un raffinement ultérieur (cf. #71 / #65).
        if (CsvSupport.isBlank(eauNom)) {
            return null;
        }
        return withContext(ctx -> ctx.select(WATER_ENTITY.ID).from(WATER_ENTITY)
                .where(unaccentEq(WATER_ENTITY.NAME, eauNom).or(unaccentEq(WATER_ENTITY.EXPORT_AS, eauNom)))
                .limit(1).fetchOne(WATER_ENTITY.ID));
    }

    public Bounds sizeBounds(UUID speciesId) {
        if (speciesId == null) {
            return null;
        }
        Record r = withContext(ctx -> ctx
                .select(SPECIES_SIZE_BOUNDS.MIN_SIZE_CM, SPECIES_SIZE_BOUNDS.MAX_SIZE_CM)
                .from(SPECIES_SIZE_BOUNDS)
                .where(SPECIES_SIZE_BOUNDS.SPECIES_ID.eq(speciesId))
                .fetchOne());
        return r == null ? null : new Bounds(r.get(SPECIES_SIZE_BOUNDS.MIN_SIZE_CM), r.get(SPECIES_SIZE_BOUNDS.MAX_SIZE_CM));
    }

    /** {@code species.mandatory_size} (#143) : la taille est-elle obligatoire pour cette espèce ? */
    public boolean isSizeMandatory(UUID speciesId) {
        if (speciesId == null) {
            return false;
        }
        Boolean mandatory = withContext(ctx -> ctx.select(SPECIES.MANDATORY_SIZE).from(SPECIES)
                .where(SPECIES.ID.eq(speciesId)).fetchOne(SPECIES.MANDATORY_SIZE));
        return mandatory != null && mandatory;
    }

    // --- Persistance ---------------------------------------------------------

    /**
     * Persiste en une transaction : le job, ses erreurs, et (si {@code doInsert}) une sortie
     * par {@code session_ref} avec ses captures. Renvoie l'id du job et le nombre de sorties créées.
     */
    @Transactional
    public Persisted persist(String fileName, String fileHash, String status, int total, int rejected,
                             UUID createdBy, List<ImportError> errors, boolean doInsert,
                             Map<String, List<ParsedRow>> sessions) {
        int insertedCount = doInsert ? sessions.size() : 0;
        // Atomicité portée par JTA (@Transactional), comme dans les autres DAO : ouvrir en plus
        // une transaction jOOQ sur une connexion déjà enrôlée fait échouer le commit — l'import
        // était alors annulé en silence.
        DSLContext ctx = newContext();

        UUID jobId = ctx.insertInto(IMPORT_JOB,
                        IMPORT_JOB.FILE_NAME, IMPORT_JOB.FILE_HASH, IMPORT_JOB.STATUS,
                        IMPORT_JOB.TOTAL, IMPORT_JOB.REJECTED, IMPORT_JOB.INSERTED, IMPORT_JOB.CREATED_BY)
                .values(fileName, fileHash, status, total, rejected, insertedCount, createdBy)
                .returning(IMPORT_JOB.ID)
                .fetchOne()
                .getId();

        for (ImportError e : errors) {
            ctx.insertInto(IMPORT_ROW_ERROR,
                            IMPORT_ROW_ERROR.IMPORT_ID, IMPORT_ROW_ERROR.LINE, IMPORT_ROW_ERROR.COLUMN_NAME,
                            IMPORT_ROW_ERROR.STAGE, IMPORT_ROW_ERROR.CODE, IMPORT_ROW_ERROR.MESSAGE)
                    .values(jobId, e.line(), e.column(), e.stage(), e.code(), e.message())
                    .execute();
        }

        if (doInsert) {
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, List<ParsedRow>> entry : sessions.entrySet()) {
                String sref = entry.getKey();
                List<ParsedRow> rows = entry.getValue();
                ParsedRow s = rows.get(0);
                String name = "Import " + sref + " " + s.day.format(DAY_FMT);

                UUID tripId = insertTrip(ctx, s.collectionMethod, s.day, s.start, s.end, s.waterEntityId, name, now,
                        TripExtras.NONE);

                for (ParsedRow p : rows) {
                    if (!p.hasCapture) {
                        continue;
                    }
                    insertCatch(ctx, tripId, p.speciesId, s.techniqueId, p.longueur, p.weight, p.kept,
                            p.quantity == null ? 1 : p.quantity, p.sizeClass, p.description, now, CatchExtras.NONE);
                }
                stampDepartment(ctx, tripId);
            }
        }

        return new Persisted(jobId, insertedCount);
    }

    /**
     * Variante de {@link #persist} pour le pipeline dédié « carnet volontaire » (#143) :
     * même mécanique (job, erreurs, sorties + captures par {@code session_ref}), sur des
     * {@link CarnetVolontaireParsedRow}. Une capture en lot (quantité > 1) synthétise
     * {@code catch.size_class} depuis les bornes du lot ({@code "min-max"}) faute de colonnes
     * dédiées (cf. #145) ; une capture individuelle utilise {@code catch.size} normalement.
     */
    @Transactional
    public Persisted persistCarnetVolontaire(String fileName, String fileHash, String status, int total,
                                             int rejected, UUID createdBy, List<ImportError> errors,
                                             boolean doInsert, Map<String, List<CarnetVolontaireParsedRow>> sessions) {
        int insertedCount = doInsert ? sessions.size() : 0;
        DSLContext ctx = newContext();

        UUID jobId = ctx.insertInto(IMPORT_JOB,
                        IMPORT_JOB.FILE_NAME, IMPORT_JOB.FILE_HASH, IMPORT_JOB.STATUS,
                        IMPORT_JOB.TOTAL, IMPORT_JOB.REJECTED, IMPORT_JOB.INSERTED, IMPORT_JOB.CREATED_BY,
                        IMPORT_JOB.COLLECTION_METHOD)
                .values(fileName, fileHash, status, total, rejected, insertedCount, createdBy,
                        CollectionMethod.carnet_volontaire)
                .returning(IMPORT_JOB.ID)
                .fetchOne()
                .getId();

        for (ImportError e : errors) {
            ctx.insertInto(IMPORT_ROW_ERROR,
                            IMPORT_ROW_ERROR.IMPORT_ID, IMPORT_ROW_ERROR.LINE, IMPORT_ROW_ERROR.COLUMN_NAME,
                            IMPORT_ROW_ERROR.STAGE, IMPORT_ROW_ERROR.CODE, IMPORT_ROW_ERROR.MESSAGE)
                    .values(jobId, e.line(), e.column(), e.stage(), e.code(), e.message())
                    .execute();
        }

        if (doInsert) {
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, List<CarnetVolontaireParsedRow>> entry : sessions.entrySet()) {
                String sref = entry.getKey();
                List<CarnetVolontaireParsedRow> rows = entry.getValue();
                CarnetVolontaireParsedRow s = rows.get(0);
                String name = "Carnet volontaire " + sref + " " + s.day.format(DAY_FMT);

                UUID tripId = insertTrip(ctx, "carnet_volontaire", s.day, s.start, s.end, s.waterEntityId, name, now);

                for (CarnetVolontaireParsedRow p : rows) {
                    if (!p.hasCapture) {
                        continue;
                    }
                    String sizeClass = (p.lotMinSize != null && p.lotMaxSize != null)
                            ? (p.lotMinSize + "-" + p.lotMaxSize)
                            : null;
                    insertCatch(ctx, tripId, p.speciesId, p.captureTechniqueId, p.size, p.weight, p.kept,
                            p.quantity == null ? 1 : p.quantity, sizeClass, null, now);
                }
                stampDepartment(ctx, tripId);
            }
        }

        return new Persisted(jobId, insertedCount);
    }

    public record SurveyPersisted(UUID jobId, int inserted) {}

    private static final Map<DayPeriod, LocalTime[]> NOMINAL_TIMES_BY_PERIOD = Map.of(
            DayPeriod.matin, new LocalTime[] {LocalTime.of(8, 0), LocalTime.of(12, 0)},
            DayPeriod.apres_midi, new LocalTime[] {LocalTime.of(13, 0), LocalTime.of(18, 0)},
            DayPeriod.journee_entiere, new LocalTime[] {LocalTime.of(8, 0), LocalTime.of(20, 0)},
            DayPeriod.soiree, new LocalTime[] {LocalTime.of(18, 0), LocalTime.of(22, 0)});

    /**
     * Persiste l'import « enquête terrain » (#144) : job, erreurs, puis (si {@code doInsert})
     * une {@code survey_session} par session, un {@code surveyed_angler} par pêcheur enquêté,
     * une {@code Trip} par {@code (Code sortie, Code pêcheur)} avec ses captures, et une
     * {@code Trip} indépendante par {@code Session souvenir} ({@code collection_method =
     * 'enquete_souvenir'}, sans heures précises -- {@link #NOMINAL_TIMES_BY_PERIOD} ne sert
     * qu'à satisfaire la contrainte NOT NULL de {@code trip.begin_timestamp/end_timestamp} ;
     * la donnée de référence reste {@code trip.day_period}).
     */
    @Transactional
    public SurveyPersisted persistSurvey(String fileName, String fileHash, String status, int total, int rejected,
                                         UUID createdBy, List<ImportError> errors, boolean doInsert,
                                         Map<String, SurveyParsedSession> sessions,
                                         Map<String, SurveyParsedSortie> sorties,
                                         Map<String, List<SurveyParsedCapture>> tripsByKey,
                                         Map<String, SurveyAnglerOrigin> anglerOrigins,
                                         List<SurveyParsedSouvenir> souvenirs) {
        int insertedCount = doInsert ? tripsByKey.size() + souvenirs.size() : 0;
        DSLContext ctx = newContext();

        UUID jobId = ctx.insertInto(IMPORT_JOB,
                        IMPORT_JOB.FILE_NAME, IMPORT_JOB.FILE_HASH, IMPORT_JOB.STATUS,
                        IMPORT_JOB.TOTAL, IMPORT_JOB.REJECTED, IMPORT_JOB.INSERTED, IMPORT_JOB.CREATED_BY,
                        IMPORT_JOB.COLLECTION_METHOD)
                .values(fileName, fileHash, status, total, rejected, insertedCount, createdBy,
                        CollectionMethod.enquete)
                .returning(IMPORT_JOB.ID)
                .fetchOne()
                .getId();

        for (ImportError e : errors) {
            ctx.insertInto(IMPORT_ROW_ERROR,
                            IMPORT_ROW_ERROR.IMPORT_ID, IMPORT_ROW_ERROR.LINE, IMPORT_ROW_ERROR.COLUMN_NAME,
                            IMPORT_ROW_ERROR.STAGE, IMPORT_ROW_ERROR.CODE, IMPORT_ROW_ERROR.MESSAGE)
                    .values(jobId, e.line(), e.column(), e.stage(), e.code(), e.message())
                    .execute();
        }

        if (doInsert) {
            LocalDateTime now = LocalDateTime.now();

            Map<String, UUID> sessionIds = new java.util.HashMap<>();
            for (SurveyParsedSession s : sessions.values()) {
                UUID id = ctx.insertInto(SURVEY_SESSION,
                                SURVEY_SESSION.CODE, SURVEY_SESSION.WATER_ENTITY_ID, SURVEY_SESSION.DAY,
                                SURVEY_SESSION.UNSURVEYED_SHORE_ANGLERS, SURVEY_SESSION.UNSURVEYED_BOAT_ANGLERS)
                        .values(s.code, s.waterEntityId, s.day, s.unsurveyedShoreAnglers, s.unsurveyedBoatAnglers)
                        .returning(SURVEY_SESSION.ID)
                        .fetchOne()
                        .getId();
                sessionIds.put(s.code, id);
            }

            Map<String, UUID> anglerIds = new java.util.HashMap<>();
            for (Map.Entry<String, SurveyAnglerOrigin> entry : anglerOrigins.entrySet()) {
                SurveyAnglerOrigin origin = entry.getValue();
                UUID id = ctx.insertInto(SURVEYED_ANGLER,
                                SURVEYED_ANGLER.CODE, SURVEYED_ANGLER.ORIGIN_DEPARTMENT, SURVEYED_ANGLER.ORIGIN_COUNTRY)
                        .values(entry.getKey(), origin.department(), origin.country())
                        .returning(SURVEYED_ANGLER.ID)
                        .fetchOne()
                        .getId();
                anglerIds.put(entry.getKey(), id);
            }

            for (List<SurveyParsedCapture> rows : tripsByKey.values()) {
                SurveyParsedCapture first = rows.get(0);
                SurveyParsedSortie sortie = sorties.get(first.sortieCode);
                SurveyParsedSession session = sessions.get(sortie.sessionCode);
                UUID sessionId = sessionIds.get(sortie.sessionCode);
                UUID anglerId = anglerIds.get(first.anglerCode);

                String name = "Enquête " + first.sortieCode + "/" + first.anglerCode + " " + session.day.format(DAY_FMT);
                TripExtras extras = new TripExtras(first.expectedSpeciesId, null, first.baitOrLure,
                        first.rodCount == null ? null : first.rodCount.shortValue(), first.fishingMode, null,
                        null, first.sortieCode + "/" + first.anglerCode, sessionId, anglerId);
                UUID tripId = insertTrip(ctx, "enquete", session.day, sortie.startTime, sortie.endTime,
                        session.waterEntityId, name, now, extras);

                for (SurveyParsedCapture row : rows) {
                    if (!row.hasCapture) {
                        continue;
                    }
                    String sizeClass = (row.lotMinSize != null && row.lotMaxSize != null)
                            ? (row.lotMinSize + "-" + row.lotMaxSize) : null;
                    CatchExtras catchExtras = new CatchExtras(null,
                            row.lotMinSize == null ? null : row.lotMinSize.shortValue(),
                            row.lotMaxSize == null ? null : row.lotMaxSize.shortValue(),
                            null, null, first.baitOrLure, null);
                    insertCatch(ctx, tripId, row.speciesId, first.techniqueId, row.size, null, row.kept,
                            row.quantity == null ? 1 : row.quantity, sizeClass, null, now, catchExtras);
                }
                stampDepartment(ctx, tripId);
            }

            for (SurveyParsedSouvenir s : souvenirs) {
                UUID anglerId = anglerIds.get(s.anglerCode);
                LocalTime[] nominal = NOMINAL_TIMES_BY_PERIOD.get(s.dayPeriod);

                String name = "Enquête souvenir " + s.anglerCode + " " + s.day.format(DAY_FMT);
                TripExtras extras = new TripExtras(s.expectedSpeciesId, null, s.baitOrLure,
                        s.rodCount == null ? null : s.rodCount.shortValue(), s.fishingMode, null,
                        s.dayPeriod, s.anglerCode, null, anglerId);
                UUID tripId = insertTrip(ctx, "enquete_souvenir", s.day, nominal[0], nominal[1],
                        s.waterEntityId, name, now, extras);

                if (s.hasCapture) {
                    String sizeClass = (s.lotMinSize != null && s.lotMaxSize != null)
                            ? (s.lotMinSize + "-" + s.lotMaxSize) : null;
                    CatchExtras catchExtras = new CatchExtras(null,
                            s.lotMinSize == null ? null : s.lotMinSize.shortValue(),
                            s.lotMaxSize == null ? null : s.lotMaxSize.shortValue(),
                            null, null, s.baitOrLure, null);
                    insertCatch(ctx, tripId, s.speciesId, s.techniqueId, s.size, null, s.kept,
                            s.quantity == null ? 1 : s.quantity, sizeClass, null, now, catchExtras);
                }
                stampDepartment(ctx, tripId);
            }
        }

        return new SurveyPersisted(jobId, insertedCount);
    }

    // --- Saisie manuelle (#72) : réutilise la même persistance trip + catch --

    public record ManualCatch(UUID speciesId, UUID techniqueId, Integer size, Integer weight, boolean kept,
                              Integer quantity, String sizeClass, String description) {}

    public boolean existsWaterEntity(UUID id) {
        return id != null && withContext(ctx -> ctx.fetchExists(WATER_ENTITY, WATER_ENTITY.ID.eq(id)));
    }

    public boolean existsTechnique(UUID id) {
        return id != null && withContext(ctx -> ctx.fetchExists(TECHNIQUE, TECHNIQUE.ID.eq(id)));
    }

    public boolean existsSpecies(UUID id) {
        return id != null && withContext(ctx -> ctx.fetchExists(SPECIES, SPECIES.ID.eq(id)));
    }

    /**
     * Persiste une saisie manuelle (une sortie + ses captures) en une transaction.
     * La technique d'une capture retombe sur celle de la sortie si absente.
     */
    @Transactional
    public UUID saveManualEntry(String collectionMethod, LocalDate day, LocalTime start, LocalTime end,
                                UUID waterEntityId, String name, UUID tripTechniqueId, List<ManualCatch> catches) {
        // Atomicité JTA (cf. remarque sur persist()).
        DSLContext ctx = newContext();
        LocalDateTime now = LocalDateTime.now();
        UUID tripId = insertTrip(ctx, collectionMethod, day, start, end, waterEntityId, name, now);
        for (ManualCatch c : catches) {
            UUID technique = c.techniqueId() != null ? c.techniqueId() : tripTechniqueId;
            insertCatch(ctx, tripId, c.speciesId(), technique, c.size(), c.weight(), c.kept(),
                    c.quantity() == null ? 1 : c.quantity(), c.sizeClass(), c.description(), now);
        }
        stampDepartment(ctx, tripId);
        return tripId;
    }

    /** Une capture (ou un lot) saisie manuellement, format « enquête terrain » (#144, #145). */
    public record SurveyManualCatch(UUID speciesId, UUID techniqueId, Integer size, Integer quantity, boolean kept,
                                    Short lotMinSize, Short lotMaxSize) {}

    /** Un pêcheur interrogé, saisi manuellement (#144) : sa sortie en cours + sa session souvenir facultative. */
    public record SurveyManualAngler(SurveyAnglerOrigin origin, FishingMode fishingMode, UUID techniqueId,
                                     Short rodCount, String baitOrLure, UUID expectedSpeciesId,
                                     List<SurveyManualCatch> catches, SurveyManualSouvenir souvenir) {}

    /** Bloc « session souvenir » facultatif d'un pêcheur, saisi manuellement (#144). {@code catch_} nul = bredouille. */
    public record SurveyManualSouvenir(LocalDate day, DayPeriod dayPeriod, UUID waterEntityId, FishingMode fishingMode,
                                       UUID techniqueId, Short rodCount, String baitOrLure, UUID expectedSpeciesId,
                                       SurveyManualCatch catch_) {}

    public record ManualSurveyResult(UUID sessionId, List<UUID> tripIds) {}

    /**
     * Persiste une saisie manuelle « enquête terrain » (#144) en une transaction : une
     * {@code survey_session} et, par pêcheur interrogé, un {@code surveyed_angler}, une
     * {@code Trip} (+ ses captures) et, si renseignée, une {@code Trip} « session souvenir »
     * indépendante. Codes session / pêcheur générés ici (jamais saisis, cf. issue #144).
     */
    @Transactional
    public ManualSurveyResult saveManualEntrySurvey(UUID waterEntityId, LocalDate day, LocalTime controlTime,
                                                     LocalTime startTime, LocalTime endTime,
                                                     Short unsurveyedShoreAnglers, Short unsurveyedBoatAnglers,
                                                     List<SurveyManualAngler> anglers) {
        DSLContext ctx = newContext();
        LocalDateTime now = LocalDateTime.now();

        String sessionCode = "MANUEL-" + UUID.randomUUID();
        UUID sessionId = ctx.insertInto(SURVEY_SESSION,
                        SURVEY_SESSION.CODE, SURVEY_SESSION.WATER_ENTITY_ID, SURVEY_SESSION.DAY,
                        SURVEY_SESSION.UNSURVEYED_SHORE_ANGLERS, SURVEY_SESSION.UNSURVEYED_BOAT_ANGLERS)
                .values(sessionCode, waterEntityId, day, unsurveyedShoreAnglers, unsurveyedBoatAnglers)
                .returning(SURVEY_SESSION.ID)
                .fetchOne()
                .getId();

        List<UUID> tripIds = new java.util.ArrayList<>();
        int anglerIndex = 0;
        for (SurveyManualAngler angler : anglers) {
            anglerIndex++;
            String anglerCode = "MANUEL-" + UUID.randomUUID();
            UUID anglerId = ctx.insertInto(SURVEYED_ANGLER,
                            SURVEYED_ANGLER.CODE, SURVEYED_ANGLER.ORIGIN_DEPARTMENT, SURVEYED_ANGLER.ORIGIN_COUNTRY)
                    .values(anglerCode, angler.origin().department(), angler.origin().country())
                    .returning(SURVEYED_ANGLER.ID)
                    .fetchOne()
                    .getId();

            String externalRef = sessionCode + "/angler-" + anglerIndex;
            String name = "Enquête " + externalRef + " " + day.format(DAY_FMT);
            TripExtras extras = new TripExtras(angler.expectedSpeciesId(), null, angler.baitOrLure(),
                    angler.rodCount(), angler.fishingMode(), null, null, externalRef, sessionId, anglerId);
            UUID tripId = insertTrip(ctx, "enquete", day, startTime, endTime, waterEntityId, name, now, extras);
            tripIds.add(tripId);

            for (SurveyManualCatch c : angler.catches()) {
                insertSurveyManualCatch(ctx, tripId, c, angler.techniqueId(), angler.baitOrLure(), now);
            }
            stampDepartment(ctx, tripId);

            SurveyManualSouvenir souvenir = angler.souvenir();
            if (souvenir != null) {
                LocalTime[] nominal = NOMINAL_TIMES_BY_PERIOD.get(souvenir.dayPeriod());
                String souvenirName = "Enquête souvenir " + anglerCode + " " + souvenir.day().format(DAY_FMT);
                TripExtras souvenirExtras = new TripExtras(souvenir.expectedSpeciesId(), null, souvenir.baitOrLure(),
                        souvenir.rodCount(), souvenir.fishingMode(), null, souvenir.dayPeriod(), anglerCode,
                        null, anglerId);
                UUID souvenirTripId = insertTrip(ctx, "enquete_souvenir", souvenir.day(), nominal[0], nominal[1],
                        souvenir.waterEntityId(), souvenirName, now, souvenirExtras);
                tripIds.add(souvenirTripId);
                if (souvenir.catch_() != null) {
                    insertSurveyManualCatch(ctx, souvenirTripId, souvenir.catch_(), souvenir.techniqueId(),
                            souvenir.baitOrLure(), now);
                }
                stampDepartment(ctx, souvenirTripId);
            }
        }
        return new ManualSurveyResult(sessionId, tripIds);
    }

    private void insertSurveyManualCatch(DSLContext ctx, UUID tripId, SurveyManualCatch c, UUID fallbackTechniqueId,
                                         String tripBaitOrLure, LocalDateTime now) {
        UUID technique = c.techniqueId() != null ? c.techniqueId() : fallbackTechniqueId;
        String sizeClass = (c.lotMinSize() != null && c.lotMaxSize() != null)
                ? (c.lotMinSize() + "-" + c.lotMaxSize()) : null;
        CatchExtras extras = new CatchExtras(null, c.lotMinSize(), c.lotMaxSize(), null, null, tripBaitOrLure, null);
        insertCatch(ctx, tripId, c.speciesId(), technique, c.size(), null, c.kept(),
                c.quantity() == null ? 1 : c.quantity(), sizeClass, null, now, extras);
    }

    // --- Inserts partagés import / saisie manuelle ---------------------------

    private UUID insertTrip(DSLContext ctx, String collectionMethod, LocalDate day, LocalTime start, LocalTime end,
                            UUID waterEntityId, String name, LocalDateTime now) {
        return insertTrip(ctx, collectionMethod, day, start, end, waterEntityId, name, now, TripExtras.NONE);
    }

    private UUID insertTrip(DSLContext ctx, String collectionMethod, LocalDate day, LocalTime start, LocalTime end,
                            UUID waterEntityId, String name, LocalDateTime now, TripExtras extras) {
        LocalDateTime beginTimestamp = LocalDateTime.of(day, start);
        LocalDateTime endTimestamp = LocalDateTime.of(day, end);
        if (endTimestamp.isBefore(beginTimestamp)) {
            endTimestamp = endTimestamp.plusDays(1);
        }
        return ctx.insertInto(TRIP,
                        TRIP.COLLECTION_METHOD, TRIP.BEGIN_TIMESTAMP, TRIP.END_TIMESTAMP,
                        TRIP.WATER_ENTITY_ID, TRIP.NAME, TRIP.TYPE, TRIP.MODE, TRIP.SOURCE,
                        TRIP.HIDDEN, TRIP.CREATED_ON, TRIP.EXPECTED_SPECIES_ID, TRIP.SECONDARY_TECHNIQUE_ID,
                        TRIP.BAIT_OR_LURE, TRIP.ROD_COUNT, TRIP.FISHING_MODE, TRIP.TRIP_OBSERVATIONS,
                        TRIP.DAY_PERIOD, TRIP.EXTERNAL_REF, TRIP.SURVEY_SESSION_ID, TRIP.SURVEYED_ANGLER_ID)
                .values(CollectionMethod.valueOf(collectionMethod), beginTimestamp, endTimestamp,
                        waterEntityId, name, TripType.Border, TripMode.Afterwards, DeviceType.web,
                        false, now, extras.expectedSpeciesId(), extras.secondaryTechniqueId(),
                        extras.baitOrLure(), extras.rodCount(), extras.fishingMode(), extras.observations(),
                        extras.dayPeriod(), extras.externalRef(), extras.surveySessionId(), extras.surveyedAnglerId())
                .returning(TRIP.ID)
                .fetchOne()
                .getId();
    }

    /**
     * Estampille le département d'une sortie importée et de ses prises (#159).
     * Aucune position n'est saisie à l'import : le COALESCE retombe sur le
     * département de l'entité hydro rattachée (dérivé de commune, #154). À appeler
     * une fois la sortie et ses prises insérées.
     */
    void stampDepartment(DSLContext ctx, UUID tripId) {
        ctx.execute("UPDATE trip t SET department = COALESCE("
                + "(SELECT d.code FROM departement d"
                + " WHERE ST_Contains(d.geom, COALESCE(t.snapped_position, t.begin_position, t.end_position)) LIMIT 1),"
                + "(SELECT we.department FROM water_entity we WHERE we.id = t.water_entity_id))"
                + " WHERE t.id = ?", tripId);
        ctx.execute("UPDATE catch c SET department = COALESCE("
                + "(SELECT d.code FROM departement d WHERE ST_Contains(d.geom, c.position) LIMIT 1),"
                + "(SELECT t.department FROM trip t WHERE t.id = c.trip_id))"
                + " WHERE c.trip_id = ?", tripId);
    }

    private void insertCatch(DSLContext ctx, UUID tripId, UUID speciesId, UUID techniqueId, Integer size,
                             Integer weight, boolean kept, int quantity, String sizeClass, String description,
                             LocalDateTime now) {
        insertCatch(ctx, tripId, speciesId, techniqueId, size, weight, kept, quantity, sizeClass, description,
                now, CatchExtras.NONE);
    }

    private void insertCatch(DSLContext ctx, UUID tripId, UUID speciesId, UUID techniqueId, Integer size,
                             Integer weight, boolean kept, int quantity, String sizeClass, String description,
                             LocalDateTime now, CatchExtras extras) {
        ctx.insertInto(CATCH,
                        CATCH.CREATED_ON, CATCH.TRIP_ID, CATCH.SPECIES_ID, CATCH.TECHNIQUE_ID,
                        CATCH.SIZE, CATCH.WEIGHT, CATCH.KEPT, CATCH.QUANTITY,
                        CATCH.SIZE_CLASS, CATCH.DESCRIPTION, CATCH.TROUT_ORIGIN, CATCH.LOT_MIN_SIZE_CM,
                        CATCH.LOT_MAX_SIZE_CM, CATCH.IS_TAGGED, CATCH.TAG_REFERENCE, CATCH.BAIT_OR_LURE,
                        CATCH.CATCH_TIMESTAMP)
                .values(now, tripId, speciesId, techniqueId, size, weight, kept, quantity, sizeClass, description,
                        extras.troutOrigin(), extras.lotMinSize(), extras.lotMaxSize(), extras.tagged(),
                        extras.tagReference(), extras.baitOrLure(), extras.catchTimestamp())
                .execute();
    }
}
