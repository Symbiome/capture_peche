package fr.inrae.fishola.rest.referential;

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

import java.util.UUID;

/**
 * Minimal water entity listing (id + name only). Result item of
 * {@code GET /api/v1/referential/waterEntities/names}, used by the operator
 * back-office trip/catch entry forms, which only populate an "Entité
 * hydrographique" select and never read kind/centroid — carrying those extra
 * fields (as {@link WaterEntitySummary} does) is needless payload for ~181 000
 * rows.
 */
@ImmutableObject
@JsonSerialize(as = ImmutableWaterEntityName.class)
public interface WaterEntityName {

    UUID id();

    String name();
}
