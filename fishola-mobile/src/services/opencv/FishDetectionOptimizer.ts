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
  private markerElement: HTMLElement;
  private config: OpenCVDetectionConfig;

  constructor(
    cv: any,
    imgElement: HTMLElement,
    markerElement: HTMLElement,
    config: OpenCVDetectionConfig
  ) {
    this.cv = cv;
    this.imgElement = imgElement;
    this.markerElement = markerElement;
    this.config = JSON.parse(JSON.stringify(config));
  }

  /**
   * - Finds all closed shapes in the given picture
   * - Tries to identify a marker amoung them
   * - Evaluates the other shapes size according to marker size
   * - Tries to modify config and result until one single fish is detected
   */
  calculateAndOptimizeFishSizes(retries: number): Array<DetectedShape> {
    // Step 1: get raw results
    const markerAndPotentialFishes = FisholaOpenCVService.INSTANCE.calculateFishSizes(
      this.cv,
      this.imgElement,
      this.markerElement,
      this.config
    );
    let biggestFishSize: number = 0;
    let biggestFish: DetectedShape | undefined;
    let fishCount: number = 0;
    let marker: DetectedShape | undefined;

    // Step 2: get biggest fish and marker
    markerAndPotentialFishes.forEach((potentialFish: DetectedShape) => {
      if (potentialFish.isFish) {
        fishCount++;
        if (potentialFish.calculatedLenght > biggestFishSize) {
          biggestFishSize = potentialFish.calculatedLenght;
          biggestFish = potentialFish;
        }
      }

      if (potentialFish.isMarker && !marker) {
        marker = potentialFish;
      } else {
        potentialFish.isMarker = false;
      }
    });

    // If we detected more than one fish, only keep the biggest one
    if (fishCount > 1) {
      markerAndPotentialFishes.forEach((potentialFish: DetectedShape) => {
        if (
          potentialFish.isFish &&
          potentialFish.calculatedLenght != biggestFishSize
        ) {
          potentialFish.isFish = false;
        }
      });
    }

    if (retries <= this.config.maxRetries) {
      // If we are allowed to retry with different parameters, check if we can improve result
      let resultCanBeImproved = true;

      // Case 1 : we did not detect the marker but were expecting one
      if (!marker && this.config.pictureIsSupposedToContainMarker) {
        // If we found a fish, maybe marker and fish are too close and were considered as a single fish
        if (fishCount >= 1) {
          // => try again with differents threeshold for canny edge detection & thickness size
          this.config.dilationThickness = 3;
          this.config.cannyEdgeUpperThreshold += 20;
          console.error(
            "Failed to detect marker -> increasing cannyEdgeUpperThreshold to " +
              this.config.cannyEdgeUpperThreshold +
              " and dilation thickness to " +
              this.config.dilationThickness
          );
        } else {
          // Here we did not found neither fish nor marker, let's increase resize size
          this.config.resizeSize *= 1.5;
          console.error(
            "Failed to detect marker and fish, increase resize size to " +
              this.config.resizeSize
          );
        }
        return this.calculateAndOptimizeFishSizes(retries + 1);
      } else {
        // 1 - Check if the marker we detected is inside our biggest fish. If so it means that fish and marker are too close
        if (marker && this.markerIsInsideFish(marker, biggestFish)) {
          // => try again with a higher resize size so that marker can be separated
          this.config.cannyEdgeUpperThreshold += 20;
          this.config.dilationThickness -= 2;
          console.error(
            "Marker is inside fish => decreasing dilation thickness to " +
              this.config.dilationThickness +
              " and cannyEdgeUpperThreshold to " +
              this.config.cannyEdgeUpperThreshold
          );
        }
        // Step 2: see if raw results are ok or if we have to try again with another config
        else if (fishCount < 1) {
          // We did not detected any fish. Try to decrease picture resolute and try again
          this.config.resizeSize *= 0.75;
          console.error(
            "Failed to detect fish, decrease resize size to " +
              this.config.resizeSize
          );
        } else {
          // Here we have a marker and a fish, both separated from each other
          // Let's stop here
          resultCanBeImproved = false;
        }
      }

      if (resultCanBeImproved) {
        // Relaunch detection with the modified configuration
        return this.calculateAndOptimizeFishSizes(retries + 1);
      }
    }

    return markerAndPotentialFishes;
  }

  markerIsInsideFish(
    marker: DetectedShape,
    fish: DetectedShape | undefined
  ): boolean {
    if (fish) {
      const markerRightX = marker.leftX + (marker.centerX - marker.leftX) * 2;
      const markerBottomY = marker.topY + (marker.centerY - marker.topY) * 2;
      const fishRightX = fish.leftX + (fish.centerX - fish.leftX) * 2;
      const fishBottomY = fish.topY + (fish.centerY - fish.topY) * 2;
      return (
        marker.leftX > fish.leftX &&
        markerRightX < fishRightX &&
        marker.topY > fish.topY &&
        markerBottomY < fishBottomY
      );
    }
    return false;
  }
}
