---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

-- Référentiel communal (IGN ADMIN EXPRESS COG), nécessaire à la recherche d'un
-- lieu de pêche « par nom du cours d'eau ou commune » (note Q1). La géométrie
-- communale permet la jointure spatiale communes <-> entités hydro. Clé
-- naturelle : le code INSEE (insee_com). L'extension pg_trgm et l'opérateur
-- gin_trgm_ops sont fournis par V1.1.0.
CREATE TABLE commune (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    insee_com character varying(5) NOT NULL,
    name text NOT NULL,
    geom geometry(MultiPolygon, 4326) NOT NULL,
    latitude double precision GENERATED ALWAYS AS (st_y(st_centroid(geom))) STORED,
    longitude double precision GENERATED ALWAYS AS (st_x(st_centroid(geom))) STORED,
    CONSTRAINT commune_pkey PRIMARY KEY (id),
    CONSTRAINT commune_insee_com_key UNIQUE (insee_com)
);

-- GIST pour les jointures spatiales (ST_Intersects / ST_DWithin commune<->hydro),
-- GIN trigram pour la recherche textuelle tolérante (casse, fautes de frappe).
CREATE INDEX commune_geom_idx ON commune USING gist (geom);
CREATE INDEX commune_name_trgm_idx ON commune USING gin (name gin_trgm_ops);

COMMENT ON TABLE commune IS 'Communes (référentiel IGN ADMIN EXPRESS)';
COMMENT ON COLUMN commune.id IS 'Identifiant technique';
COMMENT ON COLUMN commune.insee_com IS 'Code INSEE de la commune (clé naturelle)';
COMMENT ON COLUMN commune.name IS 'Nom de la commune';
COMMENT ON COLUMN commune.geom IS 'Contour communal (MultiPolygon, EPSG:4326)';
COMMENT ON COLUMN commune.latitude IS 'Latitude du centroïde (générée)';
COMMENT ON COLUMN commune.longitude IS 'Longitude du centroïde (générée)';
