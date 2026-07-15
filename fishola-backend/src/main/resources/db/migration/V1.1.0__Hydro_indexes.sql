---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
-- %%
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program.  If not, see <http://www.gnu.org/licenses/>.
-- #L%
---

-- Scaling the hydrographic network to the whole of France (millions of river
-- sections and water surfaces) makes ad-hoc name lookups unusable without a
-- dedicated text index. The plain B-tree unique index on water_entity.name
-- only serves equality, not the substring / fuzzy search needed by the
-- "search a river by name or commune" feature (#7).
--
-- GIST(geom) indexes on the raw geometry (SRID 4326, degrees) already exist
-- since V1.0.0 on river_section, water_entity and water_surface. They serve
-- geometry operators, but metric proximity queries (nearby, #5) use
-- ST_DWithin(geom::geography, point::geography, metres): to keep those queries
-- index-accelerated and exact (no latitude approximation), a functional GIST
-- index on the geography cast is added on the two tables the search hits.
-- Statements are idempotent (IF NOT EXISTS) so the migration is safe to re-run
-- on databases where the extension or index was created manually.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS water_entity_name_trgm_idx
    ON water_entity USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS river_section_geom_geog_idx
    ON river_section USING gist ((geom::geography));

CREATE INDEX IF NOT EXISTS water_surface_geom_geog_idx
    ON water_surface USING gist ((geom::geography));
