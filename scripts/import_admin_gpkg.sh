#!/bin/bash
# Charge la couche COMMUNE d'IGN ADMIN EXPRESS (GeoPackage) dans la base fishola :
# staging brut via ogr2ogr (schéma bdtopo_raw, réutilisé), puis transformation
# upsert vers la table applicative commune (import_admin_gpkg.sql).
#
# Même chaîne 100 % Docker que import_hydro_gpkg.sh (aucun binaire client requis
# sur l'OS hôte) :
#   - psql     : exécuté DANS le conteneur PostgreSQL ;
#   - ogr2ogr  : conteneur GDAL éphémère partageant le réseau du conteneur PG.
#
# Réexécutable à volonté : le staging est recréé à chaque run (-lco OVERWRITE=YES),
# et la transformation utilise insee_com comme clé naturelle d'upsert.
#
# ── Où récupérer les données ────────────────────────────────────────────────
# IGN ADMIN EXPRESS (ou ADMIN EXPRESS COG), au format GeoPackage :
#   https://geoservices.ign.fr/adminexpress
# Extraire la couche COMMUNE et la placer en <dossier_data>/commune.gpkg
# (colonnes attendues : insee_com, nom).
#
# ── Usage ───────────────────────────────────────────────────────────────────
#   ./import_admin_gpkg.sh [dossier_data]
#     dossier_data (optionnel) : dossier contenant commune.gpkg
#                                (par défaut : ../data/admin_express)
#
# ── Variables d'environnement ───────────────────────────────────────────────
#   PG_CONTAINER (défaut postgres-18-fishola), GDAL_IMAGE, PGDATABASE, PGUSER,
#   PGPASSWORD — cf. import_hydro_gpkg.sh.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$(cd "${1:-${SCRIPT_DIR}/../data/admin_express}" && pwd)"

PG_CONTAINER=${PG_CONTAINER:-postgres-18-fishola}
GDAL_IMAGE=${GDAL_IMAGE:-ghcr.io/osgeo/gdal:alpine-normal-latest}

PGDATABASE=${PGDATABASE:-fishola}
PGUSER=${PGUSER:-postgres}
PGPASSWORD=${PGPASSWORD:-whatever}

# Depuis l'intérieur du réseau du conteneur PG, la base écoute sur 127.0.0.1:5432.
CONN="host=127.0.0.1 port=5432 dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}"

COMMUNE_GPKG=commune.gpkg

if [ ! -f "${DATA_DIR}/${COMMUNE_GPKG}" ]; then
  echo "Fichier introuvable : ${DATA_DIR}/${COMMUNE_GPKG}" >&2
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

echo "==> Staging commune (${COMMUNE_GPKG})..."
ogr2ogr_db -f PostgreSQL "PG:${CONN}" "/data/${COMMUNE_GPKG}" \
  -nln "bdtopo_raw.commune" \
  -lco SCHEMA=bdtopo_raw -lco GEOMETRY_NAME=geom -lco FID=ogc_fid \
  -lco OVERWRITE=YES \
  -t_srs EPSG:4326 -dim XY \
  -progress

echo "==> Transformation vers commune..."
psql_db < "${SCRIPT_DIR}/import_admin_gpkg.sql"

echo "==> Terminé."
psql_db -c "SELECT count(*) AS communes FROM commune;"
