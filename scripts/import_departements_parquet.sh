#!/bin/bash
# Charge les contours des départements français (BD TOPO IGN, GeoParquet) dans la
# base fishola : staging brut via ogr2ogr (schéma bdtopo_raw, réutilisé), puis
# transformation upsert vers la table applicative departement
# (import_departements_parquet.sql).
#
# Même chaîne 100 % Docker que import_admin_gpkg.sh / import_hydro_gpkg.sh (aucun
# binaire client requis sur l'OS hôte) :
#   - psql     : exécuté DANS le conteneur PostgreSQL ;
#   - ogr2ogr  : conteneur GDAL éphémère partageant le réseau du conteneur PG.
#
# Réexécutable à volonté : le staging est recréé à chaque run (-lco OVERWRITE=YES),
# et la transformation utilise code_insee comme clé naturelle d'upsert. Un seul
# fichier couvre les 101 départements en une passe (pas d'orchestration
# département par département, contrairement à import_hydro_france.sh).
#
# ── Où récupérer les données ────────────────────────────────────────────────
# GeoParquet de la couche DEPARTEMENT, thème ADMINISTRATIF de la BD TOPO® IGN :
#   https://geoservices.ign.fr/bdtopo
# Le fichier attendu est ./data/departement.parquet (EPSG:4326, MultiPolygon,
# colonnes code_insee, nom_officiel, cleabs ; 101 entités : 96 métropole + 5 DOM
# 971/972/973/974/976 ; code_insee couvre « 2A »/« 2B » et les codes DOM à
# 3 chiffres). ogr2ogr lit le Parquet nativement, aucune conversion préalable.
#
# ── Usage ───────────────────────────────────────────────────────────────────
#   ./import_departements_parquet.sh [dossier_data]
#     dossier_data (optionnel) : dossier contenant departement.parquet
#                                (par défaut : ../data)
#
# ── Variables d'environnement ───────────────────────────────────────────────
#   PG_CONTAINER (défaut postgres-18-fishola), GDAL_IMAGE, PGDATABASE, PGUSER,
#   PGPASSWORD — cf. import_hydro_gpkg.sh.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$(cd "${1:-${SCRIPT_DIR}/../data}" && pwd)"

PG_CONTAINER=${PG_CONTAINER:-postgres-18-fishola}
GDAL_IMAGE=${GDAL_IMAGE:-ghcr.io/osgeo/gdal:alpine-normal-latest}

PGDATABASE=${PGDATABASE:-fishola}
PGUSER=${PGUSER:-postgres}
PGPASSWORD=${PGPASSWORD:-whatever}

# Depuis l'intérieur du réseau du conteneur PG, la base écoute sur 127.0.0.1:5432.
CONN="host=127.0.0.1 port=5432 dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}"

DEPARTEMENT_PARQUET=departement.parquet

if [ ! -f "${DATA_DIR}/${DEPARTEMENT_PARQUET}" ]; then
  echo "Fichier introuvable : ${DATA_DIR}/${DEPARTEMENT_PARQUET}" >&2
  exit 1
fi

# Le conteneur PostgreSQL doit tourner.
if [ -z "$(docker ps -q -f name=^/${PG_CONTAINER}$)" ]; then
  echo "Conteneur ${PG_CONTAINER} introuvable ou arrêté." >&2
  echo "Démarrez d'abord la base : fishola-backend/start_db.sh" >&2
  exit 1
fi

# psql via le conteneur PostgreSQL (SQL sur stdin).
psql_db() {
  docker exec -i -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" \
    psql -v ON_ERROR_STOP=1 "${CONN}" "$@"
}

# ogr2ogr via un conteneur GDAL éphémère partageant le réseau du conteneur PG.
ogr2ogr_db() {
  docker run --rm \
    --network "container:${PG_CONTAINER}" \
    -v "${DATA_DIR}:/data:ro" \
    "${GDAL_IMAGE}" ogr2ogr "$@"
}

echo "==> Création du schéma de staging bdtopo_raw..."
psql_db -c "CREATE SCHEMA IF NOT EXISTS bdtopo_raw;"

echo "==> Staging departement (${DEPARTEMENT_PARQUET})..."
ogr2ogr_db -f PostgreSQL "PG:${CONN}" "/data/${DEPARTEMENT_PARQUET}" \
  -nln "bdtopo_raw.departement" \
  -lco SCHEMA=bdtopo_raw -lco GEOMETRY_NAME=geom -lco FID=ogc_fid \
  -lco OVERWRITE=YES \
  -nlt MULTIPOLYGON \
  -t_srs EPSG:4326 -dim XY \
  -progress

echo "==> Transformation vers departement..."
psql_db < "${SCRIPT_DIR}/import_departements_parquet.sql"

echo "==> Terminé."
psql_db -c "SELECT count(*) AS departements FROM departement;"
