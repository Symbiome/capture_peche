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
 * Test suite related to automatic marker detection from picture with opencv
 */
import { markerPic } from "./cypress-test-utils";

describe("Détection de marqueurs", () => {
  const testResults = [];

  // Get Test Data
  const testPictures = getMarkerPicturesToTest();

  for (let i = 0; i < testPictures.length; i++) {
    const testPicture = testPictures[i];

    it(
      "Photo " + testPicture.comment + '"(' + testPicture.filePath + ")",
      () => {
        // Go to fish measurement page
        cy.visit("/#/fish-measure-test/marker");
        // Make sure OpenCV is ready
        cy.get("div[id=status").contains("OpenCV.js is ready");

        // Attach picture file
        cy.get("[id=markerFile]").attachFile(testPicture.markerPath);
        cy.get("[id=fileInput]").attachFile(testPicture.filePath);

        // Wait for result
        cy.get("div[id=calculating]").contains("Calcul terminé");

        // Test if we detected fish and marker as expected
        let expectedMarkerNumber = 1;
        cy.get("span[id=shapesNumber]")
          .invoke("text")
          .then((shapesNumber) => {
            if (parseInt(shapesNumber) !== expectedMarkerNumber) {
              failWithGrade(
                testResults[i],
                "Mauvais nombre de marqueurs détectés (" +
                  shapesNumber +
                  " au lieu de " +
                  expectedMarkerNumber +
                  ")"
              );
            }
          });
      }
    );
  }
});

function getMarkerPicturesToTest() {
  const markerPics = [];
  markerPics.push(markerPic("marker_1.jpg", 1));
  markerPics.push(markerPic("marker_2.jpg", 2));
  markerPics.push(markerPic("marker_3.jpg", 3));
  return markerPics;
}
