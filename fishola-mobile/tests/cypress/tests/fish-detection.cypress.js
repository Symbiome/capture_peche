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
  const markerTestPictures = getMarkerPicturesToTest();

  for (let i = 0; i < markerTestPictures.length; i++) {
    const markerTestPicture = markerTestPictures[i];

    // One test per picture to test
    it("Picture " + markerTestPicture.filePath, () => {
      // Go to fish measurement page
      cy.visit("/#/fish-measure-test/measure");
      // Make sure OpenCV is ready
      cy.get("div[id=status").contains("OpenCV.js is ready");

      // Attach marker file
      cy.get("[id=fileInput]").attachFile(markerTestPicture.filePath);

      // Wait for result
      cy.get("div[id=calculating]").contains("Calcul terminé");
    });
  }
});

function getMarkerPicturesToTest() {
  const markerPics = [];
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
      "markers/IMG_20210427_103130.jpg",
      true
    )
  );
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "fish-measures/b99in367.jpg",
      false
    )
  );
  return markerPics;
}
