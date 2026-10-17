--
-- #%L
-- Carnet de pêche :: Backend
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

-- Inscription libre dans l'app pêcheur (#192), en mode bêta-testeurs
-- (fishola.beta-testers-mode, production seulement) : ouverte ou fermée par
-- un administrateur national depuis le back-office, sans redéploiement.
-- Fermée par défaut : les comptes des bêta-testeurs sont importés par CSV.
-- Hors mode bêta-testeurs, ce réglage est ignoré et l'inscription reste ouverte.

CREATE TABLE registration_setting (
    id boolean DEFAULT true NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT registration_setting_pkey PRIMARY KEY (id),
    CONSTRAINT registration_setting_single_row CHECK (id)
);

COMMENT ON TABLE registration_setting IS 'Ouverture de l''inscription libre dans l''app pêcheur (#192), ligne unique';
COMMENT ON COLUMN registration_setting.enabled IS 'Mode bêta-testeurs : true, création de compte possible depuis l''app ; false, comptes créés uniquement par import';

INSERT INTO registration_setting (id, enabled) VALUES (true, false);
