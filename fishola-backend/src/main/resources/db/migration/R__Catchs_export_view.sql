---
-- #%L
-- Fishola :: Backend
-- %%
-- Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

-- Suppression des vues d'export

DROP VIEW IF EXISTS catchs_export;
DROP VIEW IF EXISTS catchs_openadom_export;
DROP VIEW IF EXISTS personal_catchs_export;

-- VUE : trip_species_names

CREATE OR REPLACE VIEW trip_species_names AS
SELECT
    tes.trip_id AS trip_id,
    string_agg(s.export_as, ',' ORDER BY s.export_as) AS species
FROM trip_expected_species tes
INNER JOIN species s
    ON s.id = tes.species_id
GROUP BY tes.trip_id;
COMMENT ON VIEW trip_species_names IS 'Permet d''avoir, pour chaque sortie, la liste des espèces recherchées séparées par des virgules';

-- VUE : trip_techniques_names

CREATE OR REPLACE VIEW trip_techniques_names AS
SELECT
    tt.trip_id AS trip_id,
    string_agg(t.export_as, ',' ORDER BY t.export_as) AS techniques
FROM trip_techniques tt
INNER JOIN technique t
    ON t.id = tt.technique_id
GROUP BY tt.trip_id;
COMMENT ON VIEW trip_techniques_names IS 'Permet d''avoir, pour chaque sortie, la liste des techniques séparées par des virgules';

-- VUE : catch_picture_url (obsolète)

DROP VIEW IF EXISTS catch_picture_url;

-- VUE : catch_picture_urls

CREATE OR REPLACE VIEW catch_picture_urls AS
SELECT
    c.id AS catch_id,
    ('${baseUrl}/api/v1/pictures/' || cp.catch_id || '/' || cp.picture_index) AS url
FROM catch c
INNER JOIN catch_picture cp
    ON cp.catch_id = c.id
UNION
SELECT
    c.id AS catch_id,
    ('${baseUrl}/api/v1/pictures/measure/' || cmp.catch_id) AS url
FROM catch c
INNER JOIN catch_measurement_picture cmp
    ON cmp.catch_id = c.id;

COMMENT ON VIEW catch_picture_urls IS 'Permet d''avoir, pour chaque capture, les URLs pour télécharger les images';

-- VUE : catch_picture_urls

CREATE OR REPLACE VIEW catch_picture_joined_urls AS
SELECT
    cpu.catch_id,
    string_agg(cpu.url, ',') as urls
FROM catch_picture_urls cpu
GROUP BY cpu.catch_id;

COMMENT ON VIEW catch_picture_joined_urls IS 'Permet d''avoir, pour chaque capture, la concaténation des URLs pour télécharger les images';

-- VUE : catchs_export
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE OR REPLACE FUNCTION normalize_for_export(target VARCHAR) returns VARCHAR language plpgsql as
$$
begin
    return REPLACE(REPLACE(REPLACE(REPLACE(lower(unaccent(target)),' _', '_'),' ', '_'), '-', '_'), ':', '_');
end;
$$;

