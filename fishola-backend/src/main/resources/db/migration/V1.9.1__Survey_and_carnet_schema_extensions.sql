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

-- Extensions de schéma (#145) pour les formats opérateur « carnet volontaire » (#143) et
-- « enquête terrain » (#144). Le format carnet volontaire est déjà en production : son
-- import et sa saisie manuelle parsent et valident déjà plusieurs des champs ci-dessous
-- (CarnetVolontaireParsedRow / CarnetVolontaireCatchBean) mais les perdent faute de colonne
-- de destination -- cette migration comble ce trou en plus de préparer #144.
--
-- Toutes les colonnes ajoutées sont NULLables : aucun impact sur les données existantes ni
-- sur les pipelines #71/#72 déjà en production.

-- Mode de pêche dédié aux formats carnet volontaire / enquête (vocabulaire fermé, propre à
-- ces deux formats -- distinct de la liste ouverte ImportSchema.FISHING_MODES du format
-- générique #71, qui n'est pas concerné par cette migration).
CREATE TYPE public.fishing_mode AS ENUM (
    'bateau',
    'float_tube_canoe',
    'bord_itinerant',
    'bord_statique'
);

COMMENT ON TYPE public.fishing_mode IS 'Mode de pêche (carnet volontaire #143 / enquête #144)';

-- Période de la journée d'une « session souvenir » (enquête #144, sortie passée du pêcheur
-- enquêté, dont seule la période de la journée est connue -- pas d'heures précises).
CREATE TYPE public.day_period AS ENUM (
    'matin',
    'apres_midi',
    'journee_entiere',
    'soiree'
);

COMMENT ON TYPE public.day_period IS 'Période de la journée d''une session souvenir (enquête #144)';

-- Origine d'une capture de truite fario (TRF uniquement -- contrôle applicatif, pas de
-- contrainte SQL conditionnelle sur l'espèce, cohérent avec le reste du pipeline import où
-- ces règles métier vivent côté Java).
CREATE TYPE public.trout_origin AS ENUM (
    'naturelle',
    'deversement',
    'inconnue'
);

COMMENT ON TYPE public.trout_origin IS 'Origine d''une capture de truite fario (carnet volontaire #143 / enquête #144)';

-- 5e méthode de collecte : sortie passée reconstituée de mémoire par un pêcheur enquêté
-- (« session souvenir », #144), distincte de 'enquete' (sortie en cours, observée).
ALTER TYPE public.collection_method ADD VALUE 'enquete_souvenir';

-- Session d'enquête terrain (#144) : un enquêteur sur un site à une date donnée. Regroupe
-- plusieurs sorties (trip.survey_session_id).
CREATE TABLE public.survey_session (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    water_entity_id uuid,
    day date NOT NULL,
    unsurveyed_shore_anglers smallint,
    unsurveyed_boat_anglers smallint,
    created_on timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT survey_session_pkey PRIMARY KEY (id),
    CONSTRAINT survey_session_code_key UNIQUE (code),
    CONSTRAINT survey_session_water_entity_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity (id)
);

COMMENT ON TABLE public.survey_session IS 'Session d''enquête terrain (#144) : enquêteur + site + date, regroupe des sorties';
COMMENT ON COLUMN public.survey_session.code IS 'Code session du fichier source (clé de liaison), unique';
COMMENT ON COLUMN public.survey_session.unsurveyed_shore_anglers IS 'Nb pêcheurs carnassiers du bord observés non-enquêtés';
COMMENT ON COLUMN public.survey_session.unsurveyed_boat_anglers IS 'Nb pêcheurs carnassiers en bateau observés non-enquêtés';

-- Profil non nominatif d'un pêcheur enquêté (#144) : pas de compte, pas de donnée
-- personnelle directe -- seule une donnée d'analyse (département/pays d'origine) est
-- conservée, cf. le point RGPD identifié dans #65.
CREATE TABLE public.surveyed_angler (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    origin_department character(3),
    origin_country text,
    created_on timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT surveyed_angler_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE public.surveyed_angler IS 'Profil non nominatif d''un pêcheur enquêté (#144) : aucune donnée personnelle directe';
COMMENT ON COLUMN public.surveyed_angler.code IS 'Code pêcheur du fichier source, local à une session (pas d''unicité globale)';
COMMENT ON COLUMN public.surveyed_angler.origin_department IS 'Département d''origine (France), donnée d''analyse uniquement';
COMMENT ON COLUMN public.surveyed_angler.origin_country IS 'Pays d''origine si l''enquêté n''est pas résident français';

-- trip : champs additionnels carnet volontaire (#143) + enquête (#144).
ALTER TABLE public.trip ADD COLUMN expected_species_id uuid;
ALTER TABLE public.trip ADD COLUMN secondary_technique_id uuid;
ALTER TABLE public.trip ADD COLUMN bait_or_lure text;
ALTER TABLE public.trip ADD COLUMN rod_count smallint;
ALTER TABLE public.trip ADD COLUMN fishing_mode public.fishing_mode;
ALTER TABLE public.trip ADD COLUMN trip_observations text[];
ALTER TABLE public.trip ADD COLUMN day_period public.day_period;
ALTER TABLE public.trip ADD COLUMN external_ref text;
ALTER TABLE public.trip ADD COLUMN survey_session_id uuid;
ALTER TABLE public.trip ADD COLUMN surveyed_angler_id uuid;

ALTER TABLE public.trip ADD CONSTRAINT trip_expected_species_id_fkey FOREIGN KEY (expected_species_id) REFERENCES public.species (id);
ALTER TABLE public.trip ADD CONSTRAINT trip_secondary_technique_id_fkey FOREIGN KEY (secondary_technique_id) REFERENCES public.technique (id);
ALTER TABLE public.trip ADD CONSTRAINT trip_survey_session_id_fkey FOREIGN KEY (survey_session_id) REFERENCES public.survey_session (id);
ALTER TABLE public.trip ADD CONSTRAINT trip_surveyed_angler_id_fkey FOREIGN KEY (surveyed_angler_id) REFERENCES public.surveyed_angler (id);

CREATE INDEX trip_survey_session_id_idx ON public.trip (survey_session_id);

COMMENT ON COLUMN public.trip.expected_species_id IS 'Espèce recherchée (carnet volontaire #143 / enquête #144) ; NULL si non renseignée ou modalité "Aucune"/"Toutes espèces"';
COMMENT ON COLUMN public.trip.secondary_technique_id IS 'Technique secondaire (carnet volontaire #143)';
COMMENT ON COLUMN public.trip.bait_or_lure IS 'Appât / type de leurre par défaut de la sortie';
COMMENT ON COLUMN public.trip.rod_count IS 'Nombre de lignes (carnet volontaire #143 / enquête #144)';
COMMENT ON COLUMN public.trip.trip_observations IS 'Observations diverses, multi-valeurs (carnet volontaire #143)';
COMMENT ON COLUMN public.trip.day_period IS 'Période de la journée (enquête #144, session souvenir uniquement)';
COMMENT ON COLUMN public.trip.external_ref IS 'Code sortie / code pêcheur du fichier source (auditabilité import)';
COMMENT ON COLUMN public.trip.survey_session_id IS 'Session d''enquête terrain d''origine (#144) ; NULL hors format enquête';
COMMENT ON COLUMN public.trip.surveyed_angler_id IS 'Pêcheur enquêté d''origine (#144) ; NULL hors format enquête';

-- catch : champs additionnels carnet volontaire (#143) + enquête (#144).
ALTER TABLE public.catch ADD COLUMN trout_origin public.trout_origin;
ALTER TABLE public.catch ADD COLUMN lot_min_size_cm smallint;
ALTER TABLE public.catch ADD COLUMN lot_max_size_cm smallint;
ALTER TABLE public.catch ADD COLUMN is_tagged boolean;
ALTER TABLE public.catch ADD COLUMN tag_reference text;
ALTER TABLE public.catch ADD COLUMN bait_or_lure text;

COMMENT ON COLUMN public.catch.trout_origin IS 'Origine d''une capture de truite fario (TRF uniquement, contrôle applicatif)';
COMMENT ON COLUMN public.catch.lot_min_size_cm IS 'Taille min du lot (cm) -- donnée saisie, distincte de species_size_bounds (règle de validation)';
COMMENT ON COLUMN public.catch.lot_max_size_cm IS 'Taille max du lot (cm) -- donnée saisie, distincte de species_size_bounds (règle de validation)';
COMMENT ON COLUMN public.catch.is_tagged IS 'Poisson marqué/bagué';
COMMENT ON COLUMN public.catch.tag_reference IS 'Numéro de marquage/bague, obligatoire si is_tagged';
COMMENT ON COLUMN public.catch.bait_or_lure IS 'Appât / type de leurre de cette capture (pré-rempli d''après la sortie)';
