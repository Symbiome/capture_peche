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

-- BD TOPO import happens in stages (river network geometry first, plan_d_eau /
-- cours_d_eau referential later), so a water_surface row can legitimately have
-- neither parent resolved yet. Still forbid having both set at once.
ALTER TABLE water_surface DROP CONSTRAINT water_surface_exactly_one_parent;
ALTER TABLE water_surface ADD CONSTRAINT water_surface_at_most_one_parent
    CHECK (num_nonnulls(water_entity_id, river_section_id) <= 1);
