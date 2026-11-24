# Statistics of the objects of an ImageJ image with seaborn: the objects are
# measured (area, shape, intensity), classified by size and shown as pair plot,
# distributions and correlations in the plot view.
#
# Open a thresholdable image in ImageJ (e.g., File > Open Samples > Blobs).
# Without an open image a sample is used.
#
# Requires: numpy, pandas, scipy, scikit-image, matplotlib, seaborn

import bio7
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from scipy import ndimage as ndi
from skimage import filters, measure, segmentation

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
    labels, intensity_image=gray,
    properties=["label", "area", "perimeter", "eccentricity", "solidity", "mean_intensity"]))
objects = objects[objects["area"] >= 20]
objects["circularity"] = (4 * np.pi * objects["area"] / objects["perimeter"].clip(lower=1) ** 2).clip(upper=1)
objects["size"] = pd.qcut(objects["area"], 3, labels=["small", "medium", "large"])
print(objects.groupby("size", observed=True)[["area", "circularity", "mean_intensity"]].mean().round(2))

sns.set_theme(style="whitegrid")

# The relations of the measurements by size class (a figure of its own)
sns.pairplot(objects, hue="size", vars=["area", "circularity", "eccentricity", "mean_intensity"],
             height=2.0, plot_kws={"s": 20})
plt.show()

# Distributions and correlations
fig, axes = plt.subplots(1, 3, figsize=(14, 4), constrained_layout=True)
sns.histplot(objects, x="area", hue="size", multiple="stack", bins=20, ax=axes[0])
axes[0].set_title("Area")
sns.boxplot(objects, x="size", y="mean_intensity", color="#9ecae1", ax=axes[1])
sns.stripplot(objects, x="size", y="mean_intensity", color="black", size=3, ax=axes[1])
axes[1].set_title("Mean intensity")
corr = objects[["area", "perimeter", "circularity", "eccentricity", "solidity", "mean_intensity"]].corr()
sns.heatmap(corr, annot=True, fmt=".2f", cmap="vlag", vmin=-1, vmax=1, ax=axes[2])
axes[2].set_title("Correlations")
plt.show()
