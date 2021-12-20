/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
 * Test suite related to fish measurement using opencv
 */

describe("Stress tests", () => {
  it("Stress test", () => {
    // Go to fish measurement page
    cy.visit("/#/fish-measure-test");

    // Make sure OpenCV is ready
    cy.get("div[id=status").contains("OpenCV.js is ready");
    // Launch 100 detections to make sure that there is no memory leak
    for (let i = 0; i < 20; i++) {
      cy.task("log", "*** Attempt #" + i);
      // Attach picture file
      if (i % 2 == 0) {
        cy.get("[id=fileInput]").attachFile("markers/beta-TAN519b.jpg");
      } else {
        cy.get("[id=fileInput]").attachFile("markers/beta-TAN478b.jpg");
      }
      cy.get("div[id=calculating]").contains("Calcul en cours");

      // Wait for result
      Cypress.config("defaultCommandTimeout", 10000);
      cy.get("div[id=calculating]").contains("Calcul terminé");

      // Test if we detected fish and marker as expected
      if (i % 2 == 0) {
        cy.get("span[id=resultText]").contains("495mm");
      } else {
        cy.get("span[id=resultText]").contains("498mm");
      }
      cy.get("span[id=resultText]").contains("Détecté : Poisson");
      cy.get("span[id=resultText]").contains("Détecté : Marqueur");
    }
  });
});
