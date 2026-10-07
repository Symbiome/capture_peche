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
 * Saisie d'enquête — date et heure du contrôle pré-renseignées avec celles de
 * l'appareil (#234), backend stubbé.
 *
 * L'horloge est figée à 00:30 heure locale : avec l'ancien calcul UTC
 * (`toISOString()`), la date affichée était celle de la veille dès que le
 * fuseau du navigateur est en avance sur UTC (France, Europe/Paris).
 */
const ANNECY = { id: "11111111-1111-4111-8111-111111111111", name: "Annecy" };
const HALF_PAST_MIDNIGHT = new Date(2026, 6, 15, 0, 30, 42);
const SIXTY_FIVE_MINUTES_MS = 65 * 60 * 1000;

function fieldInput(label) {
  return cy.contains("label", new RegExp(`^${label}$`)).parents(".field").first().find("input");
}

describe("Saisie d'enquête — pré-remplissage date et heure du contrôle (#234)", () => {

  beforeEach(() => {
    cy.clock(HALF_PAST_MIDNIGHT, ["Date"]);
    cy.intercept("POST", "**/v1/admin/login", { statusCode: 204 });
    cy.intercept("GET", "**/v1/admin/check", {
      statusCode: 200,
      body: { email: Cypress.env("staffEmail"), isNationalAdmin: false, canCreateAdmins: false, isOperator: true, departmentCodes: ["74"] },
    });
    cy.intercept("GET", "**/v1/referential/departments", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/techniques", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/species", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/waterEntities/names/search*", [ANNECY]).as("search");
    cy.intercept("POST", "**/v1/admin/manual-entries/survey", {
      statusCode: 201,
      body: { tripIds: ["33333333-3333-4333-8333-333333333333"], captures: 0, errors: [] },
    }).as("submit");

    cy.loginStaff();
    cy.visit("/operator/manual-entry/survey");
  });

  it("pré-renseigne la date locale du jour et l'heure du contrôle, laisse début et fin vides", () => {
    fieldInput("Date").should("have.value", "2026-07-15").and("have.attr", "max", "2026-07-15");
    fieldInput("Heure du contrôle").should("have.value", "00:30");
    fieldInput("Heure de début").should("have.value", "");
    fieldInput("Heure de fin prévue").should("have.value", "");
  });

  it("laisse la date et l'heure du contrôle modifiables", () => {
    fieldInput("Date").clear().type("2026-07-10").should("have.value", "2026-07-10");
    fieldInput("Heure du contrôle").clear().type("1415").should("have.value", "14:15");
  });

  it("garde la règle contrôle antérieur au début", () => {
    fieldInput("Heure de début").type("0100");
    cy.contains("L'heure du contrôle ne peut pas être antérieure à l'heure de début").should("be.visible");
  });

  it("ne pré-renseigne pas la date de la session souvenir", () => {
    cy.contains("Session souvenir").click();
    cy.get(".souvenir-box input[type=date]").should("have.value", "");
  });

  it("reprend la date et l'heure courantes après enregistrement", () => {
    fieldInput("Secteur").type("Ann");
    cy.wait("@search");
    cy.contains(".dropdown-item", ANNECY.name).click();

    cy.tick(SIXTY_FIVE_MINUTES_MS);
    cy.contains("button", "Enregistrer la sortie enquêtée").click();
    cy.wait("@submit").its("request.body").should((body) => {
      expect(body.day).to.eq("2026-07-15");
      expect(body.controlTime).to.eq("00:30");
    });

    fieldInput("Secteur").should("have.value", "");
    fieldInput("Date").should("have.value", "2026-07-15");
    fieldInput("Heure du contrôle").should("have.value", "01:35");
  });
});
