---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

CREATE TYPE licence_type
    AS ENUM('PDF', 'JPEG');

CREATE TABLE fishola_user_licences (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    user_id UUID REFERENCES fishola_user(id) NOT NULL,
    expiration_date DATE NOT NULL,
    type licence_type NOT NULL DEFAULT licence_type('PDF'),
    content BYTEA NOT NULL
);