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

-- Référentiels personnalisés par compte staff (#239) : chaque opérateur ou admin local
-- masque, dans ses listes de saisie, des éléments du référentiel national (espèces
-- absentes de ses sites, techniques jamais pratiquées...). Le référentiel national
-- n'est pas modifié et l'import de fichiers n'est pas filtré.
--
-- On stocke les éléments MASQUÉS : un élément ajouté au référentiel national est donc
-- affiché par défaut chez tous les comptes. item_id pointe vers species.id ou
-- technique.id selon referential_type, d'où l'absence de clé étrangère sur item_id.

CREATE TYPE hideable_referential AS ENUM ('species', 'technique');

CREATE TABLE fishola_admin_hidden_referential (
    fishola_admin_id uuid NOT NULL REFERENCES fishola_admin(id) ON DELETE CASCADE,
    referential_type hideable_referential NOT NULL,
    item_id uuid NOT NULL,
    PRIMARY KEY (fishola_admin_id, referential_type, item_id)
);

COMMENT ON TABLE fishola_admin_hidden_referential IS 'Éléments du référentiel national masqués dans les listes de saisie d''un compte staff (#239)';
COMMENT ON COLUMN fishola_admin_hidden_referential.item_id IS 'species.id ou technique.id selon referential_type';
