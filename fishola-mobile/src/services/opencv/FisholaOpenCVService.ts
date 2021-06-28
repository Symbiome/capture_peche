import { FishDetectionOptimizer } from "./FishDetectionOptimizer";
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
import { OpenCVUtils } from "./OpenCVUtils";
import { MarkerDetectionResult } from "./MarkerDetectionResult";
import { DetectedShape } from "./DetectedShape";
import { OpenCVDetectionConfig } from "./OpenCVDetectionConfig";
import { config } from "vue/types/umd";
import { SharePluginWeb } from "@capacitor/core";
export default class FisholaOpenCVService {
  static INSTANCE = new FisholaOpenCVService();
  public cv: any;

  /**
   * Indicates if the opencv.js library has done loaded.
   */
  isOpenCVReady(): boolean {
    return this.cv;
  }

  /**
   * - Finds all closed shapes in the given picture
   * - Tries to identify a marker amoung them
   * - Evaluates the other shapes size according to marker size
   * - Draws result in a picture
   * @param imgElement the <img> in wich shapes should be searched
   * @param imgElement the <img> holding the searcher marker
   * @param config the OpenCVDetectionConfig detailing what is the expected markerSize in mm, the proportion of a fish...
   */
  async calculateAndDrawFishSizes(
    imgElement: HTMLElement,
    markerElement: HTMLElement,
    config: OpenCVDetectionConfig
  ): Promise<Array<DetectedShape>> {
    // Step 1: load open cv and prepare output image
    await this.loadOpenCVIfNeeded();
    const cv = this.cv;
    const output = this.readAndResize(cv, imgElement, config.resizeSize);

    // Step 2: detect fishes and calculate sizes
    const fishDetectionOptimizer = new FishDetectionOptimizer(
      cv,
      imgElement,
      markerElement,
      config
    );
    const markerAndPotentialFishes = fishDetectionOptimizer.calculateAndOptimizeFishSizes();

    // Step 4: draw result in output pictures
    markerAndPotentialFishes.forEach((shape) => {
      OpenCVUtils.drawShape(cv, output, shape, config.drawDebugCanvas);
    });
    cv.imshow("canvasOutput3", output);
    output.delete();
    return markerAndPotentialFishes;
  }
  /**
   * - Finds all closed shapes in the given picture
   * - Tries to identify a marker amoung them
   * - Evaluates the other shapes size according to marker size
   * @param cv the openCV instance
   * @param imgElement the <img> in wich shapes should be searched
   * @param config the OpenCVDetectionConfig detailing what is the expected markerSize in mm, the proportion of a fish...
   */
  calculateFishSizes(
    cv: any,
    imgElement: HTMLElement,
    markerElement: HTMLElement,
    config: OpenCVDetectionConfig
  ): Array<DetectedShape> {
    // Step 1: find all closed shapes withing the picture
    const closedShapes = this.detectClosedShapesAndMarker(
      cv,
      imgElement,
      markerElement,
      config
    );

    // Step 2: extract marker form detected shapes
    // TODO change this fixed value
    let ratioBetweenMarkerInCmndMarkerInPx: number = 0.15;
    let marker: DetectedShape | null = null;
    const markerShapes = closedShapes.filter((shape: DetectedShape) => {
      return shape.isMarker;
    });
    if (markerShapes.length > 0) {
      marker = markerShapes[0];
      ratioBetweenMarkerInCmndMarkerInPx =
        config.markerSizeInMm / markerShapes[0].width;
    }

    // Step 3: calculate "real-world" size for each detected shape
    closedShapes.forEach((shape: DetectedShape) => {
      // We simple deduct "real-world" size from ratio between marker size in px and marker known cm size
      shape.calculatedLenght = Math.round(
        shape.width * ratioBetweenMarkerInCmndMarkerInPx
      );
    });
    return closedShapes;
  }

