#!/bin/bash
# Charge les contours de communes d'un département dans la table `commune`
# (RECETTE / DEV). Source : geo.api.gouv.fr (données officielles INSEE/IGN,
# contours COG) — plus léger que le GeoPackage ADMIN EXPRESS national, même
# donnée pour le périmètre voulu. Pour la V1, utiliser plutôt
# import_admin_gpkg.sh avec le gpkg ADMIN EXPRESS officiel (cf. issue #14).
#
# Usage : ./import_communes_geoapi.sh [code_departement]   (défaut : 74)
# Tout passe par Docker (psql du conteneur PG + GDAL éphémère), comme les
# autres imports.
set -euo pipefail

DEPT="${1:-74}"
PG_CONTAINER="${PG_CONTAINER:-postgres-18-fishola}"
GDAL_IMAGE="${GDAL_IMAGE:-ghcr.io/osgeo/gdal:alpine-normal-latest}"
PGDATABASE="${PGDATABASE:-fishola}"
PGUSER="${PGUSER:-postgres}"
PGPASSWORD="${PGPASSWORD:-whatever}"
CONN="host=127.0.0.1 port=5432 dbname=${PGDATABASE} user=${PGUSER} password=${PGPASSWORD}"

if [ -z "$(docker ps -q -f name="^/${PG_CONTAINER}$")" ]; then
  echo "Conteneur ${PG_CONTAINER} introuvable ou arrêté." >&2
  exit 1
fi

TMPDIR="$(mktemp -d)"
trap 'rm -rf "${TMPDIR}"' EXIT
GEOJSON="${TMPDIR}/communes_${DEPT}.geojson"

echo "==> Téléchargement des contours communes du département ${DEPT} (geo.api.gouv.fr)..."
curl -fsS "https://geo.api.gouv.fr/communes?codeDepartement=${DEPT}&fields=code,nom&format=geojson&geometry=contour" -o "${GEOJSON}"
echo "    $(python3 -c "import json;print(len(json.load(open('${GEOJSON}'))['features']))" 2>/dev/null || echo '?') communes."

echo "==> Staging via ogr2ogr..."
docker run --rm --network "container:${PG_CONTAINER}" -v "${TMPDIR}:/data:ro" "${GDAL_IMAGE}" \
  ogr2ogr -f PostgreSQL "PG:${CONN}" "/data/$(basename "${GEOJSON}")" \
  -nln commune_staging -overwrite -lco GEOMETRY_NAME=geom -nlt MULTIPOLYGON -t_srs EPSG:4326

echo "==> Upsert dans commune (clé naturelle insee_com)..."
docker exec -i -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" psql -v ON_ERROR_STOP=1 "${CONN}" <<'SQL'
INSERT INTO commune (insee_com, name, geom)
SELECT code, nom, ST_Multi(ST_Force2D(geom)) FROM commune_staging
ON CONFLICT ON CONSTRAINT commune_insee_com_key DO UPDATE
    SET name = EXCLUDED.name, geom = EXCLUDED.geom;
DROP TABLE commune_staging;
SQL

echo "==> Codes postaux (code_postal principal, #15)..."
# Garde si le script est lancé avant la migration V1.1.4 (colonne créée par Flyway).
docker exec -i -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" psql -q -c \
  "ALTER TABLE public.commune ADD COLUMN IF NOT EXISTS code_postal varchar(5)" "${CONN}"
CP_JSON="${TMPDIR}/cp_${DEPT}.json"
curl -fsS "https://geo.api.gouv.fr/communes?codeDepartement=${DEPT}&fields=code,codesPostaux&format=json" -o "${CP_JSON}"
python3 - "${CP_JSON}" "${TMPDIR}/cp_update.sql" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
with open(sys.argv[2], "w") as out:
    for c in d:
        cps = c.get("codesPostaux") or []
        if cps:
            out.write(f"UPDATE commune SET code_postal='{cps[0]}' WHERE insee_com='{c['code']}';\n")
PY
docker exec -i -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" psql -q "${CONN}" < "${TMPDIR}/cp_update.sql"

docker exec -i -e PGPASSWORD="${PGPASSWORD}" "${PG_CONTAINER}" psql -tAc \
  "SELECT count(*) || ' communes (' || count(code_postal) || ' avec CP)' FROM commune" "${CONN}"
echo "==> Terminé."
