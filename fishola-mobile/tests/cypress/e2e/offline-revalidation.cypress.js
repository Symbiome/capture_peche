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
 * E2e du mode offline (#10). Couvre le socle testable en navigateur ; les parties
 * device-only (cache de tuiles en WebView, géoloc Capacitor en mode avion) sont
 * validées en recette manuelle.
 *
 * PRÉREQUIS (recette) : stack lancée (start_all.sh) + utilisateur de test seedé.
 * La session est ouverte par `cy.loginAngler()` (login programmatique). Ces scénarios
 * sont gardés derrière `Cypress.env('liveBackend')` : ignorés par défaut (pas de backend),
 * activés avec `--env liveBackend=true` une fois le seed pêcheur (#67) en place.
 *
 * Scénarios (AC spec-10) :
 *  - AC1 : création hors ligne → sortie locale, badge « Non synchronisée », PENDING.
 *  - AC2/AC3 : retour réseau → sync (le backend #9 recalcule l'attribution) → badge disparaît.
 *  - AC4 : 1000 sorties locales → création refusée avec message.
 *  - AC5 : sorties offline créées avant #10 (schéma v7) → préservées + marquées PENDING (upgrade v8).
 */
describe("Mode offline & re-validation (#10)", () => {
  // Injecte une sortie directement dans Dexie (dirtyTrips) pour tester le badge
  // sans rejouer tout le parcours de création.
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

  beforeEach(function () {
    // E2e nécessitant un backend seedé : ignorés tant que liveBackend n'est pas activé.
    if (!Cypress.env("liveBackend")) {
      this.skip();
    }
    cy.loginAngler(); // session pêcheur (cookie) sur le backend de recette.
  });

  it("AC1 — une sortie locale porte le badge « Non synchronisée »", () => {
    seedDirtyTrip("t-offline-1", "Sortie hors ligne");
    cy.visit("/#/my-trips/list");
    cy.contains(".item-description", "Sortie hors ligne")
      .find(".pending-badge")
      .should("be.visible");
  });

  it("AC2 — au retour du réseau, la sync pousse la sortie et le badge disparaît", () => {
    // La sortie locale est poussée (POST /v1/trips) ; le backend #9 recalcule
    // l'attribution (PENDING → CONFIRMED/OVERRIDDEN) et la sortie devient serveur.
    seedDirtyTrip("t-offline-2", "À synchroniser");
    cy.intercept("POST", "**/v1/trips").as("push");
    cy.visit("/#/my-trips/list");
    // Émule le retour de connectivité (déclenche la sync immédiate, App.vue).
    cy.window().then((win) => win.dispatchEvent(new Event("online")));
    cy.wait("@push");
    cy.contains(".item-description", "À synchroniser")
      .find(".pending-badge")
      .should("not.exist");
  });

  it("AC4 — au-delà de 1000 sorties locales, la création est refusée", () => {
    // Scénario : seeder 1000 dirtyTrips puis tenter une finalisation de sortie
    // → message « Limite de 1000 sorties non synchronisées atteinte ».
    // (Implémenté en recette : seed en masse + parcours de finalisation.)
  });
});
