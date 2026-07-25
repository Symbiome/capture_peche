#!/bin/bash
# Applique le seed de recette à la base de dev locale (rejouable, idempotent).
# Usage : ./scripts/seed_recette.sh
set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-postgres-18-fishola}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -z "$(docker ps -q -f name="^${DB_CONTAINER}$")" ]; then
  echo "Base de dev absente (conteneur '${DB_CONTAINER}' non démarré). Lancez d'abord ./start_all.sh" >&2
  exit 1
fi

echo "==> Seed de recette -> base 'fishola' (conteneur ${DB_CONTAINER})"
docker exec -i "${DB_CONTAINER}" psql -v ON_ERROR_STOP=1 -U postgres -d fishola < "${SCRIPT_DIR}/seed_recette.sql"
echo "==> OK. Comptes : national/regional/operateur.recette@fishola.test + pecheur.recette@fishola.test (mdp « Recette2026! »)."
