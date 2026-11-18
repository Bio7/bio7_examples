# OpenCV filters on the current ImageJ image, the results as a stack in ImageJ.
#
# Open an image in ImageJ (e.g., File > Open Samples > Blobs or a micrograph),
# else an image file is selected in the file dialog of Bio7 (canceled: a
# synthetic image of cells). OpenCV works with numpy arrays: the
# images of ImageJ (bio7.image()) are numpy arrays, the results are shown with
# bio7.show_image(). Note: OpenCV orders the colors blue, green, red (BGR).
#
# Requires: opencv-python-headless (Python Packages: the bundle "OpenCV")

import sys

import cv2
import numpy as np

import bio7

sys.modules.pop("cv_samples", None)   # the helper of this folder, imported again after changes
import cv_samples  # noqa: E402

img = cv_samples.image()
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
print("OpenCV", cv2.__version__, "- image", gray.shape[1], "x", gray.shape[0])

filters = {
    "Original": gray,
    "Gaussian blur (sigma 2)": cv2.GaussianBlur(gray, (0, 0), 2),
    "Median (5 x 5)": cv2.medianBlur(gray, 5),
    "Bilateral (edge preserving)": cv2.bilateralFilter(gray, 9, 50, 7),
    "CLAHE (local contrast)": cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8)).apply(gray),
    "Background removed (top hat)": cv2.morphologyEx(gray, cv2.MORPH_TOPHAT, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (41, 41))),
    "Morphological gradient": cv2.morphologyEx(gray, cv2.MORPH_GRADIENT, np.ones((3, 3), np.uint8)),
    "Canny edges": cv2.Canny(cv2.GaussianBlur(gray, (0, 0), 1.5), 30, 90),
}

# One stack: the slices in the order of the dict (the names as slice labels)
bio7.show_image(np.stack(list(filters.values())), "OpenCV filters")
imp = bio7.java.ij.IJ.getImage()
for i, name in enumerate(filters, start=1):
    imp.getStack().setSliceLabel(name, i)
imp.updateAndDraw()
for i, name in enumerate(filters, start=1):
    print("  slice %d: %s" % (i, name))

# The color image: the edges drawn in red on the original
edges = filters["Canny edges"] > 0
overlay = img.copy()
overlay[edges] = (0, 0, 255)                     # BGR: red
bio7.show_image(cv_samples.to_rgb(overlay), "Edges on the image")
