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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inrae.fishola.database.AbstractFisholaDao;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Accès base du socle de gamification (#7, construit à l'occasion de #146) : catalogue
 * des badges, déblocages, et petites requêtes d'agrégat partagées par les stratégies de
 * {@link fr.inrae.fishola.gamification.rules}. {@code rule_params}/{@code context} (jsonb)
 * sont lus/écrits en texte brut plutôt que via le type jOOQ {@code JSONB}, comme
 * {@code AuditLogDao} le fait déjà pour {@code audit_log.details}.
 */
@Singleton
public class GamificationDao extends AbstractFisholaDao {

    @Inject
    ObjectMapper objectMapper;

    public record BadgeRow(UUID id, String code, String category, String name, String description,
                           String icon, String ruleType, JsonNode ruleParams, Integer tier,
                           boolean annualReset, boolean active) {}

    public record UnlockRow(UUID badgeId, short periodYear, java.time.LocalDateTime unlockedAt,
                            JsonNode context, UUID attributedBy) {}

    private static final String BADGE_COLUMNS = "id, code, category::text AS category, name, description, "
            + "icon, rule_type::text AS rule_type, rule_params::text AS rule_params, tier, annual_reset, active";

    private BadgeRow toBadgeRow(Record r) {
        return new BadgeRow(
                r.get("id", UUID.class), r.get("code", String.class), r.get("category", String.class),
                r.get("name", String.class), r.get("description", String.class), r.get("icon", String.class),
                r.get("rule_type", String.class), parseJson(r.get("rule_params", String.class)),
                r.get("tier", Integer.class), Boolean.TRUE.equals(r.get("annual_reset", Boolean.class)),
                Boolean.TRUE.equals(r.get("active", Boolean.class)));
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json == null ? "{}" : json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            return "{}";
        }
    }

    // --- Catalogue -------------------------------------------------------------

    public List<BadgeRow> listActiveBadges() {
        return withContext(ctx -> ctx.fetch("SELECT " + BADGE_COLUMNS + " FROM gamification_badge WHERE active = true")
                .map(this::toBadgeRow));
    }

    /** Toutes les lignes, actives ou non -- utilisé par l'admin (sélecteur d'attribution manuelle). */
    public List<BadgeRow> listAllBadges() {
        return withContext(ctx -> ctx
                .fetch("SELECT " + BADGE_COLUMNS + " FROM gamification_badge ORDER BY category, name")
                .map(this::toBadgeRow));
    }

    public Optional<BadgeRow> findBadge(UUID badgeId) {
        return withContext(ctx -> ctx.fetch("SELECT " + BADGE_COLUMNS + " FROM gamification_badge WHERE id = ?", badgeId)
                .map(this::toBadgeRow)).stream().findFirst();
    }

    // --- Déblocages --------------------------------------------------------------

    public List<UnlockRow> listUnlocksForUser(UUID userId) {
        return withContext(ctx -> ctx.fetch(
                "SELECT badge_id, period_year, unlocked_at, context::text AS context, attributed_by "
                        + "FROM gamification_badge_unlock WHERE fishola_user_id = ?", userId)
                .map(r -> new UnlockRow(r.get("badge_id", UUID.class), r.get("period_year", Short.class),
                        r.get("unlocked_at", java.time.LocalDateTime.class),
                        parseJson(r.get("context", String.class)), r.get("attributed_by", UUID.class))));
    }

    public Optional<UnlockRow> findUnlock(UUID badgeId, UUID userId, short periodYear) {
        return withContext(ctx -> ctx.fetch(
                        "SELECT badge_id, period_year, unlocked_at, context::text AS context, attributed_by "
                                + "FROM gamification_badge_unlock "
                                + "WHERE badge_id = ? AND fishola_user_id = ? AND period_year = ?",
                        badgeId, userId, periodYear)
                .map(r -> new UnlockRow(r.get("badge_id", UUID.class), r.get("period_year", Short.class),
                        r.get("unlocked_at", java.time.LocalDateTime.class),
                        parseJson(r.get("context", String.class)), r.get("attributed_by", UUID.class))))
                .stream().findFirst();
    }

    public Set<UUID> unlockedBadgeIds(UUID userId, short periodYear) {
        return withContext(ctx -> ctx.fetch(
                        "SELECT badge_id FROM gamification_badge_unlock WHERE fishola_user_id = ? AND period_year = ?",
                        userId, periodYear))
                .stream().map(r -> r.get("badge_id", UUID.class)).collect(Collectors.toSet());
    }

    /**
     * Insère (ou met à jour si {@code updateIfExists}, cas des badges "record" qui
     * évoluent) le déblocage d'un badge. Idempotent via la contrainte unique
     * {@code (badge_id, fishola_user_id, period_year)}.
     */
    @Transactional
    public void upsertUnlock(UUID badgeId, UUID userId, short periodYear, Map<String, Object> context,
                             boolean updateIfExists, UUID attributedBy) {
        DSLContext ctx = newContext();
        String contextJson = toJson(context);
        String sql = "INSERT INTO gamification_badge_unlock (badge_id, fishola_user_id, period_year, context, attributed_by) "
                + "VALUES (?, ?, ?, ?::jsonb, ?) ON CONFLICT (badge_id, fishola_user_id, period_year) "
                + (updateIfExists ? "DO UPDATE SET context = EXCLUDED.context, unlocked_at = now()" : "DO NOTHING");
        ctx.execute(sql, badgeId, userId, periodYear, contextJson, attributedBy);
    }

    public boolean existsUser(UUID userId) {
        return withContext(ctx -> ctx.fetchExists(ctx.selectOne().from(fr.inrae.fishola.entities.Tables.FISHOLA_USER)
                .where(fr.inrae.fishola.entities.Tables.FISHOLA_USER.ID.eq(userId))));
    }

    // --- Requêtes d'agrégat partagées par les stratégies (fr.inrae.fishola.gamification.rules) --

    private static final String OWNED_TRIP_JOIN_CATCH =
            "FROM catch c JOIN trip t ON t.id = c.trip_id WHERE t.owner_id = ?";

    public Optional<Integer> maxSize(UUID userId, List<String> speciesNames) {
        Integer max = withContext(ctx -> ctx.fetchOne(
                        "SELECT max(c.size) AS m " + OWNED_TRIP_JOIN_CATCH
                                + " AND c.size IS NOT NULL AND c.species_id IN "
                                + "(SELECT id FROM species WHERE name = ANY(?))",
                        userId, speciesNames.toArray(new String[0])))
                .get("m", Integer.class);
        return Optional.ofNullable(max);
    }

    public Optional<Integer> maxWeight(UUID userId, List<String> speciesNames) {
        Integer max = withContext(ctx -> ctx.fetchOne(
                        "SELECT max(c.weight) AS m " + OWNED_TRIP_JOIN_CATCH
                                + " AND c.weight IS NOT NULL AND c.species_id IN "
                                + "(SELECT id FROM species WHERE name = ANY(?))",
                        userId, speciesNames.toArray(new String[0])))
                .get("m", Integer.class);
        return Optional.ofNullable(max);
    }

    public Map<String, Integer> maxSizePerSpecies(UUID userId) {
        return withContext(ctx -> ctx.fetch(
                        "SELECT s.name AS species_name, max(c.size) AS m FROM catch c "
                                + "JOIN trip t ON t.id = c.trip_id JOIN species s ON s.id = c.species_id "
                                + "WHERE t.owner_id = ? AND c.size IS NOT NULL GROUP BY s.name", userId))
                .stream().collect(Collectors.toMap(r -> r.get("species_name", String.class), r -> r.get("m", Integer.class)));
    }

    public Map<String, Integer> maxWeightPerSpecies(UUID userId) {
        return withContext(ctx -> ctx.fetch(
                        "SELECT s.name AS species_name, max(c.weight) AS m FROM catch c "
                                + "JOIN trip t ON t.id = c.trip_id JOIN species s ON s.id = c.species_id "
                                + "WHERE t.owner_id = ? AND c.weight IS NOT NULL GROUP BY s.name", userId))
                .stream().collect(Collectors.toMap(r -> r.get("species_name", String.class), r -> r.get("m", Integer.class)));
    }

    /** Vrai si le premier poisson d'au moins une sortie a été capturé dans les {@code windowMinutes} suivant le début. */
    public boolean hasFirstCatchWithinStart(UUID userId, int windowMinutes) {
        return withContext(ctx -> ctx.fetch(
                "SELECT 1 FROM ("
                        + "  SELECT t.id, min(c.catch_timestamp) AS first_catch, t.begin_timestamp "
                        + "  FROM catch c JOIN trip t ON t.id = c.trip_id "
                        + "  WHERE t.owner_id = ? AND c.catch_timestamp IS NOT NULL "
                        + "  GROUP BY t.id, t.begin_timestamp"
                        + ") x WHERE first_catch <= begin_timestamp + (?::text || ' minutes')::interval",
                userId, windowMinutes).isNotEmpty());
    }

    /** Vrai si le premier poisson d'au moins une sortie a été capturé dans les {@code windowMinutes} avant la fin. */
    public boolean hasFirstCatchWithinEnd(UUID userId, int windowMinutes) {
        return withContext(ctx -> ctx.fetch(
                "SELECT 1 FROM ("
                        + "  SELECT t.id, min(c.catch_timestamp) AS first_catch, t.end_timestamp "
                        + "  FROM catch c JOIN trip t ON t.id = c.trip_id "
                        + "  WHERE t.owner_id = ? AND c.catch_timestamp IS NOT NULL "
                        + "  GROUP BY t.id, t.end_timestamp"
                        + ") x WHERE first_catch >= end_timestamp - (?::text || ' minutes')::interval",
                userId, windowMinutes).isNotEmpty());
    }

    public boolean hasCatchBeforeHour(UUID userId, int hour) {
        return withContext(ctx -> ctx.fetch(
                "SELECT 1 " + OWNED_TRIP_JOIN_CATCH + " AND c.catch_timestamp IS NOT NULL "
                        + "AND c.catch_timestamp::time < (?::text || ':00')::time",
                userId, hour).isNotEmpty());
    }

    public Optional<Integer> maxCatchCountInSingleTrip(UUID userId) {
        Integer max = withContext(ctx -> ctx.fetchOne(
                        "SELECT max(cnt) AS m FROM (SELECT count(*) AS cnt " + OWNED_TRIP_JOIN_CATCH
                                + " GROUP BY t.id) x", userId))
                .get("m", Integer.class);
        return Optional.ofNullable(max);
    }

    /** Nombre de sorties distinctes où au moins une capture utilise cette technique. */
    public int countDistinctTripsWithTechnique(UUID userId, String techniqueName) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(DISTINCT c.trip_id) AS n " + OWNED_TRIP_JOIN_CATCH
                                + " AND c.technique_id = (SELECT id FROM technique WHERE name = ?)",
                        userId, techniqueName))
                .get("n", Integer.class);
    }

    /** Vrai si, pour CHAQUE technique du référentiel, le pêcheur a >= threshold sorties. */
    public boolean hasVersatility(UUID userId, int threshold) {
        Record r = withContext(ctx -> ctx.fetchOne(
                "SELECT count(*) AS total, "
                        + "count(*) FILTER (WHERE coalesce(sub.trip_count, 0) >= ?) AS satisfied "
                        + "FROM technique tech LEFT JOIN ("
                        + "  SELECT c.technique_id, count(DISTINCT c.trip_id) AS trip_count "
                        + "  FROM catch c JOIN trip t ON t.id = c.trip_id WHERE t.owner_id = ? "
                        + "  GROUP BY c.technique_id"
                        + ") sub ON sub.technique_id = tech.id",
                threshold, userId));
        int total = r.get("total", Integer.class);
        int satisfied = r.get("satisfied", Integer.class);
        return total > 0 && total == satisfied;
    }

    public int countDistinctSpecies(UUID userId) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(DISTINCT c.species_id) AS n " + OWNED_TRIP_JOIN_CATCH, userId))
                .get("n", Integer.class);
    }

    public int countCatchesForSpeciesInYear(UUID userId, List<String> speciesNames, int year) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT coalesce(sum(c.quantity), 0) AS n " + OWNED_TRIP_JOIN_CATCH
                                + " AND c.species_id IN (SELECT id FROM species WHERE name = ANY(?))"
                                + " AND extract(year FROM t.begin_timestamp) = ?",
                        userId, speciesNames.toArray(new String[0]), year))
                .get("n", Integer.class);
    }

    public boolean hasCatchOfSpecies(UUID userId, List<String> speciesNames) {
        return withContext(ctx -> ctx.fetch(
                "SELECT 1 " + OWNED_TRIP_JOIN_CATCH + " AND c.species_id IN (SELECT id FROM species WHERE name = ANY(?))",
                userId, speciesNames.toArray(new String[0])).isNotEmpty());
    }

    public int countCatchesWithPhoto(UUID userId) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(DISTINCT c.id) AS n " + OWNED_TRIP_JOIN_CATCH
                                + " AND EXISTS (SELECT 1 FROM catch_picture cp WHERE cp.catch_id = c.id)",
                        userId))
                .get("n", Integer.class);
    }

    /** Une date (lundi ISO) par semaine calendaire où le pêcheur a eu au moins une sortie. */
    public List<LocalDate> distinctWeekStarts(UUID userId) {
        return withContext(ctx -> ctx.fetch(
                        "SELECT DISTINCT date_trunc('week', begin_timestamp)::date AS week_start "
                                + "FROM trip WHERE owner_id = ? ORDER BY week_start", userId))
                .stream().map(r -> r.get("week_start", LocalDate.class)).toList();
    }

    public int countTotalTrips(UUID userId) {
        return withContext(ctx -> ctx.fetchOne("SELECT count(*) AS n FROM trip WHERE owner_id = ?", userId))
                .get("n", Integer.class);
    }

    /** Nombre de saisons distinctes (hiver/printemps/été/automne, dérivées du mois) parmi les sorties. */
    public int countDistinctSeasons(UUID userId) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(DISTINCT CASE "
                                + "  WHEN extract(month FROM begin_timestamp) IN (12,1,2) THEN 'hiver' "
                                + "  WHEN extract(month FROM begin_timestamp) IN (3,4,5) THEN 'printemps' "
                                + "  WHEN extract(month FROM begin_timestamp) IN (6,7,8) THEN 'ete' "
                                + "  ELSE 'automne' END) AS n "
                                + "FROM trip WHERE owner_id = ?", userId))
                .get("n", Integer.class);
    }

    public boolean hasTripOnDate(UUID userId, List<LocalDate> dates) {
        return withContext(ctx -> ctx.fetch(
                "SELECT 1 FROM trip WHERE owner_id = ? AND begin_timestamp::date = ANY(?)",
                userId, dates.toArray(new LocalDate[0])).isNotEmpty());
    }

    /** Sorties sans aucune capture ("bredouille"). */
    public int countBredouilleTrips(UUID userId) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(*) AS n FROM trip t "
                                + "WHERE t.owner_id = ? AND NOT EXISTS (SELECT 1 FROM catch c WHERE c.trip_id = t.id)",
                        userId))
                .get("n", Integer.class);
    }

    /**
     * Rang centile (0-1, plus haut = plus de sorties) du pêcheur parmi tous les pêcheurs
     * ayant au moins une sortie, avec garde de cohorte minimale (RGPD -- pas de percentile
     * exposé sur une cohorte trop petite pour rester anonyme).
     */
    public Optional<Double> contributorPercentile(UUID userId, int minCohortSize) {
        Record cohort = withContext(ctx -> ctx.fetchOne(
                "SELECT count(*) AS n FROM (SELECT owner_id FROM trip WHERE owner_id IS NOT NULL "
                        + "GROUP BY owner_id) x"));
        if (cohort.get("n", Integer.class) < minCohortSize) {
            return Optional.empty();
        }
        Record r = withContext(ctx -> ctx.fetchOne(
                "SELECT percent_rank FROM ("
                        + "  SELECT owner_id, percent_rank() OVER (ORDER BY session_count) AS percent_rank "
                        + "  FROM (SELECT owner_id, count(*) AS session_count FROM trip "
                        + "        WHERE owner_id IS NOT NULL GROUP BY owner_id) counts"
                        + ") ranked WHERE owner_id = ?", userId));
        return r == null ? Optional.empty() : Optional.ofNullable(r.get("percent_rank", Double.class));
    }

    public int countDistinctDepartments(UUID userId) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(DISTINCT department) AS n FROM trip WHERE owner_id = ? AND department IS NOT NULL",
                        userId))
                .get("n", Integer.class);
    }

    public Set<String> distinctDepartments(UUID userId) {
        return withContext(ctx -> ctx.fetch(
                        "SELECT DISTINCT department FROM trip WHERE owner_id = ? AND department IS NOT NULL", userId))
                .stream().map(r -> r.get("department", String.class)).collect(Collectors.toSet());
    }

    public boolean hasTripAboveAltitude(UUID userId, double minAltitude) {
        return withContext(ctx -> ctx.fetch(
                "SELECT 1 FROM trip t JOIN water_entity w ON w.id = t.water_entity_id "
                        + "WHERE t.owner_id = ? AND w.altitude_moyenne > ?", userId, minAltitude).isNotEmpty());
    }

    /** Sorties en bateau (trip.type = 'Craft') sur l'un des cours d'eau nommés. */
    public int countBoatTripsOnWaterEntities(UUID userId, List<String> waterEntityNames) {
        if (waterEntityNames.isEmpty()) {
            return 0;
        }
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(*) AS n FROM trip t JOIN water_entity w ON w.id = t.water_entity_id "
                                + "WHERE t.owner_id = ? AND t.type = 'Craft' AND w.name = ANY(?)",
                        userId, waterEntityNames.toArray(new String[0])))
                .get("n", Integer.class);
    }

    /** Nombre de sorties sur des plans d'eau (STILL) / cours d'eau (FLOWING). */
    public int countTripsByWaterEntityKind(UUID userId, String kind) {
        return withContext(ctx -> ctx.fetchOne(
                        "SELECT count(*) AS n FROM trip t JOIN water_entity w ON w.id = t.water_entity_id "
                                + "WHERE t.owner_id = ? AND w.kind = ?::water_entity_kind", userId, kind))
                .get("n", Integer.class);
    }

    /** Plus grand nombre de sorties du pêcheur concentrées sur un même plan d'eau/cours d'eau. */
    public int maxTripsOnSingleWaterEntity(UUID userId, String kind) {
        Integer max = withContext(ctx -> ctx.fetchOne(
                        "SELECT max(cnt) AS m FROM ("
                                + "  SELECT count(*) AS cnt FROM trip t JOIN water_entity w ON w.id = t.water_entity_id "
                                + "  WHERE t.owner_id = ? AND w.kind = ?::water_entity_kind GROUP BY t.water_entity_id"
                                + ") x", userId, kind))
                .get("m", Integer.class);
        return max == null ? 0 : max;
    }
}
