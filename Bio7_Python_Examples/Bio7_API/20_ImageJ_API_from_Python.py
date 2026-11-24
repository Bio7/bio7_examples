# The ImageJ API (Java) from Python with bio7.java: images, processors,
# thresholds, the particle analyzer and the results table called directly,
# the pixels exchanged as numpy arrays.
#
#   from bio7 import java
#   IJ = java.ij.IJ                    # a class (also java.type("ij.IJ"))
#   imp = IJ.getImage()                # static methods; Java objects are proxies
#   ip = imp.getProcessor()            # methods of objects
#   px = ip.getPixels()                # Java arrays as numpy arrays (byte/short as uint8/uint16)
#   roi = java.ij.gui.OvalRoi(10, 10, 50, 50)  # constructors: call the class
#
# Open an image in ImageJ (e.g., File > Open Samples > Blobs). Without an open
# image the sample is loaded (internet) or a test image is created.
#
# Requires: numpy

import numpy as np
from bio7 import java

IJ = java.ij.IJ
ImagePlus = java.ij.ImagePlus
WindowManager = java.ij.WindowManager

# The current image of ImageJ or a sample
imp = WindowManager.getCurrentImage()
if imp is None:
    imp = IJ.openImage("https://imagej.net/images/blobs.gif")
    if imp is None:
        # Offline: blobs with numpy
        yy, xx = np.mgrid[0:200, 0:240]
        rng = np.random.default_rng(5)
        img = np.zeros((200, 240))
        for _ in range(30):
            cy, cx, r = rng.uniform(15, 185), rng.uniform(15, 225), rng.uniform(5, 11)
            img += 200 * np.exp(-((yy - cy) ** 2 + (xx - cx) ** 2) / (2 * r ** 2))
        bp = java.ij.process.ByteProcessor(240, 200, np.clip(img + 20, 0, 255).astype(np.uint8).ravel())
        imp = ImagePlus("Blobs (test)", bp)
    imp.show()
print("Image:", imp.getTitle(), imp.getWidth(), "x", imp.getHeight(), "bit depth", imp.getBitDepth())

# A copy of the processor (8-bit), smoothed with ImageJ
ip = imp.getProcessor().convertToByte(True).duplicate()
ip.blurGaussian(1.5)
w, h = ip.getWidth(), ip.getHeight()

# The threshold of ImageJ (Otsu) from the histogram
histogram = ip.getHistogram()                       # int[] -> numpy
otsu = java.ij.process.AutoThresholder().getThreshold("Otsu", histogram)
pixels = ip.getPixels().reshape(h, w)               # byte[] -> uint8 (height, width)
# The objects are the brighter or the darker part (the smaller one)
mask = pixels > otsu
if mask.mean() > 0.5:
    mask = ~mask
print("Otsu threshold:", otsu, "| object pixels: %.1f %%" % (100 * mask.mean()))

# A new ImageJ image from numpy (ByteProcessor(width, height, byte[]))
mask_ip = java.ij.process.ByteProcessor(w, h, (mask * 255).astype(np.uint8).ravel())
mask_ip.setThreshold(255, 255, java.ij.process.ImageProcessor.NO_LUT_UPDATE)
mask_imp = ImagePlus("Mask (Python)", mask_ip)
mask_imp.show()

# The particle analyzer of ImageJ: constants are static fields
PA = java.ij.plugin.filter.ParticleAnalyzer
M = java.ij.measure.Measurements
rt = java.ij.measure.ResultsTable()
pa = PA(PA.SHOW_NONE | PA.EXCLUDE_EDGE_PARTICLES, M.AREA | M.MEAN | M.CENTROID | M.SHAPE_DESCRIPTORS,
        rt, 20.0, float("inf"))
pa.analyze(mask_imp, mask_ip)
print("Particles:", rt.size())

# The columns of the results table as numpy arrays
area = rt.getColumn("Area")
circ = rt.getColumn("Circ.")
print("Area: mean %.1f, max %.1f | circularity: mean %.3f" % (area.mean(), area.max(), circ.mean()))

# Values back to ImageJ: a new column computed with numpy, the table shown
for i, a in enumerate(area):
    rt.setValue("Area/mean", i, float(a / area.mean()))
java.ui(rt).show("Particles (Python)")

# A float image from a 2-d numpy array: the distance of each pixel to the image center
yy, xx = np.mgrid[0:h, 0:w]
distance = np.hypot(yy - h / 2, xx - w / 2).astype(np.float32)
# FloatProcessor(float[][]) takes [x][y]: the transposed array
fp = java.ij.process.FloatProcessor(distance.T.copy())
ImagePlus("Distance (Python)", fp).show()
