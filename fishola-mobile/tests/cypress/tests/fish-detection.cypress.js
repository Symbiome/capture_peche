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

const bigRatioError = 0.4;
const mediumRatioError = 0.25;
const lowRationError = 0.1;
const perfectRatioError = 0.05;

describe("Mesures de poissons: tests automatiques", () => {
  const testResults = [];

  // Get Test Data
  const allowedRationDiff = 0.01;
  const testPictures = getPicturesToTest();

  for (let i = 0; i < testPictures.length; i++) {
    const testPicture = testPictures[i];
    testResults.push(new MeasureTestResult());

    // One test per picture to test
    it(
      "Photo " + testPicture.comment + " (" + testPicture.filePath + ")",
      () => {
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
            if (parseInt(shapesNumber) !== expectedShapesNumber) {
              failWithGrade(
                testResults[i],
                "Mauvais nombre de poissons détectés (" +
                  shapesNumber +
                  " au lieu de " +
                  expectedShapesNumber +
                  ")"
              );
            }
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
                      Math.round(
                        (parseInt(fishSize) / parseInt(imageSize)) * 1000
                      ) / 1000;
                    const ratioDiff = Math.abs(
                      actualFishOnPictureSizeRatio -
                        testPicture.expectedFishOnImageRatio
                    );
                    testResults[i].diffBetweenExpectRationAndActual = ratioDiff;
                    testResults[i].grade = computeGrade(testResults[i]);
                    if (testResults[i].grade < 20) {
                      failWithGrade(
                        testResults[i],
                        "Taille du poisson trop peu précise (" +
                          parseInt(fishSize) +
                          "px = " +
                          Math.round(actualFishOnPictureSizeRatio * 1000) / 10 +
                          "% de l'image totale) au lieu de " +
                          Math.round(
                            testPicture.expectedFishOnImageRatio * 1000
                          ) /
                            10 +
                          "%"
                      );
                    }
                  });
              });
          });
      }
    );
  }
  it("Note finale", () => {
    assert.fail(computeFinalGradeString(testResults));
  });
  it("Détails techniques", () => {
    let technicalDetails = "";
    cy.get("input[id=resizeSize]")
      .invoke("val")
      .then((imageSize) => {
        cy.get("input[id=minSizeRatio]")
          .invoke("val")
          .then((minSizeRatio) => {
            cy.get("input[id=maxSizeRatio]")
              .invoke("val")
              .then((maxSizeRatio) => {
                cy.get("input[id=minWithLengthRatio]")
                  .invoke("val")
                  .then((minWithLengthRatio) => {
                    cy.get("input[id=maxWithLengthRatio]")
                      .invoke("val")
                      .then((maxWithLengthRatio) => {
                        technicalDetails +=
                          "Configuration initiale utilisée : Taille image: " +
                          imageSize +
                          "px, MinSizeRation " +
                          minSizeRatio +
                          ", MaxSizeRation " +
                          maxSizeRatio +
                          ", minWithLengthRatio " +
                          minWithLengthRatio +
                          ", maxWithLengthRatio " +
                          maxWithLengthRatio +
                          "   ";
                        technicalDetails += JSON.stringify(testResults);
                        assert.fail(technicalDetails);
                      });
                  });
              });
          });
      });
  });
});

/**
 * Computes a grade (/20) for our measure system out of test results.
 */
function computeFinalGradeString(testResults) {
  const wrongMarkers = testResults.filter((ts) => {
    return !ts.markerDetectedAsExpected;
  }).length;
  const wrongFishes = testResults.filter((ts) => {
    return !ts.fishDetectedAsExpected;
  }).length;
  const bigRationErrorCounts = testResults.filter((ts) => {
    return ts.diffBetweenExpectRationAndActual >= bigRatioError;
  }).length;
  const finalGrade =
    testResults.reduce((gradesSum, ts) => {
      return gradesSum + ts.grade;
    }, 0) / testResults.length;

  // Compute final grade String
  let finalGradeString = Math.round(finalGrade * 10) / 10 + "/20 (";
  finalGradeString +=
    Math.round((wrongMarkers / testResults.length) * 100) +
    "% d'erreurs de marqueurs, ";
  finalGradeString +=
    Math.round((wrongFishes / testResults.length) * 100) +
    "% d'erreurs de poissons, ";
  finalGradeString +=
    Math.round((bigRationErrorCounts / testResults.length) * 100) +
    "% avec des erreurs de mesures très imprécises)";
  return finalGradeString;
}

/**
 * Provides a grade (/20) for the given test result
 */
