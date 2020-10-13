
    function onOpenCvReady() {
    document.getElementById('status').innerHTML = 'OpenCV.js is ready.';
    }
    
    let imgElement = document.getElementById('imageSrc');
    let inputElement = document.getElementById('fileInput');
    inputElement.addEventListener('change', (e) => {
    imgElement.src = URL.createObjectURL(e.target.files[0]);
    }, false);
    imgElement.onload = function() {
        calculateSizes(imgElement, 0.15, 133);
    };


    function calculateSizes(imgElement, minSizeRatio, leftSizeObjectSizeMm) {
        // See https://github.com/ucisysarch/opencvjs/blob/master/test/img_proc.html
        // And https://www.pyimagesearch.com/2016/03/28/measuring-size-of-objects-in-an-image-with-opencv/
        // Step 1: load the image, convert it to grayscale, and blur it slightly
        let src = cv.imread(imgElement);
        let imgSize = Math.max(src.cols, src.rows);
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
       /* TODO
        var borderValue = cv.Scalar.all(Number.MAX_VALUE);
        var erosion_type = cv.MorphShapes.MORPH_RECT.value;
        var erosion_size = [2*Control.erosion_size+1, 2*Control.erosion_size+1];
        var element = cv.getStructuringElement(erosion_type, erosion_size, [-1, -1]);
        cv.erode(refined2, refined1, element, [-1, -1], 1, cv.BORDER_CONSTANT, borderValue);*/
       
        // Step 3: find coutours in the edge map
        // Step 3.1 : find contours
        cv.threshold(refined2, refined1, 120, 200, cv.THRESH_BINARY);
        let dst = cv.imread(imgElement);
        let contours = new cv.MatVector();
        let hierarchy = new cv.Mat();
        let poly = new cv.MatVector();
        cv.findContours(refined1, contours, hierarchy, cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE);

        // Step 3.2 : approximates each contour to polygon
        for (let i = 0; i < contours.size(); ++i) {
            let tmp = new cv.Mat();
            let cnt = contours.get(i);
            cv.approxPolyDP(cnt, tmp, 3, true);
            poly.push_back(tmp);
            cnt.delete(); tmp.delete();
        }

        // Step 4: bounding boxes (min area rects)
        // Only keep big rectancles
        // Sort from left to right
        let results = [];
        for (let i = 0; i < poly.size(); ++i) {
            let color = new cv.Scalar(Math.round(Math.random() * 255), Math.round(Math.random() * 255),
                                    Math.round(Math.random() * 255));
            let cnt = poly.get(i);
            let rotatedRect = cv.minAreaRect(cnt);
            let vertices = cv.RotatedRect.points(rotatedRect);
            let rectangleSize = Math.max(rotatedRect.size.width, rotatedRect.size.height);
            // If rectangle width or height is at least minSizeRatio% of full image size
            if (rectangleSize / imgSize >= minSizeRatio) {
                console.log(vertices);
                console.log("=> ", rectangleSize);
                for (let i = 0; i < 4; i++) {
                    cv.line(dst, vertices[i], vertices[(i + 1) % 4], color, 2, cv.LINE_AA, 0);
                }
                results.push({'x': vertices[0].x, 'y': vertices[0].y, 'size': rectangleSize});
            }
        }
        console.log(results);

        // Step 5: sort from left to right
        results = results.sort((a,b) =>  a.x - b.x);
        console.log(results.length, results.size, results);

        // Step 6: compute sizes
        let ratio = -1;
        let html1 =  "Size in pixels : ";
        let html2 =  "If leftmost object is " + leftSizeObjectSizeMm + "mm, then here are the sizes from left to right : ";
        for (let i = 0; i < results.length; ++i) {
            let rectangleSize = results[i].size;
            html1 += Math.round(rectangleSize) + "px,";
            if (ratio == -1) {
                ratio = (rectangleSize / leftSizeObjectSizeMm); 
            } else {
                html2 += Math.round(ratio * rectangleSize) + "mm,";
            }
        };


        document.getElementById('status').innerHTML = html1 + "<br/>" + html2;
        cv.imshow('canvasOutput1', src);
        cv.imshow('canvasOutput2', refined1);
        cv.imshow('canvasOutput3', dst);

        src.delete();
        refined1.delete();
        refined2.delete();
        dst.delete();
    }

