#!/bin/bash
# Lance les suites de tests du projet, avec un scope optionnel.
#
#   ./run_tests.sh              # tout : mobile (unit) + backend
#   ./run_tests.sh mobile       # front pêcheur — contrôle de types (tsc) + vitest
#   ./run_tests.sh backend      # Quarkus — mvn (Testcontainers → Docker + JDK >= 25)
#   ./run_tests.sh e2e          # Cypress headless — NÉCESSITE la stack lancée (start_all.sh)
#
# Notes :
# - Le front admin n'a pas de tests. Les tests unitaires du mobile tournent
#   sous vitest (vitest.config.js, qui réutilise vite.config.js) via `npm run tests`.
# - Le scope mobile lance aussi `npm run type-check` (tsc --noEmit) : le build Vite
#   passe par esbuild, qui retire les annotations de type sans les vérifier, donc
#   sans cette étape les erreurs de typage ne sont vues nulle part.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SCOPE="${1:-all}"
FAILED=0
RESULTS=""

record() {  # $1 = nom, $2 = code retour
  if [ "$2" -eq 0 ]; then RESULTS="${RESULTS}  [OK] $1\n"; else RESULTS="${RESULTS}  [KO] $1\n"; FAILED=1; fi
}

run_mobile() {
  echo ""
  echo "==> Mobile — front pêcheur (contrôle de types puis vitest)"
  (
    cd fishola-mobile
    [ -d node_modules ] || npm install
  )
  # Deux étapes enregistrées séparément : enchaînées dans un même sous-shell,
  # un tsc rouge serait masqué par des tests verts (le code retour d'un
  # sous-shell est celui de sa dernière commande).
  ( cd fishola-mobile && npm run type-check )
  record "Mobile (contrôle de types)" $?
  ( cd fishola-mobile && npm run tests )
  record "Mobile (vitest)" $?
}

run_backend() {
  echo ""
  echo "==> Backend — Quarkus (mvn ; Testcontainers -> Docker + JDK >= 25)"
  (
    cd fishola-backend
    mvn clean test
  )
  record "Backend (Quarkus)" $?
}

run_e2e() {
  echo ""
  echo "==> E2E — Cypress (front pêcheur, headless)"
  echo "    /!\\ nécessite au moins le front lancé sur le port 8081 (npm run serve)."
  echo "        Les scénarios gardés par liveBackend=true demandent la stack complète"
  echo "        (./start_all.sh) ; sans elle ils sont ignorés, pas en échec."
  (
    cd fishola-mobile
    [ -d node_modules ] || npm install
    # cypress:run => specPattern tests/cypress/e2e/** (parcours fonctionnels).
    # Les bancs CV/perf ont leur propre script (cypress:bench / cypress-report).
    npm run cypress:run
  )
  record "E2E (Cypress)" $?
}

case "$SCOPE" in
  mobile|front)   run_mobile ;;
  backend|back)   run_backend ;;
  e2e|cypress)    run_e2e ;;
  all)            run_mobile; run_backend ;;
  *)
    echo "Scope inconnu : $SCOPE"
    echo "Usage : $0 [all|mobile|backend|e2e]"
    exit 2
    ;;
esac

echo ""
echo "================ Récapitulatif ================"
printf "%b" "$RESULTS"
if [ "$FAILED" -eq 0 ]; then echo "Tout est vert."; else echo "Des suites ont échoué."; fi
exit $FAILED
