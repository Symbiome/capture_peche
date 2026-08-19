#!/bin/bash
# Télécharge depuis la Géoplateforme IGN la livraison BD TOPO d'une zone
# (département par défaut) et en extrait les 4 couches hydrographiques attendues
# par import_hydro_gpkg.sh, aux noms de fichiers attendus :
#   plan_d_eau.gpkg, cours_d_eau.gpkg, troncon_hydrographique.gpkg,
#   surface_hydrographique.gpkg
#
# Remplace l'étape « aller chercher les fichiers à la main sur geoservices.ign.fr » :
# la Géoplateforme expose des URLs de téléchargement direct, sans authentification,
# décrites par un flux Atom interrogeable par zone.
#
# ── Ce que fait le script ───────────────────────────────────────────────────
#   1. résout la dernière édition GPKG publiée pour la zone (flux Atom) ;
#   2. télécharge l'archive .7z (reprise sur coupure, vérification MD5) ;
#   3. en extrait l'unique GeoPackage « tous thèmes » (~1,7 Go pour un
#      département) sans dépendance 7-Zip : GDAL sait lire /vsi7z ;
#   4. en découpe les 4 couches hydro vers <dossier_dest> ;
#   5. supprime le GeoPackage intermédiaire (et l'archive, sauf KEEP_ARCHIVE=1).
#
# Attention : BD TOPO 3.x n'est plus livrée par thème (sauf TRANSPORT) — il faut
# donc rapatrier l'archive « TOUSTHEMES » complète pour n'en garder que l'hydro.
# Prévoir ~2,5 Go de disque transitoire par département (archive + GeoPackage).
#
# ── Usage ───────────────────────────────────────────────────────────────────
#   ./download_hydro_ign.sh <zone> [dossier_dest]
#     zone          : 74, 1, 2A, 971 (normalisés en D074/D001/D02A/D971),
#                     ou une zone Géoplateforme explicite : D074, R84 (région),
#                     FXX (France métropolitaine).
#     dossier_dest  : défaut ../data/hydro_<zone_normalisée_sans_D>
#
#   Exemples :
#     ./download_hydro_ign.sh 74
#     ./download_hydro_ign.sh 2A ../data/hydro_2A
#
# ── Variables d'environnement ───────────────────────────────────────────────
#   GDAL_IMAGE   : image GDAL (défaut ghcr.io/osgeo/gdal:alpine-normal-latest) ;
#                  GDAL >= 3.7 requis (/vsi7z) et >= 3.11 (gdal vsi).
#   WORK_DIR     : dossier de travail des téléchargements
#                  (défaut <dossier_dest>/../.hydro_archives)
#   KEEP_ARCHIVE : 1 pour conserver le .7z (défaut 0 — supprimé après extraction)
#   IGN_EDITION  : fige une livraison précise, p. ex.
#                  BDTOPO_3-5_TOUSTHEMES_GPKG_LAMB93_D074_2026-03-15
#                  (défaut : la plus récente publiée pour la zone)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GDAL_IMAGE=${GDAL_IMAGE:-ghcr.io/osgeo/gdal:alpine-normal-latest}
KEEP_ARCHIVE=${KEEP_ARCHIVE:-0}

IGN_RESOURCE="https://data.geopf.fr/telechargement/resource/BDTOPO"
IGN_DOWNLOAD="https://data.geopf.fr/telechargement/download/BDTOPO"

# La Géoplateforme limite à ~1 requête/seconde : on temporise entre les appels
# au flux Atom (le téléchargement de l'archive, lui, est une requête unique).
FEED_SLEEP=${FEED_SLEEP:-1.2}

# Couches hydro consommées par import_hydro_gpkg.sql.
THEMES=(plan_d_eau cours_d_eau troncon_hydrographique surface_hydrographique)

if [ "$#" -lt 1 ]; then
  echo "Usage : $0 <zone> [dossier_dest]" >&2
  exit 2
fi

# ── Normalisation de la zone ────────────────────────────────────────────────
# 74 -> D074, 1 -> D001, 2A -> D02A, 971 -> D971 ; D074/R84/FXX repris tels quels.
raw_zone="$(echo "$1" | tr '[:lower:]' '[:upper:]')"
shift
case "${raw_zone}" in
  D*|R*|FXX|FRA|GLP|GUF|MTQ|MYT|SPM|BLM|MAF) ZONE="${raw_zone}" ;;
  *)
    padded="${raw_zone}"
    while [ "${#padded}" -lt 3 ]; do padded="0${padded}"; done
    ZONE="D${padded}"
    ;;
