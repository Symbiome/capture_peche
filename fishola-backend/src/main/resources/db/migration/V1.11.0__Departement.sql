-- Référentiel des contours départementaux français (BD TOPO IGN), prérequis de
-- toute agrégation ou tout cloisonnement « par département » : compter captures,
-- pêcheurs et sorties par département (#159) et fiabiliser water_entity.department
-- là où le référentiel commune ne couvre pas la zone (#156).
--
-- Jusqu'ici seules commune et water_entity portaient une géométrie ; le code
-- département n'existait que dérivé du préfixe du code INSEE commune, par
-- jointure spatiale water_entity <-> commune (cf. V1.10.0 et DepartmentDao).
-- Cette table apporte les contours eux-mêmes, pour des jointures
-- ST_Intersects / ST_Contains directes.
--
-- Clé naturelle : le code INSEE (« 01 » à « 95 », « 2A »/« 2B », DOM à
-- 3 chiffres). Chargement par scripts/import_departements_parquet.sh depuis
-- ./data/departement.parquet (101 entités : 96 métropole + 5 DOM). La liste des
-- codes/noms reste par ailleurs disponible en constante Java embarquée
-- (fr.inrae.fishola.rest.department.Departments) pour la validation sans base.

CREATE TABLE departement (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    code character varying(3) NOT NULL,
    name text NOT NULL,
    bdtopo_cleabs character varying(24),
    geom geometry(MultiPolygon, 4326) NOT NULL,
    CONSTRAINT departement_pkey PRIMARY KEY (id),
    CONSTRAINT departement_code_key UNIQUE (code),
    CONSTRAINT departement_bdtopo_cleabs_key UNIQUE (bdtopo_cleabs)
);

-- GIST pour les jointures spatiales (ST_Intersects / ST_Contains entité <-> département).
CREATE INDEX departement_geom_idx ON departement USING gist (geom);

COMMENT ON TABLE departement IS 'Départements français (contours BD TOPO IGN) — référentiel des agrégations et du cloisonnement par département (#156)';
COMMENT ON COLUMN departement.id IS 'Identifiant technique';
COMMENT ON COLUMN departement.code IS 'Code INSEE du département (clé naturelle) : « 01 » à « 95 », « 2A »/« 2B », DOM à 3 chiffres';
COMMENT ON COLUMN departement.name IS 'Nom officiel du département';
COMMENT ON COLUMN departement.bdtopo_cleabs IS 'Identifiant national BD TOPO (cleabs), pour ré-import idempotent';
COMMENT ON COLUMN departement.geom IS 'Contour départemental (MultiPolygon, EPSG:4326)';
