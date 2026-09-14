-- Consolidation date+heure -> timestamp. Les sorties stockaient un jour et deux
-- heures (début/fin) comme colonnes indépendantes : rien n'empêchait une durée
-- calculée en soustrayant directement les deux TIME de donner un résultat
-- négatif pour une sortie à cheval sur minuit (fin < début en valeur brute).
-- Même souci côté capture : catch_time seul ne porte aucun jour, impossible de
-- savoir si une capture après minuit appartient à la sortie ou au lendemain. On
-- remplace par de vrais timestamps, qui portent la date ET l'heure sans
-- ambiguïté.

ALTER TABLE public.trip
    ADD COLUMN begin_timestamp timestamp without time zone,
    ADD COLUMN end_timestamp timestamp without time zone;

-- Repli sur le jour suivant pour la fin si l'heure de fin est antérieure à
-- l'heure de début (sortie à cheval sur minuit) : même règle que celle
-- qu'appliquait TripResource#toTripLight avant cette migration.
UPDATE public.trip
SET begin_timestamp = day + start_time,
    end_timestamp = CASE
        WHEN end_time < start_time THEN (day + end_time) + interval '1 day'
        ELSE day + end_time
    END;

ALTER TABLE public.trip
    ALTER COLUMN begin_timestamp SET NOT NULL,
    ALTER COLUMN end_timestamp SET NOT NULL;

COMMENT ON COLUMN public.trip.begin_timestamp IS 'Date et heure de début de la sortie';
COMMENT ON COLUMN public.trip.end_timestamp IS 'Date et heure de fin de la sortie (jour suivant le début si la sortie passe minuit)';

ALTER TABLE public.catch
    ADD COLUMN catch_timestamp timestamp without time zone;

-- Une capture ne porte qu'une heure (catch_time), jamais un jour : on la
-- rattache au jour de début de la sortie, ou au lendemain si c'est la seule
-- façon de la faire tomber dans la fenêtre [début, fin] de la sortie (capture
-- après minuit sur une sortie à cheval sur minuit).
WITH combos AS (
    SELECT c.id AS catch_id,
           (t.begin_timestamp::date + c.catch_time) AS same_day,
           (t.begin_timestamp::date + interval '1 day' + c.catch_time) AS next_day,
           t.begin_timestamp AS begin_ts,
           t.end_timestamp AS end_ts
    FROM public.catch c
    JOIN public.trip t ON t.id = c.trip_id
    WHERE c.catch_time IS NOT NULL
)
UPDATE public.catch c
SET catch_timestamp = CASE
    WHEN combos.same_day BETWEEN combos.begin_ts AND combos.end_ts THEN combos.same_day
    WHEN combos.next_day BETWEEN combos.begin_ts AND combos.end_ts THEN combos.next_day
    ELSE combos.same_day
END
FROM combos
WHERE combos.catch_id = c.id;

COMMENT ON COLUMN public.catch.catch_timestamp IS 'Date et heure de la capture (déduite de l''heure saisie et de la fenêtre de la sortie) ; NULL si aucune heure saisie';

-- Vues d'export : reprises à l'identique, seule la source de la date/heure
-- change. (t.end_timestamp - t.begin_timestamp) corrige au passage un calcul de
-- durée faux pour les sorties à cheval sur minuit (l'ancien (t.end_time -
-- t.start_time) donnait un intervalle négatif dans ce cas).
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
        END AS mode_de_peche
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
    c.id AS catch_id
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
        END AS mode_de_peche
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

ALTER TABLE public.trip
    DROP COLUMN day,
    DROP COLUMN start_time,
    DROP COLUMN end_time;

ALTER TABLE public.catch
    DROP COLUMN catch_time;
