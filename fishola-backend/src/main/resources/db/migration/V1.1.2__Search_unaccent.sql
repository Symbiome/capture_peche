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

-- Accent-insensitive textual search on water entity and commune names (#7):
-- "leman" must match "Léman". unaccent() alone is only STABLE, so it cannot be
-- used directly in an index expression; the documented workaround is an
-- IMMUTABLE wrapper pinned to the 'unaccent' dictionary. The GIN trigram index
-- on f_unaccent(name) then serves typo-tolerant, accent-insensitive lookups.
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT AS
$func$
    SELECT public.unaccent('public.unaccent', $1)
$func$;

CREATE INDEX IF NOT EXISTS water_entity_name_unaccent_trgm_idx
    ON water_entity USING gin (f_unaccent(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS commune_name_unaccent_trgm_idx
    ON commune USING gin (f_unaccent(name) gin_trgm_ops);
