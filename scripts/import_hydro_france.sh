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
# ── Récupération des données ────────────────────────────────────────────────
# Par défaut, les données manquantes sont téléchargées automatiquement depuis la
# Géoplateforme IGN via download_hydro_ign.sh (URLs de téléchargement direct,
# sans authentification) : rien à récupérer à la main.
#
# Le mode manuel reste possible avec --download=never : placer alors dans
# <racine_data>/hydro_<dept>/ les 4 couches attendues, renommées ainsi :
#   - plan_d_eau.gpkg
#   - cours_d_eau.gpkg
#   - troncon_hydrographique.gpkg
#   - surface_hydrographique.gpkg
# (mêmes fichiers que ceux consommés par import_hydro_gpkg.sh).
#
# Libellés des plans d'eau : charger au préalable les communes des départements
# visés (scripts/import_communes_geoapi.sh <dept>) permet de départager les
# homonymes par un nom lisible — « Lac Blanc (Valloire) » au lieu d'un suffixe
# technique. Facultatif : sans référentiel commune, l'import réussit et retombe
# sur l'identifiant BD TOPO.
#
# Attention volumétrie : BD TOPO 3.x n'étant plus livrée par thème, chaque
# département implique une archive « tous thèmes » (~300 Mo) et un GeoPackage
# intermédiaire (~1,7 Go), tous deux supprimés après extraction. Prévoir ~2,5 Go
# de disque transitoire, mais pas de cumul d'un département à l'autre.
#
# ── Usage ───────────────────────────────────────────────────────────────────
#   ./import_hydro_france.sh [options] <racine_data> [dept...]
#     racine_data : dossier contenant les sous-dossiers hydro_<dept>/
#     dept...     : liste de départements à importer (ex. 74 73 01 2A) ;
#                   si omis, tous les sous-dossiers hydro_* de <racine_data>
#                   sont découverts et importés (aucun téléchargement : on ne
#                   peut pas deviner les départements souhaités).
#
#   Options :
#     --download=auto   (défaut) télécharge ce qui manque, garde l'existant
#     --download=never  n'appelle jamais l'IGN ; échoue si des fichiers manquent
#     --download=force  retélécharge la dernière édition et réimporte, même si
#                       le département était déjà marqué importé
#     --keep-archives   conserve les .7z téléchargés (sinon supprimés)
#
#   Exemples :
#     ./import_hydro_france.sh ../data 74 73 01
#     ./import_hydro_france.sh --download=never ../data      # tout hydro_* trouvé
#     ./import_hydro_france.sh --download=force ../data 74   # rafraîchit le 74
#
# ── Variables d'environnement ───────────────────────────────────────────────
#   PG_CONTAINER (défaut postgres-18-fishola), GDAL_IMAGE, PGDATABASE, PGUSER,
#   PGPASSWORD — voir import_hydro_gpkg.sh.
#   IGN_EDITION — fige une livraison BD TOPO précise, voir download_hydro_ign.sh.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UNIT_SCRIPT="${SCRIPT_DIR}/import_hydro_gpkg.sh"
DOWNLOAD_SCRIPT="${SCRIPT_DIR}/download_hydro_ign.sh"

# Couches attendues par la brique unitaire (sert au test de complétude).
THEMES=(plan_d_eau cours_d_eau troncon_hydrographique surface_hydrographique)

DOWNLOAD_MODE=auto
KEEP_ARCHIVES=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --download=auto|--download=never|--download=force)
      DOWNLOAD_MODE="${1#--download=}"; shift ;;
    --keep-archives) KEEP_ARCHIVES=1; shift ;;
    --) shift; break ;;
    -*)
      echo "Option inconnue : $1" >&2
      echo "Usage : $0 [--download=auto|never|force] [--keep-archives] <racine_data> [dept...]" >&2
      exit 2 ;;
    *) break ;;
  esac
done

if [ "$#" -lt 1 ]; then
  echo "Usage : $0 [--download=auto|never|force] [--keep-archives] <racine_data> [dept...]" >&2
  exit 2
fi

DATA_ROOT="$(cd "$1" && pwd)"
shift

PG_CONTAINER=${PG_CONTAINER:-postgres-18-fishola}
PGDATABASE=${PGDATABASE:-fishola}
PGUSER=${PGUSER:-postgres}
PGPASSWORD=${PGPASSWORD:-whatever}

