/*-
 * #%L
 * Fishola :: Mobile
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
/* eslint-disable no-undef */
/**
 * Reprise de la synchronisation après un échec réseau.
 *
 * Non couvert par `offline-revalidation.cypress.js`, qui ne teste qu'une passe de
 * synchronisation réussie. Le cas régressif est celui du terrain : une PREMIÈRE
 * passe échoue (réseau coupé), et il faut vérifier qu'une passe ULTÉRIEURE peut
 * encore s'exécuter une fois le réseau revenu — c'est-à-dire que le verrou
 * `syncInProgress` a bien été relâché malgré l'échec.
 *
 * Entièrement bouchonné (aucun backend requis) pour rester exécutable en CI.
 */
describe("Reprise de la synchronisation après échec réseau", () => {
  function stubSession() {
    cy.intercept("GET", "**/v1/security/profile", {
      statusCode: 200,
      body: { id: "u-1", email: "pecheur@test", firstName: "Test", lastName: "Pecheur" },
    }).as("profile");
    cy.intercept("GET", "**/v1/security/settings", { statusCode: 200, body: {} });
    cy.intercept("GET", "**/v1/trips/markers", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/trips?*", { statusCode: 200, body: { elements: [], count: 0 } });
    cy.intercept("GET", "**/v1/documentations", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/species-per-waterEntity", { statusCode: 200, body: {} });
    cy.intercept("GET", "**/v1/referential/waterEntities", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/waterEntities/favorites", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/species", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/species-custom", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/weathers", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/techniques", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/released-fish-states", { statusCode: 200, body: [] });
  }

  // Sortie complète (date + plan d'eau) : seule une sortie synchronisable est poussée.
  function seedDirtyTrip(id, name) {
    return cy.window().then((win) => {
      const req = win.indexedDB.open("Fishola");
      return new Cypress.Promise((resolve) => {
        req.onsuccess = () => {
          const db = req.result;
          const tx = db.transaction("dirtyTrips", "readwrite");
          tx.objectStore("dirtyTrips").put({
            id,
            name,
            lakeId: "lac-1",
            date: new Date().toISOString(),
            startedAt: "08:00",
            finishedAt: "10:00",
            catchs: [],
            speciesIds: [],
            techniqueIds: [],
          });
          tx.oncomplete = () => { db.close(); resolve(); };
        };
      });
    });
  }

  it("une passe en échec ne bloque pas les synchronisations suivantes", () => {
    stubSession();

    // 1re passe : le réseau est coupé, le push échoue.
    cy.intercept("POST", "**/v1/trips", { forceNetworkError: true }).as("pushKO");

    cy.visit("/#/my-trips/list");
    cy.wait("@profile");
    seedDirtyTrip("t-sync-recovery", "Sortie à resynchroniser");

    cy.window().then((win) => win.dispatchEvent(new Event("online")));
    cy.wait("@pushKO");

    // 2e passe : le réseau est revenu. Sans relâchement du verrou `syncInProgress`,
    // aucune requête ne repart et ce `cy.wait` échoue en timeout.
    cy.intercept("POST", "**/v1/trips", { statusCode: 201, body: { id: "t-sync-recovery" } }).as("pushOK");
    cy.window().then((win) => win.dispatchEvent(new Event("online")));
    cy.wait("@pushOK").its("response.statusCode").should("eq", 201);
  });

  it("le polling periodique pousse une sortie en attente sans action utilisateur", () => {
    stubSession();
    // Horloge controlee AVANT le montage de l'app, pour piloter le setInterval(30 s)
    // installe par App.vue plutot que d'attendre reellement.
    cy.clock();
    cy.intercept("POST", "**/v1/trips", { statusCode: 201, body: { id: "t-poll" } }).as("pushPoll");

    cy.visit("/#/my-trips/list");
    seedDirtyTrip("t-poll", "Sortie poussee par le polling");

    // Une seule tranche de 30 s doit suffire a declencher une passe de synchro.
    cy.tick(31000);
    cy.wait("@pushPoll").its("response.statusCode").should("eq", 201);
  });
});
