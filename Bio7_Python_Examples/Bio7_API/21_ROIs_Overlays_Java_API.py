# Selections (ROIs) and overlays of ImageJ from Python with bio7.java: the
# objects of a thresholded image as ROIs, measured with ImageJ, the largest ones
# outlined in an overlay, a polygon ROI from numpy coordinates. Also the Java
# collections and the Java standard library from Python.
#
# Open an image in ImageJ (e.g., File > Open Samples > Blobs). Without an open
# image a test image is created.
#
# Requires: numpy

import numpy as np
from bio7 import java

IJ = java.ij.IJ
imp = java.ij.WindowManager.getCurrentImage()
if imp is None:
    yy, xx = np.mgrid[0:220, 0:260]
    rng = np.random.default_rng(11)
    img = np.zeros((220, 260))
    for _ in range(25):
        cy, cx, r = rng.uniform(20, 200), rng.uniform(20, 240), rng.uniform(6, 14)
        img += 220 * np.exp(-((yy - cy) ** 2 + (xx - cx) ** 2) / (2 * r ** 2))
    bp = java.ij.process.ByteProcessor(260, 220, np.clip(img, 0, 255).astype(np.uint8).ravel())
    imp = java.ij.ImagePlus("Objects (test)", bp)
    imp.show()

# A thresholded copy: the selection of all objects (ThresholdToSelection)
ip = imp.getProcessor().convertToByte(True).duplicate()
ip.blurGaussian(1.0)
ip.setAutoThreshold("Otsu dark")
# The objects are the smaller part: else the darker pixels
px = ip.getPixels()
if ((px >= ip.getMinThreshold()) & (px <= ip.getMaxThreshold())).mean() > 0.5:
    ip.setAutoThreshold("Otsu")
work = java.ij.ImagePlus("work", ip)
selection = java.ij.plugin.filter.ThresholdToSelection.run(work)
if selection is None:
    raise SystemExit("No objects found")

# The single objects: a ShapeRoi splits into its parts (a Java array -> a list of proxies)
rois = selection.getRois() if java.is_java(selection) and selection.getClass().getSimpleName() == "ShapeRoi" else [selection]
print("Objects:", len(rois))

# Measured with ImageJ: the statistics of each ROI on the original image
M = java.ij.measure.Measurements
measures = []
for roi in rois:
    imp.setRoi(roi)
    # ImageStatistics: the measurements are public fields (the centroid must be requested)
    s = imp.getStatistics(M.AREA | M.MEAN | M.CENTROID)
    measures.append((s.area, s.mean, s.xCentroid, s.yCentroid))
imp.deleteRoi()
measures = np.array(measures)
order = np.argsort(measures[:, 0])[::-1]
print("Largest areas:", np.round(measures[order[:5], 0], 1))

# An overlay: the five largest objects in red, the others in yellow, with labels
Color = java.java.awt.Color
overlay = java.ij.gui.Overlay()
for rank, i in enumerate(order):
    roi = rois[int(i)]
    roi.setStrokeColor(Color.RED if rank < 5 else Color.YELLOW)
    roi.setStrokeWidth(2.0 if rank < 5 else 1.0)
    overlay.add(roi)
    if rank < 5:
        label = java.ij.gui.TextRoi(float(measures[i, 2]) + 6, float(measures[i, 3]) - 6, str(rank + 1))
        label.setStrokeColor(Color.RED)
        overlay.add(label)

# A polygon ROI from numpy: the convex hull of the centroids (gift wrapping)
pts = measures[:, 2:4]
hull = [int(np.argmin(pts[:, 0]))]
while True:
    p, q = hull[-1], (hull[-1] + 1) % len(pts)
    for r in range(len(pts)):
        cross = (pts[q, 0] - pts[p, 0]) * (pts[r, 1] - pts[p, 1]) - (pts[q, 1] - pts[p, 1]) * (pts[r, 0] - pts[p, 0])
        if cross < 0:
            q = r
    if q == hull[0] or len(hull) > len(pts):
        break
    hull.append(q)
xs, ys = pts[hull, 0].astype(np.float32), pts[hull, 1].astype(np.float32)
polygon = java.ij.gui.PolygonRoi(xs, ys, java.ij.gui.Roi.POLYGON)   # PolygonRoi(float[], float[], int)
polygon.setStrokeColor(Color.CYAN)
overlay.add(polygon)
imp.setOverlay(overlay)
# A ROI is measured on an image: set it, measure, remove it again
imp.setRoi(polygon)
hull_area = imp.getStatistics().area
imp.deleteRoi()
print("Convex hull of the centroids: %d corners, area %.0f" % (len(hull), hull_area))

# Java collections from Python: iterate, len(), [] and the Java methods
areas = java.java.util.ArrayList()
for a in measures[:, 0]:
    areas.add(float(a))
java.java.util.Collections.sort(areas)
print("Java list: %d values, median %.1f, first %s" % (len(areas), areas[len(areas) // 2], list(areas)[:3]))

# The Java standard library
print("Java", java.java.lang.System.getProperty("java.version"), "| today:", java.java.time.LocalDate.now())
