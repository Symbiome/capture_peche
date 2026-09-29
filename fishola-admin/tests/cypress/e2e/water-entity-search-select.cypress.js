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
 * Sélecteur d'entité hydrographique en v-model (#189), backend stubbé.
 *
 * Régression : le composant ne déclarait pas la prop `modelValue`, qui retombait sur le
 * <b-autocomplete> interne et écrasait son texte ; la sélection était perdue et la saisie
 * d'enquête partait avec `waterEntityId: null` (400).
 */
const ANNECY = { id: "11111111-1111-4111-8111-111111111111", name: "Annecy" };
const BOURGET = { id: "22222222-2222-4222-8222-222222222222", name: "Bourget" };

function fieldInput(label) {
  return cy.contains("label", label).parents(".field").first().find("input");
}

function selectWaterEntity(label, term, option) {
  fieldInput(label).type(term);
  cy.wait("@search");
  cy.contains(".dropdown-item", option.name).click();
  fieldInput(label).should("have.value", option.name);
}

describe("Saisie d'enquête — sélection de l'entité hydrographique (#189)", () => {

  beforeEach(() => {
    cy.intercept("POST", "**/v1/admin/login", { statusCode: 204 });
    cy.intercept("GET", "**/v1/admin/check", {
      statusCode: 200,
      body: { email: Cypress.env("staffEmail"), isNationalAdmin: false, canCreateAdmins: false, isOperator: true, departmentCodes: ["74"] },
    });
    cy.intercept("GET", "**/v1/referential/departments", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/techniques", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/species", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/waterEntities/names/search*", (req) => {
      const q = String(req.query.q || "").toLowerCase();
      req.reply([ANNECY, BOURGET].filter((we) => we.name.toLowerCase().includes(q)));
    }).as("search");
    cy.intercept("POST", "**/v1/admin/manual-entries/survey", {
      statusCode: 201,
      body: { tripIds: ["33333333-3333-4333-8333-333333333333"], captures: 0, errors: [] },
    }).as("submit");

    cy.loginStaff();
    cy.visit("/operator/manual-entry/survey");
  });

  it("garde le nom affiché et envoie l'UUID du secteur et du site pêché", () => {
    selectWaterEntity("Secteur", "Ann", ANNECY);

    cy.contains("Session souvenir").click();
    selectWaterEntity("Site pêché", "Bour", BOURGET);

    cy.contains("button", "Enregistrer la sortie enquêtée").click();
    cy.wait("@submit").its("request.body").should((body) => {
      expect(body.waterEntityId).to.eq(ANNECY.id);
      expect(body.anglers[0].souvenir.waterEntityId).to.eq(BOURGET.id);
    });

    // Formulaire réinitialisé après succès : le secteur est vidé.
    fieldInput("Secteur").should("have.value", "");
  });

  it("vider le champ remet le secteur à null sans effacer la frappe", () => {
    selectWaterEntity("Secteur", "Ann", ANNECY);

    // Retaper après une sélection (curseur en fin : Buefy laisse le texte sélectionné) :
    // Buefy désélectionne, le texte saisi reste.
    fieldInput("Secteur").type("{end}x");
    fieldInput("Secteur").should("have.value", "Annecyx");

    cy.contains("button", "Enregistrer la sortie enquêtée").click();
    cy.wait("@submit").its("request.body.waterEntityId").should("eq", null);
  });

  describe("saisie de la position sur la carte", () => {
    const LEMAN = { waterEntityId: "44444444-4444-4444-8444-444444444444", name: "Léman", distanceM: 850 };

    beforeEach(() => {
      // Tuiles IGN et hydro : hors sujet ici, on ne dépend pas du réseau.
      cy.intercept("GET", "https://data.geopf.fr/**", { statusCode: 204 });
      cy.intercept("GET", "**/v1/tiles/hydro/**", { statusCode: 204 });
      cy.intercept("GET", "**/v1/referential/waterEntities/attribution*", {
        statusCode: 200,
        body: {
          proposal: { waterEntityId: ANNECY.id, name: ANNECY.name, distanceM: 120 },
          alternatives: [LEMAN]
        }
      }).as("attribution");
    });

    function pickOnMap(optionName) {
      cy.contains("button", "Carte").first().click();
      cy.get("[data-cy=map-picker-map] canvas", { timeout: 10000 }).should("be.visible").click("center");
      cy.wait("@attribution").its("request.query").should((query) => {
        expect(Number(query.lat)).to.be.within(-90, 90);
        expect(Number(query.lng)).to.be.within(-180, 180);
      });
      cy.contains(".modal-card button", optionName).click();
      cy.get(".modal-card").should("not.exist");
    }

    it("rattache le point cliqué à l'entité choisie et l'envoie avec la sortie", () => {
      pickOnMap("Léman");
      fieldInput("Secteur").should("have.value", "Léman");
      cy.contains(".position-label", "Position :").should("be.visible");

      cy.contains("button", "Enregistrer la sortie enquêtée").click();
      cy.wait("@submit").its("request.body").should((body) => {
        expect(body.waterEntityId).to.eq(LEMAN.waterEntityId);
        expect(body.latitude).to.be.a("number");
        expect(body.longitude).to.be.a("number");
      });
    });

    it("choisir ensuite une entité par son nom retire le point de la carte", () => {
      pickOnMap("Léman");
      fieldInput("Secteur").clear();
      selectWaterEntity("Secteur", "Ann", ANNECY);
      cy.get(".position-label").should("not.exist");

      cy.contains("button", "Enregistrer la sortie enquêtée").click();
      cy.wait("@submit").its("request.body").should((body) => {
        expect(body.waterEntityId).to.eq(ANNECY.id);
        expect(body.latitude).to.eq(null);
        expect(body.longitude).to.eq(null);
      });
    });
  });

  it("carnet volontaire : la position de la carte part en latitude/longitude", () => {
    cy.intercept("GET", "https://data.geopf.fr/**", { statusCode: 204 });
    cy.intercept("GET", "**/v1/tiles/hydro/**", { statusCode: 204 });
    cy.intercept("GET", "**/v1/referential/waterEntities/attribution*", {
      statusCode: 200,
      body: { proposal: { waterEntityId: ANNECY.id, name: ANNECY.name, distanceM: 120 }, alternatives: [] }
    }).as("attribution");
    cy.intercept("POST", "**/v1/admin/manual-entries/carnet-volontaire", {
      statusCode: 201,
      body: { tripId: "55555555-5555-4555-8555-555555555555", captures: 0, errors: [] }
    }).as("submitCarnet");

    cy.visit("/operator/manual-entry/carnet-volontaire");
    cy.contains("button", "Carte").click();
    cy.get("[data-cy=map-picker-map] canvas", { timeout: 10000 }).should("be.visible").click("center");
    cy.wait("@attribution");
    cy.contains(".modal-card button", "Annecy").click();

    cy.contains("button", "Enregistrer la sortie").click();
    cy.wait("@submitCarnet").its("request.body").should((body) => {
      expect(body.waterEntityId).to.eq(ANNECY.id);
      expect(body.latitude).to.be.a("number");
      expect(body.longitude).to.be.a("number");
      expect(body).not.to.have.property("position");
    });
  });
});
