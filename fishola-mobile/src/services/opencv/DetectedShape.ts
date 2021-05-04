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
 * Represents a shape detected by the open cv shape recognition.
 */
export class DetectedShape {
  isMarker: boolean;
  isFish: boolean;
  centerX: number;
  centerY: number;
  leftX: number;
  topY: number;
  vertices: Array<number>;

  /**
   * By convention we will define width as the greater value compared to height.
   */
  width: number;
  height: number;

  /**
   * The lenght "in the real world", calculated compared to marker size
   */
  calculatedLenght: number;

  constructor(
    centerX: number,
    centerY: number,
    leftX: number,
    topY: number,
    width: number,
    length: number,
    vertices: Array<number>
  ) {
    this.centerX = centerX;
    this.centerY = centerY;
    this.leftX = leftX;
    this.topY = topY;
    this.width = width;
    this.height = length;
    this.vertices = vertices;
  }
}
