#!/bin/bash
# Vérifie que tous les services Fishola/capture_peche répondent, et que les
# sauvegardes pgBackRest sont à jour (#279, sur le serveur : docker requis).
#
# Usage :
#   ./health_check.sh          # via les domaines publics HTTPS (.env)
#   ./health_check.sh --local  # via 127.0.0.1:<port> (avant DNS/certbot)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

if [ ! -f "${ENV_FILE}" ]; then
  echo "Fichier .env introuvable (${ENV_FILE})." >&2
  exit 2
fi

set -a
# shellcheck disable=SC1090
. "${ENV_FILE}"
set +a

LOCAL_MODE=0
[ "${1:-}" = "--local" ] && LOCAL_MODE=1

BACKUP_MAX_AGE_HOURS=26

check() {
  local name="$1" domain="$2" port="$3" path="$4" url code

  if [ "${LOCAL_MODE}" -eq 1 ]; then
    url="http://127.0.0.1:${port}${path}"
  else
    url="https://${domain}${path}"
  fi

  code="$(curl -ks -o /dev/null -w '%{http_code}' --max-time 5 "${url}")"
  case "${code}" in
    200|301|302) echo "OK    ${name} (${code}) — ${url}" ;;
    *)           echo "ÉCHEC ${name} (${code}) — ${url}"; return 1 ;;
  esac
}

# Commande exécutée dans docker_pg en tant que postgres (propriétaire des
# données et du dépôt pgBackRest).
pg_exec() {
  docker compose -f "${SCRIPT_DIR}/docker-compose.yml" exec -T -u postgres docker_pg "$@"
}

# Âge en secondes de la dernière sauvegarde pgBackRest, vide s'il n'y en a aucune.
last_backup_age_seconds() {
  pg_exec pgbackrest --stanza=fishola --output=json info | python3 -c '
import json, sys, time
backups = json.load(sys.stdin)[0].get("backup", [])
print(int(time.time() - backups[-1]["timestamp"]["stop"]) if backups else "")
'
}

check_last_backup() {
  local age
  if ! age="$(last_backup_age_seconds 2>/dev/null)"; then
    echo "ÉCHEC Sauvegarde — pgbackrest info a échoué (docker_pg arrêté ?)"
    return 1
  fi
  if [ -z "${age}" ]; then
    echo "ÉCHEC Sauvegarde — aucune sauvegarde dans le dépôt pgBackRest"
    return 1
  fi
  if [ "${age}" -gt $((BACKUP_MAX_AGE_HOURS * 3600)) ]; then
    echo "ÉCHEC Sauvegarde — dernière il y a $((age / 3600)) h (max ${BACKUP_MAX_AGE_HOURS} h)"
    return 1
  fi
  echo "OK    Sauvegarde — dernière il y a $((age / 3600)) h"
}

check_wal_archiving() {
  local failed
  failed="$(pg_exec psql -At -c 'SELECT failed_count FROM pg_stat_archiver' 2>/dev/null)"
  if [ "${failed}" = "0" ]; then
    echo "OK    Archivage WAL — aucun échec"
  else
    echo "ÉCHEC Archivage WAL — failed_count=${failed:-?} (cf. DEPLOYMENT.md, « Sauvegardes »)"
    return 1
  fi
}

status=0
check "Backend"       "${API_DOMAIN}"        "${BACKEND_PORT:-8080}"    "/api/v1/status" || status=1
check "Front pêcheur" "${APP_DOMAIN}"        "${MOBILE_PORT:-8081}"     "/"              || status=1
check "Front admin"   "${ADMIN_DOMAIN}"      "${ADMIN_PORT:-8082}"      "/"              || status=1

# Maildev n'est pas exposé derrière le reverse proxy (outil dev interne uniquement) :
# on ne peut le vérifier qu'en local, directement sur le port du conteneur.
if [ "${LOCAL_MODE}" -eq 1 ]; then
  check "Maildev" "" "${MAILDEV_PORT:-41080}" "/" || status=1
fi

if command -v docker >/dev/null 2>&1; then
  check_last_backup   || status=1
  check_wal_archiving || status=1
else
  echo "IGNORÉ Sauvegardes — docker indisponible sur cette machine"
fi

exit "${status}"
