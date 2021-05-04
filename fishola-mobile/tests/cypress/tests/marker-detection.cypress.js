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
/**
 * Test suite related to automatic marker detection from picture with opencv
 */

import opencv from "../../commons/opencv.js";

// Test Data
FisholaOpenCVService.INSTANCE.cv = opencv;
const defaultMarkerPath = "marker/marker.png";
const markerTestPictures = getMarkerPicturesToTest();

// Pre-load opencv (would normally be loaded lazily by FisholaOpenCVService)
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";
import MarkerTestPicture from "../../commons/MarkerTestPicture";

describe("My First Test", () => {
  for (let i = 0; i < markerTestPictures.length; i++) {
    const expectedResult = markerTestPictures[i];

    // One test per picture to test
    it("File " + expectedResult.filePath, (done) => {
      expect("TODO").to.equal("File " + expectedResult.filePath);
    });
  }
});

function getMarkerPicturesToTest() {
  const markerPics = [];
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "marker/IMG_20210427_103107.jpg",
      true
    )
  );
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "marker/IMG_20210427_103121.jpg",
      true
    )
  );
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "marker/IMG_20210427_103130.jpg",
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
  markerPics.push(
    new MarkerTestPicture(
      defaultMarkerPath,
      "fish-measures/95ch67.sep.jpg",
      false
    )
  );
  return markerPics;
}
