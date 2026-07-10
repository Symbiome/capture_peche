---
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
---

-- Consolidated baseline schema, replacing the full V0.0.0-V0.4.0 migration history.
-- Generated via `pg_dump --schema-only` against the fully-migrated dev database and
-- hand-cleaned: dropped psql-only \restrict meta-commands, OWNER TO statements,
-- flyway_schema_history (Flyway manages that table itself), and the
-- postgis_tiger_geocoder/postgis_topology extensions (unused by this app, only
-- present because the postgis/postgis Docker image enables them by default).
--
-- No seed/reference data (species, techniques, weather, editorial...) is included;
-- that is intentionally a separate, later migration.

CREATE EXTENSION IF NOT EXISTS fuzzystrmatch WITH SCHEMA public;

--
-- Name: EXTENSION fuzzystrmatch; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION fuzzystrmatch IS 'determine similarities and distance between strings';

--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;

--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry and geography spatial types and functions';

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

--
-- Name: device_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.device_type AS ENUM (
    'web',
    'application'
);



--
-- Name: gender; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.gender AS ENUM (
    'Male',
    'Female',
    'NonBinary'
);



--
-- Name: TYPE gender; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TYPE public.gender IS 'Genres de l''utilisateur';

--
-- Name: licence_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.licence_type AS ENUM (
    'PDF',
    'JPEG',
    'PNG'
);



--
-- Name: maillage; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.maillage AS ENUM (
    'MAILLEE',
    'NON_MAILLEE',
    'NON_DEFINI'
);



--
-- Name: river_bank; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.river_bank AS ENUM (
    'GAUCHE',
    'DROITE',
    'INDETERMINE'
);



--
-- Name: social_reaction; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.social_reaction AS ENUM (
    'LIKE',
    'LOVE',
    'LETS_MEET'
);



--
-- Name: trip_mode; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.trip_mode AS ENUM (
    'Live',
    'Afterwards'
);



--
-- Name: trip_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.trip_type AS ENUM (
    'Border',
    'Craft'
);



--
-- Name: TYPE trip_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TYPE public.trip_type IS 'Types de sortie';

--
-- Name: water_entity_kind; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.water_entity_kind AS ENUM (
    'STILL',
    'FLOWING'
);



