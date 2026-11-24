# Texture features of tiles (the gray-level co-occurrence matrix, GLCM, of
# scikit-image): contrast, homogeneity, energy and correlation as maps.
#
# Open a grey image with textures in ImageJ (e.g., a micrograph). Without an
# open image a sample is used. The feature maps are shown as a 32-bit stack in
# ImageJ (one slice per feature, one pixel per tile).
#
# Requires: numpy, scikit-image, matplotlib

import numpy as np
import matplotlib.pyplot as plt
from skimage.feature import graycomatrix, graycoprops
import bio7

TILE = 16       # the size of a tile (pixels)
LEVELS = 32     # the grey levels of the GLCM

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.brick()
    bio7.show_image(img, "Brick (sample)")

gray = img.astype("float64")
if gray.ndim == 3 and gray.shape[-1] == 3:
    gray = gray @ np.array([0.299, 0.587, 0.114])
elif gray.ndim == 3:
    gray = gray[0]
q = np.floor((gray - gray.min()) / max(np.ptp(gray), 1e-12) * (LEVELS - 1)).astype("uint8")

features = ["contrast", "homogeneity", "energy", "correlation"]
rows, cols = q.shape[0] // TILE, q.shape[1] // TILE
maps = np.zeros((len(features), rows, cols), dtype="float32")
for r in range(rows):
    for c in range(cols):
        tile = q[r * TILE:(r + 1) * TILE, c * TILE:(c + 1) * TILE]
        glcm = graycomatrix(tile, distances=[1], angles=[0, np.pi / 4, np.pi / 2, 3 * np.pi / 4], levels=LEVELS, symmetric=True, normed=True)
        for k, f in enumerate(features):
            maps[k, r, c] = graycoprops(glcm, f).mean()

bio7.show_image(np.nan_to_num(maps), "GLCM features (tile %d)" % TILE)

fig, axes = plt.subplots(1, len(features) + 1, figsize=(16, 4), constrained_layout=True)
axes[0].imshow(gray, cmap="gray")
axes[0].set_title("Image")
for ax, f, m in zip(axes[1:], features, maps):
    im = ax.imshow(m, cmap="viridis")
    ax.set_title(f.capitalize())
    fig.colorbar(im, ax=ax, shrink=0.7)
for ax in axes:
    ax.set_axis_off()
plt.show()
