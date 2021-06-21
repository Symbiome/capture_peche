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
export class OpenCVDetectionConfig {
  /* Picture to analyse will first be resized to this given size (for performance considerations) - int in px */
  resizeSize = 300;

  /*  The marker size "in real world" - float (in cm) */
  markerSizeInMm = 133;

  /* The minimum % of the screen each shape except marker should cover - float between 0 & 1*/
  minSizeRatio = 0.3;

  /* The maximum % of the screen each shape except marker should cover - float between 0 & 1*/
  maxSizeRatio = 0.999;

  /* The minimum % of the marker should cover - float between 0 & 1*/
  markerMinSizeRatio = 0.15;

  /* The minimum width / length ratio each shape should respect(trims "line" shapes) - float between 0 & 1*/
  minWidthLengthRatio = 0.1;

  /* The maximum widht/length ratio each shape should respect (trims "square" shapes) - float between 0 & 1*/
  maxWidthLengthRatio = 0.6;

  /* Indicates if we should draw debug shapes in dedicated canvas*/
  drawDebugCanvas = true;
}
