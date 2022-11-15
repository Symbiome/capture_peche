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

ALTER TABLE fishola_user ADD COLUMN accepts_mail_notifications BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE next_scheduled_courriel_notification_check (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    next_check_date TIMESTAMP WITHOUT TIME ZONE NOT NULL
);