function computeGrade(testResult) {
  // Grade each test result on 20 points
  let grade = 0;
  // 3 point for marker detection
  if (testResult.markerDetectedAsExpected) {
    grade += 3;
  }
  if (testResult.fishDetectedAsExpected) {
    // 3 point for fish detection
    grade += 3;

    // 3 points if ratio below 'bigRatioError'
    if (testResult.diffBetweenExpectRationAndActual < bigRatioError) {
      grade += 3;
      if (testResult.diffBetweenExpectRationAndActual < mediumRatioError) {
        // 3 additional points if ratio below 'bigRatioError'
        grade += 3;
        mediumRatioError;
        if (testResult.diffBetweenExpectRationAndActual < lowRationError) {
          // 3 additional points if ratio below 'lowRationError'
          grade += 3;
          if (testResult.diffBetweenExpectRationAndActual < perfectRatioError) {
            // Grade = 15 (total up to this point) + diff squared
            let diff = 1 - testResult.diffBetweenExpectRationAndActual * 15;
            grade = 15 + Math.round(diff * 5 * 10) / 10;
          }
        }
      }
    }
  }
  return grade;
}

function failWithGrade(testResult, failureMessage) {
  const grade = computeGrade(testResult);
  if (grade < 20) {
    assert.fail("Note : " + grade + "/20 - " + failureMessage);
  }
}

function getPicturesToTest() {
  const pics = [];
  pics.push(markerPic("marker_1.jpg", "optimale", 6.8 / 15));
  pics.push(fishPic("IMG_20201001_102419.jpg", "correcte", 10.9 / 15));
  pics.push(fishPic("P1010100.JPG", "correcte", 0.1));
  pics.push(fishPic("pisci_Bourget 2013 054.jpg", "correcte", 0.1));
  pics.push(fishPic("WP_20151001_064.jpg", "correcte", 0.1));
  pics.push(fishPic("WP_20151001_065.jpg", "correcte", 0.1));
  pics.push(fishPic("COR1_42cm.jpg", "correcte", 0.1));
  pics.push(fishPic("COR_ 42cm.jpg", "correcte", 0.1));

  pics.push(fishPic("COR1_36cm.jpg", "avec queue ton sur ton", 11.3 / 15));
  pics.push(fishPic("IMG_2539.jpg", "avec rainures de bois", 8.6 / 15));
  pics.push(fishPic("annecy IV43-2.jpg", "avec ombres fortes", 0.1));
  pics.push(fishPic("P1010081.JPG", "poisson-chat", 0.1));
  pics.push(fishPic("DSCN1714.JPG", "sur lattes", 0.1));
  pics.push(fishPic("b99in406.jpg", "avec étiquette collée", 0.1));
  pics.push(fishPic("IMG_20210412.jpg", "avec lignes parasites 1", 13.8 / 15));
  pics.push(fishPic("COR2_42cm.jpg", "avec lignes parasites 2", 0.1));
  pics.push(fishPic("95ch43.sep.jpg", "paysage recadré 1", 0.1));
  pics.push(fishPic("95ch67.sep.jpg", "paysage recadré 2", 0.1));
  pics.push(fishPic("95ch115.sep.jpg", "paysage recadré 3", 0.1));
  pics.push(fishPic("b99in357.jpg", "sur fond bleu 1", 0.1));
  pics.push(fishPic("b99in360.jpg", "sur fond bleu 2", 0.1));
  pics.push(fishPic("b99in365.jpg", "sur fond bleu 3", 0.1));
  pics.push(fishPic("8b99in367.jpg", "sur fond bleu 4", 0.1));
  pics.push(fishPic("b99in416.jpg", "autres éléments sur image 1", 0.1));
  pics.push(fishPic("b99in422.jpg", "autres éléments sur image 2", 0.1));
  pics.push(fishPic("ch951623.rep.jpg", "sur règle 1", 0.1));
  pics.push(fishPic("ch951688.rep.jpg", "sur règle 2", 0.1));
  pics.push(fishPic("DSC_0739.JPG", "sur règle 3", 0.1));
  pics.push(fishPic("DSC_0746.JPG", "sur règle 4", 0.1));
  pics.push(fishPic("DSC_0785.JPG", "sur règle 5", 0.1));
  pics.push(fishPic("ch951762.rep.jpg", "tâcheté", 0.1));
  pics.push(fishPic("DSCN1244.JPG", "dans boîte 1", 0.1));
  pics.push(fishPic("DSCN1246.JPG", "dans boîte 2", 0.1));
  pics.push(fishPic("GM_3469.JPG", "dans boîte 3", 0.1));
  pics.push(fishPic("IMG_2547 (2).JPG", "dans boîte 4", 0.1));
  pics.push(fishPic("IMG_2548 (2).JPG", "dans boîte 5", 0.1));
  pics.push(fishPic("IMG_20181004_114926.jpg", "dans boîte 7", 0.1));
  pics.push(fishPic("IMG_20181004_114934.jpg", "dans boîte 8", 0.1));

  return pics;
}

function markerPic(imgPath, comment, expectedFishOnImageRatio) {
  return new MarkerTestPicture(
    defaultMarkerPath,
    "markers/" + imgPath,
    true,
    expectedFishOnImageRatio,
    comment
  );
}

function fishPic(imgPath, comment, expectedFishOnImageRatio) {
  return new MarkerTestPicture(
    defaultMarkerPath,
    "fishes/test_" + imgPath,
    false,
    expectedFishOnImageRatio,
    comment
  );
}
