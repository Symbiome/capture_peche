#!/bin/bash
# Arrête tout ce que `start_all.sh` démarre : serveurs de dev natifs (Quarkus,
# fronts Vue, Django) + conteneurs Docker (maildev, PostgreSQL).
# `start_all.sh` nettoie déjà ses processus au Ctrl+C ; `down_all.sh` sert à
# tout couper d'un coup (y compris les conteneurs Docker et d'éventuels orphelins).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DB_CONTAINER=postgres-18-fishola

# Ports des serveurs de dev lancés en natif (cf. start_all.sh).
#   8080 backend Quarkus · 8081 mobile · 8082 admin · 8083 backoffice Django
echo "==> Stopping dev servers (by port)..."
for port in 8080 8081 8082 8083; do
  pids="$(lsof -ti "tcp:${port}" 2>/dev/null || true)"
  if [ -n "${pids}" ]; then
    kill ${pids} 2>/dev/null || true
    echo "    :${port} stopped."
  else
    echo "    :${port} not running."
  fi
done

echo "==> Stopping Docker containers..."
# maildev est lancé en --rm : `stop` le supprime aussi.
if [ -n "$(docker ps -q -f name='^maildev$')" ]; then
  docker stop maildev >/dev/null && echo "    maildev stopped."
else
  echo "    maildev not running."
fi

# PostgreSQL : on stoppe le conteneur ; les données restent dans le volume
# (redémarrage via start_all.sh / start_db.sh).
if [ -n "$(docker ps -q -f name="^${DB_CONTAINER}$")" ]; then
  docker stop "${DB_CONTAINER}" >/dev/null && echo "    ${DB_CONTAINER} stopped (données conservées dans le volume)."
else
  echo "    ${DB_CONTAINER} not running."
fi

echo "Done."
