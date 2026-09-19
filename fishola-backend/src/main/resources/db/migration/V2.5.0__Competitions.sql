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

-- Badge de participation à un concours (#90) : un opérateur crée un concours (nom, date,
-- plan d'eau/cours d'eau, fédération organisatrice) puis y associe manuellement des
-- pêcheurs, qui débloquent un badge "concours" portant le nom et la date du concours.
-- S'appuie sur le socle de gamification existant (V2.1.0/V2.2.0, #146) plutôt que d'en
-- créer un second : un unique badge catalogue CONCOURS (rule_type = 'manual', comme
-- TESTEUR_EMERITE), chaque attribution référençant en plus le concours concerné.
-- La catégorie 'concours' est ajoutée séparément par V2.4.0 (contrainte Postgres).

CREATE TABLE public.competition (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    competition_date date NOT NULL,
    water_entity_id uuid NOT NULL,
    federation_name text NOT NULL,
    created_by uuid NOT NULL,
    created_on timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT competition_pkey PRIMARY KEY (id),
    CONSTRAINT competition_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity (id),
    CONSTRAINT competition_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.fishola_admin (id)
);

COMMENT ON TABLE public.competition IS
    'Concours de pêche (#90) créé par un opérateur ; support de l''attribution manuelle du badge CONCOURS (gamification_badge_unlock.competition_id)';
COMMENT ON COLUMN public.competition.federation_name IS
    'Fédération organisatrice : texte libre (aucune table "fédération" dans ce schéma -- le périmètre d''un opérateur est déjà porté par fishola_admin_water_entities/department)';
COMMENT ON COLUMN public.competition.created_by IS 'Opérateur ou administrateur ayant créé le concours';

ALTER TABLE public.gamification_badge_unlock
    ADD COLUMN competition_id uuid REFERENCES public.competition (id);

COMMENT ON COLUMN public.gamification_badge_unlock.competition_id IS
    'Concours ayant donné lieu à ce déblocage (#90) ; non nul seulement pour le badge CONCOURS. Permet à un même pêcheur de débloquer plusieurs fois ce badge (un par concours), ce que la contrainte historique (badge_id, fishola_user_id, period_year) interdisait.';

-- La contrainte historique visait des badges à instance unique par (pêcheur, année). Le
-- badge CONCOURS doit au contraire pouvoir être débloqué plusieurs fois par le même
-- pêcheur (une fois par concours) : on la remplace par deux index partiels équivalents,
-- l'un pour les lignes hors-concours (comportement inchangé), l'autre dédié aux lignes de
-- concours (au plus un déblocage par (pêcheur, concours)). GamificationDao.upsertUnlock
-- cible désormais explicitement le premier via une clause WHERE sur son ON CONFLICT.
ALTER TABLE public.gamification_badge_unlock DROP CONSTRAINT gamification_badge_unlock_unique;

CREATE UNIQUE INDEX gamification_badge_unlock_unique
    ON public.gamification_badge_unlock (badge_id, fishola_user_id, period_year)
    WHERE competition_id IS NULL;

CREATE UNIQUE INDEX gamification_badge_unlock_competition_unique
    ON public.gamification_badge_unlock (fishola_user_id, competition_id)
    WHERE competition_id IS NOT NULL;

INSERT INTO public.gamification_badge (code, category, name, description, icon, rule_type, rule_params, tier, annual_reset)
VALUES ('CONCOURS', 'concours', 'Participation à un concours',
        'Attribué par un opérateur de fédération lors d''un concours de pêche', '🥇', 'manual', NULL, NULL, false);
