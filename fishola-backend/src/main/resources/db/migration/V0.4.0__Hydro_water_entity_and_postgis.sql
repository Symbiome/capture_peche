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

-- Generalize "lake" (still water only) into "water_entity" (still or flowing water),
-- switch all lat/lon pairs to proper PostGIS geometry columns, and add the linear
-- river network (river_section) and wet-surface footprints (water_surface) needed
-- to extend Fishola beyond alpine lakes to rivers/streams/canals.

-- Flyway applies repeatable (R__) migrations after all pending versioned ones in the
-- same run, so the export views (still referencing lake/lat/lon columns dropped
-- below) must be dropped explicitly here; R__Catchs_export_view.sql recreates them
-- against the new schema later in the same batch.
DROP VIEW IF EXISTS catchs_export;
DROP VIEW IF EXISTS catchs_openadom_export;
DROP VIEW IF EXISTS personal_catchs_export;

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TYPE water_entity_kind AS ENUM ('STILL', 'FLOWING');
CREATE TYPE river_bank AS ENUM ('GAUCHE', 'DROITE', 'INDETERMINE');

-- ---------------------------------------------------------------------------
-- 1. lake -> water_entity
-- ---------------------------------------------------------------------------

ALTER TABLE lake RENAME TO water_entity;
ALTER TABLE water_entity RENAME COLUMN lake_code TO water_entity_code;

ALTER TABLE water_entity
    ADD COLUMN kind water_entity_kind NOT NULL DEFAULT 'STILL',
    ADD COLUMN nature text,
    ADD COLUMN altitude_moyenne double precision,
    ADD COLUMN bdtopo_cleabs varchar(24),
    ADD COLUMN geom geometry(Geometry, 4326);

ALTER TABLE water_entity ADD CONSTRAINT water_entity_bdtopo_cleabs_key UNIQUE (bdtopo_cleabs);

UPDATE water_entity
SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

ALTER TABLE water_entity
    DROP COLUMN latitude,
    DROP COLUMN longitude;

-- Read-only companions kept in sync from geom by Postgres itself (ST_Centroid works
-- for a Point, a LineString or a Polygon alike), so existing "give me a display pin"
-- call sites don't need to become PostGIS-aware to read a coordinate back.
ALTER TABLE water_entity
    ADD COLUMN latitude  double precision GENERATED ALWAYS AS (ST_Y(ST_Centroid(geom))) STORED,
    ADD COLUMN longitude double precision GENERATED ALWAYS AS (ST_X(ST_Centroid(geom))) STORED;

CREATE INDEX water_entity_geom_idx ON water_entity USING GIST (geom);

ALTER TABLE water_entity RENAME CONSTRAINT lake_pkey TO water_entity_pkey;
ALTER INDEX lake_export_as_key RENAME TO water_entity_export_as_key;
ALTER INDEX lake_lake_code_key RENAME TO water_entity_water_entity_code_key;
ALTER INDEX lake_name_key RENAME TO water_entity_name_key;

COMMENT ON TABLE water_entity IS 'Milieu aquatique (plan d''eau, cours d''eau...) fréquenté par les pêcheurs';
COMMENT ON COLUMN water_entity.kind IS 'STILL = eau stagnante (lac, étang...), FLOWING = eau courante (rivière, canal...)';
COMMENT ON COLUMN water_entity.geom IS 'Empreinte géographique : point/polygone pour un plan d''eau, ligne simplifiée pour un cours d''eau nommé';
COMMENT ON COLUMN water_entity.bdtopo_cleabs IS 'Identifiant national BD TOPO (cleabs), pour ré-import idempotent';

-- ---------------------------------------------------------------------------
-- 2. Rename every *_lake* join table / column to *_water_entity*
-- ---------------------------------------------------------------------------

ALTER TABLE species_by_lake RENAME TO species_by_water_entity;
ALTER TABLE species_by_water_entity RENAME COLUMN lake_id TO water_entity_id;
ALTER TABLE species_by_water_entity RENAME CONSTRAINT species_by_lake_pkey TO species_by_water_entity_pkey;
ALTER TABLE species_by_water_entity RENAME CONSTRAINT species_by_lake_lake_id_fkey TO species_by_water_entity_water_entity_id_fkey;
ALTER TABLE species_by_water_entity RENAME CONSTRAINT species_by_lake_species_id_fkey TO species_by_water_entity_species_id_fkey;

