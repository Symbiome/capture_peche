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

import fr.inrae.fishola.database.AbstractFisholaDao;
import jakarta.inject.Singleton;
import org.jooq.Record;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Prises « preuves » des badges (#275) : pour chaque famille de règles, la ou les sorties
 * et captures du pêcheur qui justifient le déblocage, renvoyées en {@link UnlockProof}.
 * Pendant des requêtes d'agrégat de {@link GamificationDao} (qui comptent) : celles-ci
 * désignent. Toutes filtrent sur {@code trip.owner_id}. Les captures sont ordonnées par
 * leur heure de capture, à défaut par le début de leur sortie ({@link #CATCH_DATE}).
 */
@Singleton
public class BadgeProofDao extends AbstractFisholaDao {

    /** Métrique d'un record personnel : la colonne SQL vient d'ici, jamais de l'appelant. */
    public enum CatchMetric {
        SIZE("c.size"), WEIGHT("c.weight");

        private final String column;

        CatchMetric(String column) {
            this.column = column;
        }
    }

    private static final String CATCH_DATE = "coalesce(c.catch_timestamp, t.begin_timestamp)";
    private static final String BEST_CATCH_ORDER =
            "c.size DESC NULLS LAST, c.weight DESC NULLS LAST, " + CATCH_DATE + ", c.id";
    private static final String HAS_PICTURE = "EXISTS (SELECT 1 FROM catch_picture cp WHERE cp.catch_id = c.id)";
    private static final String OWNED_CATCH = "FROM catch c JOIN trip t ON t.id = c.trip_id WHERE t.owner_id = ?";
    private static final String SPECIES_IN = "c.species_id IN (SELECT id FROM species WHERE name = ANY(?))";
    private static final String TECHNIQUE_IS = "c.technique_id IN (SELECT id FROM technique WHERE name = ?)";
    private static final String SEASON = "CASE "
            + "WHEN extract(month FROM begin_timestamp) IN (12,1,2) THEN 'hiver' "
            + "WHEN extract(month FROM begin_timestamp) IN (3,4,5) THEN 'printemps' "
            + "WHEN extract(month FROM begin_timestamp) IN (6,7,8) THEN 'ete' "
            + "ELSE 'automne' END";

    // --- Records personnels ----------------------------------------------------------

    /** Capture record de l'espèce pour cette métrique ; la plus ancienne à égalité. */
    public Optional<UnlockProof> findRecordCatch(UUID userId, String speciesName, CatchMetric metric) {
        return firstCatch("SELECT c.id AS catch_id, c.trip_id " + OWNED_CATCH
                        + " AND " + metric.column + " IS NOT NULL"
                        + " AND c.species_id IN (SELECT id FROM species WHERE name = ?)"
                        + " ORDER BY " + metric.column + " DESC, " + CATCH_DATE + ", c.id LIMIT 1",
                userId, speciesName);
    }

    /** Sortie au plus grand nombre de captures (la plus ancienne à égalité) et sa plus belle prise. */
    public Optional<UnlockProof> findTripWithMostCatches(UUID userId) {
        return firstTripId("SELECT t.id AS trip_id " + OWNED_CATCH
                        + " GROUP BY t.id, t.begin_timestamp ORDER BY count(*) DESC, t.begin_timestamp, t.id LIMIT 1",
                userId)
                .map(tripId -> tripWithBestCatch(tripId, null));
    }

    // --- Fenêtres horaires -----------------------------------------------------------

    /** Première sortie dont le premier poisson a été pris dans les {@code windowMinutes} après le début. */
    public Optional<UnlockProof> findFirstCatchWithinStart(UUID userId, int windowMinutes) {
        return findFirstCatchOfTripWhere("catch_timestamp <= begin_timestamp + (?::text || ' minutes')::interval",
                userId, windowMinutes);
    }

    /** Première sortie dont le premier poisson a été pris dans les {@code windowMinutes} avant la fin. */
    public Optional<UnlockProof> findFirstCatchWithinEnd(UUID userId, int windowMinutes) {
        return findFirstCatchOfTripWhere("catch_timestamp >= end_timestamp - (?::text || ' minutes')::interval",
                userId, windowMinutes);
    }

    private Optional<UnlockProof> findFirstCatchOfTripWhere(String condition, UUID userId, int windowMinutes) {
        return firstCatch("SELECT catch_id, trip_id FROM ("
                        + "  SELECT DISTINCT ON (t.id) t.id AS trip_id, c.id AS catch_id, c.catch_timestamp, "
                        + "         t.begin_timestamp, t.end_timestamp "
                        + "  " + OWNED_CATCH + " AND c.catch_timestamp IS NOT NULL "
                        + "  ORDER BY t.id, c.catch_timestamp, c.id"
                        + ") first_catches WHERE " + condition + " ORDER BY begin_timestamp, trip_id LIMIT 1",
                userId, windowMinutes);
    }

    /** Première capture (chronologiquement) renseignée avant {@code hour} heures. */
    public Optional<UnlockProof> findFirstCatchBeforeHour(UUID userId, int hour) {
        return firstCatch("SELECT c.id AS catch_id, c.trip_id " + OWNED_CATCH
                        + " AND c.catch_timestamp IS NOT NULL AND c.catch_timestamp::time < (?::text || ':00')::time"
                        + " ORDER BY c.catch_timestamp, c.id LIMIT 1",
                userId, hour);
    }

    // --- Espèces ---------------------------------------------------------------------

    /** Première capture de l'une de ces espèces. */
    public Optional<UnlockProof> findFirstCatchOfSpecies(UUID userId, List<String> speciesNames) {
        return firstCatch("SELECT c.id AS catch_id, c.trip_id " + OWNED_CATCH + " AND " + SPECIES_IN
                        + " ORDER BY " + CATCH_DATE + ", c.id LIMIT 1",
                userId, speciesNames.toArray(new String[0]));
    }

    /**
     * Palier par espèce sur une année : la capture qui fait franchir {@code threshold}
     * (cumul de {@code quantity}, comme le compteur de la stratégie), puis la plus grande
     * capture de l'année.
     */
    public Optional<UnlockProof> findSpeciesTierCatches(UUID userId, List<String> speciesNames, int year,
                                                         int threshold) {
        String[] names = speciesNames.toArray(new String[0]);
        String yearFilter = " AND " + SPECIES_IN + " AND extract(year FROM t.begin_timestamp) = ?";
        Optional<UnlockProof> crossing = firstCatch("SELECT catch_id, trip_id FROM ("
                        + "  SELECT c.id AS catch_id, c.trip_id, " + CATCH_DATE + " AS caught_at, "
                        + "         sum(c.quantity) OVER (ORDER BY " + CATCH_DATE + ", c.id "
                        + "             ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_count "
                        + "  " + OWNED_CATCH + yearFilter
                        + ") x WHERE running_count >= ? ORDER BY caught_at, catch_id LIMIT 1",
                userId, names, year, threshold);
        Optional<UnlockProof> biggest = firstCatch("SELECT c.id AS catch_id, c.trip_id " + OWNED_CATCH + yearFilter
                        + " ORDER BY " + BEST_CATCH_ORDER + " LIMIT 1",
                userId, names, year);
        return crossing.map(c -> biggest.map(c::plus).orElse(c));
    }

    /**
     * Mosaïque « Collectionneur » : une capture par espèce, pour les {@code limit}
     * premières espèces découvertes ; par espèce, une capture avec photo puis la plus grande.
     */
    public UnlockProof findOneCatchPerSpecies(UUID userId, int limit) {
        return catches("SELECT catch_id, trip_id FROM ("
                        + "  SELECT DISTINCT ON (c.species_id) c.id AS catch_id, c.trip_id, "
                        + "         min(" + CATCH_DATE + ") OVER (PARTITION BY c.species_id) AS discovered_at "
                        + "  " + OWNED_CATCH + " AND c.species_id IS NOT NULL "
                        + "  ORDER BY c.species_id, " + HAS_PICTURE + " DESC, " + BEST_CATCH_ORDER
                        + ") per_species ORDER BY discovered_at, catch_id LIMIT ?",
                userId, Math.min(limit, UnlockProof.MAX_IDS));
    }

    /** Mosaïque « Photographe » : les {@code limit} premières captures avec photo. */
    public UnlockProof findCatchesWithPhoto(UUID userId, int limit) {
        return catches("SELECT c.id AS catch_id, c.trip_id " + OWNED_CATCH + " AND " + HAS_PICTURE
                        + " ORDER BY " + CATCH_DATE + ", c.id LIMIT ?",
                userId, Math.min(limit, UnlockProof.MAX_IDS));
    }

    // --- Techniques ------------------------------------------------------------------

    /** N-ième sortie avec au moins une capture à cette technique, et sa plus belle prise à cette technique. */
    public Optional<UnlockProof> findNthTripWithTechnique(UUID userId, String techniqueName, int n) {
        return firstTripId("SELECT t.id AS trip_id FROM trip t WHERE t.owner_id = ? "
                        + "AND EXISTS (SELECT 1 FROM catch c WHERE c.trip_id = t.id AND " + TECHNIQUE_IS + ") "
                        + "ORDER BY t.begin_timestamp, t.id OFFSET ? LIMIT 1",
                userId, techniqueName, offsetOf(n))
                .map(tripId -> tripWithBestCatch(tripId, techniqueName));
    }

    /**
     * « Polyvalent » : la sortie où la dernière technique a atteint {@code n} sorties
     * (la plus tardive des N-ièmes sorties de chaque technique), et sa plus belle prise.
     */
    public Optional<UnlockProof> findVersatilityCrossingTrip(UUID userId, int n) {
        return firstTripId("SELECT trip_id FROM ("
                        + "  SELECT d.trip_id, d.begin_timestamp, "
                        + "         row_number() OVER (PARTITION BY d.technique_id ORDER BY d.begin_timestamp, d.trip_id) AS rn "
                        + "  FROM (SELECT DISTINCT c.technique_id, t.id AS trip_id, t.begin_timestamp "
                        + "        " + OWNED_CATCH + " AND c.technique_id IS NOT NULL) d"
                        + ") x WHERE rn = ? ORDER BY begin_timestamp DESC, trip_id DESC LIMIT 1",
                userId, n)
                .map(tripId -> tripWithBestCatch(tripId, null));
    }

    // --- Fidélité ----------------------------------------------------------------------

    /** N-ième sortie du pêcheur. */
    public Optional<UnlockProof> findNthTrip(UUID userId, int n) {
        return firstTrip("SELECT id AS trip_id FROM trip WHERE owner_id = ? ORDER BY begin_timestamp, id OFFSET ? LIMIT 1",
                userId, offsetOf(n));
    }

    /** N-ième sortie sans capture (« bredouille »). */
    public Optional<UnlockProof> findNthBredouilleTrip(UUID userId, int n) {
        return firstTrip("SELECT t.id AS trip_id FROM trip t WHERE t.owner_id = ? "
                        + "AND NOT EXISTS (SELECT 1 FROM catch c WHERE c.trip_id = t.id) "
                        + "ORDER BY t.begin_timestamp, t.id OFFSET ? LIMIT 1",
                userId, offsetOf(n));
    }

    /** Première sortie de la semaine (lundi ISO {@code weekStart}). */
    public Optional<UnlockProof> findFirstTripOfWeek(UUID userId, LocalDate weekStart) {
        return firstTrip("SELECT id AS trip_id FROM trip WHERE owner_id = ? "
                        + "AND date_trunc('week', begin_timestamp)::date = ? ORDER BY begin_timestamp, id LIMIT 1",
                userId, weekStart);
    }

    /** Première sortie de chaque saison (hiver/printemps/été/automne), par ordre chronologique. */
    public UnlockProof findFirstTripPerSeason(UUID userId) {
        return trips("SELECT trip_id FROM ("
                        + "  SELECT DISTINCT ON (season) trip_id, begin_timestamp FROM ("
                        + "    SELECT id AS trip_id, begin_timestamp, " + SEASON + " AS season FROM trip WHERE owner_id = ?"
                        + "  ) s ORDER BY season, begin_timestamp, trip_id"
                        + ") x ORDER BY begin_timestamp, trip_id",
                userId);
    }

    /** Première sortie tombant l'un de ces jours (dates d'ouverture). */
    public Optional<UnlockProof> findFirstTripOnDates(UUID userId, List<LocalDate> dates) {
        return firstTrip("SELECT id AS trip_id FROM trip WHERE owner_id = ? AND begin_timestamp::date = ANY(?) "
                        + "ORDER BY begin_timestamp, id LIMIT 1",
                userId, dates.toArray(new LocalDate[0]));
    }

    // --- Exploration -------------------------------------------------------------------

    /** Première sortie dans chaque département, par ordre chronologique (département -> sortie). */
    public Map<String, UUID> findFirstTripPerDepartment(UUID userId) {
        Map<String, UUID> result = new LinkedHashMap<>();
        withContext(ctx -> ctx.fetch("SELECT department, trip_id FROM ("
                        + "  SELECT DISTINCT ON (department) department, id AS trip_id, begin_timestamp FROM trip "
                        + "  WHERE owner_id = ? AND department IS NOT NULL ORDER BY department, begin_timestamp, id"
                        + ") x ORDER BY begin_timestamp, trip_id", userId))
                .forEach(r -> result.put(r.get("department", String.class), r.get("trip_id", UUID.class)));
        return result;
    }

    /** Première sortie sur un plan d'eau d'altitude moyenne supérieure à {@code minAltitude}. */
    public Optional<UnlockProof> findFirstTripAboveAltitude(UUID userId, double minAltitude) {
        return firstTrip("SELECT t.id AS trip_id FROM trip t JOIN water_entity w ON w.id = t.water_entity_id "
                        + "WHERE t.owner_id = ? AND w.altitude_moyenne > ? ORDER BY t.begin_timestamp, t.id LIMIT 1",
                userId, minAltitude);
    }

    /** Carte « Navigateur » : première sortie en bateau sur chacun des cours d'eau nommés. */
    public UnlockProof findFirstBoatTripPerWaterEntity(UUID userId, List<String> waterEntityNames) {
        return firstTripPerWaterEntity("t.type = 'Craft' AND w.name = ANY(?)", userId,
                waterEntityNames.toArray(new String[0]));
    }

    /** Carte « Tout-terrain » : première sortie sur chaque plan d'eau et cours d'eau. */
    public UnlockProof findFirstTripPerStillOrFlowingWaterEntity(UUID userId) {
        return firstTripPerWaterEntity("w.kind IN ('STILL', 'FLOWING')", userId);
    }

    /** « Lacustre » : la sortie qui a fait atteindre {@code n} sorties sur un même milieu de ce type (la plus ancienne). */
    public Optional<UnlockProof> findNthTripOnSingleWaterEntity(UUID userId, String kind, int n) {
        return firstTrip("SELECT trip_id FROM ("
                        + "  SELECT t.id AS trip_id, t.begin_timestamp, "
                        + "         row_number() OVER (PARTITION BY t.water_entity_id ORDER BY t.begin_timestamp, t.id) AS rn "
                        + "  FROM trip t JOIN water_entity w ON w.id = t.water_entity_id "
                        + "  WHERE t.owner_id = ? AND w.kind = ?::water_entity_kind"
                        + ") x WHERE rn = ? ORDER BY begin_timestamp, trip_id LIMIT 1",
                userId, kind, n);
    }

    private UnlockProof firstTripPerWaterEntity(String tripFilter, UUID userId, Object... filterBindings) {
        Object[] bindings = new Object[filterBindings.length + 1];
        bindings[0] = userId;
        System.arraycopy(filterBindings, 0, bindings, 1, filterBindings.length);
        return trips("SELECT trip_id FROM ("
                        + "  SELECT DISTINCT ON (t.water_entity_id) t.id AS trip_id, t.begin_timestamp "
                        + "  FROM trip t JOIN water_entity w ON w.id = t.water_entity_id "
                        + "  WHERE t.owner_id = ? AND " + tripFilter
                        + "  ORDER BY t.water_entity_id, t.begin_timestamp, t.id"
                        + ") x ORDER BY begin_timestamp, trip_id LIMIT " + UnlockProof.MAX_IDS,
                bindings);
    }

    // --- Outils --------------------------------------------------------------------------

    /** La sortie et sa plus belle capture (plus grande, puis plus lourde), éventuellement à une technique donnée. */
    private UnlockProof tripWithBestCatch(UUID tripId, String techniqueName) {
        String techniqueFilter = techniqueName == null ? "" : " AND " + TECHNIQUE_IS;
        Object[] bindings = techniqueName == null ? new Object[]{tripId} : new Object[]{tripId, techniqueName};
        List<UUID> bestCatch = withContext(ctx -> ctx.fetch("SELECT c.id AS catch_id FROM catch c "
                        + "JOIN trip t ON t.id = c.trip_id WHERE c.trip_id = ?" + techniqueFilter
                        + " ORDER BY " + BEST_CATCH_ORDER + " LIMIT 1", bindings))
                .map(r -> r.get("catch_id", UUID.class));
        return new UnlockProof(List.of(tripId), bestCatch);
    }

    /** {@code OFFSET} de la N-ième ligne ; un seuil nul ou négatif désigne la première. */
    private static int offsetOf(int n) {
        return Math.max(n - 1, 0);
    }

    private Optional<UnlockProof> firstCatch(String sql, Object... bindings) {
        UnlockProof proof = catches(sql, bindings);
        return proof.catchIds().isEmpty() ? Optional.empty() : Optional.of(proof);
    }

    private Optional<UnlockProof> firstTrip(String sql, Object... bindings) {
        return firstTripId(sql, bindings).map(tripId -> UnlockProof.ofTrips(List.of(tripId)));
    }

    private Optional<UUID> firstTripId(String sql, Object... bindings) {
        return withContext(ctx -> ctx.fetch(sql, bindings)).stream()
                .map(r -> r.get("trip_id", UUID.class)).findFirst();
    }

    private UnlockProof catches(String sql, Object... bindings) {
        List<Record> rows = withContext(ctx -> ctx.fetch(sql, bindings));
        List<UUID> catchIds = rows.stream().map(r -> r.get("catch_id", UUID.class)).toList();
        List<UUID> tripIds = rows.stream().map(r -> r.get("trip_id", UUID.class)).distinct().toList();
        return new UnlockProof(tripIds, catchIds);
    }

    private UnlockProof trips(String sql, Object... bindings) {
        return UnlockProof.ofTrips(withContext(ctx -> ctx.fetch(sql, bindings))
                .map(r -> r.get("trip_id", UUID.class)));
    }
}
