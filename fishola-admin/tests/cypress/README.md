# Tests Cypress — fishola-admin

- `e2e/auth-smoke.cypress.js` — smoke connexion staff (stubbé par défaut, vert sans backend).
- `support/commands.js` — `cy.loginStaff()` (connexion staff par l'UI).

Prérequis : `npm install` (Cypress pas encore installé côté admin), puis `npm run serve`
(port 8082). Lancer : `npm run cypress:run` (e2e) · `npm run cypress:open` (UI).

Conventions, drapeau `liveBackend`, variables `env` et amorce CI :
voir [`docs/tests-e2e-cypress.md`](../../../docs/tests-e2e-cypress.md).
