# Measurements of objects (area, shape, intensity) as a pandas DataFrame, the
# distributions in the plot view and the table in R.
#
# Open a thresholdable image in ImageJ (e.g., File > Open Samples > Blobs).
# Without an open image a sample is used. Start R (toolbar "Start R") to get
# the measurements as data frame "objects" in R.
#
# Requires: numpy, pandas, scikit-image, matplotlib

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy import ndimage as ndi
from skimage import filters, measure, segmentation
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
mask = ndi.binary_fill_holes(mask)
# separate touching objects a little, no objects at the border (as 'exclude on edges' of ImageJ)
mask = segmentation.clear_border(ndi.binary_opening(mask, iterations=2))
labels = measure.label(mask)

props = measure.regionprops_table(labels, intensity_image=gray,
                                  properties=["label", "area", "perimeter", "eccentricity", "solidity",
                                              "major_axis_length", "minor_axis_length", "mean_intensity"])
objects = pd.DataFrame(props)
objects = objects[objects["area"] >= 20]   # no tiny objects (noise)
objects["circularity"] = 4 * np.pi * objects["area"] / objects["perimeter"].clip(lower=1) ** 2
print(objects.describe().round(2))

fig, axes = plt.subplots(1, 2, figsize=(10, 4), constrained_layout=True)
axes[0].hist(objects["area"], bins=20, color="#4C72B0", alpha=0.85)
axes[0].set(title="Area", xlabel="Area (pixels)", ylabel="Count")
sc = axes[1].scatter(objects["area"], objects["circularity"], c=objects["mean_intensity"], cmap="viridis")
fig.colorbar(sc, ax=axes[1], label="Mean intensity")
axes[1].set(title="Shape", xlabel="Area (pixels)", ylabel="Circularity")
plt.show()

try:
    bio7.to_r("objects", objects)
    print("The measurements are in R: objects (a data frame), e.g. summary(objects)")
except RuntimeError as e:
    print("Not transferred to R:", e)
