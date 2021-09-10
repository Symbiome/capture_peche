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
import { DetectedShape } from "./DetectedShape";
export class OpenCVUtils {
  /**
   * Draws a recognized shape in the given picture
   */
  static drawShape(
    cv: any,
    output: any,
    shapeToDraw: DetectedShape,
    drawIgnoredShape: boolean,
    drawSizeText: boolean
  ) {
    // Color : marker & fish in green, ignored shape in grey
    let color = [0, 0, 220, 255];
    if (shapeToDraw.isFish) {
      color = [0, 220, 0, 255];
    } else if (shapeToDraw.isMarker) {
      color = [30, 155, 196, 255];
    } else if (shapeToDraw.isDebug) {
      color = [255, 127, 0, 255];
    }

    // Draw rectangle around shape
    if (drawIgnoredShape || shapeToDraw.isFish || shapeToDraw.isMarker) {
      for (let j = 0; j < 4; j++) {
        cv.line(
          output,
          shapeToDraw.vertices[j],
          shapeToDraw.vertices[(j + 1) % 4],
          color,
          2,
          cv.LINE_AA,
          0
        );
      }

      // If shape is not a marker, draw calculated length
      if (shapeToDraw.isFish && drawSizeText) {
        cv.putText(
          output,
          shapeToDraw.calculatedLenght + "mm",
          {
            x: (shapeToDraw.leftX + shapeToDraw.centerX) / 2,
            y: shapeToDraw.centerY,
          },
          cv.FONT_HERSHEY_SIMPLEX,
          0.8,
          [0, 0, 0, 255],
          3
        );
        cv.putText(
          output,
          shapeToDraw.calculatedLenght + "mm",
          {
            x: (shapeToDraw.leftX + shapeToDraw.centerX) / 2,
            y: shapeToDraw.centerY,
          },
          cv.FONT_HERSHEY_SIMPLEX,
          0.8,
          [255, 255, 255, 255],
          1
        );
      }
    }
  }
}
