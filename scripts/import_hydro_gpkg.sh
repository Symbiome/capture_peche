#!/bin/bash
# Charge les GeoPackages BD TOPO (plan_d_eau, cours_d_eau, troncon_hydrographique,
# surface_hydrographique) dans la base fishola : staging brut via ogr2ogr (schéma
# bdtopo_raw, colonnes BD TOPO d'origine), puis transformation upsert vers
# water_entity / river_section / water_surface (import_hydro_gpkg.sql).
#
# Tout passe par Docker (aucun binaire client requis sur l'OS hôte) :
#   - psql     : exécuté DANS le conteneur PostgreSQL (qui embarque psql) ;
#   - ogr2ogr  : conteneur GDAL éphémère partageant le réseau du conteneur PG.
# Le script fonctionne donc à l'identique sur macOS/Linux et arm/amd.
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

# Conteneur PostgreSQL (démarré par fishola-backend/start_db.sh) et image GDAL.
# Surchargeables via l'environnement. L'image GDAL "alpine-normal" embarque le
# driver PostgreSQL/PostGIS (contrairement à "alpine-small") et est multi-arch.
PG_CONTAINER=${PG_CONTAINER:-postgres-18-fishola}
GDAL_IMAGE=${GDAL_IMAGE:-ghcr.io/osgeo/gdal:alpine-normal-latest}

PGDATABASE=${PGDATABASE:-fishola}
PGUSER=${PGUSER:-postgres}
PGPASSWORD=${PGPASSWORD:-whatever}

# Depuis l'intérieur du réseau du conteneur PG, la base écoute sur 127.0.0.1:5432
# (port interne), indépendamment du port publié côté hôte (15432).
CONN="host=127.0.0.1 port=5432 dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}"

# Le conteneur PostgreSQL doit tourner.
if [ -z "$(docker ps -q -f name=^/${PG_CONTAINER}$)" ]; then
  echo "Conteneur ${PG_CONTAINER} introuvable ou arrêté." >&2
  echo "Démarrez d'abord la base : fishola-backend/start_db.sh" >&2
  exit 1
fi

# psql via le conteneur PostgreSQL. Le SQL est passé sur stdin, car le conteneur
# ne voit pas les chemins de l'hôte.
psql_db() {
  docker exec -i -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" \
    psql -v ON_ERROR_STOP=1 "${CONN}" "$@"
}

# ogr2ogr via un conteneur GDAL éphémère : partage le réseau du conteneur PG
# (base joignable sur 127.0.0.1:5432) et monte le dossier data en lecture seule
# sous /data.
ogr2ogr_db() {
  docker run --rm \
    --network "container:${PG_CONTAINER}" \
    -v "${DATA_DIR}:/data:ro" \
    "${GDAL_IMAGE}" ogr2ogr "$@"
}

# Noms de fichiers (relatifs à DATA_DIR côté hôte, /data côté conteneur GDAL).
PLAN_D_EAU_GPKG=plan_d_eau.gpkg
COURS_D_EAU_GPKG=cours_d_eau.gpkg
TRONCON_GPKG=troncon_hydrographique.gpkg
SURFACE_GPKG=surface_hydrographique.gpkg

for f in "${PLAN_D_EAU_GPKG}" "${COURS_D_EAU_GPKG}" "${TRONCON_GPKG}" "${SURFACE_GPKG}"; do
  if [ ! -f "${DATA_DIR}/${f}" ]; then
    echo "Fichier introuvable : ${DATA_DIR}/${f}" >&2
    exit 1
  fi
done

echo "==> Création du schéma de staging bdtopo_raw..."
psql_db -c "CREATE SCHEMA IF NOT EXISTS bdtopo_raw;"

stage() {
  local gpkg="$1" table="$2"
  echo "==> Staging ${table} (${gpkg})..."
  ogr2ogr_db -f PostgreSQL "PG:${CONN}" "/data/${gpkg}" \
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
psql_db < "${SCRIPT_DIR}/import_hydro_gpkg.sql"

echo "==> Terminé."
psql_db -c "SELECT kind, count(*) FROM water_entity GROUP BY kind;"
psql_db -c "SELECT count(*) AS river_sections FROM river_section;"
psql_db -c "SELECT count(*) AS water_surfaces FROM water_surface;"
psql_db -c "SELECT count(*) AS river_sections_linked FROM river_section WHERE water_entity_id IS NOT NULL;"
psql_db -c "SELECT count(*) AS water_surfaces_linked FROM water_surface WHERE water_entity_id IS NOT NULL;"
