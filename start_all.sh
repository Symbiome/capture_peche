#!/bin/bash
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DB_CONTAINER=postgres-12-fishola
PIDS=()

cleanup() {
  echo ""
  echo "==> Stopping services..."
  for pid in "${PIDS[@]}"; do
    kill "${pid}" 2>/dev/null
  done
  wait 2>/dev/null
}
trap cleanup EXIT INT TERM

echo "==> PostgreSQL (Docker)..."
if [ -n "$(docker ps -q -f name="^${DB_CONTAINER}$")" ]; then
  echo "    already running."
elif [ -n "$(docker ps -aq -f name="^${DB_CONTAINER}$")" ]; then
  docker start "${DB_CONTAINER}"
else
  (cd fishola-backend && ./start_db.sh)
fi

echo "==> Waiting for PostgreSQL to accept connections..."
until docker exec "${DB_CONTAINER}" pg_isready -U postgres >/dev/null 2>&1; do
  sleep 1
done

echo "==> Starting backend (Quarkus dev) on :8080..."
(cd fishola-backend && mvn clean compile quarkus:dev) &
PIDS+=($!)

echo "==> Starting mobile front (Vue) on :8081..."
(
  cd fishola-mobile
  [ -d node_modules ] || npm install
  npm run serve
) &
PIDS+=($!)

echo "==> Starting admin front (Vue) on :8082..."
(
  cd fishola-admin
  [ -d node_modules ] || npm install
  npm run serve
) &
PIDS+=($!)

echo ""
echo "All services starting (Ctrl+C stops everything). Logs are interleaved below."
echo "  Backend : http://localhost:8080/api/v1/status"
echo "  Mobile  : http://localhost:8081"
echo "  Admin   : http://localhost:8082"
echo ""

wait