CREATE VIEW catchs_export AS
SELECT
    'FISHOLA' AS nom_du_projet,
    l.export_as AS nom_du_site,
    (l.export_as || ' : peche amateur') AS nom_de_la_plateforme,
    to_char(t.day, 'DD/MM/YYYY') AS date_de_la_sortie,
    u.id AS id_login,
    u.birth_year AS annee_naissance_utilisateur,
    CASE u.gender WHEN 'Male' THEN 'H'
                  WHEN 'Female' THEN 'F'
                  WHEN 'NonBinary' THEN '?'
                  END AS sexe_utilisateur,
    to_char(t.day, 'MM') AS mois_de_la_sortie,
    to_char(t.day, 'YYYY') AS annee_de_la_sortie,
    CASE t.type WHEN 'Craft' THEN 'Embarcation'
                WHEN 'Border' THEN 'Bord'
                END AS type_de_peche,
    t.name AS nom_de_la_sortie,
    t.id AS id_sortie,
    tsn.species AS espece_recherchee,
    to_char(t.start_time, 'HH24:MI:SS') AS debut_de_peche,
    to_char(t.end_time, 'HH24:MI:SS') AS fin_de_peche,
    (t.end_time - t.start_time) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    c.id AS id_capture,
    ct.export_as AS technique_de_peche_par_capture,
    s.export_as AS espece_capturee,
    c.size * 10 AS longueur_totale_du_poisson,
    c.automatic_measure * 10 AS longueur_totale_du_poisson_calculee,
    c.weight AS poids_du_poisson,
    c.latitude AS latitude_de_la_capture,
    c.longitude AS longitude_de_la_capture,
    t.begin_latitude AS latitude_debut_de_peche,
    t.begin_longitude AS longitude_debut_de_peche,
    t.end_latitude AS latitude_fin_de_peche,
    t.end_longitude AS longitude_fin_de_peche,
    CASE c.kept WHEN true THEN 'non'
                WHEN false THEN 'oui'
                END AS poisson_relache,
    cpju.urls AS url_photos,
    c.sample_id AS id_prelevement,
    w.export_as AS conditions_meteo,
    c.description AS commentaires,
    CASE t.mode WHEN 'Live' THEN 'En direct'
                WHEN 'Afterwards' THEN 'A posteriori'
                END AS mode_de_peche
FROM trip t
INNER JOIN lake l ON l.id = t.lake_id
LEFT JOIN fishola_user u ON u.id = t.owner_id
LEFT JOIN trip_species_names tsn ON tsn.trip_id = t.id
LEFT JOIN trip_techniques_names ttn ON ttn.trip_id = t.id
LEFT JOIN weather w ON w.id = t.weather_id
LEFT JOIN catch c ON t.id = c.trip_id
LEFT JOIN technique ct ON ct.id = c.technique_id
LEFT JOIN species s ON s.id = c.species_id
LEFT JOIN catch_picture_joined_urls cpju ON cpju.catch_id = c.id
WHERE (t.owner_id IS NULL OR u.exclude_from_exports = false)
AND c.exclude_from_exports = false
AND t.created_on < ((now() - INTERVAL '${exportSafeHours} hours') at time zone 'Europe/Paris');

COMMENT ON VIEW catchs_export IS 'Génère le CSV pour les exports';

CREATE VIEW catchs_openadom_export AS
SELECT
    'FISHOLA' AS nom_du_projet,
    normalize_for_export(l.export_as) AS nom_du_site,
    normalize_for_export(l.export_as || ':peche amateur') AS nom_de_la_plateforme,
    to_char(t.day, 'DD/MM/YYYY') AS date_de_la_sortie,
    u.id AS id_login,
    to_char(t.day, 'MM') AS mois_de_la_sortie,
    to_char(t.day, 'YYYY') AS annee_de_la_sortie,
    CASE t.type WHEN 'Craft' THEN 'embarcation'
                WHEN 'Border' THEN 'bord'
                END AS type_de_peche,
    t.id AS id_sortie,
    normalize_for_export(tsn.species) AS espece_recherchee,
    to_char(t.start_time, 'HH24:MI:SS') AS debut_de_peche,
    to_char(t.end_time, 'HH24:MI:SS') AS fin_de_peche,
    (t.end_time - t.start_time) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    c.id AS id_capture,
    normalize_for_export(ct.export_as) AS technique_de_peche_par_capture,
    normalize_for_export(s.export_as) AS espece_capturee,
    c.size * 10 AS longueur_totale_du_poisson,
    c.automatic_measure * 10 AS longueur_totale_du_poisson_calculee,
    c.weight AS poids_du_poisson,
    CASE c.kept WHEN true THEN 'non'
                WHEN false THEN 'oui'
                END AS poisson_relache,
    c.sample_id AS id_prelevement,
    normalize_for_export(w.export_as) AS conditions_meteo,
    CASE t.mode WHEN 'Live' THEN 'en_direct'
                WHEN 'Afterwards' THEN 'a_posteriori'
                END AS mode_de_peche,
    CASE c.exclude_from_exports WHEN true THEN 'oui' ELSE 'non' END as a_exclure
