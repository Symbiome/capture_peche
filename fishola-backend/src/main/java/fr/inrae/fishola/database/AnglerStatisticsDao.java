package fr.inrae.fishola.database;

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

import fr.inrae.fishola.rest.dashboard.AnglerEffortStatistics.MonthlyEffort;
import fr.inrae.fishola.rest.dashboard.AnglerEffortStatistics.SectorContribution;
import fr.inrae.fishola.rest.dashboard.ImmutableMonthlyEffort;
import fr.inrae.fishola.rest.dashboard.ImmutableSectorContribution;
import jakarta.inject.Singleton;
import org.jooq.Record;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agrégats d'effort de pêche du tableau de bord personnel (#210), calculés en
 * SQL pour ne pas charger sorties et prises en mémoire (cf. #175). Mêmes
 * filtres que le tableau de bord : sorties non masquées du pêcheur, année de
 * début de sortie, milieu.
 */
@Singleton
public class AnglerStatisticsDao extends AbstractFisholaDao {

    private static final String HOURS = "GREATEST(EXTRACT(EPOCH FROM (t.end_timestamp - t.begin_timestamp)) / 3600.0, 0)";

    // Statistiques publiques (#87) : comptes exclus des exports ignorés, prise
    // PROBABLE / UNCERTAIN seulement une fois validée par un opérateur.
    private static final String PUBLIC_TRIP = "(t.owner_id IS NULL OR u.exclude_from_exports = false)";
    private static final String PUBLIC_CATCH = "c.exclude_from_exports = false "
            + "AND (c.certainty = 'CERTAIN' OR c.validated_at IS NOT NULL)";

    /** Filtre de période / milieu, rendu en SQL avec ses paramètres. */
    public record TripFilter(Optional<Integer> year, Optional<UUID> waterEntityId) {

        String sql() {
            String result = "t.hidden = false ";
            if (year.isPresent()) {
                result += "AND t.begin_timestamp BETWEEN ? AND ? ";
            }
            if (waterEntityId.isPresent()) {
                result += "AND t.water_entity_id = ? ";
            }
            return result;
        }

        List<Object> binds() {
            List<Object> result = new ArrayList<>();
            year.ifPresent(y -> {
                result.add(LocalDate.of(y, Month.JANUARY, 1).atStartOfDay());
                result.add(LocalDateTime.of(y, Month.DECEMBER, 31, 23, 59, 59));
            });
            waterEntityId.ifPresent(result::add);
            return result;
        }
    }

    private static Object[] binds(UUID userId, TripFilter filter) {
        List<Object> result = new ArrayList<>();
        result.add(userId);
        result.addAll(filter.binds());
        return result.toArray();
    }

    /** Nombre de sessions et heures de pêche par mois de début de sortie. */
    public Map<Month, MonthlyEffort> monthlyEffort(UUID userId, TripFilter filter) {
        String sql = "SELECT EXTRACT(MONTH FROM t.begin_timestamp)::int AS month, count(*) AS trips, "
                + "sum(" + HOURS + ") AS hours "
                + "FROM trip t WHERE t.owner_id = ? AND " + filter.sql()
                + "GROUP BY 1";
        Map<Month, MonthlyEffort> result = new EnumMap<>(Month.class);
        withContext(context -> context.fetch(sql, binds(userId, filter))).forEach(rec ->
                result.put(Month.of(rec.get("month", Integer.class)), ImmutableMonthlyEffort.builder()
                        .tripsCount(rec.get("trips", Integer.class))
                        .hours(rec.get("hours", Double.class))
                        .build()));
        return result;
    }

    /** Heures par technique, durée d'une sortie répartie entre ses techniques. */
    public Map<UUID, Double> hoursPerTechnique(UUID userId, TripFilter filter) {
        String sql = "WITH trips AS ( "
                + "  SELECT t.id, " + HOURS + " AS hours, "
                + "         (SELECT count(*) FROM trip_techniques x WHERE x.trip_id = t.id) AS techniques "
                + "  FROM trip t WHERE t.owner_id = ? AND " + filter.sql()
                + ") "
                + "SELECT tt.technique_id, sum(trips.hours / trips.techniques) AS hours "
                + "FROM trips JOIN trip_techniques tt ON tt.trip_id = trips.id "
                + "GROUP BY tt.technique_id";
        return withContext(context -> context.fetch(sql, binds(userId, filter))).stream()
                .collect(Collectors.toMap(
                        rec -> rec.get("technique_id", UUID.class),
                        rec -> rec.get("hours", Double.class)));
    }

    /** Nombre de poissons capturés par espèce (lots comptés à leur quantité). */
    public Map<UUID, Integer> catchesPerSpecies(UUID userId, TripFilter filter) {
        String sql = "SELECT c.species_id, sum(c.quantity) AS catches "
                + "FROM catch c JOIN trip t ON t.id = c.trip_id "
                + "WHERE c.species_id IS NOT NULL AND t.owner_id = ? AND " + filter.sql()
                + "GROUP BY c.species_id";
        return withContext(context -> context.fetch(sql, binds(userId, filter))).stream()
                .collect(Collectors.toMap(
                        rec -> rec.get("species_id", UUID.class),
                        rec -> rec.get("catches", Integer.class)));
    }

    /**
     * Part du pêcheur dans les sorties et prises du secteur (milieu du filtre),
     * selon les règles des statistiques publiques. Agrégats uniquement ; vide
     * sous {@code minAnglers} pêcheurs distincts, pour ne rien laisser déduire
     * de l'activité d'un autre pêcheur.
     */
    public Optional<SectorContribution> sectorContribution(UUID userId, TripFilter filter, int minAnglers) {
        if (filter.waterEntityId().isEmpty()) {
            return Optional.empty();
        }
        String from = "FROM trip t LEFT JOIN fishola_user u ON u.id = t.owner_id "
                + "WHERE " + PUBLIC_TRIP + " AND " + filter.sql();
        String tripsSql = "SELECT count(DISTINCT t.owner_id) AS anglers, count(*) AS total, "
                + "count(*) FILTER (WHERE t.owner_id = ?) AS mine " + from;
        String catchesSql = "SELECT coalesce(sum(c.quantity), 0) AS total, "
                + "coalesce(sum(c.quantity) FILTER (WHERE t.owner_id = ?), 0) AS mine "
                + from.replace("FROM trip t ", "FROM trip t JOIN catch c ON c.trip_id = t.id ")
                + "AND " + PUBLIC_CATCH;
        Record trips = withContext(context -> context.fetchOne(tripsSql, binds(userId, filter)));
        if (trips.get("anglers", Integer.class) < minAnglers) {
            return Optional.empty();
        }
        Record catches = withContext(context -> context.fetchOne(catchesSql, binds(userId, filter)));
        return Optional.of(ImmutableSectorContribution.builder()
                .anglersCount(trips.get("anglers", Integer.class))
                .myTripsCount(trips.get("mine", Integer.class))
                .totalTripsCount(trips.get("total", Integer.class))
                .myCatchesCount(catches.get("mine", Integer.class))
                .totalCatchesCount(catches.get("total", Integer.class))
                .build());
    }
}