esac
# Suffixe de dossier : D074 -> 74, D02A -> 2A, R84 -> R84.
case "${ZONE}" in
  D0*) ZONE_SHORT="${ZONE#D0}" ;;
  D*)  ZONE_SHORT="${ZONE#D}" ;;
  *)   ZONE_SHORT="${ZONE}" ;;
esac

DEST_DIR_RAW="${1:-${SCRIPT_DIR}/../data/hydro_${ZONE_SHORT}}"
mkdir -p "${DEST_DIR_RAW}"
DEST_DIR="$(cd "${DEST_DIR_RAW}" && pwd)"

WORK_DIR_RAW="${WORK_DIR:-${DEST_DIR}/../.hydro_archives}"
mkdir -p "${WORK_DIR_RAW}"
WORK_DIR="$(cd "${WORK_DIR_RAW}" && pwd)"

echo "==> Zone ${ZONE} — destination ${DEST_DIR}"

# ── 1. Résolution de la livraison ───────────────────────────────────────────
# Le flux Atom est paginé (10 entrées/page) ; les titres portent toute
# l'information : BDTOPO_<version>_<THEME>_<FORMAT>_<CRS>_<ZONE>_<AAAA-MM-JJ>.
# On ne garde donc que les livraisons GPKG « tous thèmes » et on prend la date
# la plus récente. Le CRS varie selon la zone (LAMB93 en métropole,
# RGAF09UTM20 aux Antilles...), il n'est volontairement pas filtré.
resolve_edition() {
  local titles page pagecount body
  titles="$(mktemp)"
  pagecount=1
  page=1
  while [ "${page}" -le "${pagecount}" ]; do
    body="$(curl -fsS -m 90 --retry 3 --retry-delay 2 "${IGN_RESOURCE}?zone=${ZONE}&page=${page}")" || {
      echo "Flux Atom IGN injoignable (zone ${ZONE}, page ${page})." >&2
      rm -f "${titles}"; return 1
    }
    if [ "${page}" -eq 1 ]; then
      pagecount="$(printf '%s' "${body}" | grep -o 'gpf_dl:pagecount="[0-9]*"' | head -1 | tr -dc '0-9')"
      [ -n "${pagecount}" ] || pagecount=1
    fi
    printf '%s' "${body}" | grep -o '<title>BDTOPO[^<]*</title>' | sed 's/<[^>]*>//g' >>"${titles}"
    page=$((page + 1))
    [ "${page}" -le "${pagecount}" ] && sleep "${FEED_SLEEP}"
  done
  # Tri sur la date de fin de titre, indépendant du nombre de champs du nom.
  grep '_TOUSTHEMES_GPKG_' "${titles}" \
    | awk -F_ '{ print $NF "\t" $0 }' \
    | sort \
    | tail -1 \
    | cut -f2
  rm -f "${titles}"
}

if [ -n "${IGN_EDITION:-}" ]; then
  EDITION="${IGN_EDITION}"
  echo "--> Livraison figée par IGN_EDITION : ${EDITION}"
else
  echo "--> Résolution de la dernière livraison GPKG..."
  EDITION="$(resolve_edition)"
  if [ -z "${EDITION}" ]; then
    echo "Aucune livraison TOUSTHEMES GPKG trouvée pour la zone ${ZONE}." >&2
    echo "Vérifiez la zone : ${IGN_RESOURCE}?zone=${ZONE}" >&2
    exit 1
  fi
  echo "--> Livraison retenue : ${EDITION}"
fi

ARCHIVE_URL="${IGN_DOWNLOAD}/${EDITION}/${EDITION}.7z"
ARCHIVE="${WORK_DIR}/${EDITION}.7z"

# Taille et MD5 attendus, publiés dans le flux de la livraison.
echo "--> Lecture des métadonnées de la livraison..."
sleep "${FEED_SLEEP}"
LEAF="$(curl -fsS -m 90 --retry 3 --retry-delay 2 "${IGN_RESOURCE}/${EDITION}")" || {
  echo "Métadonnées introuvables pour ${EDITION}." >&2
  exit 1
}
EXPECTED_SIZE="$(printf '%s' "${LEAF}" | grep -o 'gpf_dl:length="[0-9]*"' | head -1 | tr -dc '0-9')"
EXPECTED_MD5="$(printf '%s' "${LEAF}" | grep -o '<content>[0-9a-f]\{32\}</content>' | head -1 | sed 's/<[^>]*>//g')"
echo "    taille annoncée : ${EXPECTED_SIZE:-inconnue} octets — MD5 : ${EXPECTED_MD5:-absent}"