# Le conteneur PostgreSQL doit tourner (fail-fast avant toute itération) : inutile
# de télécharger des gigaoctets si la base n'est pas joignable.
if [ -z "$(docker ps -q -f name=^/${PG_CONTAINER}$)" ]; then
  echo "Conteneur ${PG_CONTAINER} introuvable ou arrêté." >&2
  echo "Démarrez d'abord la base : fishola-backend/start_db.sh" >&2
  exit 1
fi

if [ "${DOWNLOAD_MODE}" != "never" ] && [ ! -x "${DOWNLOAD_SCRIPT}" ]; then
  echo "Brique de téléchargement introuvable ou non exécutable : ${DOWNLOAD_SCRIPT}" >&2
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

# Vrai si les 4 couches attendues sont présentes dans le dossier départemental.
dept_data_complete() {
  local dir="$1" theme
  [ -d "${dir}" ] || return 1
  for theme in "${THEMES[@]}"; do
    [ -f "${dir}/${theme}.gpkg" ] || return 1
  done
  return 0
}

# Liste des départements : arguments explicites, sinon découverte des hydro_*.
DEPTS=()
DEPTS_EXPLICIT=1
if [ "$#" -gt 0 ]; then
  DEPTS=("$@")
else
  DEPTS_EXPLICIT=0
  for d in "${DATA_ROOT}"/hydro_*/; do
    [ -d "$d" ] || continue
    local_name="$(basename "$d")"
    DEPTS+=("${local_name#hydro_}")
  done
fi

if [ "${#DEPTS[@]}" -eq 0 ]; then
  echo "Aucun département demandé et aucun dossier hydro_<dept> sous ${DATA_ROOT}." >&2
  echo "Passez les départements en arguments, ex. : $0 ${DATA_ROOT} 74 73 01" >&2
  exit 1
fi

log "=== Import hydro France : ${#DEPTS[@]} département(s) : ${DEPTS[*]} (téléchargement : ${DOWNLOAD_MODE}) ==="

imported=0
skipped=0
downloaded=0
for dept in "${DEPTS[@]}"; do
  dept_dir="${DATA_ROOT}/hydro_${dept}"
  done_marker="${STATE_DIR}/${dept}.done"

  # Déjà importé : on ne retélécharge ni ne recharge, sauf --download=force.
  if [ -f "${done_marker}" ] && [ "${DOWNLOAD_MODE}" != "force" ]; then
    log "-- Département ${dept} : déjà importé (${done_marker}) — rien à faire."
    skipped=$((skipped + 1))
    continue
  fi

  # Récupération des données si nécessaire.
  need_download=0
  if [ "${DOWNLOAD_MODE}" = "force" ]; then
    need_download=1
  elif ! dept_data_complete "${dept_dir}"; then
    need_download=1
  fi

  if [ "${need_download}" -eq 1 ]; then
    if [ "${DOWNLOAD_MODE}" = "never" ]; then
      log "!! Département ${dept} : couches manquantes dans ${dept_dir} et --download=never — ignoré."
      continue
    fi
    if [ "${DEPTS_EXPLICIT}" -eq 0 ]; then
      # En mode découverte, un dossier incomplet est une anomalie locale : on ne
      # déclenche pas un téléchargement que l'utilisateur n'a pas demandé.
      log "!! Département ${dept} : dossier incomplet (${dept_dir}) — ignoré (mode découverte)."
      continue
    fi
    log ">> Département ${dept} : téléchargement IGN vers ${dept_dir}..."
    if KEEP_ARCHIVE="${KEEP_ARCHIVES}" "${DOWNLOAD_SCRIPT}" "${dept}" "${dept_dir}" >>"${LOG_FILE}" 2>&1; then
      log "<< Département ${dept} : téléchargement OK."
      downloaded=$((downloaded + 1))
      # Données rafraîchies : le marqueur ne vaut plus rien, l'import doit rejouer.
      rm -f "${done_marker}"
    else
      log "!! Département ${dept} : ÉCHEC du téléchargement (voir ${LOG_FILE})."
      exit 1
    fi
  fi

  if ! dept_data_complete "${dept_dir}"; then
    log "!! Département ${dept} : couches toujours incomplètes dans ${dept_dir} — ignoré."
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

log "=== Terminé : ${downloaded} téléchargé(s), ${imported} importé(s), ${skipped} déjà à jour. ==="
