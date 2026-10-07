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
 * Sorties de plusieurs jours (#237) dans les saisies manuelles opérateur, backend stubbé :
 * date de fin pré-remplie avec la date de début, envoyée au backend, fin avant début signalée.
 */
const ANNECY = { id: "11111111-1111-4111-8111-111111111111", name: "Annecy" };
const END_NOT_AFTER_START = "La fin doit être postérieure au début";

function fieldInput(label) {
  return cy.contains("label", new RegExp(`^${label}$`)).parents(".field").first().find("input");
}

function typeDate(label, isoDate) {
  fieldInput(label).clear().type(isoDate);
}

function typeTime(label, time) {
  fieldInput(label).clear().type(time.replace(":", ""));
}

describe("Saisie manuelle — sorties de plusieurs jours (#237)", () => {

  beforeEach(() => {
    cy.intercept("POST", "**/v1/admin/login", { statusCode: 204 });
    cy.intercept("GET", "**/v1/admin/check", {
      statusCode: 200,
      body: { email: Cypress.env("staffEmail"), isNationalAdmin: false, canCreateAdmins: false, isOperator: true, departmentCodes: ["74"] },
    });
    cy.intercept("GET", "**/v1/referential/departments", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/techniques", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/species", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/waterEntities/names/search*", [ANNECY]).as("search");
    cy.intercept("POST", "**/v1/admin/manual-entries/carnet-volontaire", {
      statusCode: 201,
      body: { tripId: "55555555-5555-4555-8555-555555555555", captures: 0, errors: [] },
    }).as("submitCarnet");
    cy.intercept("POST", "**/v1/admin/manual-entries/survey", {
      statusCode: 201,
      body: { tripIds: ["33333333-3333-4333-8333-333333333333"], captures: 0, errors: [] },
    }).as("submitSurvey");
    cy.loginStaff();
  });

  describe("carnet volontaire", () => {
    beforeEach(() => cy.visit("/operator/manual-entry/carnet-volontaire"));

    it("pré-remplit la date de fin avec la date de début et envoie une sortie de trois jours", () => {
      typeDate("Date de début", "2026-07-01");
      fieldInput("Date de fin").should("have.value", "2026-07-01");

      typeDate("Date de fin", "2026-07-03");
      typeTime("Heure de début", "18:00");
      typeTime("Heure de fin", "10:00");
      cy.contains(END_NOT_AFTER_START).should("not.exist");

      cy.contains("button", "Enregistrer la sortie").click();
      cy.wait("@submitCarnet").its("request.body").should((body) => {
        expect(body.day).to.eq("2026-07-01");
        expect(body.endDay).to.eq("2026-07-03");
        expect(body.startTime).to.eq("18:00");
        expect(body.endTime).to.eq("10:00");
      });
    });

    it("signale une fin antérieure au début le même jour", () => {
      typeDate("Date de début", "2026-07-01");
      typeTime("Heure de début", "18:00");
      typeTime("Heure de fin", "10:00");
      cy.contains(END_NOT_AFTER_START).should("be.visible");
    });
  });

  describe("enquête terrain", () => {
    beforeEach(() => cy.visit("/operator/manual-entry/survey"));

    it("fait suivre la date de fin prévue et l'envoie", () => {
      typeDate("Date", "2026-07-01");
      fieldInput("Date de fin prévue").should("have.value", "2026-07-01");

      typeDate("Date de fin prévue", "2026-07-03");
      typeTime("Heure du contrôle", "09:00");
      typeTime("Heure de début", "08:00");
      typeTime("Heure de fin prévue", "07:00");
      cy.contains(END_NOT_AFTER_START).should("not.exist");

      fieldInput("Secteur").type("Ann");
      cy.wait("@search");
      cy.contains(".dropdown-item", ANNECY.name).click();
      cy.contains("button", "Enregistrer la sortie enquêtée").click();
      cy.wait("@submitSurvey").its("request.body").should((body) => {
        expect(body.day).to.eq("2026-07-01");
        expect(body.endDay).to.eq("2026-07-03");
        expect(body.endTime).to.eq("07:00");
      });
    });
  });
});
