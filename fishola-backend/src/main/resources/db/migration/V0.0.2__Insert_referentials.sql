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
INSERT INTO lake (name, latitude, longitude, export_as) VALUES
    ('Lac d''Annecy', 45.787, 6.147, 'Annecy'),
    ('Léman', 46.426, 6.5499, 'Léman'),
    ('Lac du Bourget', 45.7249, 5.8684, 'Bourget'),
    ('Lac d''Aiguebelette', 45.5508, 5.8015, 'Aiguebelette');

INSERT INTO species (name, built_in, export_as) VALUES
    ('Omble chevalier', true, 'omble chevalier'),
    ('Corégone', true, 'coregone'),
    ('Brochet', true, 'brochet'),
    ('Perche', true, 'perche'),
    ('Truite', true, 'truite');

UPDATE species
    SET mandatory_size=false
    WHERE name='Perche';

INSERT INTO weather (name, export_as) VALUES
    ('Ensoleillé', 'Ensoleillé'),
    ('Sec couvert', 'Sec couvert'),
    ('Neigeux', 'Neigeux'),
    ('Orageux', 'Orageux'),
    ('Pluvieux', 'Pluvieux'),
    ('Brumeux', 'Brumeux');

INSERT INTO technique (name, built_in, export_as) VALUES
    ('Pêche à la traîne', true, 'traine'),
    ('Pêche à la sonde/gambe/nymphes/canin', true, 'sonde'),
    ('Pêche au leurre', true, 'leurre'),
    ('Pêche au vif', true, 'vif'),
    ('Pêche au coup', true, 'coup'),
    ('Autre', true, 'autre');

INSERT INTO released_fish_state (name, export_as) VALUES
    ('Comme neuf', 'Comme neuf'),
    ('Frais comme un gardon', 'Frais comme un gardon'),
    ('Légèrement blessé', 'Légèrement blessé'),
    ('Blessé', 'Blessé'),
    ('Mort', 'Mort');

INSERT INTO sample_type (name, export_as) VALUES
    ('Écailles', 'ecailles'),
    ('Estomac', 'estomac'),
    ('Tête', 'tete');
