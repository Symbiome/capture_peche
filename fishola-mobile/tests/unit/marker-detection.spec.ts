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
import MarkerTestPicture from "./MarkerTestPicture";
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";
// Explicitely load opencv (would normally be loaded lazily by FisholaOpenCVService)
import opencv from "./opencv.js";
FisholaOpenCVService.INSTANCE.cv = opencv;

const defaultMarkerPath = "marker/marker.png";
const markerTestPictures = new Array<MarkerTestPicture>();
markerTestPictures.push(
  new MarkerTestPicture(
    defaultMarkerPath,
    "marker/IMG_20210427_103107.jpg",
    true
  )
);
markerTestPictures.push(
  new MarkerTestPicture(
    defaultMarkerPath,
    "marker/IMG_20210427_103121.jpg",
    true
  )
);
markerTestPictures.push(
  new MarkerTestPicture(
    defaultMarkerPath,
    "marker/IMG_20210427_103130.jpg",
    true
  )
);
markerTestPictures.push(
  new MarkerTestPicture(defaultMarkerPath, "fish-measures/b99in367.jpg", false)
);
markerTestPictures.push(
  new MarkerTestPicture(
    defaultMarkerPath,
    "fish-measures/95ch67.sep.jpg",
    false
  )
);

// Test suite related to automatic marker detection from picture with opencv
describe("Marker detection", () => {

  // Setup: create marker and load openCV
  const marker = document.createElement("img");
  marker.setAttribute(
    "src",
    "blob:http://localhost:8081/f41b7aff-c980-4194-9104-8046497250cb"
  );

  for (let i = 0; i < markerTestPictures.length; i++) {
    const expectedResult = markerTestPictures[i];
    test("File " + expectedResult.filePath, async () => {
      // Check that open cv service is correctly loaded
      expect(FisholaOpenCVService.INSTANCE.isOpenCVReady()).toBeTruthy();

      try {
        const picture = document.createElement("picture-" + i);
        picture.setAttribute(
          "src",
          "blob:http://localhost:8081/f41b7aff-c980-4194-9104-8046497250cb"
        );
        const markerDetectionResult = await FisholaOpenCVService.INSTANCE.detectMarker(
          picture,
          marker
        );
        // Check that marker is detected (or not) as expected
        expect(markerDetectionResult.markerDetected).toEqual(
          expectedResult.hasMarker
        );
      } catch (e) {
        fail(e);
      }
    });
  }
});
