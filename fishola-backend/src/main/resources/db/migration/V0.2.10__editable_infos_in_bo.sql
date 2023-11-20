---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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

ALTER TABLE catch
    ADD COLUMN edited_species_id UUID,
    ADD COLUMN edited_size INT NOT NULL DEFAULT 0,
    ADD COLUMN edited_weight INT NOT NULL DEFAULT 0,
    ADD COLUMN exclude_from_exports BOOLEAN NOT NULL default false;

UPDATE catch c SET edited_species_id = species_id, edited_size = c.size, edited_weight = weight, exclude_from_exports = false;