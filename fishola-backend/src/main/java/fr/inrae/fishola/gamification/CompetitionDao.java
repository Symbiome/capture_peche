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
import jakarta.transaction.Transactional;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Accès base des concours (#90) : création par un opérateur, attribution manuelle du badge
 * CONCOURS (cf. {@link GamificationDao#attributeCompetitionBadge}, réutilisé ici) à des
 * pêcheurs sélectionnés. Le badge catalogue lui-même (unique, code {@code CONCOURS}) et le
 * déblocage restent portés par {@link GamificationDao}/{@code gamification_badge_unlock} ;
 * cette classe ne gère que le référentiel {@code competition} et la recherche de pêcheurs.
 */
@Singleton
public class CompetitionDao extends AbstractFisholaDao {

    public record CompetitionRow(UUID id, String name, LocalDate competitionDate, UUID waterEntityId,
                                 String waterEntityName, String department, String federationName,
                                 UUID createdBy, LocalDateTime createdOn) {}

    public record ParticipantRow(UUID userId, String pseudo, String firstName, String lastName,
                                 String email, LocalDateTime unlockedAt) {}

    public record UserSearchRow(UUID id, String pseudo, String firstName, String lastName, String email) {}

    private static final String COMPETITION_COLUMNS = "co.id, co.name, co.competition_date, co.water_entity_id, "
            + "w.name AS water_entity_name, w.department, co.federation_name, co.created_by, co.created_on";

    private static final String COMPETITION_FROM_JOIN =
            "FROM competition co JOIN water_entity w ON w.id = co.water_entity_id";

    private CompetitionRow toCompetitionRow(Record r) {
        return new CompetitionRow(r.get("id", UUID.class), r.get("name", String.class),
                r.get("competition_date", LocalDate.class), r.get("water_entity_id", UUID.class),
                r.get("water_entity_name", String.class), r.get("department", String.class),
                r.get("federation_name", String.class), r.get("created_by", UUID.class),
                r.get("created_on", LocalDateTime.class));
    }

    /** Périmètre national (allowedDepartments vide) ou filtré par département (#159, comme les autres écrans admin). */
    public List<CompetitionRow> listCompetitions(Set<String> allowedDepartments) {
        if (allowedDepartments.isEmpty()) {
            return withContext(ctx -> ctx
                    .fetch("SELECT " + COMPETITION_COLUMNS + " " + COMPETITION_FROM_JOIN + " ORDER BY co.competition_date DESC")
                    .map(this::toCompetitionRow));
        }
        return withContext(ctx -> ctx
                .fetch("SELECT " + COMPETITION_COLUMNS + " " + COMPETITION_FROM_JOIN
                                + " WHERE w.department = ANY(?) ORDER BY co.competition_date DESC",
                        (Object) allowedDepartments.toArray(new String[0]))
                .map(this::toCompetitionRow));
    }

    public Optional<CompetitionRow> findCompetition(UUID id) {
        return withContext(ctx -> ctx
                        .fetch("SELECT " + COMPETITION_COLUMNS + " " + COMPETITION_FROM_JOIN + " WHERE co.id = ?", id)
                        .map(this::toCompetitionRow))
                .stream().findFirst();
    }

    @Transactional
    public UUID createCompetition(String name, LocalDate competitionDate, UUID waterEntityId,
                                   String federationName, UUID createdBy) {
        DSLContext ctx = newContext();
        return ctx.fetchOne(
                "INSERT INTO competition (name, competition_date, water_entity_id, federation_name, created_by) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id",
                name, competitionDate, waterEntityId, federationName, createdBy
        ).get("id", UUID.class);
    }

    public List<ParticipantRow> listParticipants(UUID competitionId) {
        return withContext(ctx -> ctx.fetch(
                        "SELECT u.fishola_user_id, fu.pseudo, fu.first_name, fu.last_name, fu.email, u.unlocked_at "
                                + "FROM gamification_badge_unlock u JOIN fishola_user fu ON fu.id = u.fishola_user_id "
                                + "WHERE u.competition_id = ? ORDER BY u.unlocked_at DESC",
                        competitionId)
                .map(r -> new ParticipantRow(r.get("fishola_user_id", UUID.class), r.get("pseudo", String.class),
                        r.get("first_name", String.class), r.get("last_name", String.class),
                        r.get("email", String.class), r.get("unlocked_at", LocalDateTime.class))));
    }

    /** Recherche libre (pseudo/nom/prénom/email) pour le sélecteur d'attribution (#90), bornée pour rester légère. */
    public List<UserSearchRow> searchUsers(String query, int limit) {
        String pattern = "%" + query.toLowerCase() + "%";
        return withContext(ctx -> ctx.fetch(
                        "SELECT id, pseudo, first_name, last_name, email FROM fishola_user "
                                + "WHERE lower(pseudo) LIKE ? OR lower(email) LIKE ? "
                                + "OR lower(first_name) LIKE ? OR lower(last_name) LIKE ? "
                                + "ORDER BY pseudo LIMIT ?",
                        pattern, pattern, pattern, pattern, limit)
                .map(r -> new UserSearchRow(r.get("id", UUID.class), r.get("pseudo", String.class),
                        r.get("first_name", String.class), r.get("last_name", String.class),
                        r.get("email", String.class))));
    }
}
