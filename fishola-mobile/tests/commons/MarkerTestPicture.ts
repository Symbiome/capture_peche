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
 * Represents a picture to test.
 */
export default class MarkerTestPicture {
  markerPath: string;
  filePath: string;
  shouldHaveMarker: boolean;
  expectedFishOnImageRatio: number;
  comment: string;
  quality: number;
  expectedMeasureInMm: number;

  constructor(
    markerPath: string,
    filePath: string,
    shouldHaveMarker: boolean,
    expectedFishMeasure: number,
    comment: string,
    quality: number,
    expectedMeasureInMm: number
  ) {
    this.markerPath = markerPath;
    this.filePath = filePath;
    this.shouldHaveMarker = shouldHaveMarker;
    this.expectedFishOnImageRatio = expectedFishMeasure;
    this.comment = comment;
    this.quality = quality;
    if (expectedMeasureInMm) {
      this.expectedMeasureInMm = expectedMeasureInMm;
    }
  }
}
