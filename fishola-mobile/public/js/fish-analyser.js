
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
        let output1 = new cv.Mat();
        let output2 = new cv.Mat();
        cv.cvtColor(src, output1, cv.COLOR_BGR2GRAY, 0);
        let blurSize = new cv.Size(7, 7);
        cv.GaussianBlur(output1, output2, blurSize, 0, 0, cv.BORDER_DEFAULT);
        // Step 2: perform edge detection, then perform a dilation + erosion to
        // close gaps in between object edges
        // Step 2.1 : edge detection
        cv.Canny(output2, output1, 50, 100, 3, false);
        // Step 2.2 : dilation
        const kernel = cv.getStructuringElement(cv.MORPH_RECT,new cv.Size(5,5));
        cv.dilate(output1, output2, kernel, new cv.Point(-1, 1), 1);
        // Step 2.3 : erosion
       /* var borderValue = cv.Scalar.all(Number.MAX_VALUE);
        var erosion_type = cv.MorphShapes.MORPH_RECT.value;
        var erosion_size = [2*Control.erosion_size+1, 2*Control.erosion_size+1];
        var element = cv.getStructuringElement(erosion_type, erosion_size, [-1, -1]);
        cv.erode(output2, output1, element, [-1, -1], 1, cv.BORDER_CONSTANT, borderValue);*/
       
        // Step 3: find coutours in the edge map
        cv.threshold(output2, output1, 120, 200, cv.THRESH_BINARY);
        let dst = cv.Mat.zeros(output1.cols, output1.rows, cv.CV_8UC3);
        let contours = new cv.MatVector();
        let hierarchy = new cv.Mat();
        // You can try more different parameters
        cv.findContours(output1, contours, hierarchy, cv.RETR_CCOMP, cv.CHAIN_APPROX_SIMPLE);
        // draw contours with random Scalar
        for (let i = 0; i < contours.size(); ++i) {
            let color = new cv.Scalar(Math.round(Math.random() * 255), Math.round(Math.random() * 255),
                                    Math.round(Math.random() * 255));
            cv.drawContours(dst, contours, i, color, 1, cv.LINE_8, hierarchy, 100);
        }
        cv.imshow('canvasOutput1', src);
        cv.imshow('canvasOutput2', output1);
        cv.imshow('canvasOutput3', dst);
        // find contours in the edge map
        //cnts = cv.findContours(edged.copy(), cv.RETR_EXTERNAL,
        //  cv.CHAIN_APPROX_SIMPLE);
        //cnts = imutils.grab_contours(cnts);
        // sort the contours from left-to-right and initialize the
        // 'pixels per metric' calibration variable
        //(cnts, _) = contours.sort_contours(cnts);
        //pixelsPerMetric = None;
        src.delete();
        dst.delete();
      };