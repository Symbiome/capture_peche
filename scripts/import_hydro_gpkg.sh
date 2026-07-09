#!/bin/bash
# Charge les GeoPackages BD TOPO (plan_d_eau, cours_d_eau, troncon_hydrographique,
# surface_hydrographique) dans la base fishola : staging brut via ogr2ogr (schéma
# bdtopo_raw, colonnes BD TOPO d'origine), puis transformation upsert vers
# water_entity / river_section / water_surface (import_hydro_gpkg.sql).
#
# Réexécutable à volonté : le staging est recréé à chaque run (-lco OVERWRITE=YES),
# et la transformation utilise bdtopo_cleabs comme clé naturelle d'upsert.
#
# Usage : ./import_hydro_gpkg.sh [dossier_data]
#   dossier_data (optionnel) : dossier contenant plan_d_eau.gpkg, cours_d_eau.gpkg,
#                               troncon_hydrographique.gpkg et
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

PLAN_D_EAU_GPKG="${DATA_DIR}/plan_d_eau.gpkg"
COURS_D_EAU_GPKG="${DATA_DIR}/cours_d_eau.gpkg"
TRONCON_GPKG="${DATA_DIR}/troncon_hydrographique.gpkg"
SURFACE_GPKG="${DATA_DIR}/surface_hydrographique.gpkg"

for f in "${PLAN_D_EAU_GPKG}" "${COURS_D_EAU_GPKG}" "${TRONCON_GPKG}" "${SURFACE_GPKG}"; do
  if [ ! -f "$f" ]; then
    echo "Fichier introuvable : $f" >&2
    exit 1
  fi
done

CONN="host=${PGHOST} port=${PGPORT} dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}"

echo "==> Création du schéma de staging bdtopo_raw..."
psql "${CONN}" -v ON_ERROR_STOP=1 -c "CREATE SCHEMA IF NOT EXISTS bdtopo_raw;"

stage() {
  local gpkg="$1" table="$2"
  echo "==> Staging ${table} (${gpkg})..."
  ogr2ogr -f PostgreSQL "PG:${CONN}" "${gpkg}" \
    -nln "bdtopo_raw.${table}" \
    -lco SCHEMA=bdtopo_raw -lco GEOMETRY_NAME=geom -lco FID=ogc_fid \
    -lco OVERWRITE=YES \
    -t_srs EPSG:4326 -dim XY \
    -progress
}

stage "${PLAN_D_EAU_GPKG}" plan_d_eau
stage "${COURS_D_EAU_GPKG}" cours_d_eau
stage "${TRONCON_GPKG}" troncon_hydrographique
stage "${SURFACE_GPKG}" surface_hydrographique

echo "==> Transformation vers water_entity / river_section / water_surface..."
psql "${CONN}" -v ON_ERROR_STOP=1 -f "${SCRIPT_DIR}/import_hydro_gpkg.sql"

echo "==> Terminé."
psql "${CONN}" -c "SELECT kind, count(*) FROM water_entity GROUP BY kind;"
psql "${CONN}" -c "SELECT count(*) AS river_sections FROM river_section;"
psql "${CONN}" -c "SELECT count(*) AS water_surfaces FROM water_surface;"
psql "${CONN}" -c "SELECT count(*) AS river_sections_linked FROM river_section WHERE water_entity_id IS NOT NULL;"
psql "${CONN}" -c "SELECT count(*) AS water_surfaces_linked FROM water_surface WHERE water_entity_id IS NOT NULL;"