# ── 2. Téléchargement ───────────────────────────────────────────────────────
local_size() { [ -f "$1" ] && wc -c <"$1" | tr -d ' ' || echo 0; }

if [ -n "${EXPECTED_SIZE}" ] && [ "$(local_size "${ARCHIVE}")" = "${EXPECTED_SIZE}" ]; then
  echo "--> Archive déjà complète (${ARCHIVE}) — réutilisée."
else
  echo "--> Téléchargement de ${ARCHIVE_URL}"
  # Barre de progression seulement en interactif : redirigée vers un fichier
  # (cas de import_hydro_france.sh, qui journalise), elle noierait le journal
  # sous des milliers de retours chariot.
  if [ -t 2 ]; then
    CURL_PROGRESS=(--progress-bar)
  else
    CURL_PROGRESS=(--no-progress-meter)
  fi
  # -C - reprend un téléchargement interrompu ; l'archive est immuable côté IGN.
  curl -fL --retry 5 --retry-delay 3 -C - "${CURL_PROGRESS[@]}" \
    -o "${ARCHIVE}" "${ARCHIVE_URL}"
fi

if [ -n "${EXPECTED_SIZE}" ] && [ "$(local_size "${ARCHIVE}")" != "${EXPECTED_SIZE}" ]; then
  echo "Taille inattendue pour ${ARCHIVE} : $(local_size "${ARCHIVE}") != ${EXPECTED_SIZE}." >&2
  echo "Supprimez le fichier et relancez." >&2
  exit 1
fi

# Vérification d'intégrité si un outil MD5 est disponible (md5sum sous Linux,
# md5 sous macOS) — sinon on prévient sans bloquer.
if [ -n "${EXPECTED_MD5}" ]; then
  if command -v md5sum >/dev/null 2>&1; then
    ACTUAL_MD5="$(md5sum "${ARCHIVE}" | cut -d' ' -f1)"
  elif command -v md5 >/dev/null 2>&1; then
    ACTUAL_MD5="$(md5 -q "${ARCHIVE}")"
  else
    ACTUAL_MD5=""
    echo "!! Ni md5sum ni md5 disponibles — intégrité non vérifiée."
  fi
  if [ -n "${ACTUAL_MD5}" ]; then
    if [ "${ACTUAL_MD5}" != "${EXPECTED_MD5}" ]; then
      echo "MD5 incorrect pour ${ARCHIVE} : ${ACTUAL_MD5} != ${EXPECTED_MD5}." >&2
      exit 1
    fi
    echo "--> MD5 vérifié."
  fi
fi

# ── 3. Extraction du GeoPackage « tous thèmes » ─────────────────────────────
# Tout passe par l'image GDAL (aucun 7-Zip requis sur l'hôte) : /vsi7z lit
# l'archive, `gdal vsi copy` en extrait le GeoPackage en flux séquentiel.
gdal_run() {
  docker run --rm \
    -v "${WORK_DIR}:/work" \
    -v "${DEST_DIR}:/out" \
    "${GDAL_IMAGE}" "$@"
}

VSI_ARCHIVE="/vsi7z//work/${EDITION}.7z"

echo "--> Localisation du GeoPackage dans l'archive..."
INNER="$(gdal_run gdal vsi list -R "${VSI_ARCHIVE}" \
  | tr -d '\r' \
  | grep '1_DONNEES_LIVRAISON' \
  | grep '\.gpkg$' \
  | head -1)"
if [ -z "${INNER}" ]; then
  echo "Aucun .gpkg trouvé sous 1_DONNEES_LIVRAISON dans ${ARCHIVE}." >&2
  exit 1
fi
echo "    ${INNER}"

FULL_GPKG_NAME="${EDITION}_full.gpkg"
FULL_GPKG="${WORK_DIR}/${FULL_GPKG_NAME}"
if [ -f "${FULL_GPKG}" ]; then
  echo "--> GeoPackage déjà extrait (${FULL_GPKG}) — réutilisé."
