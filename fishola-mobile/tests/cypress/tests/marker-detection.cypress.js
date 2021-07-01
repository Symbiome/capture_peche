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
      'Photo "' + testPicture.comment + '" (' + testPicture.filePath + ")",
      () => {
        // Go to fish measurement page
        cy.visit("/#/fish-measure-test");
        // Make sure OpenCV is ready
        cy.get("div[id=status").contains("OpenCV.js is ready");

        // Always launch feature matching (even if only one candidate)
        cy.get("[id=alwaysCheckMarkerCandidates]").check();

        // Attach picture file
        cy.get("[id=markerFile]").attachFile(testPicture.markerPath);
        cy.get("[id=fileInput]").attachFile(testPicture.filePath);

        // Wait for result
        cy.get("div[id=calculating]").contains("Calcul terminé");

        // Test if we detected fish and marker as expected
        let expectedMarkerCount = 0;
        if (testPicture.expectedFishOnImageRatio) {
          expectedMarkerCount = 1;
        }

        cy.get("span[id=resultText]")
          .invoke("text")
          .then((rawMeasureText) => {
            const actualMarkerCount =
              rawMeasureText.split("Détecté : Marqueur").length - 1;
            assert.equal(
              actualMarkerCount,
              expectedMarkerCount,
              "Mauvais nombre de marqueurs détectés"
            );
            const markerSize = rawMeasureText
              .split("Détecté : Marqueur")[1]
              .split("mm (")[1]
              .split("px")[0]
              .trim();

            cy.get("input[id=resizeSize]")
              .invoke("val")
              .then((imageSize) => {
                const actuaMarkerOnPictureSizeRatio =
                  Math.round(
                    (parseInt(markerSize) / parseInt(imageSize)) * 1000
                  ) / 1000;
                const ratioDiff = Math.abs(
                  actuaMarkerOnPictureSizeRatio -
                    testPicture.expectedFishOnImageRatio
                );
                assert.equal(
                  Math.round(ratioDiff * 1000) / 1000,
                  0,
                  "Légère différence entre la taille attendue et détectée (" +
                    Math.round(ratioDiff * 1000) / 10 +
                    "% )"
                );
              });
          });
      }
    );
  }
});

function getMarkerPicturesToTest() {
  const markerPics = [];
  markerPics.push(markerPic("marker_1.jpg", "Marqueur seul", 57 / 300));
  markerPics.push(markerPic("marker_2.jpg", "Marqueur absent", 0));
  markerPics.push(markerPic("marker_3.jpg", "nombreux marqueurs", 58 / 300));
  markerPics.push(markerPic("marker_4.jpg", "nombreux marqueurs 2", 48 / 300));
  markerPics.push(markerPic("marker_6.jpg", "nombreux marqueurs 3", 57 / 300));
  markerPics.push(markerPic("marker_5.jpg", "qr code", 83 / 300));
  return markerPics;
}
