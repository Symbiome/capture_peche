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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inrae.fishola.rest.audit.AuditLogEntry;
import fr.inrae.fishola.rest.audit.ImmutableAuditLogEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

/**
 * Accès au journal d'audit (CdC 3.1.5.1, cf. #11 §4). Écriture append-only via
 * l'intercepteur {@code @Audited} ; lecture paginée pour la consultation admin.
 */
@ApplicationScoped
public class AuditLogDao extends AbstractFisholaDao {

    @Inject
    ObjectMapper objectMapper;

    /**
     * Insère une ligne de journal, dans une transaction DÉDIÉE : le filtre de
     * réponse s'exécute après la transaction métier (déjà committée), et l'audit
     * ne doit ni dépendre d'elle ni la faire échouer. {@code detailsJson} doit
     * être un objet JSON valide (ou null → {@code {}}).
     */
    @Transactional(REQUIRES_NEW)
    public void insert(String actorType, UUID actorId, String action,
                       String entityType, UUID entityId, String detailsJson) {
        final String details = (detailsJson == null || detailsJson.isBlank()) ? "{}" : detailsJson;
        withContextNoResult(context -> context.execute(
                "INSERT INTO audit_log (actor_type, actor_id, action, entity_type, entity_id, details) "
                        + "VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                actorType, actorId, action, entityType, entityId, details));
    }

    // Consultation admin : filtres optionnels (acteur, action) + pagination, du
    // plus récent au plus ancien. Bind order : actorType×2, action×2, limit, offset.
    private static final String LIST_SQL = ""
            + "SELECT id, actor_type, actor_id, action, entity_type, entity_id, at, details::text AS details "
            + "FROM audit_log "
            + "WHERE (?::text IS NULL OR actor_type = ?) "
            + "  AND (?::text IS NULL OR action = ?) "
            + "ORDER BY at DESC "
            + "LIMIT ? OFFSET ?";

    public List<AuditLogEntry> list(Optional<String> actorType, Optional<String> action,
                                    int limit, int offset) {
        String actor = actorType.orElse(null);
        String act = action.orElse(null);
        return withContext(context -> context
                .fetch(LIST_SQL, actor, actor, act, act, limit, offset)
                .map(rec -> (AuditLogEntry) ImmutableAuditLogEntry.builder()
                        .id(rec.get("id", UUID.class))
                        .actorType(rec.get("actor_type", String.class))
                        .actorId(Optional.ofNullable(rec.get("actor_id", UUID.class)))
                        .action(rec.get("action", String.class))
                        .entityType(Optional.ofNullable(rec.get("entity_type", String.class)))
                        .entityId(Optional.ofNullable(rec.get("entity_id", UUID.class)))
                        .at(rec.get("at", OffsetDateTime.class))
                        .details(parseDetails(rec.get("details", String.class)))
                        .build()));
    }

    private JsonNode parseDetails(String json) {
        try {
            return objectMapper.readTree(json == null ? "{}" : json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }
}
