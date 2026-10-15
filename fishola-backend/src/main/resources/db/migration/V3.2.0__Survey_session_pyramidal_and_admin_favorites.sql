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

-- Saisie pyramidale des enquêtes terrain (#236) : session d'enquête → sortie(s) →
-- pêcheur(s) → captures.
--
-- Une session est ouverte une fois, reçoit successivement ses sorties, puis est
-- clôturée (heure de fin et compteurs de pêcheurs non enquêtés). Heures de session et
-- nom de l'opérateur (enquêteur, pas forcément le compte qui saisit) sont facultatifs :
-- les sessions antérieures et celles importées par fichier n'en ont pas. Les sessions
-- existantes et importées sont considérées clôturées.

CREATE TYPE survey_session_status AS ENUM ('ouverte', 'cloturee');

ALTER TABLE survey_session
    ADD COLUMN start_time time without time zone,
    ADD COLUMN end_time time without time zone,
    ADD COLUMN operator_name text,
    ADD COLUMN created_by uuid REFERENCES fishola_admin(id),
    ADD COLUMN status survey_session_status NOT NULL DEFAULT 'cloturee';

CREATE INDEX survey_session_open_by_creator_idx ON survey_session (created_by) WHERE status = 'ouverte';

-- Secteurs favoris propres à chaque compte opérateur, proposés en tête de la recherche
-- de secteur (#236), sur le modèle des favoris pêcheur (fishola_user_favorite_water_entities).
CREATE TABLE fishola_admin_favorite_water_entity (
    fishola_admin_id uuid NOT NULL REFERENCES fishola_admin(id) ON DELETE CASCADE,
    water_entity_id uuid NOT NULL REFERENCES water_entity(id) ON DELETE CASCADE,
    PRIMARY KEY (fishola_admin_id, water_entity_id)
);
