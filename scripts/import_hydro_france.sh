#!/bin/bash
# Orchestration multi-départements de l'import hydro BD TOPO.
#
# Compose la brique unitaire import_hydro_gpkg.sh (100 % Docker) sur une série
# de dossiers départementaux, avec journalisation et reprise sur erreur — pour
# charger l'ensemble du réseau hydrographique (note Q1 : AURA & Corse ; cible
# France entière). L'upsert sur bdtopo_cleabs (clé nationale) déduplique de lui-
# même les entités partagées entre départements limitrophes, donc le rejeu est
# sûr par construction.
#
# ── Où récupérer les données ────────────────────────────────────────────────
# BD TOPO® IGN, thème « Hydrographie », téléchargement par département au format
# GeoPackage : https://geoservices.ign.fr/bdtopo
# Pour chaque département, placer dans <racine_data>/hydro_<dept>/ les 4 couches
# attendues, renommées ainsi :
#   - plan_d_eau.gpkg
#   - cours_d_eau.gpkg
#   - troncon_hydrographique.gpkg
#   - surface_hydrographique.gpkg
# (mêmes fichiers que ceux consommés par import_hydro_gpkg.sh).
#
# ── Usage ───────────────────────────────────────────────────────────────────
#   ./import_hydro_france.sh <racine_data> [dept...]
#     racine_data : dossier contenant les sous-dossiers hydro_<dept>/
#     dept...     : liste de départements à importer (ex. 74 73 01 2A) ;
#                   si omis, tous les sous-dossiers hydro_* de <racine_data>
#                   sont découverts et importés.
#
#   Exemples :
#     ./import_hydro_france.sh ../data 74 73 01
#     ./import_hydro_france.sh ../data                 # tout hydro_* trouvé
#
# ── Variables d'environnement (héritées par import_hydro_gpkg.sh) ────────────
#   PG_CONTAINER (défaut postgres-18-fishola), GDAL_IMAGE, PGDATABASE, PGUSER,
#   PGPASSWORD — voir import_hydro_gpkg.sh.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UNIT_SCRIPT="${SCRIPT_DIR}/import_hydro_gpkg.sh"

if [ "$#" -lt 1 ]; then
  echo "Usage : $0 <racine_data> [dept...]" >&2
  exit 2
fi

DATA_ROOT="$(cd "$1" && pwd)"
shift

PG_CONTAINER=${PG_CONTAINER:-postgres-18-fishola}
PGDATABASE=${PGDATABASE:-fishola}
PGUSER=${PGUSER:-postgres}
PGPASSWORD=${PGPASSWORD:-whatever}

# Le conteneur PostgreSQL doit tourner (fail-fast avant toute itération).
if [ -z "$(docker ps -q -f name=^/${PG_CONTAINER}$)" ]; then
  echo "Conteneur ${PG_CONTAINER} introuvable ou arrêté." >&2
  echo "Démarrez d'abord la base : fishola-backend/start_db.sh" >&2
  exit 1
fi

# État de reprise et journal (dans la racine data, gitignorée).
STATE_DIR="${DATA_ROOT}/.hydro_import_state"
LOG_FILE="${DATA_ROOT}/import_hydro_france.log"
mkdir -p "${STATE_DIR}"

log() {
  # Horodatage journal + écho console.
  local msg="$1"
  echo "$(date '+%Y-%m-%d %H:%M:%S') ${msg}" | tee -a "${LOG_FILE}"
}

# Liste des départements : arguments explicites, sinon découverte des hydro_*.
DEPTS=()
if [ "$#" -gt 0 ]; then
  DEPTS=("$@")
else
  for d in "${DATA_ROOT}"/hydro_*/; do
    [ -d "$d" ] || continue
    local_name="$(basename "$d")"
    DEPTS+=("${local_name#hydro_}")
  done
fi

if [ "${#DEPTS[@]}" -eq 0 ]; then
  echo "Aucun dossier hydro_<dept> trouvé sous ${DATA_ROOT}." >&2
  exit 1
fi

log "=== Import hydro France : ${#DEPTS[@]} département(s) : ${DEPTS[*]} ==="

imported=0
skipped=0
for dept in "${DEPTS[@]}"; do
  dept_dir="${DATA_ROOT}/hydro_${dept}"
  done_marker="${STATE_DIR}/${dept}.done"

  if [ ! -d "${dept_dir}" ]; then
    log "!! Département ${dept} : dossier introuvable (${dept_dir}) — ignoré."
    continue
  fi

  if [ -f "${done_marker}" ]; then
    log "-- Département ${dept} : déjà importé (${done_marker}) — sauté."
    skipped=$((skipped + 1))
    continue
  fi

  log ">> Département ${dept} : import depuis ${dept_dir}..."
  if "${UNIT_SCRIPT}" "${dept_dir}" >>"${LOG_FILE}" 2>&1; then
    date '+%Y-%m-%d %H:%M:%S' >"${done_marker}"
    log "<< Département ${dept} : OK."
    imported=$((imported + 1))
  else
    log "!! Département ${dept} : ÉCHEC (voir ${LOG_FILE}). Reprise possible via re-run."
    exit 1
  fi
done

# Statistiques fraîches pour le planificateur (KNN / <-> sur les GIST).
log "==> VACUUM ANALYZE des tables hydro..."
docker exec -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" \
  psql -v ON_ERROR_STOP=1 \
  "host=127.0.0.1 port=5432 dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}" \
  -c "VACUUM ANALYZE water_entity;" \
  -c "VACUUM ANALYZE river_section;" \
  -c "VACUUM ANALYZE water_surface;" >>"${LOG_FILE}" 2>&1

# Récapitulatif de volumétrie.
log "==> Récapitulatif :"
docker exec -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" \
  psql -v ON_ERROR_STOP=1 \
  "host=127.0.0.1 port=5432 dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}" \
  -c "SELECT kind, count(*) FROM water_entity GROUP BY kind ORDER BY kind;" \
  -c "SELECT count(*) AS river_sections FROM river_section;" \
  -c "SELECT count(*) AS water_surfaces FROM water_surface;" \
  -c "SELECT pg_size_pretty(pg_database_size('${PGDATABASE}')) AS db_size;" | tee -a "${LOG_FILE}"

log "=== Terminé : ${imported} importé(s), ${skipped} sauté(s). ==="
