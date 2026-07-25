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
 * E2e de la carte MapLibre (#8). On charge le composant via le banc de test
 * public /#/map-test : pas d'authentification ni de données requises. Les tuiles
 * réseau (fonds IGN + réseau hydro MVT) sont stubbées, car MapLibre rend en
 * WebGL et les assertions passent par le DOM/les événements, pas par le pixel.
 *
 * Couvre : la carte s'initialise (AC1, canvas présent) et le bascule
 * Plan/Satellite (AC2). La sélection au tap (AC3) et le cache 304 (AC5)
 * nécessitent des tuiles réelles sur données seedées → validés en recette.
 */
describe("Carte MapLibre", () => {
  beforeEach(() => {
    // Fonds IGN : on renvoie un corps vide (MapLibre encaisse l'erreur de tuile
    // sans bloquer l'initialisation de la carte ni la création du canvas).
    cy.intercept("GET", "https://data.geopf.fr/wmts*", { statusCode: 204, body: "" });
    // Tuiles vectorielles du réseau hydro (endpoint backend public).
    cy.intercept("GET", "**/v1/tiles/hydro/**", { statusCode: 204, body: "" });

    cy.visit("/#/map-test");
  });

  it("initialise la carte (canvas WebGL présent)", () => {
    cy.get(".maplibregl-canvas", { timeout: 15000 }).should("exist");
  });

  it("bascule entre fond Plan et Satellite", () => {
    // Au départ le fond est le Plan IGN : le bouton propose donc « Satellite ».
    cy.contains("button", "Satellite").should("be.visible").click();
    // Après bascule, le bouton propose le retour au « Plan ».
    cy.contains("button", "Plan").should("be.visible").click();
    cy.contains("button", "Satellite").should("be.visible");
  });

  it("expose le bouton de géolocalisation", () => {
    cy.contains("button", "Ma position").should("be.visible");
  });
});