else
  echo "--> Extraction (plusieurs Go, quelques minutes)..."
  gdal_run gdal vsi copy "${VSI_ARCHIVE}/${INNER}" "/work/${FULL_GPKG_NAME}.part"
  mv "${FULL_GPKG}.part" "${FULL_GPKG}"
fi

# ── 4. Découpe des 4 couches hydro ──────────────────────────────────────────
# Les noms de couches BD TOPO sont préfixés par le nom de livraison
# (p. ex. bdt_35_gpkg_lamb93_d074ed20260615__plan_d_eau) : on résout donc chaque
# thème par son suffixe plutôt qu'en dur, pour rester insensible à l'édition.
echo "--> Inventaire des couches..."
# stderr est écarté volontairement : la BD TOPO déclare ses types de champs de
# façon non standard, GDAL émet donc un avertissement « Field format ... not
# supported » par champ. Fusionner ces flux (2>&1) entrelacerait les lignes et
# corromprait l'inventaire — l'absence de couches est détectée juste après.
LAYERS="$(gdal_run ogrinfo "/work/${FULL_GPKG_NAME}" 2>/dev/null \
  | sed -n 's/^[0-9]\{1,\}: \([^ ]*\).*/\1/p' | tr -d '\r')"
if [ -z "${LAYERS}" ]; then
  echo "Aucune couche listée dans ${FULL_GPKG}." >&2
  exit 1
fi

resolve_layer() {
  # Correspondance exacte d'abord, sinon suffixe <prefixe>__<theme> / _<theme>.
  local theme="$1" exact suffixed
  exact="$(printf '%s\n' "${LAYERS}" | grep -x "${theme}" || true)"
  if [ -n "${exact}" ]; then printf '%s\n' "${exact}"; return 0; fi
  suffixed="$(printf '%s\n' "${LAYERS}" | grep -E "_${theme}\$" || true)"
  local count; count="$(printf '%s' "${suffixed}" | grep -c . || true)"
  if [ "${count}" -eq 1 ]; then printf '%s\n' "${suffixed}"; return 0; fi
  if [ "${count}" -eq 0 ]; then
    echo "Couche « ${theme} » absente de ${FULL_GPKG}." >&2
  else
    # On préfère échouer que deviner : plusieurs candidats = nommage inattendu.
    echo "Couche « ${theme} » ambiguë (${count} candidats) :" >&2
    printf '%s\n' "${suffixed}" | sed 's/^/    /' >&2
  fi
  return 1
}

# Le pilote GPKG de GDAL se fie à l'extension du fichier de sortie : le nom
# temporaire doit donc rester en .gpkg (un suffixe .part est refusé).
for theme in "${THEMES[@]}"; do
  layer="$(resolve_layer "${theme}")"
  echo "--> ${theme} <- ${layer}"
  # Écriture atomique : fichier temporaire puis renommage, pour ne jamais laisser
  # un .gpkg tronqué que import_hydro_gpkg.sh prendrait pour valide.
  # Les avertissements « Field format ... not supported » sont normaux : la BD TOPO
  # déclare ses types de champs de façon non standard, GDAL les ramène au bon type.
  rm -f "${DEST_DIR}/.tmp_${theme}.gpkg"
  gdal_run ogr2ogr -f GPKG "/out/.tmp_${theme}.gpkg" "/work/${FULL_GPKG_NAME}" \
    "${layer}" -nln "${theme}"
  mv "${DEST_DIR}/.tmp_${theme}.gpkg" "${DEST_DIR}/${theme}.gpkg"
done

# ── 5. Traçabilité et nettoyage ─────────────────────────────────────────────
# Trace de la livraison réellement chargée : indispensable pour savoir, plus
# tard, quelle édition BD TOPO se trouve en base.
cat >"${DEST_DIR}/.ign_source" <<TRACE
edition=${EDITION}
zone=${ZONE}
url=${ARCHIVE_URL}
md5=${EXPECTED_MD5}
layer_source=${INNER}
downloaded_at=$(date '+%Y-%m-%d %H:%M:%S')
TRACE

rm -f "${FULL_GPKG}"
if [ "${KEEP_ARCHIVE}" = "1" ]; then
  echo "--> Archive conservée : ${ARCHIVE}"
else
  rm -f "${ARCHIVE}"
fi

echo "==> ${ZONE} prêt dans ${DEST_DIR} (${EDITION})."
ls -lh "${DEST_DIR}"/*.gpkg