FROM trip t
INNER JOIN lake l ON l.id = t.lake_id
LEFT JOIN fishola_user u ON u.id = t.owner_id
LEFT JOIN trip_species_names tsn ON tsn.trip_id = t.id
LEFT JOIN trip_techniques_names ttn ON ttn.trip_id = t.id
LEFT JOIN weather w ON w.id = t.weather_id
LEFT JOIN catch c ON t.id = c.trip_id
LEFT JOIN technique ct ON ct.id = c.technique_id
LEFT JOIN species s ON s.id = c.species_id
LEFT JOIN catch_picture_joined_urls cpju ON cpju.catch_id = c.id
WHERE (t.owner_id IS NULL OR u.exclude_from_exports = false)
AND t.created_on < ((now() - INTERVAL '${exportSafeHours} hours') at time zone 'Europe/Paris');

COMMENT ON VIEW catchs_openadom_export IS 'Génère le CSV pour les exports OpenAdom';

-- On peut ensuite extraire l'ensemble dans du CSV via l'une des 2 commandes suivante :
--   COPY (select * from catchs_openadom_export) TO '/tmp/catchs.csv' DELIMITER ';' CSV HEADER;
-- ou
--   psql -h 172.17.0.2 -U postgres fishola -A -F";" -c "select * from catchs_export" | head -n -1 > /tmp/catchs.csv


-- VUE : personal_catchs_export

CREATE VIEW personal_catchs_export AS
SELECT
    l.export_as AS nom_du_site,
    to_char(t.day, 'DD/MM/YYYY') AS date_de_la_sortie,
    u.id AS id_login,
    to_char(t.day, 'MM') AS mois_de_la_sortie,
    to_char(t.day, 'YYYY') AS annee_de_la_sortie,
    CASE t.type WHEN 'Craft' THEN 'Embarcation'
                WHEN 'Border' THEN 'Bord'
                END AS type_de_peche,
    t.name AS nom_de_la_sortie,
    tsn.species AS espece_recherchee,
    to_char(t.start_time, 'HH24:MI:SS') AS debut_de_peche,
    to_char(t.end_time, 'HH24:MI:SS') AS fin_de_peche,
    (t.end_time - t.start_time) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    ct.export_as AS technique_de_peche_par_capture,
    s.export_as AS espece_capturee,
    c.size * 10 AS longueur_totale_du_poisson,
    c.automatic_measure * 10 AS longueur_totale_du_poisson_calculee,
    c.weight AS poids_du_poisson,
    c.latitude AS latitude_de_la_capture,
    c.longitude AS longitude_de_la_capture,
    t.begin_latitude AS latitude_debut_de_peche,
    t.begin_longitude AS longitude_debut_de_peche,
    t.end_latitude AS latitude_fin_de_peche,
    t.end_longitude AS longitude_fin_de_peche,
    CASE c.kept WHEN true THEN 'non'
                WHEN false THEN 'oui'
                END AS poisson_relache,
    cpju.urls AS url_photos,
    c.sample_id AS id_prelevement,
    w.export_as AS conditions_meteo,
    c.description AS commentaires,
    CASE t.mode WHEN 'Live' THEN 'En direct'
                WHEN 'Afterwards' THEN 'A posteriori'
                END AS mode_de_peche
FROM trip t
INNER JOIN lake l ON l.id = t.lake_id
LEFT JOIN fishola_user u ON u.id = t.owner_id
LEFT JOIN trip_species_names tsn ON tsn.trip_id = t.id
LEFT JOIN trip_techniques_names ttn ON ttn.trip_id = t.id
LEFT JOIN weather w ON w.id = t.weather_id
LEFT JOIN catch c ON t.id = c.trip_id
LEFT JOIN technique ct ON ct.id = c.technique_id
LEFT JOIN species s ON s.id = c.species_id
LEFT JOIN catch_picture_joined_urls cpju ON cpju.catch_id = c.id
WHERE t.hidden = false
;

COMMENT ON VIEW personal_catchs_export IS 'Génère le CSV pour les exports (sans filtre) : À filtrer ensuite sur l''utilisateur';

