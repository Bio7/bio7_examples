# Separating touching objects with the distance transform and the watershed
# (scikit-image, SciPy).
#
# Open a binary or a thresholdable image with touching round objects in ImageJ
# (e.g., File > Open Samples > Blobs). Without an open image a sample is used.
# The result: the label image (16-bit) and the objects in random colors.
#
# Requires: numpy, scipy, scikit-image, matplotlib

import numpy as np
import matplotlib.pyplot as plt
from scipy import ndimage as ndi
from skimage import filters, segmentation, feature, measure
import bio7

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.coins()
    bio7.show_image(img, "Coins (sample)")

gray = img.astype("float64")
if gray.ndim == 3 and gray.shape[-1] == 3:
    gray = gray.mean(axis=-1)
elif gray.ndim == 3:
    gray = gray[0]
gray = filters.gaussian(gray, sigma=1)

# The foreground (the brighter part; inverted if the objects are dark)
mask = gray > filters.threshold_otsu(gray)
if mask.mean() > 0.5:
    mask = ~mask
mask = ndi.binary_fill_holes(ndi.binary_opening(mask, iterations=2))

# One marker per object: the maxima of the distance to the background
distance = ndi.distance_transform_edt(mask)
coords = feature.peak_local_max(distance, min_distance=7, labels=measure.label(mask), exclude_border=False)
markers = np.zeros(distance.shape, dtype="int32")
markers[tuple(coords.T)] = np.arange(1, len(coords) + 1)
labels = segmentation.watershed(-distance, markers, mask=mask)
print("Objects: %d (before the watershed: %d)" % (labels.max(), measure.label(mask).max()))

bio7.show_image(labels.astype("uint16"), "Watershed labels")
rng = np.random.default_rng(1)
palette = rng.integers(60, 256, size=(labels.max() + 1, 3), dtype=np.uint8)
palette[0] = 0
bio7.show_image(palette[labels], "Watershed objects")

fig, axes = plt.subplots(1, 3, figsize=(12, 4), constrained_layout=True)
for ax, im, title, cmap in zip(axes, [mask, distance, palette[labels]], ["Mask", "Distance", "%d objects" % labels.max()], ["gray", "viridis", None]):
    ax.imshow(im, cmap=cmap)
    ax.set_title(title)
    ax.set_axis_off()
plt.show()
