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
import { DetectedShape } from "./DetectedShape";
import { FeatureMatch } from "./FeatureMatch";
import { OpenCVDetectionConfig } from "./OpenCVDetectionConfig";

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
    config: OpenCVDetectionConfig,
    resultCanvasId: string
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
    const markerAndPotentialFishes = fishDetectionOptimizer.calculateAndOptimizeFishSizes(
      0
    );

    // Step 4: draw result in output pictures
    markerAndPotentialFishes.forEach((shape) => {
      if (!shape.isDebug) {
        OpenCVUtils.drawShape(
          cv,
          output,
          shape,
          config.drawDebugCanvas,
          config.drawDebugCanvas
        );
      } else if (config.drawDebugCanvas) {
        OpenCVUtils.drawShape(cv, output, shape, true, true);
      }
    });
    if (resultCanvasId) {
      cv.imshow(resultCanvasId, output);
    }
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
    const markerShapes = closedShapes.filter((shape: DetectedShape) => {
      return shape.isMarker;
    });
    if (markerShapes.length > 0) {
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
    const blurSize = new cv.Size(config.blurSize, config.blurSize);
    cv.GaussianBlur(refined1, refined2, blurSize, 0, 0, cv.BORDER_DEFAULT);

    // Step 3: perform edge detection, then perform a dilation
    // Step 3.1 : edge detection https://docs.opencv.org/3.4/da/d22/tutorial_py_canny.html
    cv.Canny(
      refined2,
      refined1,
      config.cannyEdgeLowerThreshold,
      config.cannyEdgeUpperThreshold,
      3,
      false
    );
    // Step 3.2 : dilation https://docs.opencv.org/3.4/db/df6/tutorial_erosion_dilatation.html
    const kernel = cv.getStructuringElement(
      cv.MORPH_RECT,
      new cv.Size(config.dilationThickness, config.dilationThickness)
    );
    cv.dilate(refined1, refined2, kernel, new cv.Point(-1, 1), 1);
    // Some literature advise to apply erosion at this step, but this was not needed in our test case
    // Something to keep in mind in the future though

    // Step 4: find contours in the edge map
    // Step 4.1 : threshold https://docs.opencv.org/4.5.2/d7/d4d/tutorial_py_thresholding.html
    cv.threshold(refined2, refined1, 120, 200, cv.THRESH_BINARY);
    const contours = new cv.MatVector();
    const hierarchy = new cv.Mat();
    const poly = new cv.MatVector();
    // Step 4.2 find contours
    cv.findContours(
      refined1,
      contours,
      hierarchy,
      cv.RETR_LIST,
      cv.CHAIN_APPROX_SIMPLE
    );

    // Step 4.3 : approximates each contour to polygon
    for (let i = 0; i < contours.size(); ++i) {
      const tmp = new cv.Mat();
      const cnt = contours.get(i);
      cv.approxPolyDP(cnt, tmp, 3, true);
      poly.push_back(tmp);
      cnt.delete();
      tmp.delete();
    }

    // Step 5: wrap each polygon in a rectangle and convert to DetectedShape
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
      detectedShape.isMarker = this.hasMarkerProportions(detectedShape, config);
      detectedShape.isFish = this.isShapePotentialFish(detectedShape, config);
      detectedShapes.push(detectedShape);
      cnt.delete();
    }

    // Step 6: if several markers detected, discriminate them using feature matching
    const markerCandidates = detectedShapes.filter((shape) => shape.isMarker);
    if (
      markerCandidates.length > 1 ||
      (markerCandidates.length == 1 && config.alwaysCheckMarkerCandidates)
    ) {
      markerCandidates.forEach((markerCandidate) => {
        markerCandidate.isMarker = false;
      });
      const featureMatchedMarker = this.findBestCandidateUsingFeatureMatching(
        cv,
        imgElement,
        markerElement,
        markerCandidates,
        config
      );
      if (featureMatchedMarker) {
        featureMatchedMarker.isMarker = true;
      }
      const debugShapes = markerCandidates.filter((shape) => shape.isDebug);
      debugShapes.forEach((debugShape) => detectedShapes.push(debugShape));
    }
    markerCandidates.forEach((mc) => {
      if (mc.isMarker) {
        const croppedInPx = mc.height - config.markerMaxCroppingInPixels;
        const croppedInProportion =
          mc.height * config.markerMaxCroppingInProportion;
        mc.height = Math.max(croppedInPx, croppedInProportion);
        mc.width = mc.height;
      }
    });
    if (config.drawDebugCanvas) {
      cv.imshow("canvasOutput1", src);
      cv.imshow("canvasOutput2", refined1);
    }

    src.delete();
    refined1.delete();
    refined2.delete();
    hierarchy.delete();
    poly.delete();
    contours.delete();
    kernel.delete();

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
   * Use feature matching to determine the best candidate.
   * @param cv the OpenCV instance
   * @param resizedPicture the picture in which the marker search should be performed
   * @param markerElement the <image> holding the marker picture to search
   * @param markerCandidates the potential candidates
   * @param config the OpenCV configuration
   */
  findBestCandidateUsingFeatureMatching(
    cv: any,
    pictureElement: HTMLElement,
    markerElement: HTMLElement,
    markerCandidates: DetectedShape[],
    config: OpenCVDetectionConfig
  ): DetectedShape | undefined {
    // First, read marker and find its feature
    // See https://scottsuhy.com/2021/02/01/image-alignment-feature-based-in-opencv-js-javascript/
    // And https://docs.opencv.org/4.5.2/dc/dc3/tutorial_py_matcher.html
    const marker = this.readAndResize(cv, markerElement, config.resizeSize);
    const grayedMarker = new cv.Mat();
    cv.cvtColor(marker, grayedMarker, cv.COLOR_BGR2GRAY);

    const markerKeypoints = new cv.KeyPointVector();
    const markerDescriptors = new cv.Mat();
    const temp = new cv.Mat();
    const orb = new cv.AKAZE();
    orb.detectAndCompute(
      grayedMarker,
      temp,
      markerKeypoints,
      markerDescriptors
    );
    temp.delete();
    const resizeCoef = 3;

    // Order marker candidates by width : study biggest ones first
    markerCandidates.sort((c1, c2) => {
      const c1Width = c1.centerX - c1.leftX;
      const c2Width = c2.centerX - c2.leftX;
      return c2Width - c1Width;
    });
    // Then for each marker candidate
    const shapeScores: number[] = new Array<number>(markerCandidates.length);
    let bestMarker: DetectedShape | undefined = undefined;
    let foundSureMatch = false;
    let bestScore = 0;
    console.info(
      markerCandidates.length + " marker candidates (squared shapes) to study"
    );
    for (let j = 0; j < markerCandidates.length && !foundSureMatch; j++) {
      const markerCandidate = markerCandidates[j];
      markerCandidate.featureMatchTested = true;
      // Step 1: read and crop image (only keep zone of image corresponding to marker candidate)
      const pictureFull = this.readAndResize(
        cv,
        pictureElement,
        config.resizeSize * resizeCoef
      );
      const zoneOfInterest = new cv.Rect(
        Math.min(pictureFull.cols, markerCandidate.leftX * resizeCoef),
        Math.min(pictureFull.rows, markerCandidate.topY * resizeCoef),
        Math.min(
          pictureFull.cols - markerCandidate.leftX * resizeCoef,
          markerCandidate.width * resizeCoef
        ),
        Math.min(
          pictureFull.rows - markerCandidate.topY * resizeCoef,
          markerCandidate.width * resizeCoef
        )
      );
      const picture = pictureFull.roi(zoneOfInterest);
      pictureFull.delete();

      //  Step 2 : convert it to grayscale
      const grayedPicture = new cv.Mat();
      cv.cvtColor(picture, grayedPicture, cv.COLOR_BGR2GRAY);

      // Step 3: Detect Features & Compute Descriptors
      const pictureKeypoints = new cv.KeyPointVector();
      const pictureDescriptors = new cv.Mat();
      const temp = new cv.Mat();
      orb.detectAndCompute(
        grayedPicture,
        temp,
        pictureKeypoints,
        pictureDescriptors
      );
      temp.delete();

      // Step 4: perform feature matching with the expected marker features
      const matchedFeatures: FeatureMatch[] = [];
      const bf = new cv.BFMatcher();
      const matches = new cv.DMatchVectorVector();

      bf.knnMatch(pictureDescriptors, markerDescriptors, matches, 2);

      for (let i = 0; i < matches.size(); ++i) {
        const match = matches.get(i);
        const dMatch1 = match.get(0);
        const dMatch2 = match.get(1);
        if (
          dMatch1.distance <=
          dMatch2.distance * config.knnDistanceForFeatureMatching
        ) {
          const featureMatch = new FeatureMatch();
          const correspondingPointInPicture = pictureKeypoints.get(
            dMatch1.queryIdx
          ).pt;
          featureMatch.x = correspondingPointInPicture.x / resizeCoef;
          featureMatch.y = correspondingPointInPicture.y / resizeCoef;
          featureMatch.distance = dMatch1.distance;
          matchedFeatures.push(featureMatch);
        }
      }

      // Step 5: sort matched features and iterate over each match
      matchedFeatures.sort((match1: any, match2: any) => {
        return match1.distance - match2.distance;
      });
      shapeScores[j] = matchedFeatures.length;
      if (shapeScores[j] >= bestScore) {
        bestMarker = markerCandidate;
        bestScore = shapeScores[j];
      }
      // If enough match are contained in the shape, then it is most likely our marker, stop searching
      if (shapeScores[j] >= config.maxFeatureMatchRequired) {
        foundSureMatch = true;
      }

      matches.delete();
      bf.delete();
      pictureKeypoints.delete();
      pictureDescriptors.delete();
      picture.delete();
      grayedPicture.delete();

      console.info(
        matchedFeatures.length +
          " matched features for marker (" +
          Math.round(markerCandidate.centerX) +
          "," +
          Math.round(markerCandidate.topY) +
          ")"
      );
    }

    markerKeypoints.delete();
    markerDescriptors.delete();
    grayedMarker.delete();
    marker.delete();
    orb.delete();

    if (bestScore >= config.minFeaturematchRequired) {
      return bestMarker;
    }
    return undefined;
  }

  /**
   * Use template matching to determine the best candidate.
   * DEPRECATED : feature matching (although more resource-taking) is way better for matching.
   * @param cv the OpenCV instance
   * @param resizedPicture the picture in which the marker search should be performed
   * @param markerElement the <image> holding the marker picture to search
   * @param markerCandidates the potential candidates
   * @param config the OpenCV configuration
   */
  findBestCandidateUsingTemplateMatching(
    cv: any,
    resizedPicture: any,
    markerElement: HTMLElement,
    markerCandidates: DetectedShape[],
    config: OpenCVDetectionConfig
  ): DetectedShape {
    // Step 1: read marker
    const marker = this.readAndResize(
      cv,
      markerElement,
      resizedPicture.cols / 4
    );

    let templateDetected = false;
    let angle = 0;
    let matchedPoint = new cv.Point();
    let closestDistance = 100000;
    let closestMarker: DetectedShape = markerCandidates[0];
    while (!templateDetected && angle < 360) {
      // Step 2: perform marker rotation in all possible angles to optimize recognition
      const rotatedMarker = new cv.Mat();
      const dsize = new cv.Size(marker.rows, marker.cols);
      const center = new cv.Point(marker.cols / 2, marker.rows / 2);
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

      // Step 3: perform template matching in search of the marker
      const matchedDst = new cv.Mat();
      const match_method = cv.TM_SQDIFF; // cv.TM_SQDIFF // TM_SQDIFF_NORMED // TM_CCORR // TM_CCORR_NORMED // TM_COEFF // TM_CCOEFF_NORMED
      cv.matchTemplate(resizedPicture, rotatedMarker, matchedDst, match_method);
      const result = cv.minMaxLoc(matchedDst);
      // According to the method, take different point from result (see https://docs.opencv.org/3.4/de/da9/tutorial_template_matching.html)
      matchedPoint = result.maxLoc;
      if (match_method == cv.TM_SQDIFF || match_method == cv.TM_SQDIFF_NORMED) {
        matchedPoint = result.minLoc;
      }

      const matchedResult: number[] = [];
      matchedResult[0] = matchedPoint;
      matchedResult[1] = new cv.Point(
        matchedPoint.x + marker.cols,
        matchedPoint.y + marker.rows
      );

      // Step 4: determine if template matching indicates this is a marker
      // Get the closest marker of template matched location
      markerCandidates.forEach((markerCandidate) => {
        markerCandidate.isMarker = false;
        const distance = this.getDistanceBetweenMatchedTemplateAndMarkerCandidate(
          matchedResult,
          markerCandidate
        );
        if (distance < closestDistance) {
          closestDistance = distance;
          closestMarker = markerCandidate;
          console.error(
            "new closest " +
              closestDistance +
              " (" +
              Math.round(markerCandidate.leftX) +
              "," +
              Math.round(markerCandidate.topY) +
              ")"
          );
        }
      });
      if (
        closestDistance < config.maxDistanceBetweenTemplateMatchedAndCandidate
      ) {
        templateDetected = true;
      }
      angle += config.rotationStep;
      rotatedMarker.delete();
      matchedDst.delete();
      console.error(
        "Distance " +
          closestDistance +
          " is not close enough, try with a different angle"
      );
    }
    const result = [];
    result[0] = matchedPoint;
    const matchedPointEnd = new cv.Point(
      matchedPoint.x + marker.cols,
      matchedPoint.y + marker.rows
    );
    result[1] = matchedPointEnd;
    const matchedMarker = new DetectedShape(
      0,
      0,
      matchedPoint.x,
      matchedPoint.y,
      100,
      100,
      JSON.parse(JSON.stringify(closestMarker.vertices))
    );
    matchedMarker.leftX = matchedPoint.x;
    matchedMarker.topY = matchedPoint.y;
    // @ts-ignore
    matchedMarker.vertices[0].x = matchedPoint.x;
    // @ts-ignore
    matchedMarker.vertices[0].y = matchedPoint.y;
    // @ts-ignore
    matchedMarker.vertices[1].x = matchedPointEnd.x;
    // @ts-ignore
    matchedMarker.vertices[1].y = matchedPoint.y;
    // @ts-ignore
    matchedMarker.vertices[2].x = matchedPointEnd.x;
    // @ts-ignore
    matchedMarker.vertices[2].y = matchedPointEnd.y;
    // @ts-ignore
    matchedMarker.vertices[3].x = matchedPoint.x;
    // @ts-ignore
    matchedMarker.vertices[3].y = matchedPointEnd.y;
    console.error(
      "MATCHED : " +
        Math.round(matchedPoint.x) +
        "," +
        Math.round(matchedPoint.y) +
        ""
    );

    marker.delete();
    return matchedMarker;
  }

  private getDistanceBetweenMatchedTemplateAndMarkerCandidate(
    templateMatchedPoint: any[],
    markerCandidate: DetectedShape
  ): number {
    const matchedPoint = templateMatchedPoint[0];
    // const matchedPointEnd = templateMatchedPoint[1];
    // If detected marker is close to marker candidate shape, then this is the marker
    // const MAX_ALLOWED_MARKER_POSITION_DIFF = markerCandidate.width / 10;
    const diffX = Math.abs(matchedPoint.x - markerCandidate.leftX);
    const diffY = Math.abs(matchedPoint.y - markerCandidate.topY);
    /*const diffWidth =
      Math.max(
        matchedPointEnd.x - matchedPoint.x,
        matchedPointEnd.y - matchedPoint.y
      ) - markerCandidate.width;*/
    return diffX + diffY;
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
    original.delete();
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
          function (_e) {
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
