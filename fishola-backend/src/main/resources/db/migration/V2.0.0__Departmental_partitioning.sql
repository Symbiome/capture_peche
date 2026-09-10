-- Cloisonnement départemental (#159, suite de #156).
--
-- 1. Le périmètre d'un compte staff (fishola_admin) passait par une liste explicite
--    d'entités hydro (fishola_admin_water_entities, #63). L'écran de choix chargeait
--    tout le référentiel hydro (~181 000 lignes, #134) — l'antipattern OOM de #154.
--    Il passe à une liste de codes département (fishola_admin_departments).
--
-- 2. trip et catch mémorisent le département où l'action s'est produite, calculé par
--    jointure spatiale sur departement.geom (#156) à chaque ajout/édition, avec repli
--    sur water_entity.department quand la sortie / prise n'a pas de position GPS
--    (imports opérateur, saisie manuelle, carnet, mode a posteriori sans position).
--    trip.water_entity_id / catch.trip_id restent la source du rattachement hydro :
--    le département est une donnée complémentaire.
--
-- 3. Les vues d'export exposent le département (colonne en dernière position :
--    CREATE OR REPLACE VIEW ne réordonne pas).


-- ── 1. Périmètre staff exprimé en départements ─────────────────────────────────

CREATE TABLE fishola_admin_departments (
    fishola_admin_id uuid NOT NULL REFERENCES fishola_admin(id) ON DELETE CASCADE,
    department_code character varying(3) NOT NULL,
    CONSTRAINT fishola_admin_departments_pkey PRIMARY KEY (fishola_admin_id, department_code)
);

-- Pas de FK sur department_code : le référentiel est la constante Java
-- Departments.NAMES (même choix que species_by_department, #133) ; la validation
-- des codes se fait côté applicatif via Departments.isValidCode().
COMMENT ON TABLE fishola_admin_departments IS 'Périmètre géographique d''un compte staff, exprimé en codes département INSEE (#159)';

-- Reprise des périmètres existants : département de chaque entité déjà affectée
-- (water_entity.department, dérivé de commune par #154). DISTINCT car plusieurs
-- entités d'un même département ne donnent qu'une ligne de périmètre.
INSERT INTO fishola_admin_departments (fishola_admin_id, department_code)
SELECT DISTINCT fawe.fishola_admin_id, we.department
FROM fishola_admin_water_entities fawe
JOIN water_entity we ON we.id = fawe.water_entity_id
WHERE we.department IS NOT NULL;

DROP TABLE fishola_admin_water_entities;


-- ── 2. Département d'une sortie / d'une prise ──────────────────────────────────

ALTER TABLE trip ADD COLUMN department character varying(3);
ALTER TABLE catch ADD COLUMN department character varying(3);

COMMENT ON COLUMN trip.department IS 'Département INSEE où s''est déroulée la sortie : jointure spatiale departement.geom sur snapped_position / begin_position / end_position, repli water_entity.department (#159)';
COMMENT ON COLUMN catch.department IS 'Département INSEE de la prise : jointure spatiale departement.geom sur position, repli trip.department (#159)';

CREATE INDEX trip_department_idx ON trip (department);
CREATE INDEX catch_department_idx ON catch (department);

-- Rattrapage des données déjà en base. La table departement doit être chargée
-- (scripts/import_departements_parquet.sh) ; sinon le COALESCE retombe entièrement
-- sur water_entity.department, puis trip.department pour les prises.
UPDATE trip t SET department = COALESCE(
    (SELECT d.code
       FROM departement d
      WHERE public.ST_Contains(d.geom, COALESCE(t.snapped_position, t.begin_position, t.end_position))
      LIMIT 1),
    (SELECT we.department FROM water_entity we WHERE we.id = t.water_entity_id));

UPDATE catch c SET department = COALESCE(
    (SELECT d.code
       FROM departement d
      WHERE public.ST_Contains(d.geom, c."position")
      LIMIT 1),
    (SELECT t.department FROM trip t WHERE t.id = c.trip_id));


-- ── 3. Vues d'export : département en dernière colonne ─────────────────────────

CREATE OR REPLACE VIEW public.catchs_export AS
 SELECT 'FISHOLA'::text AS nom_du_projet,
    l.export_as AS nom_du_site,
    (l.export_as || ' : peche amateur'::text) AS nom_de_la_plateforme,
    to_char(t.begin_timestamp, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    u.birth_year AS annee_naissance_utilisateur,
        CASE u.gender
            WHEN 'Male'::public.gender THEN 'H'::text
            WHEN 'Female'::public.gender THEN 'F'::text
            WHEN 'NonBinary'::public.gender THEN '?'::text
            ELSE NULL::text
        END AS sexe_utilisateur,
    to_char(t.begin_timestamp, 'MM'::text) AS mois_de_la_sortie,
    to_char(t.begin_timestamp, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'Embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'Bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.name AS nom_de_la_sortie,
    t.id AS id_sortie,
    tsn.species AS espece_recherchee,
    to_char(t.begin_timestamp, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char(t.end_timestamp, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_timestamp - t.begin_timestamp) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    c.id AS id_capture,
    ct.export_as AS technique_de_peche_par_capture,
    s.export_as AS espece_capturee,
    (c.size * 10) AS longueur_totale_du_poisson,
    (c.automatic_measure * 10) AS longueur_totale_du_poisson_calculee,
    c.weight AS poids_du_poisson,
    public.st_y(c."position") AS latitude_de_la_capture,
    public.st_x(c."position") AS longitude_de_la_capture,
    public.st_y(t.begin_position) AS latitude_debut_de_peche,
    public.st_x(t.begin_position) AS longitude_debut_de_peche,
    public.st_y(t.end_position) AS latitude_fin_de_peche,
    public.st_x(t.end_position) AS longitude_fin_de_peche,
        CASE c.kept
            WHEN true THEN 'non'::text
            WHEN false THEN 'oui'::text
            ELSE NULL::text
        END AS poisson_relache,
    cpju.urls AS url_photos,
    c.sample_id AS id_prelevement,
    w.export_as AS conditions_meteo,
    c.description AS commentaires,
        CASE t.mode
            WHEN 'Live'::public.trip_mode THEN 'En direct'::text
            WHEN 'Afterwards'::public.trip_mode THEN 'A posteriori'::text
            ELSE NULL::text
        END AS mode_de_peche,
    c.quantity AS nombre_de_poissons,
    c.department AS departement
   FROM (((((((((public.trip t
     JOIN public.water_entity l ON ((l.id = t.water_entity_id)))
     LEFT JOIN public.fishola_user u ON ((u.id = t.owner_id)))
     LEFT JOIN public.trip_species_names tsn ON ((tsn.trip_id = t.id)))
     LEFT JOIN public.trip_techniques_names ttn ON ((ttn.trip_id = t.id)))
     LEFT JOIN public.weather w ON ((w.id = t.weather_id)))
     LEFT JOIN public.catch c ON ((t.id = c.trip_id)))
     LEFT JOIN public.technique ct ON ((ct.id = c.technique_id)))
     LEFT JOIN public.species s ON ((s.id = c.species_id)))
     LEFT JOIN public.catch_picture_joined_urls cpju ON ((cpju.catch_id = c.id)))
  WHERE (((t.owner_id IS NULL) OR (u.exclude_from_exports = false)) AND (c.exclude_from_exports = false) AND ((t.collection_method IS DISTINCT FROM 'saisie_pecheur'::public.collection_method)
       OR (t.created_on < ((now() - '168:00:00'::interval) AT TIME ZONE 'Europe/Paris'::text))));

CREATE OR REPLACE VIEW public.catchs_openadom_export AS
 SELECT 'fishola'::text AS nom_du_projet,
    public.normalize_for_export((l.export_as)::character varying) AS nom_du_site,
    public.normalize_for_export(((l.export_as || ':peche amateur'::text))::character varying) AS nom_de_la_plateforme,
    to_char(t.begin_timestamp, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    to_char(t.begin_timestamp, 'MM'::text) AS mois_de_la_sortie,
    to_char(t.begin_timestamp, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.id AS id_sortie,
    public.normalize_for_export((tsn.species)::character varying) AS espece_recherchee,
    to_char(t.begin_timestamp, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char(t.end_timestamp, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_timestamp - t.begin_timestamp) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    c.id AS id_capture,
    public.normalize_for_export((ct.export_as)::character varying) AS technique_de_peche_par_capture,
    public.normalize_for_export((s.export_as)::character varying) AS espece_capturee,
        CASE
            WHEN ((c.edited_size IS NOT NULL) AND (c.edited_size > 0)) THEN c.edited_size
            ELSE (c.size * 10)
        END AS longueur_totale_du_poisson,
    (c.automatic_measure * 10) AS longueur_totale_du_poisson_calculee,
        CASE
            WHEN ((c.edited_weight IS NOT NULL) AND (c.edited_weight > 0)) THEN c.edited_weight
            ELSE c.weight
        END AS poids_du_poisson,
        CASE c.kept
            WHEN true THEN 'non'::text
            WHEN false THEN 'oui'::text
            ELSE NULL::text
        END AS poisson_relache,
    c.sample_id AS id_prelevement,
    public.normalize_for_export((w.export_as)::character varying) AS conditions_meteo,
        CASE t.mode
            WHEN 'Live'::public.trip_mode THEN 'en_direct'::text
            WHEN 'Afterwards'::public.trip_mode THEN 'a_posteriori'::text
            ELSE NULL::text
        END AS mode_de_peche,
        CASE c.exclude_from_exports
            WHEN true THEN 'oui'::text
            ELSE 'non'::text
        END AS a_exclure,
    c.id AS catch_id,
    c.quantity AS nombre_de_poissons,
    c.department AS departement
   FROM (((((((((public.trip t
     JOIN public.water_entity l ON ((l.id = t.water_entity_id)))
     LEFT JOIN public.fishola_user u ON ((u.id = t.owner_id)))
     LEFT JOIN public.trip_species_names tsn ON ((tsn.trip_id = t.id)))
     LEFT JOIN public.trip_techniques_names ttn ON ((ttn.trip_id = t.id)))
     LEFT JOIN public.weather w ON ((w.id = t.weather_id)))
     LEFT JOIN public.catch c ON ((t.id = c.trip_id)))
     LEFT JOIN public.technique ct ON ((ct.id = c.technique_id)))
     LEFT JOIN public.species s ON ((s.id =
        CASE
            WHEN (c.edited_species_id IS NOT NULL) THEN c.edited_species_id
            ELSE c.species_id
        END)))
     LEFT JOIN public.catch_picture_joined_urls cpju ON ((cpju.catch_id = c.id)))
  WHERE (((t.owner_id IS NULL) OR (u.exclude_from_exports = false)) AND ((t.collection_method IS DISTINCT FROM 'saisie_pecheur'::public.collection_method)
       OR (t.created_on < ((now() - '168:00:00'::interval) AT TIME ZONE 'Europe/Paris'::text))));

CREATE OR REPLACE VIEW public.personal_catchs_export AS
 SELECT l.export_as AS nom_du_site,
    to_char(t.begin_timestamp, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    to_char(t.begin_timestamp, 'MM'::text) AS mois_de_la_sortie,
    to_char(t.begin_timestamp, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.name AS nom_de_la_sortie,
    tsn.species AS espece_recherchee,
    to_char(t.begin_timestamp, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char(t.end_timestamp, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_timestamp - t.begin_timestamp) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    ct.export_as AS technique_de_peche_par_capture,
    s.export_as AS espece_capturee,
    (c.size * 10) AS longueur_totale_du_poisson,
    (c.automatic_measure * 10) AS longueur_totale_du_poisson_calculee,
    c.weight AS poids_du_poisson,
    public.st_y(c."position") AS latitude_de_la_capture,
    public.st_x(c."position") AS longitude_de_la_capture,
    public.st_y(t.begin_position) AS latitude_debut_de_peche,
    public.st_x(t.begin_position) AS longitude_debut_de_peche,
    public.st_y(t.end_position) AS latitude_fin_de_peche,
    public.st_x(t.end_position) AS longitude_fin_de_peche,
        CASE c.kept
            WHEN true THEN 'non'::text
            WHEN false THEN 'oui'::text
            ELSE NULL::text
        END AS poisson_relache,
    cpju.urls AS url_photos,
    c.sample_id AS id_prelevement,
    w.export_as AS conditions_meteo,
    c.description AS commentaires,
        CASE t.mode
            WHEN 'Live'::public.trip_mode THEN 'En direct'::text
            WHEN 'Afterwards'::public.trip_mode THEN 'A posteriori'::text
            ELSE NULL::text
        END AS mode_de_peche,
    c.quantity AS nombre_de_poissons,
    c.department AS departement
   FROM (((((((((public.trip t
     JOIN public.water_entity l ON ((l.id = t.water_entity_id)))
     LEFT JOIN public.fishola_user u ON ((u.id = t.owner_id)))
     LEFT JOIN public.trip_species_names tsn ON ((tsn.trip_id = t.id)))
     LEFT JOIN public.trip_techniques_names ttn ON ((ttn.trip_id = t.id)))
     LEFT JOIN public.weather w ON ((w.id = t.weather_id)))
     LEFT JOIN public.catch c ON ((t.id = c.trip_id)))
     LEFT JOIN public.technique ct ON ((ct.id = c.technique_id)))
     LEFT JOIN public.species s ON ((s.id = c.species_id)))
     LEFT JOIN public.catch_picture_joined_urls cpju ON ((cpju.catch_id = c.id)))
  WHERE (t.hidden = false);
