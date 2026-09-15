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

-- Catalogue de badges v1 (#146), source : « Badges et récompenses v1.xlsx » (tableau
-- résumé repris dans l'issue). Chaque palier d'une famille (Techniques, Espèces) devient
-- sa propre ligne, débloquable indépendamment -- le tableau markdown de l'issue les
-- regroupe pour la lisibilité mais l'moteur a besoin d'un badge par palier.
--
-- Décisions v1 prises en l'absence du fichier source détaillé (non joignable depuis cet
-- environnement) :
--  - Techniques : 3 paliers uniformes (5/20/50 sorties) par technique nommée -- l'issue
--    indique « 3 à 7 paliers selon la technique » sans détailler lesquelles ont combien.
--  - Espèces : 3 paliers uniformes (5/15/35 captures/an), reprenant l'exemple donné pour
--    la Truite dans l'issue.
--  - « Cyprins » n'est pas une espèce de la table `species` : regroupe Gardon, Tanche,
--    Brème commune, Rotengle, Vandoise (liste indicative, à affiner).
--  - « Espèce remarquable » et « Navigateur » (grands cours d'eau) restent `active = false`
--    : listes explicitement « à cadrer avec le MO » dans l'issue, non figées. Activer en
--    mettant à jour `rule_params` puis `active = true`, sans changement de code.
--  - « Jour J » (date d'ouverture) : liste de dates à maintenir chaque année dans
--    `rule_params.openingDates` ; une seule date d'exemple insérée ici.

-- Technique manquante au référentiel, citée par le catalogue (famille "Techniques") et
-- déjà par le format enquête terrain (#144) sans jamais avoir été créée.
INSERT INTO public.technique (name, export_as, built_in)
SELECT 'Pêche de la carpe de nuit', 'pechecarpenuit', true
WHERE NOT EXISTS (SELECT 1 FROM public.technique WHERE name = 'Pêche de la carpe de nuit');

-- --------------------------------------------------------------------------------------
-- Records personnels
-- --------------------------------------------------------------------------------------
INSERT INTO public.gamification_badge (code, category, name, description, icon, rule_type, rule_params, tier, annual_reset) VALUES
('RECORD_TAILLE', 'records_personnels', 'Nouveau record personnel — Taille',
 'Capture dépassant le record de taille du pêcheur pour une espèce', '🏆',
 'metric_record', '{"scope":"species_size"}'::jsonb, NULL, false),
('RECORD_POIDS', 'records_personnels', 'Nouveau record personnel — Poids',
 'Capture dépassant le record de poids du pêcheur pour une espèce', '🏆',
 'metric_record', '{"scope":"species_weight"}'::jsonb, NULL, false),
('RECORD_NOMBRE', 'records_personnels', 'Nouveau record personnel — Nombre',
 'Session dépassant le record de nombre de poissons capturés', '🏆',
 'metric_record', '{"scope":"session_catch_count"}'::jsonb, NULL, false),
('PRISE_ECLAIR', 'records_personnels', 'Prise éclair',
 'Première capture dans les 10 premières minutes d''une session', '⚡',
 'session_time_window', '{"mode":"first_catch_after_start","windowMinutes":10}'::jsonb, NULL, false),
('JUSTE_A_TEMPS', 'records_personnels', 'Juste à temps !',
 'Première capture dans les 10 dernières minutes d''une session', '⏱️',
 'session_time_window', '{"mode":"first_catch_before_end","windowMinutes":10}'::jsonb, NULL, false),
('LEVE_TOT', 'records_personnels', 'Lève-tôt',
 'Capture renseignée avant 7h du matin', '🌅',
 'session_time_window', '{"mode":"before_hour","hour":7}'::jsonb, NULL, false);

-- --------------------------------------------------------------------------------------
-- Techniques -- 3 paliers (5/20/50 sorties) par technique nommée + Polyvalent
-- --------------------------------------------------------------------------------------
INSERT INTO public.gamification_badge (code, category, name, description, rule_type, rule_params, tier, annual_reset) VALUES
('TECHNIQUE_MOUCHE_1', 'techniques', 'Pêche à la mouche — Novice', '5 sorties en pêche à la mouche',
 'technique_tier', '{"techniqueName":"Pêche à la mouche","threshold":5}'::jsonb, 1, false),
('TECHNIQUE_MOUCHE_2', 'techniques', 'Pêche à la mouche — Confirmé', '20 sorties en pêche à la mouche',
 'technique_tier', '{"techniqueName":"Pêche à la mouche","threshold":20}'::jsonb, 2, false),
('TECHNIQUE_MOUCHE_3', 'techniques', 'Pêche à la mouche — Expert', '50 sorties en pêche à la mouche',
 'technique_tier', '{"techniqueName":"Pêche à la mouche","threshold":50}'::jsonb, 3, false),
('TECHNIQUE_LEURRE_1', 'techniques', 'Pêche au leurre — Novice', '5 sorties en pêche aux leurres',
 'technique_tier', '{"techniqueName":"Pêche aux leurres","threshold":5}'::jsonb, 1, false),
('TECHNIQUE_LEURRE_2', 'techniques', 'Pêche au leurre — Confirmé', '20 sorties en pêche aux leurres',
 'technique_tier', '{"techniqueName":"Pêche aux leurres","threshold":20}'::jsonb, 2, false),
('TECHNIQUE_LEURRE_3', 'techniques', 'Pêche au leurre — Expert', '50 sorties en pêche aux leurres',
 'technique_tier', '{"techniqueName":"Pêche aux leurres","threshold":50}'::jsonb, 3, false),
('TECHNIQUE_COUP_1', 'techniques', 'Pêche au coup — Novice', '5 sorties en pêche au coup',
 'technique_tier', '{"techniqueName":"Pêche au coup","threshold":5}'::jsonb, 1, false),
('TECHNIQUE_COUP_2', 'techniques', 'Pêche au coup — Confirmé', '20 sorties en pêche au coup',
 'technique_tier', '{"techniqueName":"Pêche au coup","threshold":20}'::jsonb, 2, false),
('TECHNIQUE_COUP_3', 'techniques', 'Pêche au coup — Expert', '50 sorties en pêche au coup',
 'technique_tier', '{"techniqueName":"Pêche au coup","threshold":50}'::jsonb, 3, false),
('TECHNIQUE_CARPE_1', 'techniques', 'Pêche de la carpe — Novice', '5 sorties en pêche de la carpe de nuit',
 'technique_tier', '{"techniqueName":"Pêche de la carpe de nuit","threshold":5}'::jsonb, 1, false),
('TECHNIQUE_CARPE_2', 'techniques', 'Pêche de la carpe — Confirmé', '20 sorties en pêche de la carpe de nuit',
 'technique_tier', '{"techniqueName":"Pêche de la carpe de nuit","threshold":20}'::jsonb, 2, false),
('TECHNIQUE_CARPE_3', 'techniques', 'Pêche de la carpe — Expert', '50 sorties en pêche de la carpe de nuit',
 'technique_tier', '{"techniqueName":"Pêche de la carpe de nuit","threshold":50}'::jsonb, 3, false),
('TECHNIQUE_VIF_1', 'techniques', 'Pêche au vif — Novice', '5 sorties en pêche au vif',
 'technique_tier', '{"techniqueName":"Pêche au vif / poser","threshold":5}'::jsonb, 1, false),
('TECHNIQUE_VIF_2', 'techniques', 'Pêche au vif — Confirmé', '20 sorties en pêche au vif',
 'technique_tier', '{"techniqueName":"Pêche au vif / poser","threshold":20}'::jsonb, 2, false),
('TECHNIQUE_VIF_3', 'techniques', 'Pêche au vif — Expert', '50 sorties en pêche au vif',
 'technique_tier', '{"techniqueName":"Pêche au vif / poser","threshold":50}'::jsonb, 3, false),
('TECHNIQUE_POLYVALENT', 'techniques', 'Polyvalent', 'Au moins 5 sorties dans toutes les techniques',
 'technique_versatility', '{"threshold":5}'::jsonb, NULL, false);

-- --------------------------------------------------------------------------------------
-- Espèces
-- --------------------------------------------------------------------------------------
INSERT INTO public.gamification_badge (code, category, name, description, icon, rule_type, rule_params, tier, annual_reset, active) VALUES
('ESPECE_REMARQUABLE', 'especes', 'Espèce remarquable',
 'Capture d''une espèce peu commune -- liste à valider avec le MO, variable selon les départements', '⭐',
 'remarkable_species_catch',
 '{"speciesNames":["Ombre commun","Vandoise","Toxostome"],"pendingMoValidation":true}'::jsonb,
 NULL, false, false);

INSERT INTO public.gamification_badge (code, category, name, description, icon, rule_type, rule_params, tier, annual_reset) VALUES
('COLLECTIONNEUR_1', 'especes', 'Collectionneur — 5 espèces', '5 espèces différentes capturées', '🎯',
 'species_diversity', '{"threshold":5}'::jsonb, 1, false),
('COLLECTIONNEUR_2', 'especes', 'Collectionneur — 10 espèces', '10 espèces différentes capturées', '🎯',
 'species_diversity', '{"threshold":10}'::jsonb, 2, false),
('COLLECTIONNEUR_3', 'especes', 'Collectionneur — 20 espèces', '20 espèces différentes capturées', '🎯',
 'species_diversity', '{"threshold":20}'::jsonb, 3, false),
('PHOTOGRAPHE', 'especes', 'Photographe', 'Au moins 10 captures avec photo', '📷',
 'photo_count', '{"threshold":10}'::jsonb, NULL, false);

-- Badges par espèce, réinitialisés chaque année (annual_reset) -- 3 paliers 5/15/35.
INSERT INTO public.gamification_badge (code, category, name, description, rule_type, rule_params, tier, annual_reset) VALUES
('ESPECE_TRUITE_1', 'especes', 'Truite — 5 captures', '5 truites capturées cette année',
 'species_annual_tier', '{"speciesNames":["Truite"],"threshold":5}'::jsonb, 1, true),
('ESPECE_TRUITE_2', 'especes', 'Truite — 15 captures', '15 truites capturées cette année',
 'species_annual_tier', '{"speciesNames":["Truite"],"threshold":15}'::jsonb, 2, true),
('ESPECE_TRUITE_3', 'especes', 'Truite — 35 captures', '35 truites capturées cette année',
 'species_annual_tier', '{"speciesNames":["Truite"],"threshold":35}'::jsonb, 3, true),

('ESPECE_BROCHET_1', 'especes', 'Brochet — 5 captures', '5 brochets capturés cette année',
 'species_annual_tier', '{"speciesNames":["Brochet"],"threshold":5}'::jsonb, 1, true),
('ESPECE_BROCHET_2', 'especes', 'Brochet — 15 captures', '15 brochets capturés cette année',
 'species_annual_tier', '{"speciesNames":["Brochet"],"threshold":15}'::jsonb, 2, true),
('ESPECE_BROCHET_3', 'especes', 'Brochet — 35 captures', '35 brochets capturés cette année',
 'species_annual_tier', '{"speciesNames":["Brochet"],"threshold":35}'::jsonb, 3, true),

('ESPECE_SANDRE_1', 'especes', 'Sandre — 5 captures', '5 sandres capturés cette année',
 'species_annual_tier', '{"speciesNames":["Sandre"],"threshold":5}'::jsonb, 1, true),
('ESPECE_SANDRE_2', 'especes', 'Sandre — 15 captures', '15 sandres capturés cette année',
 'species_annual_tier', '{"speciesNames":["Sandre"],"threshold":15}'::jsonb, 2, true),
('ESPECE_SANDRE_3', 'especes', 'Sandre — 35 captures', '35 sandres capturés cette année',
 'species_annual_tier', '{"speciesNames":["Sandre"],"threshold":35}'::jsonb, 3, true),

('ESPECE_CARPE_1', 'especes', 'Carpe — 5 captures', '5 carpes communes capturées cette année',
 'species_annual_tier', '{"speciesNames":["Carpe commune"],"threshold":5}'::jsonb, 1, true),
('ESPECE_CARPE_2', 'especes', 'Carpe — 15 captures', '15 carpes communes capturées cette année',
 'species_annual_tier', '{"speciesNames":["Carpe commune"],"threshold":15}'::jsonb, 2, true),
('ESPECE_CARPE_3', 'especes', 'Carpe — 35 captures', '35 carpes communes capturées cette année',
 'species_annual_tier', '{"speciesNames":["Carpe commune"],"threshold":35}'::jsonb, 3, true),

('ESPECE_SILURE_1', 'especes', 'Silure — 5 captures', '5 silures capturés cette année',
 'species_annual_tier', '{"speciesNames":["Silure glane"],"threshold":5}'::jsonb, 1, true),
('ESPECE_SILURE_2', 'especes', 'Silure — 15 captures', '15 silures capturés cette année',
 'species_annual_tier', '{"speciesNames":["Silure glane"],"threshold":15}'::jsonb, 2, true),
('ESPECE_SILURE_3', 'especes', 'Silure — 35 captures', '35 silures capturés cette année',
 'species_annual_tier', '{"speciesNames":["Silure glane"],"threshold":35}'::jsonb, 3, true),

('ESPECE_PERCHE_1', 'especes', 'Perche — 5 captures', '5 perches capturées cette année',
 'species_annual_tier', '{"speciesNames":["Perche"],"threshold":5}'::jsonb, 1, true),
('ESPECE_PERCHE_2', 'especes', 'Perche — 15 captures', '15 perches capturées cette année',
 'species_annual_tier', '{"speciesNames":["Perche"],"threshold":15}'::jsonb, 2, true),
('ESPECE_PERCHE_3', 'especes', 'Perche — 35 captures', '35 perches capturées cette année',
 'species_annual_tier', '{"speciesNames":["Perche"],"threshold":35}'::jsonb, 3, true),

('ESPECE_CYPRINS_1', 'especes', 'Cyprins — 5 captures', '5 cyprinidés capturés cette année',
 'species_annual_tier',
 '{"speciesNames":["Gardon","Tanche","Brème commune","Rotengle","Vandoise"],"threshold":5}'::jsonb, 1, true),
('ESPECE_CYPRINS_2', 'especes', 'Cyprins — 15 captures', '15 cyprinidés capturés cette année',
 'species_annual_tier',
 '{"speciesNames":["Gardon","Tanche","Brème commune","Rotengle","Vandoise"],"threshold":15}'::jsonb, 2, true),
('ESPECE_CYPRINS_3', 'especes', 'Cyprins — 35 captures', '35 cyprinidés capturés cette année',
 'species_annual_tier',
 '{"speciesNames":["Gardon","Tanche","Brème commune","Rotengle","Vandoise"],"threshold":35}'::jsonb, 3, true),

('ESPECE_ALOSE_1', 'especes', 'Alose — 5 captures', '5 aloses capturées cette année',
 'species_annual_tier',
 '{"speciesNames":["Alose feinte atlantique","Alose feinte de Méditerranée","Grande alose"],"threshold":5}'::jsonb, 1, true),
('ESPECE_ALOSE_2', 'especes', 'Alose — 15 captures', '15 aloses capturées cette année',
 'species_annual_tier',
 '{"speciesNames":["Alose feinte atlantique","Alose feinte de Méditerranée","Grande alose"],"threshold":15}'::jsonb, 2, true),
('ESPECE_ALOSE_3', 'especes', 'Alose — 35 captures', '35 aloses capturées cette année',
 'species_annual_tier',
 '{"speciesNames":["Alose feinte atlantique","Alose feinte de Méditerranée","Grande alose"],"threshold":35}'::jsonb, 3, true),

('ESPECE_COREGONE_1', 'especes', 'Corégone — 5 captures', '5 corégones capturés cette année',
 'species_annual_tier', '{"speciesNames":["Corégone (féra)"],"threshold":5}'::jsonb, 1, true),
('ESPECE_COREGONE_2', 'especes', 'Corégone — 15 captures', '15 corégones capturés cette année',
 'species_annual_tier', '{"speciesNames":["Corégone (féra)"],"threshold":15}'::jsonb, 2, true),
('ESPECE_COREGONE_3', 'especes', 'Corégone — 35 captures', '35 corégones capturés cette année',
 'species_annual_tier', '{"speciesNames":["Corégone (féra)"],"threshold":35}'::jsonb, 3, true),

('ESPECE_BLACKBASS_1', 'especes', 'Black-bass — 5 captures', '5 black-bass capturés cette année',
 'species_annual_tier', '{"speciesNames":["Black-bass"],"threshold":5}'::jsonb, 1, true),
('ESPECE_BLACKBASS_2', 'especes', 'Black-bass — 15 captures', '15 black-bass capturés cette année',
 'species_annual_tier', '{"speciesNames":["Black-bass"],"threshold":15}'::jsonb, 2, true),
('ESPECE_BLACKBASS_3', 'especes', 'Black-bass — 35 captures', '35 black-bass capturés cette année',
 'species_annual_tier', '{"speciesNames":["Black-bass"],"threshold":35}'::jsonb, 3, true),

('ESPECE_ASPE_1', 'especes', 'Aspe — 5 captures', '5 aspes capturés cette année',
 'species_annual_tier', '{"speciesNames":["Aspe"],"threshold":5}'::jsonb, 1, true),
('ESPECE_ASPE_2', 'especes', 'Aspe — 15 captures', '15 aspes capturés cette année',
 'species_annual_tier', '{"speciesNames":["Aspe"],"threshold":15}'::jsonb, 2, true),
('ESPECE_ASPE_3', 'especes', 'Aspe — 35 captures', '35 aspes capturés cette année',
 'species_annual_tier', '{"speciesNames":["Aspe"],"threshold":35}'::jsonb, 3, true),

('ESPECE_OMBRE_1', 'especes', 'Ombre — 5 captures', '5 ombres communs capturés cette année',
 'species_annual_tier', '{"speciesNames":["Ombre commun"],"threshold":5}'::jsonb, 1, true),
('ESPECE_OMBRE_2', 'especes', 'Ombre — 15 captures', '15 ombres communs capturés cette année',
 'species_annual_tier', '{"speciesNames":["Ombre commun"],"threshold":15}'::jsonb, 2, true),
('ESPECE_OMBRE_3', 'especes', 'Ombre — 35 captures', '35 ombres communs capturés cette année',
 'species_annual_tier', '{"speciesNames":["Ombre commun"],"threshold":35}'::jsonb, 3, true);

-- --------------------------------------------------------------------------------------
-- Fidélité
-- --------------------------------------------------------------------------------------
INSERT INTO public.gamification_badge (code, category, name, description, rule_type, rule_params, tier, annual_reset) VALUES
('DISCIPLINE', 'fidelite', 'Discipliné', '4 semaines consécutives avec au moins une session',
 'consecutive_weeks', '{"threshold":4}'::jsonb, NULL, false),
('FIDELITE_1', 'fidelite', 'Martin pêcheur', '10 sessions enregistrées',
 'session_count_tier', '{"threshold":10}'::jsonb, 1, false),
('FIDELITE_2', 'fidelite', 'Pilier de berge', '35 sessions enregistrées',
 'session_count_tier', '{"threshold":35}'::jsonb, 2, false),
('FIDELITE_3', 'fidelite', 'Vétéran des eaux', '100 sessions enregistrées',
 'session_count_tier', '{"threshold":100}'::jsonb, 3, false),
('QUATRE_SAISONS', 'fidelite', 'Quatre saisons', 'Au moins une sortie par saison',
 'season_coverage', '{"threshold":4}'::jsonb, NULL, false),
-- Date d'ouverture 2026 donnée à titre d'exemple -- rule_params.openingDates est à
-- compléter chaque année (aucun changement de code nécessaire).
('JOUR_J', 'fidelite', 'Jour J', 'Session le jour de l''ouverture',
 'opening_day', '{"openingDates":["2026-03-14"]}'::jsonb, NULL, false),
('PERSEVERANT', 'fidelite', 'Persévérant', 'Au moins 10 bredouilles renseignées',
 'bredouille_count', '{"threshold":10}'::jsonb, NULL, false),
('GRAND_CONTRIBUTEUR', 'fidelite', 'Grand contributeur', 'Top 10 % des contributeurs (nombre de sorties)',
 'contributor_percentile', '{"topPercent":10,"minCohortSize":20}'::jsonb, NULL, false);

-- --------------------------------------------------------------------------------------
-- Exploration
-- --------------------------------------------------------------------------------------
INSERT INTO public.gamification_badge (code, category, name, description, icon, rule_type, rule_params, tier, annual_reset) VALUES
('BAROUDEUR', 'exploration', 'Baroudeur', 'Sortie sur au moins 3 départements différents', '🌍',
 'multi_department', '{"threshold":3}'::jsonb, NULL, false),
('EXPLORATEUR', 'exploration', 'Explorateur', 'Pêche sur au moins 3 bassins hydrographiques différents', NULL,
 'basin_diversity', '{"threshold":3}'::jsonb, NULL, false),
('GRIMPEUR', 'exploration', 'Grimpeur', 'Session sur un lac d''altitude supérieure à 1000 m', '🏔️',
 'altitude_threshold', '{"minAltitude":1000}'::jsonb, NULL, false),
('TOUT_TERRAIN', 'exploration', 'Tout-terrain', 'Au moins 5 sessions en lac et 5 sessions en cours d''eau', NULL,
 'mixed_environment', '{"threshold":5}'::jsonb, NULL, false),
('LACUSTRE', 'exploration', 'Lacustre', 'Au moins 20 sessions sur un même plan d''eau', NULL,
 'single_water_entity_tier', '{"threshold":20,"kind":"STILL"}'::jsonb, NULL, false);

INSERT INTO public.gamification_badge (code, category, name, description, icon, rule_type, rule_params, tier, annual_reset, active) VALUES
('NAVIGATEUR', 'exploration', 'Navigateur',
 '10 sessions en bateau sur un grand cours d''eau -- liste à valider avec le MO', '🌊',
 'boat_river_sessions', '{"waterEntityNames":[],"threshold":10,"pendingMoValidation":true}'::jsonb,
 NULL, false, false);

-- --------------------------------------------------------------------------------------
-- Autres -- attribution manuelle
-- --------------------------------------------------------------------------------------
INSERT INTO public.gamification_badge (code, category, name, description, rule_type, rule_params, tier, annual_reset) VALUES
('TESTEUR_EMERITE', 'autres', 'Testeur émérite', 'Attribué manuellement aux beta-testeurs',
 'manual', NULL, NULL, false);
