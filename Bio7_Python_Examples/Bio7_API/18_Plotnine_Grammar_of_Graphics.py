# The grammar of graphics (as ggplot2 in R) with plotnine: the objects of an
# ImageJ image in classes of size, the relation of area and intensity and a
# density per class, drawn in the plot view.
#
# Open a thresholdable image in ImageJ (e.g., File > Open Samples > Blobs).
# Without an open image a sample is used.
#
# Requires: numpy, pandas, scipy, scikit-image, plotnine

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy import ndimage as ndi
from skimage import filters, measure, segmentation
from plotnine import (ggplot, aes, geom_point, geom_smooth, geom_density, facet_wrap,
                      labs, theme_bw, scale_color_brewer, scale_fill_brewer)
import bio7

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.coins()
    bio7.show_image(img, "Coins (sample)")

gray = img if img.ndim == 2 else (img.mean(axis=-1) if img.shape[-1] == 3 else img[0])
smooth = filters.gaussian(gray.astype("float64"), sigma=1)
mask = smooth > filters.threshold_otsu(smooth)
if mask.mean() > 0.5:
    mask = ~mask
mask = segmentation.clear_border(ndi.binary_opening(ndi.binary_fill_holes(mask), iterations=2))
labels = measure.label(mask)
objects = pd.DataFrame(measure.regionprops_table(
    labels, intensity_image=gray, properties=["area", "perimeter", "mean_intensity", "eccentricity"]))
objects = objects[objects["area"] >= 20]
objects["circularity"] = (4 * np.pi * objects["area"] / objects["perimeter"].clip(lower=1) ** 2).clip(upper=1)
objects["size"] = pd.qcut(objects["area"], 3, labels=["small", "medium", "large"])

# A plot is drawn with draw() (a matplotlib figure in the plot view)
p1 = (ggplot(objects, aes("area", "mean_intensity", color="size"))
      + geom_point(size=2.5, alpha=0.8)
      + geom_smooth(aes(group=1), method="lm", color="black", se=True)
      + scale_color_brewer(type="qual", palette="Set1")
      + labs(x="Area (pixels)", y="Mean intensity", title="Area and intensity of the objects")
      + theme_bw())
p1.draw(show=True)

p2 = (ggplot(objects, aes("circularity", fill="size"))
      + geom_density(alpha=0.5)
      + facet_wrap("~size", ncol=3)
      + scale_fill_brewer(type="qual", palette="Set1")
      + labs(x="Circularity", y="Density", title="Circularity per size class")
      + theme_bw())
p2.draw(show=True)
print(objects.groupby("size", observed=True).size().to_string())
