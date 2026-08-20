#!/bin/bash
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DB_CONTAINER=postgres-18-fishola
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
  # Toujours lancé (pas juste si node_modules absent) : sinon un node_modules
  # existant mais périmé (nouvelle dépendance ajoutée par un collègue) passe
  # inaperçu. npm install est rapide et sans effet si rien n'a changé.
  npm install
  npm run serve
) &
PIDS+=($!)

echo "==> Starting admin front (Vue) on :8082..."
(
  cd fishola-admin
  npm install
  npm run serve
) &
PIDS+=($!)

echo "==> Starting backoffice (Django « gestion interne ») on :8083..."
(
  cd fishola-backoffice
  [ -d .venv ] || uv venv --python 3.12 .venv
  # Toujours lancé (même raison que npm install côté front) : un .venv existant
  # mais incomplet (dépendance ajoutée par un collègue, ou installée par erreur
  # hors du venv via `uv run pip install` sans --python) passe sinon inaperçu.
  uv pip install --python .venv -r requirements.txt
  [ -f .env ] || cp .env.example .env
  # Migrations Django (tables framework + OperatorProfile) — additif & idempotent,
  # sur la base PARTAGÉE déjà prête ; cohérent avec Flyway côté Quarkus.
  # --skip-checks : sur une base neuve, auth_permission n'existe pas encore, or
  # les checks admin de django-unfold interrogent cette table AVANT que migrate
  # n'ait pu la créer (poule/œuf). Une fois migré, runserver n'a pas besoin de
  # ce flag (la table existe alors).
  .venv/bin/python manage.py migrate --noinput --skip-checks
  .venv/bin/python manage.py runserver 8083
) &
PIDS+=($!)

echo "==> Starting maildev (Docker) on :41080..."
if [ -n "$(docker ps -q -f name=^/maildev$)" ]; then
  echo "    already running."
elif [ -n "$(docker ps -aq -f name=^/maildev$)" ]; then
  docker start maildev
else
  docker run -p 41080:80 -p 41025:25 -d --name maildev --rm djfarrelly/maildev
fi

echo ""
echo "All services starting (Ctrl+C stops everything). Logs are interleaved below."
echo "  Backend    : http://localhost:8080/api/v1/status"
echo "  Mobile     : http://localhost:8081"
echo "  Admin      : http://localhost:8082"
echo "  Backoffice : http://localhost:8083/admin/"
echo "  Maildev    : http://localhost:41080"
echo ""

wait