  /**
   * Returns all detected closed shapes in the given picture, and indicates if some of them are markers.
   *
   * See https://github.com/ucisysarch/opencvjs/blob/master/test/img_proc.html
   * and https://www.pyimagesearch.com/2016/03/28/measuring-size-of-objects-in-an-image-with-opencv/
   *
   * @param cv the openCV instance
   * @param imgElement the <img> in wich shapes should be searched
   * @param config the OpenCVDetectionConfig detailing what is the expected markerSize in mm, the proportion of a fish...
   */
  detectClosedShapesAndMarker(
    cv: any,
    imgElement: HTMLElement,
    markerElement: HTMLElement,
    config: OpenCVDetectionConfig
  ): Array<DetectedShape> {
    // Step 1: load the image & resize it
    const src = this.readAndResize(cv, imgElement, config.resizeSize);

    //  Step 2 : convert it to grayscale, and blur it slightly
    const refined1 = new cv.Mat();
    const refined2 = new cv.Mat();
    cv.cvtColor(src, refined1, cv.COLOR_BGR2GRAY, 0);
    const blurSize = new cv.Size(7, 7);
    cv.GaussianBlur(refined1, refined2, blurSize, 0, 0, cv.BORDER_DEFAULT);

    // Step 3: perform edge detection, then perform a dilation + erosion to
    // close gaps in between object edges
    // Step 3.1 : edge detection
    cv.Canny(refined2, refined1, 50, 100, 3, false);
    // Step 3.2 : dilation
    const kernel = cv.getStructuringElement(cv.MORPH_RECT, new cv.Size(5, 5));
    cv.dilate(refined1, refined2, kernel, new cv.Point(-1, 1), 1);
    // Step 3.3 : erosion
    /* TODO
        var borderValue = cv.Scalar.all(Number.MAX_VALUE);
        var erosion_type = cv.MorphShapes.MORPH_RECT.value;
        var erosion_size = [2*Control.erosion_size+1, 2*Control.erosion_size+1];
        var element = cv.getStructuringElement(erosion_type, erosion_size, [-1, -1]);
        cv.erode(refined2, refined1, element, [-1, -1], 1, cv.BORDER_CONSTANT, borderValue);*/

    // Step 4: find contours in the edge map
    // Step 4.1 : find contours
    cv.threshold(refined2, refined1, 120, 200, cv.THRESH_BINARY);
    const contours = new cv.MatVector();
    const hierarchy = new cv.Mat();
    const poly = new cv.MatVector();
    cv.findContours(
      refined1,
      contours,
      hierarchy,
      cv.RETR_EXTERNAL,
      cv.CHAIN_APPROX_SIMPLE
    );

    // Step 4.2 : approximates each contour to polygon
    for (let i = 0; i < contours.size(); ++i) {
      const tmp = new cv.Mat();
      const cnt = contours.get(i);
      cv.approxPolyDP(cnt, tmp, 3, true);
      poly.push_back(tmp);
      cnt.delete();
      tmp.delete();
    }

    // Step 5: return detected shapes
    const detectedShapes = [];
    for (let i = 0; i < poly.size(); ++i) {
      const cnt = poly.get(i);
      const rotatedRect = cv.minAreaRect(cnt);
      const vertices = cv.RotatedRect.points(rotatedRect);
      let leftX = 100000;
      let topY = 100000;
      for (let j = 0; j < 4; j++) {
        leftX = Math.min(vertices[j].x, leftX);
        topY = Math.min(vertices[j].y, topY);
      }
      const detectedShape = new DetectedShape(
        rotatedRect.center.x,
        rotatedRect.center.y,
        leftX,
        topY,
        // By convention we will define width as the greater value compared to height.
        Math.max(rotatedRect.size.width, rotatedRect.size.height),
        Math.min(rotatedRect.size.width, rotatedRect.size.height),
        vertices
      );
      detectedShape.isMarker = this.hasMarkerProportions(
        detectedShape,
        config
      );
      detectedShape.isFish = this.isShapePotentialFish(detectedShape, config);
      detectedShapes.push(detectedShape);
    }

    // Step 6: if several markers detected, discriminate them using template matching
    const markerCandidates = detectedShapes.filter((shape: DetectedShape) => {
      return shape.isMarker;
    });

    if (markerCandidates.length > 1) {
      const recognizedMarkers: DetectedShape[] = [];
      markerCandidates.forEach((markerCandidate) => {
        markerCandidate.isMarker = false;
        if (
          this.isMarkerTemplateMatched(
            cv,
            src,
            markerElement,
            markerCandidate,
            config
          )
        ) {
          markerCandidate.isMarker = true;
          recognizedMarkers.push(markerCandidate);
        }
      });
      if (recognizedMarkers.length > 1) {
        // There can only be one marker : TODO Discriminate
        recognizedMarkers[0].isMarker = true;
      } else {
        markerCandidates[0].isMarker = true;
      }
    }

    if (config.drawDebugCanvas) {
      cv.imshow("canvasOutput1", src);
      cv.imshow("canvasOutput2", refined1);
    }

    src.delete();
    refined1.delete();
    refined2.delete();

    return detectedShapes;
  }

