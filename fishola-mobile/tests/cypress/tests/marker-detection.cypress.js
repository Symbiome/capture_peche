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
const defaultMarkerPath = "markers/marker.jpg";
import MarkerTestPicture from "../../commons/MarkerTestPicture";

describe("Marker detection tests", () => {
  // Get Test Data
  const markerTestPictures = getMarkerPicturesToTest();

  for (let i = 0; i < markerTestPictures.length; i++) {
    const markerTestPicture = markerTestPictures[i];

    // One test per picture to test
    it("Marker " + markerTestPicture.filePath, () => {
      // Go to fish measurement page
      cy.visit("/#/fish-measure-test/marker");
      // Make sure OpenCV is ready
      cy.get("div[id=status").contains("OpenCV.js is ready");

      // Attach marker file
      cy.get("[id=markerFile]").attachFile(markerTestPicture.markerPath);
      cy.get("[id=fileInput]").attachFile(markerTestPicture.filePath);

      // Wait for result
      cy.get("div[id=calculating]").contains("Calcul terminé");
    });
  }
});

function getMarkerPicturesToTest() {
  const markerPics = [];  
  return markerPics;
}
