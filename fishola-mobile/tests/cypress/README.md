# Tests Cypress — fishola-mobile

- `e2e/` — parcours fonctionnels (gating CI). Requiert `npm run serve` (port 8081).
- `bench/` — bancs CV / perf (hors CI), rapport via `npm run cypress-report`.
- `support/commands.js` — `cy.loginAngler()` (connexion pêcheur programmatique).

Lancer : `npm run cypress:run` (e2e, ou `./run_tests.sh e2e` depuis la racine) · `npm run cypress:bench` (bancs).

Conventions, drapeau `liveBackend`, variables `env` et amorce CI :
voir [`docs/tests-e2e-cypress.md`](../../../docs/tests-e2e-cypress.md).
