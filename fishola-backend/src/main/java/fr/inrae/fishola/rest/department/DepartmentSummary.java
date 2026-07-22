package fr.inrae.fishola.rest.department;

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;

/**
 * A department available for an offline pack download (#54). Item of
 * {@code GET /api/v1/departments}. The {@code communeCount} is a cheap size
 * indicator (no spatial join) shown before download; the real entity count is
 * known by the client once the pack is fetched.
 */
@ImmutableObject
@JsonSerialize(as = ImmutableDepartmentSummary.class)
public interface DepartmentSummary {

    /** Department code (e.g. "74", "2A", "971"). */
    String code();

    /** Human-readable department name (e.g. "Haute-Savoie"). */
    String name();

    /** Number of communes in the department (rough download-size indicator). */
    int communeCount();
}
