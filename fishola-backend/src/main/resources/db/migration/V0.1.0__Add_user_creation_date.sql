---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

ALTER TABLE fishola_user
  ADD COLUMN created_on TIMESTAMP WITHOUT TIME ZONE;

UPDATE fishola_user SET created_on = (
  SELECT coalesce(min(t.created_on), now())
    FROM trip t
    WHERE t.owner_id = fishola_user.id
);

ALTER TABLE fishola_user
  ALTER COLUMN created_on SET NOT NULL;

COMMENT ON COLUMN fishola_user.created_on
  IS 'Date de création du compte (validation de l''email)';

