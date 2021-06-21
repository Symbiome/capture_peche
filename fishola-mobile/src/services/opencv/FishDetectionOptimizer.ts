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

import { OpenCVDetectionConfig } from "./OpenCVDetectionConfig";
import { DetectedShape } from "./DetectedShape";
import FisholaOpenCVService from "./FisholaOpenCVService";

/**
 * In charge of optimizing fish detection by playing with configuration until a correct result is found.
 */
export class FishDetectionOptimizer {
  private cv: any;
  private imgElement: HTMLElement;
  private markerElement: HTMLElement | null;
  private config: OpenCVDetectionConfig;

  constructor(
    cv: any,
    imgElement: HTMLElement,
    markerElement: HTMLElement | null,
    config: OpenCVDetectionConfig
  ) {
    this.cv = cv;
    this.imgElement = imgElement;
    this.markerElement = markerElement;
    this.config = config;
  }

  /**
   * - Finds all closed shapes in the given picture
   * - Tries to identify a marker amoung them
   * - Evaluates the other shapes size according to marker size
   * - Tries to modify config and result until one single fish is detected
   */
  calculateAndOptimizeFishSizes(): Array<DetectedShape> {
    // Step 1: get raw results
    const markerAndPotentialFishes = FisholaOpenCVService.INSTANCE.calculateFishSizes(
      this.cv,
      this.imgElement,
      this.markerElement,
      this.config
    );
    let biggestFishSize: number = 0;
    let fishCount: number = 0;
    markerAndPotentialFishes.forEach((potentialFish: DetectedShape) => {
      if (potentialFish.isFish) {
        fishCount++;
        if (potentialFish.calculatedLenght > biggestFishSize) {
          biggestFishSize = potentialFish.calculatedLenght;
        }
      }
    });

    // Step 2: see if raw results are ok or if we have to try again with another config

    // If we detected more than one fish, only keep the biggest one and return
    if (fishCount >= 1) {
      markerAndPotentialFishes.forEach((potentialFish: DetectedShape) => {
        if (
          potentialFish.isFish &&
          potentialFish.calculatedLenght != biggestFishSize
        ) {
          potentialFish.isFish = false;
        }
      });
      return markerAndPotentialFishes;
    } else {
      // We did not detected any fish. Try to increase picture resolute and try again
      // TODO
    }

    return markerAndPotentialFishes;
  }
}
