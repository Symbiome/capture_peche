#!/bin/bash
# Vérifie que tous les services Fishola/capture_peche répondent.
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

status=0
check "Backend"       "${API_DOMAIN}"        "${BACKEND_PORT:-8080}"    "/api/v1/status" || status=1
check "Front pêcheur" "${APP_DOMAIN}"        "${MOBILE_PORT:-8081}"     "/"              || status=1
check "Front admin"   "${ADMIN_DOMAIN}"      "${ADMIN_PORT:-8082}"      "/"              || status=1

# Maildev n'est pas exposé derrière le reverse proxy (outil dev interne uniquement) :
# on ne peut le vérifier qu'en local, directement sur le port du conteneur.
if [ "${LOCAL_MODE}" -eq 1 ]; then
  check "Maildev" "" "${MAILDEV_PORT:-41080}" "/" || status=1
fi

exit "${status}"