--
-- Name: normalize_for_export(character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.normalize_for_export(target character varying) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
begin
    return REPLACE(REPLACE(REPLACE(REPLACE(lower(target),' _', '_'),' ', '_'), '-', '_'), ':', '_');
end;
$$;



SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: authorized_sample; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.authorized_sample (
    water_entity_id uuid CONSTRAINT authorized_sample_lake_id_not_null NOT NULL,
    species_id uuid NOT NULL,
    min_size integer DEFAULT 0,
    max_size integer DEFAULT 1000
);



--
-- Name: catch; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.catch (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    created_on timestamp without time zone NOT NULL,
    trip_id uuid NOT NULL,
    catch_time time without time zone,
    species_id uuid NOT NULL,
    technique_id uuid NOT NULL,
    size integer,
    weight integer,
    kept boolean NOT NULL,
    released_fish_state_id uuid,
    description text,
    sample_id text,
    automatic_measure integer,
    maillee public.maillage DEFAULT 'NON_DEFINI'::public.maillage,
    edited_species_id uuid,
    edited_size integer DEFAULT 0,
    edited_weight integer DEFAULT 0,
    exclude_from_exports boolean DEFAULT false,
    "position" public.geometry(Point,4326),
    latitude double precision GENERATED ALWAYS AS (public.st_y("position")) STORED,
    longitude double precision GENERATED ALWAYS AS (public.st_x("position")) STORED
);



--
-- Name: TABLE catch; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.catch IS 'Captures';

--
-- Name: COLUMN catch.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.id IS 'Identifiant technique';

--
-- Name: COLUMN catch.trip_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.trip_id IS 'Sortie concernée';

--
-- Name: COLUMN catch.catch_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.catch_time IS 'Heure de la capture';

--
-- Name: COLUMN catch.species_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.species_id IS 'Espèce capturée';

--
-- Name: COLUMN catch.technique_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.technique_id IS 'Technique de pêche de la capture';

--
-- Name: COLUMN catch.size; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.size IS 'Taille du poisson (cm)';

--
-- Name: COLUMN catch.weight; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.weight IS 'Poids du poisson (g)';

--
-- Name: COLUMN catch.kept; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.kept IS 'Est-ce que le poisson a été conservé ?';

--
-- Name: COLUMN catch.released_fish_state_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.released_fish_state_id IS 'Si il a été relâché, dans quel état ?';

--
-- Name: COLUMN catch.description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch.description IS 'Description';

--
-- Name: catch_measurement_picture; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.catch_measurement_picture (
    catch_id uuid NOT NULL,
    content bytea NOT NULL
);



--
-- Name: catch_picture; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.catch_picture (
    catch_id uuid NOT NULL,
    content bytea NOT NULL,
    picture_index integer DEFAULT 0 NOT NULL
);



--
-- Name: TABLE catch_picture; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.catch_picture IS 'Photos des captures';

--
-- Name: COLUMN catch_picture.catch_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch_picture.catch_id IS 'Identifiant technique de la capture';

--
-- Name: COLUMN catch_picture.content; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.catch_picture.content IS 'Contenu binaire de la photo (JPEG)';

--
-- Name: catch_picture_urls; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.catch_picture_urls AS
 SELECT c.id AS catch_id,
    ((('https://localhost:8080/api/v1/pictures/'::text || cp.catch_id) || '/'::text) || cp.picture_index) AS url
   FROM (public.catch c
     JOIN public.catch_picture cp ON ((cp.catch_id = c.id)))
UNION
 SELECT c.id AS catch_id,
    ('https://localhost:8080/api/v1/pictures/measure/'::text || cmp.catch_id) AS url
   FROM (public.catch c
     JOIN public.catch_measurement_picture cmp ON ((cmp.catch_id = c.id)));



--
-- Name: VIEW catch_picture_urls; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON VIEW public.catch_picture_urls IS 'Permet d''avoir, pour chaque capture, les URLs pour télécharger les images';

--
-- Name: catch_picture_joined_urls; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.catch_picture_joined_urls AS
 SELECT catch_id,
    string_agg(url, ','::text) AS urls
   FROM public.catch_picture_urls cpu
  GROUP BY catch_id;



--
-- Name: VIEW catch_picture_joined_urls; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON VIEW public.catch_picture_joined_urls IS 'Permet d''avoir, pour chaque capture, la concaténation des URLs pour télécharger les images';

--
-- Name: catch_sample_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.catch_sample_types (
    catch_id uuid NOT NULL,
    sample_type_id uuid NOT NULL
);



--
-- Name: sample_base_id_sequence; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sample_base_id_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: fishola_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fishola_user (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    first_name text NOT NULL,
    last_name text,
    email text NOT NULL,
    password text NOT NULL,
    gender public.gender,
    birth_year integer,
    prompt_weight boolean DEFAULT true NOT NULL,
    prompt_samples boolean DEFAULT false NOT NULL,
    sample_base_id integer DEFAULT nextval('public.sample_base_id_sequence'::regclass) NOT NULL,
    exclude_from_exports boolean DEFAULT false NOT NULL,
    created_on timestamp without time zone NOT NULL,
    accepts_mail_notifications boolean DEFAULT false NOT NULL,
    last_news_seen_date timestamp without time zone DEFAULT '2021-06-06 12:00:00'::timestamp without time zone,
    accepts_share_trips boolean DEFAULT false NOT NULL,
    pseudo text DEFAULT ''::text NOT NULL
);



--
-- Name: TABLE fishola_user; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.fishola_user IS 'Comptes utilisateur';

--
-- Name: COLUMN fishola_user.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.id IS 'Identifiant technique';

--
-- Name: COLUMN fishola_user.first_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.first_name IS 'Prénom';

--
-- Name: COLUMN fishola_user.last_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.last_name IS 'Nom';

--
-- Name: COLUMN fishola_user.email; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.email IS 'Email. Il s''agit également de l''identifiant de connexion. C''est donc une clé naturelle';

--
-- Name: COLUMN fishola_user.password; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.password IS 'Mot de passe';

--
-- Name: COLUMN fishola_user.gender; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.gender IS 'Genre';

--
-- Name: COLUMN fishola_user.birth_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.birth_year IS 'Année de naissance';

--
-- Name: COLUMN fishola_user.created_on; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.fishola_user.created_on IS 'Date de création du compte (validation de l''email)';

--
-- Name: species; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.species (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    export_as text NOT NULL,
    built_in boolean NOT NULL,
    mandatory_size boolean DEFAULT true NOT NULL
);



--
-- Name: TABLE species; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.species IS 'Espèces';

--
-- Name: COLUMN species.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.species.id IS 'Identifiant technique';

--
-- Name: COLUMN species.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.species.name IS 'Nom de l''espèce';

--
-- Name: COLUMN species.export_as; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.species.export_as IS 'Valeur utilisée dans les exports';

--
-- Name: COLUMN species.built_in; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.species.built_in IS 'Est-ce que la méthode a été créée par les admins ?';

--
-- Name: technique; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.technique (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    export_as text NOT NULL,
    built_in boolean NOT NULL
);



--
-- Name: TABLE technique; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.technique IS 'Techniques de pêche';

--
-- Name: COLUMN technique.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.technique.id IS 'Identifiant technique';

--
-- Name: COLUMN technique.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.technique.name IS 'Nom de la technique';

--
-- Name: COLUMN technique.export_as; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.technique.export_as IS 'Valeur utilisée dans les exports';

--
-- Name: COLUMN technique.built_in; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.technique.built_in IS 'Est-ce que la technique a été créée par les admins ?';

--
-- Name: trip; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.trip (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    created_on timestamp without time zone NOT NULL,
    owner_id uuid,
    mode public.trip_mode NOT NULL,
    name text NOT NULL,
    day date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    type public.trip_type NOT NULL,
    water_entity_id uuid CONSTRAINT trip_lake_id_not_null NOT NULL,
    weather_id uuid,
    hidden boolean DEFAULT false NOT NULL,
    source public.device_type NOT NULL,
    frontend_version text,
    river_section_id uuid,
    river_bank public.river_bank,
    begin_position public.geometry(Point,4326),
    end_position public.geometry(Point,4326),
    begin_latitude double precision GENERATED ALWAYS AS (public.st_y(begin_position)) STORED,
    begin_longitude double precision GENERATED ALWAYS AS (public.st_x(begin_position)) STORED,
    end_latitude double precision GENERATED ALWAYS AS (public.st_y(end_position)) STORED,
    end_longitude double precision GENERATED ALWAYS AS (public.st_x(end_position)) STORED
);



--
-- Name: TABLE trip; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.trip IS 'Sorties de pêche';

--
-- Name: COLUMN trip.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.id IS 'Identifiant technique';

--
-- Name: COLUMN trip.owner_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.owner_id IS 'Utilisateur ayant fait la pêche. Peut être nul si l''utilisateur a utilisé son droit à l''oubli RGPD';

--
-- Name: COLUMN trip.mode; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.mode IS 'Sortie en mode ''Live'' ou ''Afterwards''';

--
-- Name: COLUMN trip.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.name IS 'Nom donné par l''utilisateur ou généré automatiquement';

--
-- Name: COLUMN trip.day; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.day IS 'Jour';

--
-- Name: COLUMN trip.start_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.start_time IS 'Heure de début';

--
-- Name: COLUMN trip.end_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.end_time IS 'Heure de fin';

--
-- Name: COLUMN trip.type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.type IS 'Type de sortie';

--
-- Name: COLUMN trip.water_entity_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.water_entity_id IS 'Lac sur lequel se fait la sortie';

--
-- Name: COLUMN trip.weather_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.weather_id IS 'Météo de la sortie';

--
-- Name: COLUMN trip.hidden; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.hidden IS 'Est-ce que la sortie est cachée (l''utilisateur a demandé sa suppression) ?';

--
-- Name: COLUMN trip.river_section_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.river_section_id IS 'Tronçon de cours d''eau, renseigné uniquement quand water_entity.kind = FLOWING';

--
-- Name: COLUMN trip.river_bank; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip.river_bank IS 'Rive fréquentée, renseignée uniquement pour une sortie sur cours d''eau';

--
-- Name: trip_expected_species; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.trip_expected_species (
    trip_id uuid NOT NULL,
    species_id uuid NOT NULL
);



--
-- Name: TABLE trip_expected_species; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.trip_expected_species IS 'Espèces recherchées pendant une sortie';

--
-- Name: COLUMN trip_expected_species.trip_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip_expected_species.trip_id IS 'Identifiant de la sortie';

--
-- Name: COLUMN trip_expected_species.species_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip_expected_species.species_id IS 'Identifiant de l''espèce';

--
-- Name: trip_species_names; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.trip_species_names AS
 SELECT tes.trip_id,
    string_agg(s.export_as, ','::text ORDER BY s.export_as) AS species
   FROM (public.trip_expected_species tes
     JOIN public.species s ON ((s.id = tes.species_id)))
  GROUP BY tes.trip_id;



--
-- Name: VIEW trip_species_names; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON VIEW public.trip_species_names IS 'Permet d''avoir, pour chaque sortie, la liste des espèces recherchées séparées par des virgules';

--
-- Name: trip_techniques; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.trip_techniques (
    trip_id uuid NOT NULL,
    technique_id uuid NOT NULL
);



--
-- Name: TABLE trip_techniques; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.trip_techniques IS 'Techniques de pêche utilisées pendant une sortie';

--
-- Name: COLUMN trip_techniques.trip_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip_techniques.trip_id IS 'Identifiant de la sortie';

--
-- Name: COLUMN trip_techniques.technique_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.trip_techniques.technique_id IS 'Identifiant de la technique de pêche';

--
-- Name: trip_techniques_names; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.trip_techniques_names AS
 SELECT tt.trip_id,
    string_agg(t.export_as, ','::text ORDER BY t.export_as) AS techniques
   FROM (public.trip_techniques tt
     JOIN public.technique t ON ((t.id = tt.technique_id)))
  GROUP BY tt.trip_id;



--
-- Name: VIEW trip_techniques_names; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON VIEW public.trip_techniques_names IS 'Permet d''avoir, pour chaque sortie, la liste des techniques séparées par des virgules';

--
-- Name: water_entity; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.water_entity (
    id uuid DEFAULT public.uuid_generate_v4() CONSTRAINT lake_id_not_null NOT NULL,
    name text CONSTRAINT lake_name_not_null NOT NULL,
    export_as text CONSTRAINT lake_export_as_not_null NOT NULL,
    water_entity_code character varying(10),
    kind public.water_entity_kind DEFAULT 'STILL'::public.water_entity_kind NOT NULL,
    nature text,
    altitude_moyenne double precision,
    bdtopo_cleabs character varying(24),
    geom public.geometry(Geometry,4326),
    latitude double precision GENERATED ALWAYS AS (public.st_y(public.st_centroid(geom))) STORED,
    longitude double precision GENERATED ALWAYS AS (public.st_x(public.st_centroid(geom))) STORED
);



--
-- Name: TABLE water_entity; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.water_entity IS 'Milieu aquatique (plan d''eau, cours d''eau...) fréquenté par les pêcheurs';

--
-- Name: COLUMN water_entity.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.water_entity.id IS 'Identifiant technique';

--
-- Name: COLUMN water_entity.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.water_entity.name IS 'Nom du lac';

--
-- Name: COLUMN water_entity.export_as; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.water_entity.export_as IS 'Valeur utilisée dans les exports';

--
-- Name: COLUMN water_entity.kind; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.water_entity.kind IS 'STILL = eau stagnante (lac, étang...), FLOWING = eau courante (rivière, canal...)';

--
-- Name: COLUMN water_entity.bdtopo_cleabs; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.water_entity.bdtopo_cleabs IS 'Identifiant national BD TOPO (cleabs), pour ré-import idempotent';

--
-- Name: COLUMN water_entity.geom; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.water_entity.geom IS 'Empreinte géographique : point/polygone pour un plan d''eau, ligne simplifiée pour un cours d''eau nommé';

--
-- Name: weather; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.weather (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    export_as text NOT NULL
);



--
-- Name: TABLE weather; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.weather IS 'Météos';

--
-- Name: COLUMN weather.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.weather.id IS 'Identifiant technique';

--
-- Name: COLUMN weather.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.weather.name IS 'Nom de la météo';

--
-- Name: COLUMN weather.export_as; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.weather.export_as IS 'Valeur utilisée dans les exports';

--
-- Name: catchs_export; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.catchs_export AS
 SELECT 'FISHOLA'::text AS nom_du_projet,
    l.export_as AS nom_du_site,
    (l.export_as || ' : peche amateur'::text) AS nom_de_la_plateforme,
    to_char((t.day)::timestamp with time zone, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    u.birth_year AS annee_naissance_utilisateur,
        CASE u.gender
            WHEN 'Male'::public.gender THEN 'H'::text
            WHEN 'Female'::public.gender THEN 'F'::text
            WHEN 'NonBinary'::public.gender THEN '?'::text
            ELSE NULL::text
        END AS sexe_utilisateur,
    to_char((t.day)::timestamp with time zone, 'MM'::text) AS mois_de_la_sortie,
    to_char((t.day)::timestamp with time zone, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'Embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'Bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.name AS nom_de_la_sortie,
    t.id AS id_sortie,
    tsn.species AS espece_recherchee,
    to_char((t.start_time)::interval, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char((t.end_time)::interval, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_time - t.start_time) AS duree_de_la_sortie,
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
  WHERE (((t.owner_id IS NULL) OR (u.exclude_from_exports = false)) AND (c.exclude_from_exports = false) AND (t.created_on < ((now() - '168:00:00'::interval) AT TIME ZONE 'Europe/Paris'::text)));



--
-- Name: VIEW catchs_export; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON VIEW public.catchs_export IS 'Génère le CSV pour les exports';

--
-- Name: catchs_openadom_export; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.catchs_openadom_export AS
 SELECT 'fishola'::text AS nom_du_projet,
    public.normalize_for_export((l.export_as)::character varying) AS nom_du_site,
    public.normalize_for_export(((l.export_as || ':peche amateur'::text))::character varying) AS nom_de_la_plateforme,
    to_char((t.day)::timestamp with time zone, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    to_char((t.day)::timestamp with time zone, 'MM'::text) AS mois_de_la_sortie,
    to_char((t.day)::timestamp with time zone, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.id AS id_sortie,
    public.normalize_for_export((tsn.species)::character varying) AS espece_recherchee,
    to_char((t.start_time)::interval, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char((t.end_time)::interval, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_time - t.start_time) AS duree_de_la_sortie,
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
  WHERE (((t.owner_id IS NULL) OR (u.exclude_from_exports = false)) AND (t.created_on < ((now() - '168:00:00'::interval) AT TIME ZONE 'Europe/Paris'::text)));



--
-- Name: VIEW catchs_openadom_export; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON VIEW public.catchs_openadom_export IS 'Génère le CSV pour les exports OpenAdom';

--
-- Name: documentation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.documentation (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    content bytea,
    natural_id text NOT NULL
);



--
-- Name: editorial; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.editorial (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    content text NOT NULL,
    link text
);



--
-- Name: fishola_admin; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fishola_admin (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    email text NOT NULL,
    password text NOT NULL,
    created_on timestamp without time zone DEFAULT now(),
    can_create_admin boolean DEFAULT false NOT NULL,
    is_national_admin boolean DEFAULT false NOT NULL
);



--
-- Name: fishola_admin_water_entities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fishola_admin_water_entities (
    fishola_admin_id uuid CONSTRAINT fishola_admin_lakes_fishola_admin_id_not_null NOT NULL,
    water_entity_id uuid CONSTRAINT fishola_admin_lakes_lake_id_not_null NOT NULL
);



--
-- Name: fishola_user_favorite_water_entities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fishola_user_favorite_water_entities (
    fishola_user_id uuid CONSTRAINT fishola_user_favorite_lakes_fishola_user_id_not_null NOT NULL,
    water_entity_id uuid CONSTRAINT fishola_user_favorite_lakes_lake_id_not_null NOT NULL
);



--
-- Name: fishola_user_licences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fishola_user_licences (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    user_id uuid,
    expiration_date date NOT NULL,
    type public.licence_type DEFAULT 'PDF'::public.licence_type NOT NULL,
    content bytea NOT NULL
);



--
-- Name: news; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.news (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    content text,
    date_publication_debut timestamp without time zone NOT NULL,
    date_publication_fin timestamp without time zone NOT NULL,
    date_notification_sent timestamp without time zone,
    miniature_id uuid,
    is_national boolean DEFAULT false
);



--
-- Name: news_picture; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.news_picture (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    news_id uuid,
    content bytea NOT NULL,
    is_miniature boolean DEFAULT false
);



--
-- Name: news_water_entity; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.news_water_entity (
    news_id uuid CONSTRAINT news_lake_news_id_not_null NOT NULL,
    water_entity_id uuid CONSTRAINT news_lake_lake_id_not_null NOT NULL
);



--
-- Name: next_scheduled_courriel_notification_check; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.next_scheduled_courriel_notification_check (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    next_check_date timestamp without time zone CONSTRAINT next_scheduled_courriel_notification_c_next_check_date_not_null NOT NULL
);



--
-- Name: personal_catchs_export; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.personal_catchs_export AS
 SELECT l.export_as AS nom_du_site,
    to_char((t.day)::timestamp with time zone, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    to_char((t.day)::timestamp with time zone, 'MM'::text) AS mois_de_la_sortie,
    to_char((t.day)::timestamp with time zone, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.name AS nom_de_la_sortie,
    tsn.species AS espece_recherchee,
    to_char((t.start_time)::interval, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char((t.end_time)::interval, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_time - t.start_time) AS duree_de_la_sortie,
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



--
-- Name: VIEW personal_catchs_export; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON VIEW public.personal_catchs_export IS 'Génère le CSV pour les exports (sans filtre) : À filtrer ensuite sur l''utilisateur';

--
-- Name: released_fish_state; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.released_fish_state (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    export_as text NOT NULL
);



--
-- Name: TABLE released_fish_state; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.released_fish_state IS 'État dans lequel le poisson a été relâché';

--
-- Name: COLUMN released_fish_state.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.released_fish_state.id IS 'Identifiant technique';

--
-- Name: COLUMN released_fish_state.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.released_fish_state.name IS 'Nom';

--
-- Name: COLUMN released_fish_state.export_as; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.released_fish_state.export_as IS 'Valeur utilisée dans les exports';

--
-- Name: river_section; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.river_section (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    water_entity_id uuid,
    bdtopo_cleabs character varying(24),
    persistent boolean,
    width_class text,
    flow_direction text,
    geom public.geometry(LineString,4326) NOT NULL
);



--
-- Name: TABLE river_section; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.river_section IS 'Tronçon élémentaire d''un cours d''eau (BD TOPO troncon_hydrographique)';

--
-- Name: COLUMN river_section.water_entity_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.river_section.water_entity_id IS 'Cours d''eau nommé parent (water_entity.kind = FLOWING), nullable pour les tronçons non rattachés';

--
-- Name: sample_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sample_type (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name text NOT NULL,
    export_as text NOT NULL
);



--
-- Name: species_by_water_entity; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.species_by_water_entity (
    water_entity_id uuid CONSTRAINT species_by_lake_lake_id_not_null NOT NULL,
    species_id uuid CONSTRAINT species_by_lake_species_id_not_null NOT NULL,
    alias text CONSTRAINT species_by_lake_alias_not_null NOT NULL,
    present boolean DEFAULT true CONSTRAINT species_by_lake_present_not_null NOT NULL
);



--
-- Name: TABLE species_by_water_entity; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.species_by_water_entity IS 'Espèces par lac';

--
-- Name: COLUMN species_by_water_entity.water_entity_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.species_by_water_entity.water_entity_id IS 'Lac concerné';

--
-- Name: COLUMN species_by_water_entity.species_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.species_by_water_entity.species_id IS 'Espèce concernée';

--
-- Name: COLUMN species_by_water_entity.alias; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.species_by_water_entity.alias IS 'Alias éventuel sur l''espèce a une appelation différente sur ce lac';

--
-- Name: trip_social_reaction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.trip_social_reaction (
    trip_id uuid NOT NULL,
    user_id uuid NOT NULL,
    message text NOT NULL,
    reaction public.social_reaction
);



--
-- Name: water_surface; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.water_surface (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    water_entity_id uuid,
    river_section_id uuid,
    bdtopo_cleabs character varying(24),
    nature text,
    geom public.geometry(Polygon,4326) NOT NULL,
    CONSTRAINT water_surface_exactly_one_parent CHECK ((num_nonnulls(water_entity_id, river_section_id) = 1))
);



--
-- Name: TABLE water_surface; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.water_surface IS 'Empreinte polygonale de l''eau : plan d''eau entier, ou tronçon de rivière assez large pour être surfacique (BD TOPO surface_hydrographique)';

--
-- Name: authorized_sample authorized_sample_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.authorized_sample
    ADD CONSTRAINT authorized_sample_pkey PRIMARY KEY (water_entity_id, species_id);

--
-- Name: catch_measurement_picture catch_measurement_picture_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch_measurement_picture
    ADD CONSTRAINT catch_measurement_picture_pkey PRIMARY KEY (catch_id);

--
-- Name: catch_picture catch_picture_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch_picture
    ADD CONSTRAINT catch_picture_pkey PRIMARY KEY (catch_id, picture_index);

--
-- Name: catch catch_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch
    ADD CONSTRAINT catch_pkey PRIMARY KEY (id);

--
-- Name: catch_sample_types catch_sample_types_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch_sample_types
    ADD CONSTRAINT catch_sample_types_pkey PRIMARY KEY (catch_id, sample_type_id);

--
-- Name: documentation documentation_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documentation
    ADD CONSTRAINT documentation_name_key UNIQUE (name);

--
-- Name: documentation documentation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documentation
    ADD CONSTRAINT documentation_pkey PRIMARY KEY (id);

--
-- Name: editorial editorial_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.editorial
    ADD CONSTRAINT editorial_name_key UNIQUE (name);

--
-- Name: editorial editorial_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.editorial
    ADD CONSTRAINT editorial_pkey PRIMARY KEY (id);

--
-- Name: fishola_admin fishola_admin_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_admin
    ADD CONSTRAINT fishola_admin_email_key UNIQUE (email);

--
-- Name: fishola_admin fishola_admin_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_admin
    ADD CONSTRAINT fishola_admin_pkey PRIMARY KEY (id);

--
-- Name: fishola_admin_water_entities fishola_admin_water_entities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_admin_water_entities
    ADD CONSTRAINT fishola_admin_water_entities_pkey PRIMARY KEY (fishola_admin_id, water_entity_id);

--
-- Name: fishola_user fishola_user_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user
    ADD CONSTRAINT fishola_user_email_key UNIQUE (email);

--
-- Name: fishola_user_favorite_water_entities fishola_user_favorite_water_entities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user_favorite_water_entities
    ADD CONSTRAINT fishola_user_favorite_water_entities_pkey PRIMARY KEY (fishola_user_id, water_entity_id);

--
-- Name: fishola_user_licences fishola_user_licences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user_licences
    ADD CONSTRAINT fishola_user_licences_pkey PRIMARY KEY (id);

--
-- Name: fishola_user fishola_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user
    ADD CONSTRAINT fishola_user_pkey PRIMARY KEY (id);

--
-- Name: fishola_user fishola_user_sample_base_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user
    ADD CONSTRAINT fishola_user_sample_base_id_key UNIQUE (sample_base_id);

--
-- Name: news news_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.news
    ADD CONSTRAINT news_name_key UNIQUE (name);

--
-- Name: news_picture news_picture_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.news_picture
    ADD CONSTRAINT news_picture_pkey PRIMARY KEY (id);

--
-- Name: news news_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.news
    ADD CONSTRAINT news_pkey PRIMARY KEY (id);

--
-- Name: news_water_entity news_water_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.news_water_entity
    ADD CONSTRAINT news_water_entity_pkey PRIMARY KEY (news_id, water_entity_id);

--
-- Name: next_scheduled_courriel_notification_check next_scheduled_courriel_notification_check_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.next_scheduled_courriel_notification_check
    ADD CONSTRAINT next_scheduled_courriel_notification_check_pkey PRIMARY KEY (id);

--
-- Name: released_fish_state released_fish_state_export_as_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.released_fish_state
    ADD CONSTRAINT released_fish_state_export_as_key UNIQUE (export_as);

--
-- Name: released_fish_state released_fish_state_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.released_fish_state
    ADD CONSTRAINT released_fish_state_name_key UNIQUE (name);

--
-- Name: released_fish_state released_fish_state_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.released_fish_state
    ADD CONSTRAINT released_fish_state_pkey PRIMARY KEY (id);

--
-- Name: river_section river_section_bdtopo_cleabs_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.river_section
    ADD CONSTRAINT river_section_bdtopo_cleabs_key UNIQUE (bdtopo_cleabs);

--
-- Name: river_section river_section_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.river_section
    ADD CONSTRAINT river_section_pkey PRIMARY KEY (id);

--
-- Name: sample_type sample_type_export_as_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample_type
    ADD CONSTRAINT sample_type_export_as_key UNIQUE (export_as);

--
-- Name: sample_type sample_type_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample_type
    ADD CONSTRAINT sample_type_name_key UNIQUE (name);

--
-- Name: sample_type sample_type_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample_type
    ADD CONSTRAINT sample_type_pkey PRIMARY KEY (id);

--
-- Name: species_by_water_entity species_by_water_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.species_by_water_entity
    ADD CONSTRAINT species_by_water_entity_pkey PRIMARY KEY (water_entity_id, species_id);

--
-- Name: species species_export_as_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_export_as_key UNIQUE (export_as);

--
-- Name: species species_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_name_key UNIQUE (name);

--
-- Name: species species_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_pkey PRIMARY KEY (id);

--
-- Name: technique technique_export_as_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.technique
    ADD CONSTRAINT technique_export_as_key UNIQUE (export_as);

--
-- Name: technique technique_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.technique
    ADD CONSTRAINT technique_name_key UNIQUE (name);

--
-- Name: technique technique_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.technique
    ADD CONSTRAINT technique_pkey PRIMARY KEY (id);

--
-- Name: trip_expected_species trip_expected_species_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_expected_species
    ADD CONSTRAINT trip_expected_species_pkey PRIMARY KEY (trip_id, species_id);

--
-- Name: trip trip_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_pkey PRIMARY KEY (id);

--
-- Name: trip_social_reaction trip_social_reaction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_social_reaction
    ADD CONSTRAINT trip_social_reaction_pkey PRIMARY KEY (trip_id, user_id);

--
-- Name: trip_techniques trip_techniques_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_techniques
    ADD CONSTRAINT trip_techniques_pkey PRIMARY KEY (trip_id, technique_id);

--
-- Name: water_entity water_entity_bdtopo_cleabs_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_entity
    ADD CONSTRAINT water_entity_bdtopo_cleabs_key UNIQUE (bdtopo_cleabs);

--
-- Name: water_entity water_entity_export_as_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_entity
    ADD CONSTRAINT water_entity_export_as_key UNIQUE (export_as);

--
-- Name: water_entity water_entity_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_entity
    ADD CONSTRAINT water_entity_name_key UNIQUE (name);

--
-- Name: water_entity water_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_entity
    ADD CONSTRAINT water_entity_pkey PRIMARY KEY (id);

--
-- Name: water_entity water_entity_water_entity_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_entity
    ADD CONSTRAINT water_entity_water_entity_code_key UNIQUE (water_entity_code);

--
-- Name: water_surface water_surface_bdtopo_cleabs_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_surface
    ADD CONSTRAINT water_surface_bdtopo_cleabs_key UNIQUE (bdtopo_cleabs);

--
-- Name: water_surface water_surface_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_surface
    ADD CONSTRAINT water_surface_pkey PRIMARY KEY (id);

--
-- Name: weather weather_export_as_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.weather
    ADD CONSTRAINT weather_export_as_key UNIQUE (export_as);

--
-- Name: weather weather_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.weather
    ADD CONSTRAINT weather_name_key UNIQUE (name);

--
-- Name: weather weather_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.weather
    ADD CONSTRAINT weather_pkey PRIMARY KEY (id);

--
-- Name: catch_position_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX catch_position_idx ON public.catch USING gist ("position");

--
-- Name: documentation_natural_id_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX documentation_natural_id_idx ON public.documentation USING btree (natural_id);

--
-- Name: river_section_geom_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX river_section_geom_idx ON public.river_section USING gist (geom);

--
-- Name: trip_begin_position_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX trip_begin_position_idx ON public.trip USING gist (begin_position);

--
-- Name: trip_end_position_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX trip_end_position_idx ON public.trip USING gist (end_position);

--
-- Name: water_entity_geom_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX water_entity_geom_idx ON public.water_entity USING gist (geom);

--
-- Name: water_surface_geom_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX water_surface_geom_idx ON public.water_surface USING gist (geom);

--
-- Name: authorized_sample authorized_sample_species_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.authorized_sample
    ADD CONSTRAINT authorized_sample_species_id_fkey FOREIGN KEY (species_id) REFERENCES public.species(id);

--
-- Name: authorized_sample authorized_sample_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.authorized_sample
    ADD CONSTRAINT authorized_sample_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- Name: catch_measurement_picture catch_measurement_picture_catch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch_measurement_picture
    ADD CONSTRAINT catch_measurement_picture_catch_id_fkey FOREIGN KEY (catch_id) REFERENCES public.catch(id);

--
-- Name: catch_picture catch_picture_catch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch_picture
    ADD CONSTRAINT catch_picture_catch_id_fkey FOREIGN KEY (catch_id) REFERENCES public.catch(id);

--
-- Name: catch catch_released_fish_state_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch
    ADD CONSTRAINT catch_released_fish_state_id_fkey FOREIGN KEY (released_fish_state_id) REFERENCES public.released_fish_state(id);

--
-- Name: catch_sample_types catch_sample_types_catch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch_sample_types
    ADD CONSTRAINT catch_sample_types_catch_id_fkey FOREIGN KEY (catch_id) REFERENCES public.catch(id);

--
-- Name: catch_sample_types catch_sample_types_sample_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch_sample_types
    ADD CONSTRAINT catch_sample_types_sample_type_id_fkey FOREIGN KEY (sample_type_id) REFERENCES public.sample_type(id);

--
-- Name: catch catch_species_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch
    ADD CONSTRAINT catch_species_id_fkey FOREIGN KEY (species_id) REFERENCES public.species(id);

--
-- Name: catch catch_technique_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch
    ADD CONSTRAINT catch_technique_id_fkey FOREIGN KEY (technique_id) REFERENCES public.technique(id);

--
-- Name: catch catch_trip_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catch
    ADD CONSTRAINT catch_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES public.trip(id);

--
-- Name: fishola_admin_water_entities fishola_admin_water_entities_fishola_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_admin_water_entities
    ADD CONSTRAINT fishola_admin_water_entities_fishola_admin_id_fkey FOREIGN KEY (fishola_admin_id) REFERENCES public.fishola_admin(id);

--
-- Name: fishola_admin_water_entities fishola_admin_water_entities_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_admin_water_entities
    ADD CONSTRAINT fishola_admin_water_entities_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- Name: fishola_user_favorite_water_entities fishola_user_favorite_water_entities_fishola_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user_favorite_water_entities
    ADD CONSTRAINT fishola_user_favorite_water_entities_fishola_user_id_fkey FOREIGN KEY (fishola_user_id) REFERENCES public.fishola_user(id);

--
-- Name: fishola_user_favorite_water_entities fishola_user_favorite_water_entities_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user_favorite_water_entities
    ADD CONSTRAINT fishola_user_favorite_water_entities_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- Name: fishola_user_licences fishola_user_licences_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fishola_user_licences
    ADD CONSTRAINT fishola_user_licences_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.fishola_user(id) ON DELETE CASCADE;

--
-- Name: news_water_entity news_water_entity_news_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.news_water_entity
    ADD CONSTRAINT news_water_entity_news_id_fkey FOREIGN KEY (news_id) REFERENCES public.news(id);

--
-- Name: news_water_entity news_water_entity_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.news_water_entity
    ADD CONSTRAINT news_water_entity_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- Name: river_section river_section_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.river_section
    ADD CONSTRAINT river_section_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- Name: species_by_water_entity species_by_water_entity_species_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.species_by_water_entity
    ADD CONSTRAINT species_by_water_entity_species_id_fkey FOREIGN KEY (species_id) REFERENCES public.species(id);

--
-- Name: species_by_water_entity species_by_water_entity_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.species_by_water_entity
    ADD CONSTRAINT species_by_water_entity_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- Name: trip_expected_species trip_expected_species_species_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_expected_species
    ADD CONSTRAINT trip_expected_species_species_id_fkey FOREIGN KEY (species_id) REFERENCES public.species(id);

--
-- Name: trip_expected_species trip_expected_species_trip_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_expected_species
    ADD CONSTRAINT trip_expected_species_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES public.trip(id);

--
-- Name: trip trip_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.fishola_user(id);

--
-- Name: trip trip_river_section_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_river_section_id_fkey FOREIGN KEY (river_section_id) REFERENCES public.river_section(id);

--
-- Name: trip_social_reaction trip_social_reaction_trip_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_social_reaction
    ADD CONSTRAINT trip_social_reaction_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES public.trip(id) ON DELETE CASCADE;

--
-- Name: trip_social_reaction trip_social_reaction_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_social_reaction
    ADD CONSTRAINT trip_social_reaction_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.fishola_user(id) ON DELETE CASCADE;

--
-- Name: trip_techniques trip_techniques_technique_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_techniques
    ADD CONSTRAINT trip_techniques_technique_id_fkey FOREIGN KEY (technique_id) REFERENCES public.technique(id);

--
-- Name: trip_techniques trip_techniques_trip_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip_techniques
    ADD CONSTRAINT trip_techniques_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES public.trip(id);

--
-- Name: trip trip_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- Name: trip trip_weather_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_weather_id_fkey FOREIGN KEY (weather_id) REFERENCES public.weather(id);

--
-- Name: water_surface water_surface_river_section_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_surface
    ADD CONSTRAINT water_surface_river_section_id_fkey FOREIGN KEY (river_section_id) REFERENCES public.river_section(id);

--
-- Name: water_surface water_surface_water_entity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.water_surface
    ADD CONSTRAINT water_surface_water_entity_id_fkey FOREIGN KEY (water_entity_id) REFERENCES public.water_entity(id);

--
-- PostgreSQL database dump complete
--

