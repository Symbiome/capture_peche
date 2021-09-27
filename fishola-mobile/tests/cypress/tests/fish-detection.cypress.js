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
import MeasureTestResult from "../../commons/MeasureTestResult";
import { fishPic, fishWithMarkerPic } from "./cypress-test-utils";

const bigRatioError = 0.4;
const mediumRatioError = 0.25;
const lowRationError = 0.1;
const perfectRatioError = 0.05;

describe("Mesures de poissons", () => {
  const testResults = [];

  // Get Test Data
  const testPictures = getPicturesToTest();

  for (let i = 0; i < testPictures.length; i++) {
    const testPicture = testPictures[i];
    testResults.push(new MeasureTestResult(testPicture.quality));

    // One test per picture to test
    let qualityString = "";
    switch (testPicture.quality) {
      case 5:
        qualityString = "beta";
        break;
      case 4:
        qualityString = "optimale";
        break;
      case 3:
        qualityString = "correcte";
        break;
      case 2:
        qualityString = "dégradée";
        break;
      default:
        qualityString = "indétectable";
    }
    it(
      "Photo " +
        qualityString +
        ' "' +
        testPicture.comment +
        '"(' +
        testPicture.filePath +
        ")",
      () => {
        // Go to fish measurement page
        cy.visit("/#/fish-measure-test");
        // Make sure OpenCV is ready
        cy.get("div[id=status").contains("OpenCV.js is ready");

        // Indicate if picture is suppose to contain marker
        if (!testPicture.shouldHaveMarker) {
          cy.get("[id=pictureIsSupposedToContainMarker]").uncheck();
        }

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
  it("Note finale (cas beta)", () => {
    assert.fail(computeFinalGradeString(testResults, 5, true));
  });
  it("Note finale (cas optimaux)", () => {
    assert.fail(computeFinalGradeString(testResults, 4, true));
  });
  it("Note finale (cas beta et optimaux)", () => {
    assert.fail(computeFinalGradeString(testResults, 4));
  });
  it("Note finale (cas beta, optimaux et corrects)", () => {
    assert.fail(computeFinalGradeString(testResults, 3));
  });
  it("Note finale (cas beta, optimaux, corrects et dégradés)", () => {
    assert.fail(computeFinalGradeString(testResults, 2));
  });
  it("Note finale (tous cas y compris  indétectables)", () => {
    assert.fail(computeFinalGradeString(testResults, 1));
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
function computeFinalGradeString(
  testResults,
  minimumQuality,
  strictQualityEquality
) {
  const trimmedResults = testResults.filter((ts) => {
    if (strictQualityEquality) {
      return ts.quality == minimumQuality;
    } else {
      return ts.quality >= minimumQuality;
    }
  });
  const wrongMarkers = trimmedResults.filter((ts) => {
    return !ts.markerDetectedAsExpected;
  }).length;
  const wrongFishes = trimmedResults.filter((ts) => {
    return !ts.fishDetectedAsExpected;
  }).length;
  const bigRationErrorCounts = trimmedResults.filter((ts) => {
    return ts.diffBetweenExpectRationAndActual >= bigRatioError;
  }).length;
  const finalGrade =
    trimmedResults.reduce((gradesSum, ts) => {
      return gradesSum + ts.grade;
    }, 0) / trimmedResults.length;

  // Compute final grade String
  let finalGradeString = Math.round(finalGrade * 10) / 10 + "/20 (";
  finalGradeString +=
    Math.round((wrongMarkers / trimmedResults.length) * 100) +
    "% d'erreurs de marqueurs, ";
  finalGradeString +=
    Math.round((wrongFishes / trimmedResults.length) * 100) +
    "% d'erreurs de poissons, ";
  finalGradeString +=
    Math.round((bigRationErrorCounts / trimmedResults.length) * 100) +
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
  const beta = 5;
  const optimal = 4;
  const good = 3;
  const medium = 2;
  const hard = 1;

  // Beta pictures
  pics.push(
    fishWithMarkerPic("beta-GAR130b.jpg", "marqueur tâché", 7.2 / 15, beta)
  );
  pics.push(
    fishWithMarkerPic("beta-OBL285b.jpg", "marqueur collé", 9.5 / 15, beta)
  );
  pics.push(
    fishWithMarkerPic("beta-PER297b.jpg", "chloé 13/09", 9.3 / 15, beta)
  );
  pics.push(
    fishWithMarkerPic("beta-PER313.jpg", "chloé 13/09", 8.5 / 15, beta)
  );
  pics.push(
    fishWithMarkerPic("beta-TAN418b.jpg", "chloé 13/09", 9.1 / 15, beta)
  );
  pics.push(
    fishWithMarkerPic("beta-TAN519b.jpg", "chloé 13/09", 9.5 / 15, beta)
  );
  // Optimal pictures
  pics.push(fishWithMarkerPic("marker_1.jpg", "parfaite", 6.8 / 15, optimal));
  pics.push(fishPic("IMG_20201001_102419.jpg", "correcte", 10.9 / 15, optimal));

  pics.push(fishPic("P1010100.jpg", "correcte", 13 / 15, optimal));
  pics.push(fishPic("pisci_Bourget 2013.jpg", "correcte", 12 / 15, optimal));

  // Good pictures
  pics.push(fishPic("WP_20151001_064.jpg", "correcte", 9.4 / 15, good));
  pics.push(fishPic("COR1_42cm.jpg", "queue ton sur ton", 13 / 15, good));
  pics.push(fishPic("b99in416.jpg", "éléments divers", 9.6 / 15, good));
  pics.push(fishPic("b99in422.jpg", "éléments divers 2", 10.4 / 15, good));
  pics.push(fishPic("IMG_2539.jpg", "avec rainures de bois", 8.6 / 15, good));
  pics.push(fishPic("fond_bateau_1.jpeg", "fond bateau 1", 14.4 / 15, good));
  pics.push(fishPic("fond_bateau_17.jpeg", "quadrillage", 10.1 / 15, good));
  pics.push(fishPic("fond_bateau_4.jpeg", "fond bateau", 12.1 / 15, good));
  pics.push(fishPic("fond_bateau_6.jpeg", "fond bateau", 11.2 / 15, good));
  pics.push(fishPic("fond_bateau_11.jpeg", "tâches de sang", 10.4 / 15, good));
  pics.push(fishPic("fond_bateau_12.jpeg", "rainures", 13.3 / 15, good));
  pics.push(fishPic("fond_bateau_13.jpeg", "fond tâcheté", 13.4 / 15, good));
  pics.push(fishPic("fond_bateau_14.jpeg", "avec ombres", 13.7 / 15, good));

  // Medium pictures
  pics.push(fishPic("COR_ 42cm.jpg", "queue ton sur ton", 13.5 / 15, medium));
  pics.push(fishPic("COR1_36cm.jpg", "queue ton sur ton", 11.3 / 15, medium));
  pics.push(fishPic("P1010081.jpg", "ombres portées", 10.6 / 14.3, medium));
  pics.push(fishPic("IMG_20210412.jpg", "lignes parasites", 13.8 / 15, medium));
  pics.push(fishPic("b99in360.jpg", "sur fond bleu 2", 13.5 / 15, medium));
  pics.push(fishPic("b99in365.jpg", "sur fond bleu 3", 12.3 / 15, medium));
  pics.push(fishPic("8b99in367.jpg", "sur fond bleu 4", 12 / 15, medium));
  pics.push(fishPic("ch951762.rep.jpg", "tâcheté", 14.5 / 15, medium));
  pics.push(fishPic("IMG_2548 (2).jpg", "dans boîte 5", 12.5 / 15, medium));
  pics.push(fishPic("IMG_20181004_114926.jpg", "boîte 7", 11.6 / 15, medium));
  pics.push(fishPic("fond_bateau_5.jpeg", "fond bateau", 14.5 / 15, medium));
  pics.push(fishPic("fond_bateau_8.jpeg", "fond avec vrac", 11.7 / 15, medium));
  pics.push(fishPic("fond_bateau_15.jpeg", "quadrillage", 13.5 / 15, medium));
  pics.push(fishPic("fond_bateau_16.jpeg", "rainures", 11.4 / 15, medium));
  pics.push(fishPic("quadrillage.jpeg", "quadrillage vrac", 10.3 / 15, medium));
  pics.push(fishPic("coin_table2.jpeg", "coin de table", 10.4 / 15, medium));
  pics.push(fishPic("coin_table3.jpeg", "coin de table", 14.4 / 15, medium));
  pics.push(fishPic("coin_table4.jpeg", "coin table sale", 12.2 / 15, medium));
  pics.push(fishPic("coin_table5.jpeg", "coin de table", 7.7 / 15, medium));

  // Hard pictures (ok if not detected)
  pics.push(fishPic("95ch115.sep.jpg", "paysage recadré", 13.8 / 15, hard));
  pics.push(fishPic("annecy IV43-2.jpg", "ombres fortes", 12.7 / 14.3, hard));
  pics.push(fishPic("COR2_42cm.jpg", "lignes parasites", 11.1 / 15, hard));
  pics.push(fishPic("95ch43.sep.jpg", "paysage recadré 1", 14.7 / 15, hard));
  pics.push(fishPic("b99in357.jpg", "sur fond bleu 1", 8.3 / 15, hard));
  pics.push(fishPic("ch951623.rep.jpg", "sur règle 1", 14 / 15, hard));
  pics.push(fishPic("ch951688.rep.jpg", "sur règle 2", 13.7 / 15, hard));
  pics.push(fishPic("DSC_0739.jpg", "sur règle 3", 11.5 / 15, hard));
  pics.push(fishPic("DSC_0746.jpg", "sur règle 4", 12 / 15, hard));
  pics.push(fishPic("IMG_20181004.jpg", "étiquette", 11.5 / 15, hard));
  pics.push(fishPic("DSCN1714.jpg", "sur lattes", 9.6 / 15, hard));
  pics.push(fishPic("WP_20151001_065.jpg", "correcte", 7.2 / 15, hard));
  pics.push(fishPic("coin_table.jpeg", "coin de table", 11 / 15, hard));
  pics.push(fishPic("fond_bateau_10.jpeg", "fond tâcheté", 7 / 15, hard));
  pics.push(fishPic("DSC_0785.jpg", "sur règle 5", 10.6 / 15, hard));
  pics.push(fishPic("DSCN1246.jpg", "dans boîte 2", 8.3 / 15, hard));
  pics.push(fishPic("DSCN1244.jpg", "dans boîte 1", 9.6 / 14.3, hard));
  pics.push(fishPic("DSCN1246.jpg", "dans boîte 2", 8.3 / 15, hard));
  pics.push(fishPic("GM_3469.jpg", "dans boîte 3", 13.8 / 15, hard));
  pics.push(fishPic("IMG_2547 (2).jpg", "dans boîte 4", 8.6 / 15, hard));
  pics.push(fishPic("fond_serviette.jpeg", "fond serviette", 12.4 / 15, hard));
  pics.push(fishPic("b99in406.jpg", "avec étiquette collée", 13.9 / 15, hard));
  pics.push(fishPic("ombres_portees.jpeg", "ombres portées", 10.9 / 15, hard));
  pics.push(fishPic("fond_bateau_2.jpeg", "fond avec cadenas", 9.5 / 15, hard));
  pics.push(fishPic("fond_bateau_3.jpeg", "ombres et objets", 10.6 / 15, hard));
  pics.push(fishPic("sur_fond_gradué.jpeg", "fond gradué", 10.5 / 15, hard));
  pics.push(fishPic("sur_fond_gradué2.jpeg", "sur fond gradué", 11 / 15, hard));
  pics.push(fishPic("boite.jpeg", "dans boite 8", 6.3 / 15, hard));
  pics.push(fishPic("plaque.jpeg", "sur plaque", 8.2 / 15, hard));
  pics.push(fishPic("plaque2.jpeg", "sur plaque 2", 10.2 / 15, hard));
  pics.push(fishPic("herbe.jpeg", "dans herbe", 4 / 15, hard));
  pics.push(fishPic("fond_bateau_coin_1.jpeg", "avec coin", 12.3 / 15, hard));
  pics.push(fishPic("fond_bateau_7.jpeg", "fond avec vrac", 9.9 / 15, hard));
  pics.push(fishPic("fond_bateau_9.jpeg", "vrac/rainures", 13.8 / 15, hard));

  return pics;
}
