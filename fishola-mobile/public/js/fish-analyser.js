
      function onOpenCvReady() {
        document.getElementById('status').innerHTML = 'OpenCV.js is ready.';
      }
      
      let imgElement = document.getElementById('imageSrc');
      let inputElement = document.getElementById('fileInput');
      inputElement.addEventListener('change', (e) => {
        imgElement.src = URL.createObjectURL(e.target.files[0]);
      }, false);
      imgElement.onload = function() {
        // See https://github.com/ucisysarch/opencvjs/blob/master/test/img_proc.html
        // And https://www.pyimagesearch.com/2016/03/28/measuring-size-of-objects-in-an-image-with-opencv/
        // Step 1: load the image, convert it to grayscale, and blur it slightly
        let src = cv.imread(imgElement);
        let refined1 = new cv.Mat();
        let refined2 = new cv.Mat();
        cv.cvtColor(src, refined1, cv.COLOR_BGR2GRAY, 0);
        let blurSize = new cv.Size(7, 7);
        cv.GaussianBlur(refined1, refined2, blurSize, 0, 0, cv.BORDER_DEFAULT);
        // Step 2: perform edge detection, then perform a dilation + erosion to
        // close gaps in between object edges
        // Step 2.1 : edge detection
        cv.Canny(refined2, refined1, 50, 100, 3, false);
        // Step 2.2 : dilation
        const kernel = cv.getStructuringElement(cv.MORPH_RECT,new cv.Size(5,5));
        cv.dilate(refined1, refined2, kernel, new cv.Point(-1, 1), 1);
        // Step 2.3 : erosion
       /* var borderValue = cv.Scalar.all(Number.MAX_VALUE);
        var erosion_type = cv.MorphShapes.MORPH_RECT.value;
        var erosion_size = [2*Control.erosion_size+1, 2*Control.erosion_size+1];
        var element = cv.getStructuringElement(erosion_type, erosion_size, [-1, -1]);
        cv.erode(refined2, refined1, element, [-1, -1], 1, cv.BORDER_CONSTANT, borderValue);*/
       
        // Step 3: find coutours in the edge map
        // Step 3.1 : find contours
        cv.threshold(refined2, refined1, 120, 200, cv.THRESH_BINARY);
        let dst1 = cv.Mat.zeros(refined1.cols, refined1.rows, cv.CV_8UC3);
        let dst2 = cv.Mat.zeros(refined1.cols, refined1.rows, cv.CV_8UC3);
        let contours = new cv.MatVector();
        let hierarchy = new cv.Mat();
        let poly = new cv.MatVector();
        cv.findContours(refined1, contours, hierarchy, cv.RETR_CCOMP, cv.CHAIN_APPROX_SIMPLE);

        // Step 3.2 : approximates each contour to polygon
        for (let i = 0; i < contours.size(); ++i) {
            let tmp = new cv.Mat();
            let cnt = contours.get(i);
            cv.approxPolyDP(cnt, tmp, 3, true);
            poly.push_back(tmp);
            cnt.delete(); tmp.delete();
        }

        // Step 4: bounding boxes
        // Bouding rects
        for (let i = 0; i < poly.size(); ++i) {
            let color = new cv.Scalar(Math.round(Math.random() * 255), Math.round(Math.random() * 255),
                                    Math.round(Math.random() * 255));
            let cnt = poly.get(i);
            let rect = cv.boundingRect(cnt);
            let point1 = new cv.Point(rect.x, rect.y);
            let point2 = new cv.Point(rect.x + rect.width, rect.y + rect.height);
            cv.rectangle(dst1, point1, point2, color, 2, cv.LINE_AA, 0);
            //cv.drawContours(dst2, poly, i, color, 1, 8, hierarchy, 0);
        }

        // Min area rects
        for (let i = 0; i < poly.size(); ++i) {
            let color = new cv.Scalar(Math.round(Math.random() * 255), Math.round(Math.random() * 255),
                                    Math.round(Math.random() * 255));
            let cnt = poly.get(i);
            let rotatedRect = cv.minAreaRect(cnt);
            let vertices = cv.RotatedRect.points(rotatedRect);
            for (let i = 0; i < 4; i++) {
                cv.line(dst2, vertices[i], vertices[(i + 1) % 4], color, 2, cv.LINE_AA, 0);
            }
        }

        cv.imshow('canvasOutput1', refined1);
        cv.imshow('canvasOutput2', dst1);
        cv.imshow('canvasOutput3', dst2);
        // find contours in the edge map
        //cnts = cv.findContours(edged.copy(), cv.RETR_EXTERNAL,
        //  cv.CHAIN_APPROX_SIMPLE);
        //cnts = imutils.grab_contours(cnts);
        // sort the contours from left-to-right and initialize the
        // 'pixels per metric' calibration variable
        //(cnts, _) = contours.sort_contours(cnts);
        //pixelsPerMetric = None;
        src.delete();
        refined1.delete();
        refined2.delete();
        dst1.delete();
        dst2.delete();
      };