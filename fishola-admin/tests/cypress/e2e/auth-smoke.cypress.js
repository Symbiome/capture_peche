/*-
 * #%L
 * Fishola :: Admin
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

/**
 * SMOKE — connexion staff au back-office admin (socle #76).
 *
 * Deux modes :
 *  - par défaut : backend STUBBÉ (`cy.intercept`) → aucun backend requis, vert en CI headless ;
 *  - `--env liveBackend=true` : E2E RÉEL contre un backend démarré + seedé (#67).
 *
 * C'est l'amorce demandée par #76 : un parcours de connexion + la commande réutilisable
 * `cy.loginStaff()`. La refonte complète des scénarios viendra dans une issue dédiée.
 */
const HOME_MARKER = "Bienvenue sur l'interface d'administration de FISHOLA";

describe("Smoke — connexion staff (back-office admin)", () => {

  it("connexion via cy.loginStaff() → arrivée sur la home (backend stubbé)", () => {
    // Contrat d'API de connexion, stubé : le smoke valide le parcours front sans backend.
    cy.intercept("POST", "**/v1/admin/login", { statusCode: 204 }).as("login");
    cy.intercept("GET", "**/v1/admin/check", {
      statusCode: 200,
      body: {
        email: Cypress.env("staffEmail"),
        isNationalAdmin: true,
        canCreateAdmins: true,
        isOperator: false,
      },
    }).as("check");
    cy.intercept("GET", "**/v1/referential/waterEntities", { statusCode: 200, body: [] }).as("waterEntities");

    cy.loginStaff();

    cy.wait("@login");
    cy.contains(HOME_MARKER).should("be.visible");
  });

  it("mauvais mot de passe → message d'erreur, pas de redirection", () => {
    cy.intercept("POST", "**/v1/admin/login", { statusCode: 401, body: { error: "bad credentials" } }).as("login");

    cy.visit("/login");
    cy.get('input[type="email"]').type("amorel@codelutin.com");
    cy.get('input[type="password"]').type("mauvais-mot-de-passe", { log: false });
    cy.contains("button", "Connexion").click();

    cy.wait("@login");
    cy.contains("Mot de passe incorrect").should("be.visible");
    cy.location("pathname").should("eq", "/login");
  });

  // E2E RÉEL — activé uniquement avec un backend seedé (#67) :
  //   npx cypress run --env liveBackend=true
  it("E2E réel : connexion staff contre backend seedé (#67)", function () {
    if (!Cypress.env("liveBackend")) {
      this.skip();
    }

    cy.loginStaff();
    cy.contains(HOME_MARKER).should("be.visible");

    // Le cookie de session admin est posé : le dispatcher (« / ») renvoie vers la home.
    cy.visit("/");
    cy.location("pathname", { timeout: 10000 }).should("eq", "/home");
  });
});
