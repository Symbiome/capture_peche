package fr.inrae.fishola.rest.audit;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Une ligne du journal d'audit exposée à la consultation admin (#11 §4).
 */
@ImmutableObject
@JsonSerialize(as = ImmutableAuditLogEntry.class)
public interface AuditLogEntry {

    UUID id();

    /** 'admin' | 'user' | 'system'. */
    String actorType();

    /** Identifiant de l'acteur (admin/user) ; vide si 'system'. */
    Optional<UUID> actorId();

    /** Action journalisée, ex. 'admin.update'. */
    String action();

    Optional<String> entityType();

    Optional<UUID> entityId();

    OffsetDateTime at();

    /** Contexte non structuré (method, path, httpStatus…). */
    JsonNode details();
}
