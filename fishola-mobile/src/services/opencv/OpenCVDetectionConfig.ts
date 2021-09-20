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
  markerSizeInMm = 97;

  /* The minimum % of the screen each shape except marker should cover - float between 0 & 1*/
  minSizeRatio = 0.3;

  /* The maximum % of the screen each shape except marker should cover - float between 0 & 1*/
  maxSizeRatio = 0.999;

  /* The minimum % of the marker should cover - float between 0 & 1*/
  markerMinSizeRatio = 0.05;

  /* The minimum width / length ratio each shape should respect(trims "line" shapes) - float between 0 & 1*/
  minWidthLengthRatio = 0.1;

  /* The maximum widht/length ratio each shape should respect (trims "square" shapes) - float between 0 & 1*/
  maxWidthLengthRatio = 0.6;

  /** Default url for marker src */
  defaultMarkerSrc = "/img/default_marker.jpg";

  /**
   * Knn distance for feature matching (see https://scottsuhy.com/2021/02/01/image-alignment-feature-based-in-opencv-js-javascript/). A float around 0.7.
   */
  knnDistanceForFeatureMatching = 0.75;

  /**
   * If this number of feature matched is reached for a shap, we will consider it as the marker and stop searching.
   */
  maxFeatureMatchRequired = 4;

  /**
   * Minimal number of feature match required to consider a shape as a marker.
   */
  minFeaturematchRequired = 2;

  /**
   * Indicates if we should check marker candidates even if there is only one.
   */
  alwaysCheckMarkerCandidates = false;

  /** Rotation step to use for marker detection (in degree, e.g. 90°)*/
  rotationStep = 45;

  /**
   * Maximal distance (in px) for considering that a template matched zone is the marker.
   */
  maxDistanceBetweenTemplateMatchedAndCandidate = 20;

  /**
   * Lower threeshold for the canny edge detection algorithm.
   */
  cannyEdgeLowerThreshold = 50;

  /**
   * Upper threeshold for the canny edge detection algorithm.
   */
  cannyEdgeUpperThreshold = 100;

  /**
   * Contour size to use for dilation.
   */
  dilationThickness = 5;

  /**
   * Gaussian blur size.
   */
  blurSize = 7;

  /* Indicates if we should draw debug shapes in dedicated canvas*/
  drawDebugCanvas = true;

  /**
   * Indicates if there is  marker on the pictures to analyse (alway true, only set to false for testing purpose)
   */
  pictureIsSupposedToContainMarker = true;

  /**
   * Indicates if there is  fish on the pictures to analyse (alway true, only set to false for testing purpose)
   */
  pictureIsSupposedToContainFish = true;

  /**
   * Number of retries the size computation algorithm allows itself before returning the current result.
   */
  maxRetries = 2;
}
