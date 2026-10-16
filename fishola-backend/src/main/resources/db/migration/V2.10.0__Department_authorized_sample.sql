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

-- Maillage et taille maximale par défaut au département (#246).
--
-- Jusqu'ici ces valeurs se saisissaient entité hydrographique par entité
-- (authorized_sample), intenable sur les milliers de milieux d'un département.
-- Une valeur départementale s'applique désormais à toutes les entités du
-- département (water_entity.department, V1.10.0) ; une valeur saisie sur une
-- entité reste prioritaire, champ par champ.
--
-- Les valeurs ne sont PAS recopiées sur chaque entité (~181 000 lignes, #154) :
-- la résolution entité → département se fait à la lecture (ReferentialDao).
-- Pas de clé étrangère vers departement : ce référentiel n'est chargé qu'avec
-- l'import hydro, alors que le back-office propose les 101 départements.
CREATE TABLE public.department_authorized_sample (
    department character varying(3) NOT NULL,
    species_id uuid NOT NULL,
    max_size integer NOT NULL,
    mesh_size integer,
    CONSTRAINT department_authorized_sample_pkey PRIMARY KEY (department, species_id),
    CONSTRAINT department_authorized_sample_species_id_fkey
        FOREIGN KEY (species_id) REFERENCES public.species(id) ON DELETE CASCADE,
    CONSTRAINT department_authorized_sample_max_size_check CHECK (max_size > 0),
    CONSTRAINT department_authorized_sample_mesh_size_check CHECK (mesh_size IS NULL OR mesh_size > 0)
);

COMMENT ON TABLE public.department_authorized_sample IS 'Maillage et taille maximale par défaut d''une espèce sur toutes les entités hydrographiques d''un département (#246)';
COMMENT ON COLUMN public.department_authorized_sample.department IS 'Code département INSEE (même codage que water_entity.department)';
COMMENT ON COLUMN public.department_authorized_sample.species_id IS 'Espèce concernée';
COMMENT ON COLUMN public.department_authorized_sample.max_size IS 'Taille maximale de capture par défaut, en cm';
COMMENT ON COLUMN public.department_authorized_sample.mesh_size IS 'Maillage par défaut, en cm (NULL = non défini)';
