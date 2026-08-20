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
 * E2e de l'autocomplétion du champ « Plan d'eau » (#98).
 *
 * POURQUOI CETTE SPEC : le défaut corrigé en 96da5ac6 (`RegExp.escape`, absent
 * avant Chrome 136 / Safari 18.4) échouait D'UNE FAÇON MUETTE — la requête
 * serveur RÉUSSISSAIT, seul le rendu de la liste levait. Aucune suggestion ne
 * s'affichait, le champ restait indéfiniment en « recherche en cours », et rien
 * n'apparaissait ni dans la console utilisateur ni côté serveur.
 *
 * D'où le principe des assertions ci-dessous : elles portent sur le RENDU
 * (`<li>` présents, fragment surligné), jamais sur la réponse réseau — c'est
 * exactement la distinction que le défaut exploitait. Un test qui se contenterait
 * d'attendre `@search` passerait alors même que l'écran est cassé.
 *
 * Le backend est stubbé (`cy.intercept`) : la spec doit rester déterministe et
 * jouable sans stack lancée, comme celle de la carte. Le parcours, lui, est le
 * vrai : « Nouvelle sortie » → écran de saisie (l'écran n'est pas atteignable
 * par URL directe, la sortie en cours de création vit dans IndexedDB).
 */
describe("Autocomplétion du champ « Plan d'eau » (#98)", () => {
  // Profil de recette. `acceptsMailNotifications` / `acceptsShareTrips` à true et
  // `lastNewsSeenDate` récente : sinon Menu.vue ouvre la modale « Du nouveau sur
  // FISHOLA » au premier lancement, qui bloque toute interaction.
  const PROFILE = {
    id: "00000000-0000-0000-0000-000000000001",
    firstName: "Pêcheur",
    lastName: "De Recette",
    email: "pecheur-recette@fishola.test",
    pseudo: "pecheur",
    initials: "PR",
    sampleBaseId: "PR",
    acceptsMailNotifications: true,
    acceptsShareTrips: true,
    lastNewsSeenDate: [2026, 1, 1, 0, 0],
  };

  // Forme renvoyée par GET /v1/waterEntities/search (cf. ReferentialService).
  function searchResult(id, name, commune, codePostal) {
    return {
      waterEntityId: id,
      name,
      kind: "PlanEau",
      centroid: { lat: 45.9, lng: 6.15 },
      commune,
      codePostal,
    };
  }

  function stubSearch(results) {
    cy.intercept("GET", "**/v1/waterEntities/search*", {
      statusCode: 200,
      body: results,
    }).as("search");
  }

  // « Nouvelle sortie » → « En direct ». La sortie est créée localement (Dexie),
  // aucun appel serveur : c'est le seul chemin vers l'écran de saisie.
  function goToTripMeta() {
    cy.visit("/#/trips/new");
    cy.contains(".new-trip-option", "En direct").find("button").click();
    cy.get("#lakes-autocomplete-input").should("be.visible");
  }

  beforeEach(() => {
    // Garde de route + en-tête : sans profil, on est renvoyé sur /login.
    cy.intercept("GET", "**/v1/security/profile", { statusCode: 200, body: PROFILE });
    // Référentiel complet et favoris : listes vides, la recherche passe par le serveur.
    cy.intercept("GET", "**/v1/referential/waterEntities", { statusCode: 200, body: [] });
    cy.intercept("GET", "**/v1/referential/waterEntities/favorites", { statusCode: 200, body: [] });
  });

  it("affiche des suggestions après trois lettres saisies", () => {
    stubSearch([
      searchResult("11111111-1111-1111-1111-111111111111", "Lac d'Annecy", "Annecy", "74000"),
      searchResult("22222222-2222-2222-2222-222222222222", "Lac d'Annecy-le-Vieux", "Annecy", "74940"),
    ]);

    goToTripMeta();
    // Terme saisi avec la casse du libellé : `highlightMatchingText` réinjecte le
    // terme TEL QUE SAISI à la place du fragment trouvé (recherche insensible à
    // la casse) — « ann » réécrirait « Lac d'Annecy » en « Lac d'annecy ».
    cy.get("#lakes-autocomplete-input").type("Ann");

    // Attente explicite de la réponse : la recherche est debouncée 250 ms côté
    // composant, sans ça l'assertion court après un rendu pas encore déclenché.
    cy.wait("@search");

    // L'assertion porte sur le RENDU, pas sur la réponse : au moins une
    // suggestion visible, et le fragment saisi surligné (donc
    // `highlightMatchingText` s'est exécuté sans lever).
    cy.get(".suggestions li").should("have.length.at.least", 1);
    cy.get(".suggestions li").first().should("be.visible").and("contain.text", "Lac d'Annecy");
    cy.get(".suggestions li .highlight").first().should("have.text", "Ann");
  });

  it("affiche des suggestions même quand le terme contient un métacaractère d'expression régulière", () => {
    // Le terme est réinjecté dans une RegExp pour le surlignage : non échappé,
    // « Lac ( » produit une expression invalide, le rendu lève, et la liste
    // reste vide sans le moindre message.
    stubSearch([
      searchResult("33333333-3333-3333-3333-333333333333", "Lac (retenue) de Serre-Ponçon", "Savines-le-Lac", "05160"),
    ]);

    goToTripMeta();
    cy.get("#lakes-autocomplete-input").type("Lac (");
    cy.wait("@search");

    cy.get(".suggestions li").should("have.length.at.least", 1);
    cy.get(".suggestions li").first().should("contain.text", "Serre-Ponçon");
    cy.get(".suggestions li .highlight").first().should("have.text", "Lac (");
  });
});
