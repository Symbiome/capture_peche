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

-- BD TOPO's surface_hydrographique only links to a named cours d'eau (the whole
-- river, i.e. a water_entity of kind FLOWING), never to a specific tronçon/
-- river_section -- there is no such granularity in the source data. So
-- water_surface only ever needs a single water_entity_id parent (STILL for a
-- plan d'eau footprint, FLOWING for a wide-river-reach footprint); river_section
-- as a second, alternate parent was the wrong FK target.
ALTER TABLE water_surface DROP CONSTRAINT water_surface_river_section_id_fkey;
ALTER TABLE water_surface DROP CONSTRAINT water_surface_at_most_one_parent;
ALTER TABLE water_surface DROP COLUMN river_section_id;

COMMENT ON COLUMN water_surface.water_entity_id IS 'Plan d''eau (kind=STILL) ou cours d''eau nommé (kind=FLOWING) dont cette surface est l''empreinte';
