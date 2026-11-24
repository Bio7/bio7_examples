# Principal component analysis (PCA) of the channels of an image or the
# slices of a stack (scikit-learn).
#
# Open an RGB image or a stack in ImageJ (e.g., a multispectral stack). Without
# an open image a sample image is used. Each pixel is an observation, each
# channel (slice) a variable: the principal component images are shown as a
# 32-bit stack in ImageJ, the explained variance in the plot view.
#
# Requires: numpy, matplotlib, scikit-learn

import numpy as np
import matplotlib.pyplot as plt
from sklearn.decomposition import PCA
import bio7

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.astronaut()
    bio7.show_image(img, "Astronaut (sample)")

if img.ndim == 3 and img.shape[-1] == 3:      # RGB (h, w, 3): the channels
    channels = np.moveaxis(img, -1, 0)
elif img.ndim == 3:                            # a stack (slices, h, w)
    channels = img
else:
    raise RuntimeError("PCA needs an RGB image or a stack (several channels)")
n, h, w = channels.shape
X = channels.reshape(n, -1).T.astype("float64")
X = (X - X.mean(axis=0)) / (X.std(axis=0) + 1e-12)   # standardized variables

pca = PCA(n_components=n)
scores = pca.fit_transform(X)
components = scores.T.reshape(n, h, w).astype("float32")
bio7.show_image(components, "PCA components")

fig, axes = plt.subplots(1, 2, figsize=(10, 4), constrained_layout=True)
ratio = pca.explained_variance_ratio_ * 100
axes[0].bar(range(1, n + 1), ratio, color="#4C72B0")
axes[0].plot(range(1, n + 1), np.cumsum(ratio), "o-", color="#C44E52", label="cumulative")
axes[0].set(title="Explained variance", xlabel="Component", ylabel="%", xticks=range(1, n + 1))
axes[0].legend(frameon=False)
axes[1].imshow(components[0], cmap="magma")
axes[1].set_title("First principal component")
axes[1].set_axis_off()
plt.show()
print("Loadings (components x channels):\n", np.round(pca.components_, 3))
