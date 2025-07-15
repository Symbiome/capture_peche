---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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

CREATE TABLE fishola_user_favorite_lakes(
    fishola_user_id UUID REFERENCES fishola_user(id) NOT NULL,
    lake_id UUID REFERENCES lake(id) NOT NULL,
    PRIMARY KEY (fishola_user_id, lake_id)
);

INSERT INTO fishola_user_favorite_lakes(fishola_user_id, lake_id)
SELECT DISTINCT owner_id, lake_id
FROM trip;