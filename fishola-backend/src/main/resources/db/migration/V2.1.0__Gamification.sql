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

-- Socle de gamification (#7), construit à l'occasion de #146 (catalogue de badges v1 +
-- partage social) : #7 était restée « dégrossie » sans jamais être fusionnée (issue encore
-- ouverte, aucune trace de code dans ce dépôt). Cette migration porte uniquement le
-- schéma ; le catalogue des badges est chargé séparément (V2.2.0, migration de données).

CREATE TYPE public.gamification_badge_category AS ENUM (
    'records_personnels',
    'techniques',
    'especes',
    'fidelite',
    'exploration',
    'autres'
);

COMMENT ON TYPE public.gamification_badge_category IS 'Catégorie du catalogue de badges v1 (#146)';

-- Un type de règle par stratégie de GamificationEngine. 'manual' = pas d'évaluation
-- automatique, attribution par un admin national (cf. gamification_badge_unlock.attributed_by).
CREATE TYPE public.gamification_rule_type AS ENUM (
    'metric_record',
    'session_time_window',
    'technique_tier',
    'technique_versatility',
    'species_diversity',
    'species_annual_tier',
    'remarkable_species_catch',
    'photo_count',
    'consecutive_weeks',
    'session_count_tier',
    'season_coverage',
    'opening_day',
    'bredouille_count',
    'contributor_percentile',
    'multi_department',
    'basin_diversity',
    'altitude_threshold',
    'boat_river_sessions',
    'mixed_environment',
    'single_water_entity_tier',
    'manual'
);

COMMENT ON TYPE public.gamification_rule_type IS 'Stratégie d''évaluation d''un badge (#146) ; une classe Java par valeur, sauf ''manual''';

CREATE TABLE public.gamification_badge (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code text NOT NULL,
    category public.gamification_badge_category NOT NULL,
    name text NOT NULL,
    description text,
    icon text,
    rule_type public.gamification_rule_type NOT NULL,
    rule_params jsonb,
    tier smallint,
    annual_reset boolean DEFAULT false NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_on timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT gamification_badge_pkey PRIMARY KEY (id),
    CONSTRAINT gamification_badge_code_key UNIQUE (code),
    CONSTRAINT gamification_badge_rule_params_manual_null
        CHECK (rule_type <> 'manual' OR rule_params IS NULL)
);

COMMENT ON TABLE public.gamification_badge IS 'Catalogue des badges (#146) : une ligne par badge débloquable, y compris chaque palier d''une famille (ex. un badge par niveau technique/espèce)';
COMMENT ON COLUMN public.gamification_badge.code IS 'Clé stable machine, ex. ''RECORD_TAILLE'', ''TECHNIQUE_MOUCHE_2''';
COMMENT ON COLUMN public.gamification_badge.rule_params IS 'Paramètres de la stratégie (seuils, listes, fenêtres temporelles...) ; nul pour ''manual''';
COMMENT ON COLUMN public.gamification_badge.tier IS 'Palier ordinal au sein d''une famille (1, 2, 3...) ; nul si le badge n''a pas de palier';
COMMENT ON COLUMN public.gamification_badge.annual_reset IS 'Si vrai, le déblocage est propre à une année civile (cf. gamification_badge_unlock.period_year)';
COMMENT ON COLUMN public.gamification_badge.active IS 'Badges inactifs : mécanique prête mais pas montrée au pêcheur (ex. en attente de validation MO, #146)';

CREATE TABLE public.gamification_badge_unlock (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    badge_id uuid NOT NULL,
    fishola_user_id uuid NOT NULL,
    -- 0 = badge "à vie" ; sinon année civile du déblocage (badges annual_reset). NOT NULL
    -- (plutôt que nullable) : Postgres traite deux NULL comme distincts dans une
    -- contrainte UNIQUE, ce qui casserait l'idempotence des badges non-annuels.
    period_year smallint DEFAULT 0 NOT NULL,
    unlocked_at timestamp without time zone DEFAULT now() NOT NULL,
    context jsonb,
    attributed_by uuid,
    CONSTRAINT gamification_badge_unlock_pkey PRIMARY KEY (id),
    CONSTRAINT gamification_badge_unlock_unique UNIQUE (badge_id, fishola_user_id, period_year),
    CONSTRAINT gamification_badge_unlock_badge_id_fkey FOREIGN KEY (badge_id) REFERENCES public.gamification_badge (id),
    CONSTRAINT gamification_badge_unlock_fishola_user_id_fkey FOREIGN KEY (fishola_user_id) REFERENCES public.fishola_user (id),
    CONSTRAINT gamification_badge_unlock_attributed_by_fkey FOREIGN KEY (attributed_by) REFERENCES public.fishola_admin (id)
);

CREATE INDEX gamification_badge_unlock_user_idx ON public.gamification_badge_unlock (fishola_user_id);

COMMENT ON TABLE public.gamification_badge_unlock IS 'Déblocage d''un badge par un pêcheur (#146) : automatique (GamificationEngine) ou manuel (attributed_by non nul)';
COMMENT ON COLUMN public.gamification_badge_unlock.context IS 'Détail du déblocage pour l''affichage/partage, ex. {"species":"Perche","sizeCm":34}';
COMMENT ON COLUMN public.gamification_badge_unlock.attributed_by IS 'Admin national ayant attribué le badge ; non nul seulement pour rule_type=''manual''';
