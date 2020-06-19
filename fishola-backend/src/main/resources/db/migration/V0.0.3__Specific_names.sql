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
INSERT INTO species_by_lake (lake_id, species_id)
    SELECT l.id AS lake_id, s.id AS species_id
    FROM lake l, species s;

UPDATE species_by_lake
    SET ALIAS = 'Lavaret'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Lac du Bourget' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Lavaret'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Lac d''Aiguebelette' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Féra'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Léman' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Féra'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Lac d''Annecy' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );
