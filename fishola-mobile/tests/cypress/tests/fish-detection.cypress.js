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
import MeasureTestResult from "../../commons/MeasureTestResult";

describe("Fish measurement tests", () => {
  const testResults = [];

  // Get Test Data
  const allowedRationDiff = 0.01;
  const testPictures = getPicturesToTest();

  for (let i = 0; i < testPictures.length; i++) {
    const testPicture = testPictures[i];
    testResults.push(new MeasureTestResult());

    // One test per picture to test
    it("Picture " + testPicture.filePath, () => {
      // Go to fish measurement page
      cy.visit("/#/fish-measure-test/measure");
      // Make sure OpenCV is ready
      cy.get("div[id=status").contains("OpenCV.js is ready");

      // Attach picture file
      cy.get("[id=fileInput]").attachFile(testPicture.filePath);

      // Wait for result
      cy.get("div[id=calculating]").contains("Calcul terminé");

      // Test if we detected fish and marker as expected
      let expectedShapesNumber = 1;
      cy.get("span[id=resultText]").contains("Détecté : Poisson");
      if (testPicture.shouldHaveMarker) {
        expectedShapesNumber++;
        cy.get("span[id=resultText]").contains("Détecté : Marqueur");
        testResults[i].markerDetectedAsExpected = true;
      } else {
        testResults[i].markerDetectedAsExpected = true;
      }
      // Make sure we did not detect other shapes
      cy.get("span[id=shapesNumber]")
        .invoke("text")
        .then((shapesNumber) => {
          assert.equal(
            shapesNumber,
            expectedShapesNumber,
            "Mauvais nombre de poissons détectés"
          );
          testResults[i].fishDetectedAsExpected = true;

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
                      testPicture.expectedFishOnImageRatio
                  );
                  testResults[i].diffBetweenExpectRationAndActual = ratioDiff;
                  assert.isBelow(
                    ratioDiff,
                    allowedRationDiff,
                    "La taille du poisson (" +
                      parseInt(fishSize) +
                      "px = " +
                      actualFishOnPictureSizeRatio * 100 +
                      "% de l'image totale) doit être proche de la taille attendue (" +
                      testPicture.expectedFishOnImageRatio * 100 +
                      "%)"
                  );
                });
            });
        });
    });
  }
  it("Report generation", () => {
    const wrongMarkers = testResults.filter((ts) => {
      return !ts.markerDetectedAsExpected;
    }).length;
    const wrongFishes = testResults.filter((ts) => {
      return !ts.fishDetectedAsExpected;
    }).length;
    const correctDetections = testResults.filter((ts) => {
      return ts.fishDetectedAsExpected && ts.markerDetectedAsExpected;
    });
    let stringReport = "[";
    testResults.forEach((testResult) => {
      stringReport += JSON.stringify(testResult) + ",\n";
    });
    stringReport += "]"
    stringReport +=
      "wrong markers: " +
        wrongMarkers +
        " / wrongs fishes " +
        wrongFishes +
        " / correct " +
        correctDetections.length +
        " / total " +
    testResults.length
    
    const diffSum = correctDetections.reduce((totalDiff, testResult) => {
      return totalDiff + testResult.diffBetweenExpectRationAndActual;
    }, 0);
    const diffAverage =
      diffSum / correctDetections.length;
  });
});

function getPicturesToTest() {
  const pics = [];
  pics.push(markerPic("marker_1.jpg", 0.62));
  pics.push(fishPic("test_1_COR1_36cm.jpg", false, 0.1));
  /* pics.push(fishPic("test_2_IMG_2539.jpg", false, 0.1));
  pics.push(fishPic("test_3_IMG_20201001_102419.jpg", false, 0.1));
  pics.push(fishPic("test_4_IMG_20210412_113556.jpg", false, 0.1));*/
  return pics;
}

function markerPic(imgPath, expectedFishOnImageRatio) {
  return new MarkerTestPicture(
    defaultMarkerPath,
    "markers/" + imgPath,
    true,
    expectedFishOnImageRatio
  );
}
function fishPic(imgPath, hasMarker, expectedFishOnImageRatio) {
  return new MarkerTestPicture(
    defaultMarkerPath,
    "fishes/" + imgPath,
    hasMarker,
    expectedFishOnImageRatio
  );
}