ALTER TABLE authorized_sample RENAME COLUMN lake_id TO water_entity_id;
ALTER TABLE authorized_sample RENAME CONSTRAINT authorized_sample_lake_id_fkey TO authorized_sample_water_entity_id_fkey;

ALTER TABLE fishola_admin_lakes RENAME TO fishola_admin_water_entities;
ALTER TABLE fishola_admin_water_entities RENAME COLUMN lake_id TO water_entity_id;
ALTER TABLE fishola_admin_water_entities RENAME CONSTRAINT fishola_admin_lakes_pkey TO fishola_admin_water_entities_pkey;
ALTER TABLE fishola_admin_water_entities RENAME CONSTRAINT fishola_admin_lakes_fishola_admin_id_fkey TO fishola_admin_water_entities_fishola_admin_id_fkey;
ALTER TABLE fishola_admin_water_entities RENAME CONSTRAINT fishola_admin_lakes_lake_id_fkey TO fishola_admin_water_entities_water_entity_id_fkey;

ALTER TABLE fishola_user_favorite_lakes RENAME TO fishola_user_favorite_water_entities;
ALTER TABLE fishola_user_favorite_water_entities RENAME COLUMN lake_id TO water_entity_id;
ALTER TABLE fishola_user_favorite_water_entities RENAME CONSTRAINT fishola_user_favorite_lakes_pkey TO fishola_user_favorite_water_entities_pkey;
ALTER TABLE fishola_user_favorite_water_entities RENAME CONSTRAINT fishola_user_favorite_lakes_fishola_user_id_fkey TO fishola_user_favorite_water_entities_fishola_user_id_fkey;
ALTER TABLE fishola_user_favorite_water_entities RENAME CONSTRAINT fishola_user_favorite_lakes_lake_id_fkey TO fishola_user_favorite_water_entities_water_entity_id_fkey;

ALTER TABLE news_lake RENAME TO news_water_entity;
ALTER TABLE news_water_entity RENAME COLUMN lake_id TO water_entity_id;
ALTER TABLE news_water_entity RENAME CONSTRAINT news_lake_pkey TO news_water_entity_pkey;
ALTER TABLE news_water_entity RENAME CONSTRAINT news_lake_news_id_fkey TO news_water_entity_news_id_fkey;
ALTER TABLE news_water_entity RENAME CONSTRAINT news_lake_lake_id_fkey TO news_water_entity_water_entity_id_fkey;

ALTER TABLE trip RENAME COLUMN lake_id TO water_entity_id;
ALTER TABLE trip RENAME CONSTRAINT trip_lake_id_fkey TO trip_water_entity_id_fkey;

-- ---------------------------------------------------------------------------
-- 3. trip: river section / bank + proper geometry for begin/end position
-- ---------------------------------------------------------------------------

ALTER TABLE trip
    ADD COLUMN river_section_id uuid,
    ADD COLUMN river_bank river_bank,
    ADD COLUMN begin_position geometry(Point, 4326),
    ADD COLUMN end_position geometry(Point, 4326);

UPDATE trip
SET begin_position = ST_SetSRID(ST_MakePoint(begin_longitude, begin_latitude), 4326)
WHERE begin_latitude IS NOT NULL AND begin_longitude IS NOT NULL;

UPDATE trip
SET end_position = ST_SetSRID(ST_MakePoint(end_longitude, end_latitude), 4326)
WHERE end_latitude IS NOT NULL AND end_longitude IS NOT NULL;

ALTER TABLE trip
    DROP COLUMN begin_latitude,
    DROP COLUMN begin_longitude,
    DROP COLUMN end_latitude,
    DROP COLUMN end_longitude;

CREATE INDEX trip_begin_position_idx ON trip USING GIST (begin_position);
CREATE INDEX trip_end_position_idx ON trip USING GIST (end_position);

