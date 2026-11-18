# k-means clustering of the colors of an ImageJ image (scikit-learn).
#
# Open an RGB image in ImageJ (e.g., File > Open Samples > Leaf or Fluorescent
# Cells). Without an open image a sample image of scikit-image is used.
# The pixels are clustered by their color: the result is a label image
# (16-bit) and the image in the colors of the cluster centers (RGB).
#
# Requires: numpy, matplotlib, scikit-learn (scikit-image for the sample)

import numpy as np
import matplotlib.pyplot as plt
from sklearn.cluster import KMeans
import bio7

K = 4  # the number of clusters

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.astronaut()
    bio7.show_image(img, "Astronaut (sample)")

if img.ndim == 2:  # a grey image: the intensity is the only feature
    img = np.stack([img] * 3, axis=-1)
h, w = img.shape[:2]
pixels = img.reshape(-1, 3).astype("float64")

# A sample of the pixels for the fit (fast for large images), then all pixels
rng = np.random.default_rng(0)
sample = pixels[rng.choice(len(pixels), size=min(len(pixels), 20000), replace=False)]
km = KMeans(n_clusters=K, n_init=10, random_state=0).fit(sample)
labels = km.predict(pixels).reshape(h, w)
centers = np.clip(km.cluster_centers_, 0, 255).astype("uint8")

bio7.show_image((labels + 1).astype("uint16"), "k-means labels (k=%d)" % K)
bio7.show_image(centers[labels], "k-means colors (k=%d)" % K)

# The share of each cluster and its color
counts = np.bincount(labels.ravel(), minlength=K) / labels.size
fig, ax = plt.subplots(constrained_layout=True)
ax.bar(range(1, K + 1), counts * 100, color=centers / 255.0, edgecolor="black")
ax.set(title="k-means color clusters", xlabel="Cluster", ylabel="Pixels (%)", xticks=range(1, K + 1))
plt.show()
