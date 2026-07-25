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
 * Connecte un compte staff (administrateur OU opérateur) via l'écran de connexion,
 * puis attend l'aiguillage vers la home — signal de succès de l'authentification.
 *
 * Passe par l'UI réelle (pas `cy.request`, qui contournerait `cy.intercept`) : la
 * commande fonctionne donc aussi bien contre un backend réel que contre un backend
 * stubbé par la spec appelante. Identifiants par défaut = `Cypress.env`.
 *
 * Brique réutilisable par toutes les specs staff (opérateurs, import, saisie, mot de passe).
 *
 * @param {{email?: string, password?: string}} [options]
 */
Cypress.Commands.add("loginStaff", (options = {}) => {
  const email = options.email ?? Cypress.env("staffEmail");
  const password = options.password ?? Cypress.env("staffPassword");

  cy.visit("/login");
  cy.get('input[type="email"]').clear().type(email);
  cy.get('input[type="password"]').clear().type(password, { log: false });
  cy.contains("button", "Connexion").click();
  cy.location("pathname", { timeout: 10000 }).should("eq", "/home");
});