-- Read-only companions kept in sync by Postgres, so existing lat/lon read call sites
-- don't need to become PostGIS-aware.
ALTER TABLE trip
    ADD COLUMN begin_latitude  double precision GENERATED ALWAYS AS (ST_Y(begin_position)) STORED,
    ADD COLUMN begin_longitude double precision GENERATED ALWAYS AS (ST_X(begin_position)) STORED,
    ADD COLUMN end_latitude    double precision GENERATED ALWAYS AS (ST_Y(end_position)) STORED,
    ADD COLUMN end_longitude   double precision GENERATED ALWAYS AS (ST_X(end_position)) STORED;

COMMENT ON COLUMN trip.river_section_id IS 'Tronçon de cours d''eau, renseigné uniquement quand water_entity.kind = FLOWING';
COMMENT ON COLUMN trip.river_bank IS 'Rive fréquentée, renseignée uniquement pour une sortie sur cours d''eau';

-- ---------------------------------------------------------------------------
-- 4. catch: proper geometry for capture position
-- ---------------------------------------------------------------------------

ALTER TABLE catch ADD COLUMN position geometry(Point, 4326);

UPDATE catch
SET position = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

ALTER TABLE catch
    DROP COLUMN latitude,
    DROP COLUMN longitude;

CREATE INDEX catch_position_idx ON catch USING GIST (position);

-- Read-only companions kept in sync by Postgres, so existing lat/lon read call sites
-- don't need to become PostGIS-aware.
ALTER TABLE catch
    ADD COLUMN latitude  double precision GENERATED ALWAYS AS (ST_Y(position)) STORED,
    ADD COLUMN longitude double precision GENERATED ALWAYS AS (ST_X(position)) STORED;

-- ---------------------------------------------------------------------------
-- 5. river_section : linear network a trip attaches to
-- ---------------------------------------------------------------------------

CREATE TABLE river_section (
    id               uuid NOT NULL DEFAULT uuid_generate_v4(),
    water_entity_id  uuid,
    bdtopo_cleabs    varchar(24),
    persistent       boolean,
    width_class      text,
    flow_direction   text,
    geom             geometry(LineString, 4326) NOT NULL,
    CONSTRAINT river_section_pkey PRIMARY KEY (id),
    CONSTRAINT river_section_bdtopo_cleabs_key UNIQUE (bdtopo_cleabs),
    CONSTRAINT river_section_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES water_entity(id)
);

CREATE INDEX river_section_geom_idx ON river_section USING GIST (geom);

COMMENT ON TABLE river_section IS 'Tronçon élémentaire d''un cours d''eau (BD TOPO troncon_hydrographique)';
COMMENT ON COLUMN river_section.water_entity_id IS 'Cours d''eau nommé parent (water_entity.kind = FLOWING), nullable pour les tronçons non rattachés';

ALTER TABLE trip ADD CONSTRAINT trip_river_section_id_fkey FOREIGN KEY (river_section_id) REFERENCES river_section(id);

-- ---------------------------------------------------------------------------
-- 6. water_surface : polygon footprint for a lake or a wide river reach
-- ---------------------------------------------------------------------------

CREATE TABLE water_surface (
    id                uuid NOT NULL DEFAULT uuid_generate_v4(),
    water_entity_id   uuid,
    river_section_id  uuid,
    bdtopo_cleabs     varchar(24),
    nature            text,
    geom              geometry(Polygon, 4326) NOT NULL,
    CONSTRAINT water_surface_pkey PRIMARY KEY (id),
    CONSTRAINT water_surface_bdtopo_cleabs_key UNIQUE (bdtopo_cleabs),
    CONSTRAINT water_surface_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES water_entity(id),
    CONSTRAINT water_surface_river_section_id_fkey FOREIGN KEY (river_section_id) REFERENCES river_section(id),
    CONSTRAINT water_surface_exactly_one_parent CHECK (num_nonnulls(water_entity_id, river_section_id) = 1)
);

CREATE INDEX water_surface_geom_idx ON water_surface USING GIST (geom);

COMMENT ON TABLE water_surface IS 'Empreinte polygonale de l''eau : plan d''eau entier, ou tronçon de rivière assez large pour être surfacique (BD TOPO surface_hydrographique)';
