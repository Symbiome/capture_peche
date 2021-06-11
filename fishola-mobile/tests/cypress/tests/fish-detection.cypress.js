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
const defaultMarkerPath = "markers/marker.jpg";
import MarkerTestPicture from "../../commons/MarkerTestPicture";

describe("Fish measurement tests", () => {
  // Get Test Data
  const allowedRationDiff = 0.01;
  const markerTestPictures = getMarkerPicturesToTest();

  for (let i = 0; i < markerTestPictures.length; i++) {
    const markerTestPicture = markerTestPictures[i];

    // One test per picture to test
    it("Picture " + markerTestPicture.filePath, () => {
      // Go to fish measurement page
      cy.visit("/#/fish-measure-test/measure");
      // Make sure OpenCV is ready
      cy.get("div[id=status").contains("OpenCV.js is ready");

      // Attach picture file
      cy.get("[id=fileInput]").attachFile(markerTestPicture.filePath);

      // Wait for result
      cy.get("div[id=calculating]").contains("Calcul terminé");

      // Test if we detected fish and marker as expected
      let expectedShapesNumber = 1;
      cy.get("span[id=resultText]").contains("Détecté : Poisson");
      if (markerTestPicture.shouldHaveMarker) {
        expectedShapesNumber++;
        cy.get("span[id=resultText]").contains("Détecté : Marqueur");
      }
      // Make sure we did not detect other shapes
      cy.get("span[id=shapesNumber").contains(expectedShapesNumber);

      // Compare expected vs actual fish/picture ratio
      cy.get("span[id=resultText]")
        .invoke("text")
        .then((rawMeasureText) => {
          const fishSize = rawMeasureText
            .split("Détecté : Poisson")[1]
            .split("mm (")[1]
            .split("px")[0]
            .trim();
          cy.get("input[id=resizeSize]")
            .invoke("val")
            .then((imageSize) => {
              const actualFishOnPictureSizeRatio =
                parseInt(fishSize) / parseInt(imageSize);
              const ratioDiff = Math.abs(
                actualFishOnPictureSizeRatio -
                  markerTestPicture.expectedFishOnImageRatio
              );
              assert.isBelow(
                ratioDiff,
                allowedRationDiff,
                "La taille du poisson (" +
                  parseInt(fishSize) +
                  "px = " +
                  actualFishOnPictureSizeRatio * 100 +
                  "% de l'image totale) doit être proche de la taille attendue (" +
                  markerTestPicture.expectedFishOnImageRatio * 100 +
                  "%)"
              );
            });
        });
    });
  }
});

function getMarkerPicturesToTest() {
  const markerPics = [];

  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "markers/IMG_20210427_103130.jpg",
      true,
      0.62
    )
  );
  /*
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "markers/IMG_20210427_103107.jpg",
      true
    )
  );
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "markers/IMG_20210427_103121.jpg",
      true
    )
  );
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "fish-measures/b99in367.jpg",
      false
    )
  );*/
  return markerPics;
}
