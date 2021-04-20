
    function onOpenCvReady() {
    document.getElementById('status').innerHTML = 'OpenCV.js is ready.';
    }
    
    let imgElement = document.getElementById('imageSrc');
    let inputElement = document.getElementById('fileInput');
    inputElement.addEventListener('change', (e) => {
    imgElement.src = URL.createObjectURL(e.target.files[0]);
    }, false);
    imgElement.onload = function() {
        calculateSizes(imgElement, document.getElementById('minCoverrage').value, 
        document.getElementById('leftSizeObjectSizeMm').value, 
        parseInt(document.getElementById('fixedSize').value));
    };


    function calculateSizes(imgElement, minSizeRatio, leftSizeObjectSizeMm, fixedSize) {
        /*let empty = new cv.Mat();
        cv.imshow('canvasOutput1', empty);
        cv.imshow('canvasOutput2', empty);
        cv.imshow('canvasOutput3', empty);*/
        // See https://github.com/ucisysarch/opencvjs/blob/master/test/img_proc.html
        // And https://www.pyimagesearch.com/2016/03/28/measuring-size-of-objects-in-an-image-with-opencv/
        // Step 1: load the image & resize it
        let src = resize(imgElement, fixedSize);
        //  Step 2 : convert it to grayscale, and blur it slightly
        let imgSize = Math.max(src.cols, src.rows);
        let refined1 = new cv.Mat();
        let refined2 = new cv.Mat();
        cv.cvtColor(src, refined1, cv.COLOR_BGR2GRAY, 0);
        let blurSize = new cv.Size(7, 7);
        cv.GaussianBlur(refined1, refined2, blurSize, 0, 0, cv.BORDER_DEFAULT);
        // Step 3: perform edge detection, then perform a dilation + erosion to
        // close gaps in between object edges
        // Step 3.1 : edge detection
        cv.Canny(refined2, refined1, 50, 100, 3, false);
        // Step 3.2 : dilation
        const kernel = cv.getStructuringElement(cv.MORPH_RECT,new cv.Size(5,5));
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
        let dst = resize(imgElement, fixedSize);
        let contours = new cv.MatVector();
        let hierarchy = new cv.Mat();
        let poly = new cv.MatVector();
        cv.findContours(refined1, contours, hierarchy, cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE);

        // Step 4.2 : approximates each contour to polygon
        for (let i = 0; i < contours.size(); ++i) {
            let tmp = new cv.Mat();
            let cnt = contours.get(i);
            cv.approxPolyDP(cnt, tmp, 3, true);
            poly.push_back(tmp);
            cnt.delete(); tmp.delete();
        }

        // Step 5: bounding boxes (min area rects)
        // Only keep "big" bounding boxes
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
                let minX = 100000;
                for (let j = 0; j < 4; j++) {
                    cv.line(dst, vertices[j], vertices[(j + 1) % 4], color, 2, cv.LINE_AA, 0);
                    minX = Math.min(vertices[j].x, minX);
                }
                console.log(Math.round(rectangleSize) + " " + minX);
                results.push({'left_x': minX, 'center_x': rotatedRect.center.x, 'center_y': rotatedRect.center.y, 'size': rectangleSize});
            }
        }
        // Step 6: sort from left to right
        let sorted = results.sort((a,b) =>  a.left_x - b.left_x);
        console.log(sorted);

        // Step 7: compute sizes
        let ratio = -1;
        let html1 =  "<table style='margin-left:50px;border:1px solid black'><tr><td style='border:1px solid black'>Size in pixels</td>";
        let html2 =  "<tr><td style='border:1px solid black'>Size in mm </td>";
        for (let i = 0; i < sorted.length; ++i) {
            let item = sorted[i];
            html1 += "<td style='border:1px solid black'>" + Math.round(item.size) + "px</td>";
            if (ratio == -1) {
                ratio = (leftSizeObjectSizeMm / item.size); 
            } 
            let calculatedSize = Math.round(item.size * ratio);
            html2 += "<td style='border:1px solid black'>" + calculatedSize + "mm </td>";
            cv.putText(dst, calculatedSize + "mm", {x: (item.left_x + item.center_x) /2, y: item.center_y},  cv.FONT_HERSHEY_SIMPLEX, 0.8, [0, 0, 0, 255], 3);  
            cv.putText(dst, calculatedSize + "mm", {x: (item.left_x + item.center_x) /2, y: item.center_y},  cv.FONT_HERSHEY_SIMPLEX, 0.8, [255, 255, 255, 255], 1);            
        };
        html = html1 + '</tr>' + html2 + '</tr></table>';
        document.getElementById('status').innerHTML = html;
        cv.imshow('canvasOutput1', src);origin
        cv.imshow('canvasOutput2', refined1);
        cv.imshow('canvasOutput3', dst);

        src.delete();
        refined1.delete();
        refined2.delete();
        dst.delete();
    }

    function resize(imgElement, fixedSize) {
        let original = cv.imread(imgElement);
        let src = new cv.Mat();
        // Vertical images
        if (original.cols < original.rows) {
            // Fix with
            let reducedWidth = fixedSize;
            let ratioWith = (reducedWidth/ original.cols);
            let dsize = new cv.Size(reducedWidth, ratioWith * original.rows);
            cv.resize(original, src, dsize, 0, 0, cv.INTER_AREA);
        } else {
            // Fix height
            let reduceHeight = fixedSize;
            let ratioHeight = (reduceHeight/ original.rows);
            let dsize = new cv.Size(ratioHeight * original.cols, reduceHeight);
            cv.resize(original, src, dsize, 0, 0, cv.INTER_AREA);
        }
        return src;
    }

