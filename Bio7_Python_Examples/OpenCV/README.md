# OpenCV with the embedded Python

OpenCV (`cv2`) in the embedded Python of Bio7: the images of ImageJ are numpy
arrays (`bio7.image()`), OpenCV processes them, and the results go back to
ImageJ (`bio7.show_image()`). Videos are read and written with OpenCV, and are
shown in ImageJ as stacks or played frame by frame in one window
(`bio7.update_image()`).

Setup: Python Packages, the bundle "OpenCV" (`opencv-python-headless`: OpenCV
without its own windows, because the images are shown in ImageJ). Some examples
also need pandas and matplotlib (the bundle "Scientific").

Without an open ImageJ image the examples ask for an image file, and the video
examples ask for a video file, in the file dialogs of the Bio7 API
(`Bio7Dialog`, called from Python: `cv_samples.open_file()`, `save_file()`,
`open_files()`, `choose_folder()`). If the dialog is canceled, they use
synthetic data: an image of cells and a video of moving cells (`cv_samples.py`,
written to the temporary folder `bio7_opencv_examples`).

| Example | Shows |
|---|---|
| 01_Image_Filters.py | Gaussian, median and bilateral filters, CLAHE, top hat, gradient, Canny as a stack with slice labels |
| 02_Contours_Measurements.py | Otsu threshold, watershed of touching objects, contours, measurements (area, circularity, ellipse) as a DataFrame (and in R) |
| 03_Video_to_ImageJ_Stack.py | A video file as ImageJ stack with the frame rate and time stamps; long videos: SCALE, EVERY, START, MAX_FRAMES, GRAY, or a virtual stack (VIRTUAL: JPEG files, any length) |
| 04_Play_Video_in_ImageJ.py | A video or the camera played in one ImageJ window, optionally with an effect (EFFECT: edges, cartoon, gray), stopped from the Progress view |
| 05_Motion_Tracking.py | Background subtraction (MOG2), moving objects, tracks with speed, played live in ImageJ |
| 06_Optical_Flow.py | Dense optical flow (Farneback) as a color stack, corners followed with Lucas-Kanade |
| 07_Feature_Matching_Alignment.py | ORB features, matching, homography (RANSAC): a rotated, scaled, shifted recording aligned |
| 08_ImageJ_Stack_to_Video.py | The current ImageJ stack written as an MP4/AVI video with time stamps |
| 09_Template_Matching.py | Several shapes (templates) found in an image (multi-scale, non-maximum suppression) and selected in ImageJ: ROI Manager with one color per template, a combined selection |
| 10_Webcam_Recording.py | The camera recorded into an ImageJ stack (time stamps, frame rate); PREVIEW: live view in ImageJ, or off for the fastest recording |
| 11_Webcam_to_Virtual_Stack.py | The camera recorded to JPEG or TIFF files (written by several threads) and opened as ImageJ virtual stack: long recordings, also 4K (limited by the disk, not the memory) |

`template_matching.py` holds the functions of the template matching (test image, templates
from ImageJ selections, detection, selection in ImageJ). The Java GUI
`Bio7_Java_Examples/src/python/templatematching/TemplateMatching.java` calls them: run it, create the
test image or add templates (select a shape with the rectangle tool, enter a name, "Add selection"),
then "Detect and select in ImageJ".

`camera_capture.py` opens the camera (or a video file) and grabs frames for ImageJ. The Java GUI
`Bio7_Java_Examples/src/python/camera/CameraRecorder.java` uses it: it records the camera, runs a
Java macro (the method `javaMacro` in `CameraRecorder.java`, fast: mean, motion, bright areas; edit
it for your own measurements) or an ImageJ macro (returns `name=value` pairs) on each frame, and
shows the measurements in a results table and a plot, the frames as stack and live.

Notes:

* OpenCV orders the colors blue, green, red (BGR); `cv_samples.to_rgb()` and
  `cv_samples.to_bgr()` convert them for ImageJ.
* `bio7.update_image(array, title)` replaces the pixels of the open image with
  that title, keeping its window, zoom and contrast. It creates the image if it
  is not open, or if the size or type changed.
* The camera (`SOURCE = 0` in 04): on macOS Bio7 asks for access to the camera
  at its first use (System Settings > Privacy & Security > Camera).
* Large stacks: `bio7.show_image()` transfers stacks larger than 256 MB in parts (one Java array
  holds at most 2 GB), so a stack can be as large as the Java memory of Bio7 (Bio7.ini: -Xmx).
* `cv2.imshow()` doesn't work in the embedded Python (no own windows): use
  ImageJ.
