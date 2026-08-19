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
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
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

                UUID tripId = insertTrip(ctx, s.collectionMethod, s.day, s.start, s.end, s.waterEntityId, name, now);

                for (ParsedRow p : rows) {
                    if (!p.hasCapture) {
                        continue;
                    }
                    insertCatch(ctx, tripId, p.speciesId, s.techniqueId, p.longueur, p.weight, p.kept,
                            p.quantity == null ? 1 : p.quantity, p.sizeClass, p.description, now);
                }
            }
        }

        return new Persisted(jobId, insertedCount);
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
        return tripId;
    }

    // --- Inserts partagés import / saisie manuelle ---------------------------

    private UUID insertTrip(DSLContext ctx, String collectionMethod, LocalDate day, LocalTime start, LocalTime end,
                            UUID waterEntityId, String name, LocalDateTime now) {
        LocalDateTime beginTimestamp = LocalDateTime.of(day, start);
        LocalDateTime endTimestamp = LocalDateTime.of(day, end);
        if (endTimestamp.isBefore(beginTimestamp)) {
            endTimestamp = endTimestamp.plusDays(1);
        }
        return ctx.insertInto(TRIP,
                        TRIP.COLLECTION_METHOD, TRIP.BEGIN_TIMESTAMP, TRIP.END_TIMESTAMP,
                        TRIP.WATER_ENTITY_ID, TRIP.NAME, TRIP.TYPE, TRIP.MODE, TRIP.SOURCE,
                        TRIP.HIDDEN, TRIP.CREATED_ON)
                .values(CollectionMethod.valueOf(collectionMethod), beginTimestamp, endTimestamp,
                        waterEntityId, name, TripType.Border, TripMode.Afterwards, DeviceType.web,
                        false, now)
                .returning(TRIP.ID)
                .fetchOne()
                .getId();
    }

    private void insertCatch(DSLContext ctx, UUID tripId, UUID speciesId, UUID techniqueId, Integer size,
                             Integer weight, boolean kept, int quantity, String sizeClass, String description,
                             LocalDateTime now) {
        ctx.insertInto(CATCH,
                        CATCH.CREATED_ON, CATCH.TRIP_ID, CATCH.SPECIES_ID, CATCH.TECHNIQUE_ID,
                        CATCH.SIZE, CATCH.WEIGHT, CATCH.KEPT, CATCH.QUANTITY,
                        CATCH.SIZE_CLASS, CATCH.DESCRIPTION)
                .values(now, tripId, speciesId, techniqueId, size, weight, kept, quantity, sizeClass, description)
                .execute();
    }
}
