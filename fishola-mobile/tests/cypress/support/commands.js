import "cypress-file-upload";

/**
 * Connecte un pêcheur (app mobile) de façon programmatique via l'API de sécurité,
 * puis met en cache la session (cookie) avec `cy.session`.
 *
 * Login PROGRAMMATIQUE (`cy.request`) et non par l'UI : côté mobile la session est
 * un cookie posé par le backend, et ces e2e ciblent un vrai backend seedé — on veut
 * donc une mise en session rapide, pas rejouer l'écran de connexion à chaque spec.
 * (À l'inverse, `cy.loginStaff` côté admin passe par l'UI pour rester stubbable.)
 *
 * Analogue mobile de `cy.loginStaff` (admin) : le mobile authentifie des PÊCHEURS,
 * pas du staff — d'où le nom `loginAngler`. Identifiants par défaut = `Cypress.env`.
 *
 * @param {{email?: string, password?: string}} [options]
 */
Cypress.Commands.add("loginAngler", (options = {}) => {
  const email = options.email ?? Cypress.env("anglerEmail");
  const password = options.password ?? Cypress.env("anglerPassword");
  const apiUrl = Cypress.env("apiUrl");

  cy.session(["angler", email], () => {
    cy.request("POST", `${apiUrl}/v1/security/login`, { email, password })
      .its("status")
      .should("eq", 200);
  });
});
