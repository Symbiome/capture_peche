--
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
--

-- Socle de l'import CSV opérateur (#12, cadrage 3153). Introduit la « méthode de
-- collecte » métier (distincte de trip.source qui est le canal web/application),
-- le support des lots (quantité + classe de taille) sur les captures, les bornes
-- de taille aberrante par espèce, et les tables de suivi des imports.

-- Méthode de collecte des données de la sortie (origine métier, cf. cadrage §
-- dictionnaire de données). NB : trip.source (device_type) reste le canal de
-- saisie (web/application) et n'est pas remplacé.
CREATE TYPE public.collection_method AS ENUM (
    'saisie_pecheur',
    'enquete',
    'carnet_volontaire',
    'carnet_obligatoire'
);

COMMENT ON TYPE public.collection_method IS 'Méthode de collecte métier de la sortie (import CSV #12)';

-- trip.collection_method : ajoutée nullable, rétro-remplie sur l'existant, puis
-- passée NOT NULL DEFAULT (les sorties historiques sont des saisies pêcheur).
ALTER TABLE public.trip ADD COLUMN collection_method public.collection_method;
UPDATE public.trip SET collection_method = 'saisie_pecheur' WHERE collection_method IS NULL;
ALTER TABLE public.trip ALTER COLUMN collection_method SET DEFAULT 'saisie_pecheur';
ALTER TABLE public.trip ALTER COLUMN collection_method SET NOT NULL;

COMMENT ON COLUMN public.trip.collection_method IS 'Méthode de collecte métier (import CSV #12), distincte du canal trip.source';

-- Captures en lot : nombre d'individus et classe de taille (au lieu d'une taille
-- individuelle). quantity par défaut 1 = capture individuelle (rétro-compatible).
ALTER TABLE public.catch ADD COLUMN quantity integer NOT NULL DEFAULT 1;
ALTER TABLE public.catch ADD COLUMN size_class varchar;

COMMENT ON COLUMN public.catch.quantity IS 'Nombre d''individus (lot). 1 = capture individuelle';
COMMENT ON COLUMN public.catch.size_class IS 'Classe de taille d''un lot (ex. « 10-15 »), null pour une capture individuelle';

-- Bornes de taille aberrante GLOBALES par espèce (stade métier #12). Distinctes
-- de authorized_sample (min/max de prélèvement PAR plan d'eau, autre finalité).
-- Table laissée VIDE : valeurs fournies par l'UFBRMC (placeholder).
CREATE TABLE public.species_size_bounds (
    species_id uuid NOT NULL,
    min_size_cm integer,
    max_size_cm integer,
    CONSTRAINT species_size_bounds_pkey PRIMARY KEY (species_id),
    CONSTRAINT species_size_bounds_species_fkey FOREIGN KEY (species_id) REFERENCES public.species (id)
);

COMMENT ON TABLE public.species_size_bounds IS 'Bornes de taille aberrante globales par espèce (import CSV #12, stade métier)';

-- Suivi d'un import CSV (idempotence par file_hash, comptages, statut).
CREATE TABLE public.import_job (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    file_name text,
    file_hash text NOT NULL,
    collection_method public.collection_method,
    status text NOT NULL,
    total integer DEFAULT 0 NOT NULL,
    inserted integer DEFAULT 0 NOT NULL,
    rejected integer DEFAULT 0 NOT NULL,
    created_by uuid,
    created_on timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT import_job_pkey PRIMARY KEY (id),
    CONSTRAINT import_job_file_hash_key UNIQUE (file_hash)
);

COMMENT ON TABLE public.import_job IS 'Import CSV opérateur (#12) : idempotence par empreinte, comptages et statut';
COMMENT ON COLUMN public.import_job.file_hash IS 'SHA-256 du fichier, garantit l''idempotence (rejet 409 si déjà importé)';
COMMENT ON COLUMN public.import_job.created_by IS 'Acteur ayant lancé l''import (admin/opérateur), sans FK (modèle staff en cours #11)';

-- Erreurs par ligne d'un import, avec le stade (structurel/référentiel/métier),
-- la colonne fautive, un code stable et un message lisible.
CREATE TABLE public.import_row_error (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    import_id uuid NOT NULL,
    line integer NOT NULL,
    column_name text,
    stage text NOT NULL,
    code text NOT NULL,
    message text,
    CONSTRAINT import_row_error_pkey PRIMARY KEY (id),
    CONSTRAINT import_row_error_import_fkey FOREIGN KEY (import_id) REFERENCES public.import_job (id)
);

CREATE INDEX import_row_error_import_id_idx ON public.import_row_error (import_id);

COMMENT ON TABLE public.import_row_error IS 'Erreurs par ligne d''un import CSV (#12), rapport d''erreurs';
