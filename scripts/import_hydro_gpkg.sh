#!/bin/bash
# Charge les GeoPackages BD TOPO (troncon_hydrographique, surface_hydrographique)
# dans la base fishola : staging brut via ogr2ogr (schéma bdtopo_raw, colonnes BD
# TOPO d'origine), puis transformation upsert vers river_section / water_surface
# (import_hydro_gpkg.sql).
#
# Réexécutable à volonté : le staging est recréé à chaque run (-lco OVERWRITE=YES),
# et la transformation utilise bdtopo_cleabs comme clé naturelle d'upsert.
#
# Usage : ./import_hydro_gpkg.sh [dossier_data]
#   dossier_data (optionnel) : dossier contenant troncon_hydrographique.gpkg et
#                               surface_hydrographique.gpkg
#                               (par défaut : ../data/hydro_74)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$(cd "${1:-${SCRIPT_DIR}/../data/hydro_74}" && pwd)"

PGHOST=${PGHOST:-localhost}
PGPORT=${PGPORT:-15432}
PGDATABASE=${PGDATABASE:-fishola}
PGUSER=${PGUSER:-postgres}
PGPASSWORD=${PGPASSWORD:-whatever}
export PGPASSWORD

TRONCON_GPKG="${DATA_DIR}/troncon_hydrographique.gpkg"
SURFACE_GPKG="${DATA_DIR}/surface_hydrographique.gpkg"

for f in "${TRONCON_GPKG}" "${SURFACE_GPKG}"; do
  if [ ! -f "$f" ]; then
    echo "Fichier introuvable : $f" >&2
    exit 1
  fi
done

CONN="host=${PGHOST} port=${PGPORT} dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}"

echo "==> Création du schéma de staging bdtopo_raw..."
psql "${CONN}" -v ON_ERROR_STOP=1 -c "CREATE SCHEMA IF NOT EXISTS bdtopo_raw;"

echo "==> Staging troncon_hydrographique (${TRONCON_GPKG})..."
ogr2ogr -f PostgreSQL "PG:${CONN}" "${TRONCON_GPKG}" \
  -nln bdtopo_raw.troncon_hydrographique \
  -lco SCHEMA=bdtopo_raw -lco GEOMETRY_NAME=geom -lco FID=ogc_fid \
  -lco OVERWRITE=YES \
  -t_srs EPSG:4326 -dim XY \
  -progress

echo "==> Staging surface_hydrographique (${SURFACE_GPKG})..."
ogr2ogr -f PostgreSQL "PG:${CONN}" "${SURFACE_GPKG}" \
  -nln bdtopo_raw.surface_hydrographique \
  -lco SCHEMA=bdtopo_raw -lco GEOMETRY_NAME=geom -lco FID=ogc_fid \
  -lco OVERWRITE=YES \
  -t_srs EPSG:4326 -dim XY \
  -progress

echo "==> Transformation vers river_section / water_surface..."
psql "${CONN}" -v ON_ERROR_STOP=1 -f "${SCRIPT_DIR}/import_hydro_gpkg.sql"

echo "==> Terminé."
psql "${CONN}" -c "SELECT count(*) AS river_sections FROM river_section;"
psql "${CONN}" -c "SELECT count(*) AS water_surfaces FROM water_surface;"
