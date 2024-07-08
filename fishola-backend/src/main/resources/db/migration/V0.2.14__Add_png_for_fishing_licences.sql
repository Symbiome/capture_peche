---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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

ALTER TABLE fishola_user_licences
ALTER COLUMN type TYPE VARCHAR(200);
ALTER TABLE fishola_user_licences
ALTER COLUMN type DROP DEFAULT;

DROP TYPE licence_type;

CREATE TYPE licence_type
    AS ENUM('PDF', 'JPEG', 'PNG');

ALTER TABLE fishola_user_licences
ALTER COLUMN type TYPE licence_type
USING type::licence_type;
ALTER TABLE fishola_user_licences
ALTER COLUMN type SET DEFAULT 'PDF';
