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
-- Ajoute d'une colonne natural_name servant d'id à la table
ALTER TABLE documentation ADD COLUMN natural_id TEXT;

UPDATE documentation SET natural_id = 'annecy' where name = 'Réglementation sur le lac d''Annecy';
UPDATE documentation SET natural_id = 'léman' where name = 'Réglementation sur le Léman';
UPDATE documentation SET natural_id = 'bourget' where name = 'Réglementation sur le lac du Bourget';
UPDATE documentation SET natural_id = 'aiguebelette' where name = 'Réglementation sur le lac d''Aiguebelette';
UPDATE documentation SET natural_id = 'espèces' where name = 'Documentation sur les espèces';
UPDATE documentation SET natural_id = 'prélèvements' where name = 'Documentation sur les prélèvements';
UPDATE documentation SET natural_id = 'cgu' where name = 'Conditions Générales d''Utilisation';

CREATE UNIQUE INDEX documentation_natural_id_idx ON documentation(natural_id);
ALTER TABLE documentation ALTER COLUMN natural_id SET NOT NULL;