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
 * Origine de la donnée et code session (#235), backend stubbé : libellés métier dans les
 * listes, filtre sur la valeur technique exacte, code session de la saisie carnet.
 */
const ROW = {
  catchId: "66666666-6666-4666-8666-666666666666",
  dateDeLaSortie: "01/07/2026",
  especeCapturee: "perche",
  origineDonnee: "enquete_souvenir",
  codeSession: "ENQ-001",
};

function stubStaff() {
  cy.intercept("POST", "**/v1/admin/login", { statusCode: 204 });
  cy.intercept("GET", "**/v1/admin/check", {
    statusCode: 200,
    body: { email: Cypress.env("staffEmail"), isNationalAdmin: false, canCreateAdmins: false, isOperator: true, departmentCodes: ["74"] },
  });
  cy.intercept("GET", "**/v1/referential/departments", { statusCode: 200, body: [] });
  cy.intercept("GET", "**/v1/referential/techniques", { statusCode: 200, body: [] });
  cy.intercept("GET", "**/v1/referential/species", { statusCode: 200, body: [] });
}

describe("Origine de la donnée et code session (#235)", () => {

  beforeEach(() => {
    stubStaff();
    cy.intercept("GET", "**/v1/trips/export/**", { statusCode: 200, body: { elements: [ROW], total: 1 } }).as("export");
    cy.intercept("GET", "**/v1/trips/catches/pending-validation/**", { statusCode: 200, body: { elements: [ROW], total: 1 } }).as("toValidate");
    cy.loginStaff();
  });

  it("affiche l'origine en libellé métier et le code session dans la liste des captures", () => {
    cy.visit("/trips");
    cy.wait("@export");
    cy.contains("td", "Enquête – sortie souvenir").should("be.visible");
    cy.contains("td", "ENQ-001").should("be.visible");
  });

  it("affiche l'origine et le code session dans les prises à valider", () => {
    cy.visit("/catches/to-validate");
    cy.wait("@toValidate");
    cy.contains("td", "Enquête – sortie souvenir").should("be.visible");
    cy.contains("td", "ENQ-001").should("be.visible");
  });

  it("filtre sur la valeur technique exacte de l'origine", () => {
    cy.visit("/trips");
    cy.wait("@export");
    cy.contains("option", "Carnet volontaire").parent("select").select("Carnet volontaire");
    cy.wait("@export").its("request.url").should("contain", "origine_donnee=carnet_volontaire");
  });

  it("envoie le code session saisi sur le carnet papier", () => {
    cy.intercept("POST", "**/v1/admin/manual-entries/carnet-volontaire", {
      statusCode: 201,
      body: { tripId: "55555555-5555-4555-8555-555555555555", captures: 0, errors: [] },
    }).as("submitCarnet");
    cy.visit("/operator/manual-entry/carnet-volontaire");
    cy.contains("label", "Code session (facultatif)").parents(".field").first().find("input").type("CARNET-42");
    cy.contains("button", "Enregistrer la sortie").click();
    cy.wait("@submitCarnet").its("request.body.sessionCode").should("eq", "CARNET-42");
  });
});
