# Clustering of objects by their shape and intensity: measurements of each
# object, PCA (2 components) and k-means (scikit-learn); the objects in the
# colors of their cluster in ImageJ.
#
# Open a thresholdable image with many objects in ImageJ (e.g., File > Open
# Samples > Blobs). Without an open image a sample is used.
#
# Requires: numpy, pandas, scipy, scikit-image, scikit-learn, matplotlib

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy import ndimage as ndi
from skimage import filters, measure, segmentation
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler
import bio7

K = 3
COLORS = np.array([[76, 114, 176], [221, 132, 82], [85, 168, 104], [196, 78, 82], [129, 114, 179]], dtype=np.uint8)

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
labels = measure.label(segmentation.clear_border(ndi.binary_opening(mask, iterations=2)))

features = ["area", "perimeter", "eccentricity", "solidity", "extent", "mean_intensity"]
df = pd.DataFrame(measure.regionprops_table(labels, intensity_image=gray, properties=["label"] + features))
df = df[df["area"] >= 20].reset_index(drop=True)
if len(df) < K:
    raise RuntimeError("Too few objects for %d clusters" % K)

X = StandardScaler().fit_transform(df[features])
scores = PCA(n_components=2).fit_transform(X)
df["cluster"] = KMeans(n_clusters=K, n_init=10, random_state=0).fit_predict(X)
print(df.groupby("cluster")[features].mean().round(2))

# The objects in the colors of their cluster
lut = np.zeros((labels.max() + 1, 3), dtype=np.uint8)
lut[df["label"].to_numpy()] = COLORS[df["cluster"].to_numpy() % len(COLORS)]
bio7.show_image(lut[labels], "Object clusters (k=%d)" % K)

fig, ax = plt.subplots(constrained_layout=True)
for k in range(K):
    sel = df["cluster"] == k
    ax.scatter(scores[sel, 0], scores[sel, 1], color=COLORS[k] / 255.0, label="Cluster %d (%d)" % (k + 1, sel.sum()), alpha=0.8)
ax.set(title="Objects in the PCA space", xlabel="PC 1", ylabel="PC 2")
ax.legend(frameon=False)
plt.show()
