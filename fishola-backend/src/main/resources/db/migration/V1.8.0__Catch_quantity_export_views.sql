-- `catch.quantity` existe déjà depuis V1.2.0 (Import_csv_socle) pour le pipeline
-- d'import CSV opérateur ("captures en lot : nombre d'individus [...] quantity
-- par défaut 1 = capture individuelle"), mais n'était utilisé que par ce
-- pipeline (ImportDao, insertion SQL directe). Le pêcheur ne pouvait pas
-- saisir lui-même un lot de poissons de taille homogène (ex. 15 perches de
-- 10 cm) : #148 étend la colonne existante au parcours de saisie pêcheur
-- (CatchBean / TripResource), sans rien changer côté import CSV.
--
-- Vues d'export : on ajoute la quantité en dernière colonne (CREATE OR REPLACE
-- VIEW ne peut ni réordonner ni retirer les colonnes existantes), sans quoi
-- ces exports continueraient de compter un lot comme un seul poisson.
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
    c.quantity AS nombre_de_poissons
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
    c.quantity AS nombre_de_poissons
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
    c.quantity AS nombre_de_poissons
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
