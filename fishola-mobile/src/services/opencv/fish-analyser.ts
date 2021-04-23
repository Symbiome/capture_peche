export default class FisholaOpenCVService {
  static INSTANCE = new FisholaOpenCVService();

  calculateSizes(
    cv: any,
    imgElement: HTMLElement,
    minSizeRatio: number,
    leftSizeObjectSizeMm: number,
    fixedSize: number
  ) {
    /*let empty = new cv.Mat();
        cv.imshow('canvasOutput1', empty);
        cv.imshow('canvasOutput2', empty);
        cv.imshow('canvasOutput3', empty);*/
    // See https://github.com/ucisysarch/opencvjs/blob/master/test/img_proc.html
    // And https://www.pyimagesearch.com/2016/03/28/measuring-size-of-objects-in-an-image-with-opencv/
    // Step 1: load the image & resize it
    const src = this.resize(cv, imgElement, fixedSize);
    //  Step 2 : convert it to grayscale, and blur it slightly
    const imgSize = Math.max(src.cols, src.rows);
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

    // Step 4: find coutours in the edge map
    // Step 4.1 : find contours
    cv.threshold(refined2, refined1, 120, 200, cv.THRESH_BINARY);
    const dst = this.resize(cv, imgElement, fixedSize);
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

    // Step 5: bounding boxes (min area rects)
    // Only keep "big" bounding boxes
    const results = [];
    for (let i = 0; i < poly.size(); ++i) {
      const color = new cv.Scalar(
        Math.round(Math.random() * 255),
        Math.round(Math.random() * 255),
        Math.round(Math.random() * 255)
      );
      const cnt = poly.get(i);
      const rotatedRect = cv.minAreaRect(cnt);
      const vertices = cv.RotatedRect.points(rotatedRect);
      const rectangleSize = Math.max(
        rotatedRect.size.width,
        rotatedRect.size.height
      );
      // If rectangle width or height is at least minSizeRatio% of full image size
      if (rectangleSize / imgSize >= minSizeRatio) {
        let minX = 100000;
        for (let j = 0; j < 4; j++) {
          cv.line(
            dst,
            vertices[j],
            vertices[(j + 1) % 4],
            color,
            2,
            cv.LINE_AA,
            0
          );
          minX = Math.min(vertices[j].x, minX);
        }
        console.log(Math.round(rectangleSize) + " " + minX);
        results.push({
          left_x: minX,
          center_x: rotatedRect.center.x,
          center_y: rotatedRect.center.y,
          size: rectangleSize,
        });
      }
    }
    // Step 6: sort from left to right
    const sorted = results.sort((a, b) => a.left_x - b.left_x);
    console.log(sorted);

    // Step 7: compute sizes
    let ratio = -1;
    let html1 =
      "<table style='margin-left:50px;border:1px solid black'><tr><td style='border:1px solid black'>Size in pixels</td>";
    let html2 = "<tr><td style='border:1px solid black'>Size in mm </td>";
    for (let i = 0; i < sorted.length; ++i) {
      const item = sorted[i];
      html1 +=
        "<td style='border:1px solid black'>" +
        Math.round(item.size) +
        "px</td>";
      if (ratio == -1) {
        ratio = leftSizeObjectSizeMm / item.size;
      }
      const calculatedSize = Math.round(item.size * ratio);
      html2 +=
        "<td style='border:1px solid black'>" + calculatedSize + "mm </td>";
      cv.putText(
        dst,
        calculatedSize + "mm",
        {
          x: (item.left_x + item.center_x) / 2,
          y: item.center_y,
        },
        cv.FONT_HERSHEY_SIMPLEX,
        0.8,
        [0, 0, 0, 255],
        3
      );
      cv.putText(
        dst,
        calculatedSize + "mm",
        {
          x: (item.left_x + item.center_x) / 2,
          y: item.center_y,
        },
        cv.FONT_HERSHEY_SIMPLEX,
        0.8,
        [255, 255, 255, 255],
        1
      );
    }
    const html = html1 + "</tr>" + html2 + "</tr></table>";
    const statutHTML = document.getElementById("status");
    if (statutHTML) {
      statutHTML.innerHTML = html;
    }
    cv.imshow("canvasOutput1", src);
    origin;
    cv.imshow("canvasOutput2", refined1);
    cv.imshow("canvasOutput3", dst);

    src.delete();
    refined1.delete();
    refined2.delete();
    dst.delete();
  }

  resize(cv: any, imgElement: HTMLElement, fixedSize: number) {
    const original = cv.imread(imgElement);
    const src = new cv.Mat();
    // Vertical images
    if (original.cols < original.rows) {
      // Fix with
      const reducedWidth = fixedSize;
      const ratioWith = reducedWidth / original.cols;
      const dsize = new cv.Size(reducedWidth, ratioWith * original.rows);
      cv.resize(original, src, dsize, 0, 0, cv.INTER_AREA);
    } else {
      // Fix height
      const reduceHeight = fixedSize;
      const ratioHeight = reduceHeight / original.rows;
      const dsize = new cv.Size(ratioHeight * original.cols, reduceHeight);
      cv.resize(original, src, dsize, 0, 0, cv.INTER_AREA);
    }
    return src;
  }
}
