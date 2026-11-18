# Deep learning segmentation of an ImageJ image in the embedded Python of Bio7.
#
# 1. Open an image with nuclei, cells or blobs in ImageJ, e.g.,
#    File > Open Samples > Blobs (or a fluorescence image of nuclei).
# 2. Run this script (Python editor: "Interpret Python").
#
# The current ImageJ image is transferred to Python as numpy array, segmented
# with a pretrained deep learning model (StarDist, or Cellpose if StarDist is
# not installed) and the results are displayed in ImageJ:
#   - "Labels"   a 16-bit label image (each object its own number)
#   - "Objects"  the objects in random colors (RGB)
#   - "Outlines" the image with the outlines of the objects (RGB)
# The plot view shows the image, the objects and the distribution of the sizes.
#
# Requires one of the deep learning packages for the Python of Bio7 (see the
# Python preferences of Bio7):
#   pip install stardist      (StarDist with TensorFlow: nuclei and blobs)
#   pip install cellpose      (Cellpose with PyTorch: cells and nuclei)
# and numpy, matplotlib. The pretrained model is downloaded at the first run.
# Note: a new package in an already running Python needs a restart of Bio7.

import os
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")  # less TensorFlow messages

import numpy as np
import matplotlib.pyplot as plt
import bio7

# The objects are darker than the background (e.g., cells in brightfield)?
# None: detected automatically.
DARK_OBJECTS = None

# ---------------------------------------------------------------- the image

img = bio7.image()
print("ImageJ image:", img.dtype, img.shape)
image = img.astype("float32")
if image.ndim == 3 and image.shape[-1] == 3:  # RGB: the luminance
    image = image @ np.array([0.299, 0.587, 0.114], dtype="float32")
elif image.ndim == 3:  # a stack: the first slice
    image = image[0]

# The models expect bright objects on a dark background
dark = DARK_OBJECTS
if dark is None:
    # The objects are the minority of the pixels: dark objects if the median
    # (the background) is brighter than the mean
    dark = np.median(image) > image.mean()
if dark:
    image = image.max() - image

# Normalization to the percentiles 1 and 99.8 (as in StarDist and Cellpose)
low, high = np.percentile(image, (1, 99.8))
norm = np.clip((image - low) / max(high - low, 1e-6), 0, 1).astype("float32")

# ---------------------------------------------------------------- the segmentation


def segment_stardist(x):
    from stardist.models import StarDist2D
    model = StarDist2D.from_pretrained("2D_versatile_fluo")
    labels, _ = model.predict_instances(x, show_tile_progress=False)
    return labels, "StarDist (2D_versatile_fluo)"


def segment_cellpose(x):
    from cellpose import models
    if hasattr(models, "Cellpose"):  # Cellpose 1 to 3
        model = models.Cellpose(gpu=False, model_type="nuclei")
        result = model.eval(x, diameter=None, channels=[0, 0])
    else:  # Cellpose 4
        model = models.CellposeModel(gpu=False)
        result = model.eval(x)
    return result[0], "Cellpose"


labels = None
for segment in (segment_stardist, segment_cellpose):
    try:
        labels, method = segment(norm)
        break
    except ImportError as e:
        print("Not available:", e)
if labels is None:
    raise RuntimeError("Please install StarDist (pip install stardist) or Cellpose (pip install cellpose) "
                       "for the Python of Bio7 and restart Bio7.")

labels = labels.astype("int32")
count = int(labels.max())
print("%s: %d objects" % (method, count))

# ---------------------------------------------------------------- the results in ImageJ

# The label image (16-bit: up to 65535 objects)
bio7.show_image(labels.astype("uint16"), "Labels (%s)" % method.split()[0])

# The objects in random colors (the background black)
rng = np.random.default_rng(3)
palette = rng.integers(60, 256, size=(count + 1, 3), dtype=np.uint8)
palette[0] = 0
bio7.show_image(palette[labels], "Objects (Python)")

# The outlines of the objects on the image
edge = np.zeros(labels.shape, dtype=bool)
edge[:-1, :] |= labels[:-1, :] != labels[1:, :]
edge[1:, :] |= labels[1:, :] != labels[:-1, :]
edge[:, :-1] |= labels[:, :-1] != labels[:, 1:]
edge[:, 1:] |= labels[:, 1:] != labels[:, :-1]
edge &= labels > 0
gray = (norm * 255).astype(np.uint8)
overlay = np.stack([gray, gray, gray], axis=-1)
overlay[edge] = (255, 60, 60)
bio7.show_image(overlay, "Outlines (Python)")

# ---------------------------------------------------------------- measurements and plots

counts = np.bincount(labels.ravel())
ids = np.nonzero(counts)[0]
ids = ids[ids > 0]  # the object numbers (without the background 0)
areas = counts[ids]
if areas.size:
    print("Area (pixels): mean %.1f, median %.1f, min %d, max %d" % (areas.mean(), np.median(areas), areas.min(), areas.max()))

fig, axes = plt.subplots(1, 3, figsize=(12, 4), constrained_layout=True)
axes[0].imshow(norm, cmap="gray")
axes[0].set_title("Image")
axes[1].imshow(palette[labels])
axes[1].set_title("%d objects (%s)" % (count, method.split()[0]))
for ax in axes[:2]:
    ax.set_axis_off()
axes[2].hist(areas, bins=25, color="#4C72B0", alpha=0.85)
axes[2].set(title="Object sizes", xlabel="Area (pixels)", ylabel="Count")
axes[2].spines[["top", "right"]].set_visible(False)
plt.show()

# The sizes to R as data frame (if R runs)
try:
    import pandas as pd
    sizes = pd.DataFrame({"label": ids, "area": areas})
except ImportError:
    sizes = {"label": ids, "area": areas}  # a named list in R
try:
    bio7.to_r("objectAreas", sizes)
    print("The areas are in R: objectAreas")
except RuntimeError:
    pass
