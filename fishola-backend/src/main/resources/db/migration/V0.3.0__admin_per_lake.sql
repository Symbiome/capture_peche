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

CREATE TABLE fishola_admin (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    created_on TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    canCreateAdmin BOOLEAN NOT NULL DEFAULT FALSE,
    isNationalAdmin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE fishola_admin_lakes(
    fishola_admin_id UUID REFERENCES fishola_admin(id) NOT NULL,
    lake_id UUID REFERENCES lake(id) NOT NULL,
    PRIMARY KEY (fishola_admin_id, lake_id)
);

ALTER TABLE news ADD COLUMN is_national BOOLEAN DEFAULT false;

CREATE TABLE news_lake (
    news_id UUID REFERENCES news(id) NOT NULL,
    lake_id UUID REFERENCES lake(id) NOT NULL,
    PRIMARY KEY (news_id, lake_id)
);

update news set is_national = true;

insert into fishola_admin (email, password, canCreateAdmin, isNationalAdmin) values
    ('chloe.goulon@inrae.fr', '', true, true);

insert into fishola_admin (email, password, canCreateAdmin, isNationalAdmin) values
    ('amorel@codelutin.com', '', true, true);