  /**
   * Indicates if the given shape can be considered as a potential fish or should be ignored.
   * If too "small", "liny" or "squary", the shape will be dismissed (we known it cannot be a fish)
   * @param shape the Shape to consider
   * @param config the OpenCVDetectionConfig detailing what is the expected markerSize in mm, the proportion of a fish...
   */
  isShapePotentialFish(
    shape: DetectedShape,
    config: OpenCVDetectionConfig
  ): boolean {
    // Shape bust occuppy a certain % of the full image
    const shapeSizeOnPictureRatio = shape.width / config.resizeSize;
    if (
      shapeSizeOnPictureRatio >= config.minSizeRatio &&
      shapeSizeOnPictureRatio <= config.maxSizeRatio
    ) {
      const widthLengthRatio = shape.height / shape.width;
      /// Ratio height/widht must indicate a shape that is not "squary" neither "linny"
      if (
        widthLengthRatio >= config.minWidthLengthRatio &&
        widthLengthRatio <= config.maxWidthLengthRatio
      ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Indicates if the given detected shape is a marker or a random shape based on proprtions.
   * @param cv the openCV instance
   * @param picture the picture in which the marker has been detected
   * @param config the OpenCVDetectionConfig detailing what is the expected markerSize in mm, the proportion of a fish...
   * @param markerCandidate the shape which is potentially a marker
   */
  hasMarkerProportions(
    markerCandidate: DetectedShape,
    config: OpenCVDetectionConfig
  ) {
    // Shape bust occuppy a certain % of the full image
    const shapeSizeOnPictureRatio = markerCandidate.width / config.resizeSize;
    if (
      shapeSizeOnPictureRatio >= config.markerMinSizeRatio &&
      shapeSizeOnPictureRatio <= config.maxSizeRatio
    ) {
      const diffBetweenWidthAndHeight = Math.abs(
        markerCandidate.width - markerCandidate.height
      );
      // Only square-like shapes can be potential markers
      if (diffBetweenWidthAndHeight < markerCandidate.width * 0.1) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines if the given marker candidate is actually a marker using template matching.
   * @param imgElement
   * @param imgMarker
   */
  isMarkerTemplateMatched(
    cv: any,
    resizedPicture: any,
    markerElement: HTMLElement,
    markerCandidate: DetectedShape,
    config: OpenCVDetectionConfig
  ): boolean {
    // Step 1: read marker at expected dimensions
    console.error(
      "* is (" +
        Math.round(markerCandidate.leftX) +
        "," +
        Math.round(markerCandidate.topY) +
        ") a marker of " +
        Math.round(markerCandidate.width) +
        "px ?"
    );
    const marker = this.readAndResize(
      cv,
      markerElement,
      Math.round(markerCandidate.width)
    );

    let isAMarker = false;
    let angle = 0;
    let matchedPoint = new cv.Point();
    while (!isAMarker && angle < 360) {
      // Step 2: perform marker rotation in all possible angles to optimize recognition
      const rotatedMarker = new cv.Mat();
      const dsize = new cv.Size(marker.rows, marker.cols);
      const center = new cv.Point(marker.cols / 2, marker.rows / 2);
      // You can try more different parameters
      const M = cv.getRotationMatrix2D(center, angle, 1);
      cv.warpAffine(
        marker,
        rotatedMarker,
        M,
        dsize,
        cv.INTER_LINEAR,
        cv.BORDER_CONSTANT,
        new cv.Scalar()
      );

      // Step 3: match template
      const matchedDst = new cv.Mat();
      const match_method = cv.TM_SQDIFF; // cv.TM_SQDIFF // TM_SQDIFF_NORMED // TM_CCORR // TM_CCORR_NORMED // TM_COEFF // TM_CCOEFF_NORMED
      cv.matchTemplate(resizedPicture, rotatedMarker, matchedDst, match_method);
      const result = cv.minMaxLoc(matchedDst);
      // According to the method, take different point from result (see https://docs.opencv.org/3.4/de/da9/tutorial_template_matching.html)
      matchedPoint = result.maxLoc;
      if (match_method == cv.TM_SQDIFF || match_method == cv.TM_SQDIFF_NORMED) {
        matchedPoint = result.minLoc;
      }

      // Step 4: determine if template matching indicates this is a marker
      isAMarker = this.templateMatchingResultIndicatesShapeIsAMarker(
        cv,
        markerCandidate,
        rotatedMarker,
        matchedPoint
      );

      console.error(
        "- " + isAMarker + " FOR " + angle + "° : ",
        matchedPoint,
        rotatedMarker.cols
      );
      angle += config.rotationStep;
    }
    // TODO remove this, only to easy visualising debug
    markerCandidate.leftX = matchedPoint.x;
    markerCandidate.topY = matchedPoint.y;
    markerCandidate.width = marker.cols;
    // @ts-ignore
    markerCandidate.vertices[0].x = matchedPoint.x;
    // @ts-ignore
    markerCandidate.vertices[0].y = matchedPoint.y;
    // @ts-ignore
    markerCandidate.vertices[1].x = matchedPoint.x + marker.cols;
    // @ts-ignore
    markerCandidate.vertices[1].y = matchedPoint.y;
    // @ts-ignore
    markerCandidate.vertices[2].x = matchedPoint.x + marker.cols;
    // @ts-ignore
    markerCandidate.vertices[2].y = matchedPoint.y + marker.rows;
    // @ts-ignore
    markerCandidate.vertices[3].x = matchedPoint.x;
    // @ts-ignore
    markerCandidate.vertices[3].y = matchedPoint.y + marker.rows;
    marker.delete();
    console.error(
      "*******************************************************************"
    );
    return isAMarker;
  }

  private templateMatchingResultIndicatesShapeIsAMarker(
    cv: any,
    markerCandidate: DetectedShape,
    marker: any,
    matchedPoint: any
  ): boolean {
    let isAMarker = false;
    const matchedPointEnd = new cv.Point(
      matchedPoint.x + marker.cols,
      matchedPoint.y + marker.rows
    );

    // If detected marker is close to marker candidate shape, then this is the marker
    const MAX_ALLOWED_MARKER_POSITION_DIFF = markerCandidate.width / 10;
    const diffX = Math.abs(matchedPoint.x - markerCandidate.leftX);
    const diffY = Math.abs(matchedPoint.y - markerCandidate.topY);
    const diffWidth =
      Math.max(
        matchedPointEnd.x - matchedPoint.x,
        matchedPointEnd.y - matchedPoint.y
      ) - markerCandidate.width;
    if (
      diffX < MAX_ALLOWED_MARKER_POSITION_DIFF &&
      diffY < MAX_ALLOWED_MARKER_POSITION_DIFF &&
      diffWidth < MAX_ALLOWED_MARKER_POSITION_DIFF
    ) {
      // console.error("ITS A MARKER ! ");
      isAMarker = true;
    } else {
      //console.log("NOT A MARKER");
    }
    console.log(
      "diffX : " + diffX + " = " + matchedPoint.x + "-" + markerCandidate.leftX
    );
    console.log(
      "diffX : " + diffY + " = " + matchedPoint.y + "-" + markerCandidate.topY
    );
    console.log(
      "diffWidth : " +
        diffWidth +
        " = max(" +
        matchedPointEnd.x +
        "-" +
        matchedPoint.x +
        ", " +
        matchedPointEnd.y +
        "-" +
        matchedPoint.y +
        ") -" +
        markerCandidate.width
    );
    return isAMarker;
  }

  private getMarkerRotationAngles(): number[] {
    return [0, 45, 90, 135, 180, 225, 270, 315, 360];
  }

  /**
   * Uses openCV to read & resize the picture contained in the given <img> Element to the given resizeSize.
   * @param cv the openCV instance
   * @param imgElement the <img> in wich shapes should be searched
   * @param resizeSize picture to analyse will first be resized to this given size (for performance considerations) - int in px
   * @returns the resized picture, exploitable by openCV for further manipulation
   */
  private readAndResize(cv: any, imgElement: HTMLElement, resizeSize: number) {
    const original = cv.imread(imgElement);
    const src = new cv.Mat();
    // Vertical images
    if (original.cols > original.rows) {
      // Fix with
      const reducedWidth = resizeSize;
      const ratioWith = reducedWidth / original.cols;
      const dsize = new cv.Size(reducedWidth, ratioWith * original.rows);
      cv.resize(original, src, dsize, 0, 0, cv.INTER_AREA);
    } else {
      // Fix height
      const reduceHeight = resizeSize;
      const ratioHeight = reduceHeight / original.rows;
      const dsize = new cv.Size(ratioHeight * original.cols, reduceHeight);
      cv.resize(original, src, dsize, 0, 0, cv.INTER_AREA);
    }
    return src;
  }

  /**
   * Loads the opencv.js library if required (i.e. was never loaded).
   */
  async loadOpenCVIfNeeded(): Promise<void> {
    const result = new Promise<void>((resolve, _reject) => {
      // Check whether OpenCV is loaded or not
      if (!this.cv) {
        // If not, let's create an async script to load it
        const callbackTarget = this;
        const head = document.getElementsByTagName("head").item(0);
        const script = document.createElement("script");
        script.setAttribute("type", "text/javascript");
        script.setAttribute("src", "/js/opencv.js");
        script.setAttribute("async", "");
        // Add a callback allowing this.openCVReady() to be called when OpenCV Is ready
        script.setAttribute(
          "onLoad",
          "document.dispatchEvent(new Event('open-cv-loaded'))"
        );
        document.addEventListener(
          "open-cv-loaded",
          function(e) {
            // @ts-ignore
            FisholaOpenCVService.INSTANCE.cv = window.cv;
            resolve();
          },
          false
        );
        // @ts-ignore
        head.appendChild(script);
      } else {
        resolve();
      }
    });
    return result;
  }
}
