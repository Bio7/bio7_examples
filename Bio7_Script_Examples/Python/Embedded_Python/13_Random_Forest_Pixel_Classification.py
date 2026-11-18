# Pixel classification with a random forest (scikit-learn), as the trainable
# segmentation of ImageJ/ilastik: features of each pixel (smoothing, edges,
# Laplacian of Gaussian, structure at several scales) and training pixels.
#
# Open a grey image in ImageJ (e.g., File > Open Samples > Blobs). Without an
# open image a sample is used. The training pixels are taken automatically
# from the clear foreground and background (Otsu threshold with a margin), the
# uncertain pixels between are classified by the forest. The probability map
# (32-bit) and the classes (8-bit) are shown in ImageJ.
#
# Requires: numpy, scipy, scikit-image, scikit-learn, matplotlib

import numpy as np
import matplotlib.pyplot as plt
from scipy import ndimage as ndi
from skimage import filters
from sklearn.ensemble import RandomForestClassifier
import bio7

SCALES = [1, 2, 4, 8]

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.coins()
    bio7.show_image(img, "Coins (sample)")

gray = img.astype("float64")
if gray.ndim == 3 and gray.shape[-1] == 3:
    gray = gray @ np.array([0.299, 0.587, 0.114])
elif gray.ndim == 3:
    gray = gray[0]
gray = (gray - gray.min()) / max(np.ptp(gray), 1e-12)

# The features of each pixel
layers = [gray]
for s in SCALES:
    g = ndi.gaussian_filter(gray, s)
    layers += [g, filters.sobel(g), ndi.gaussian_laplace(gray, s), ndi.gaussian_filter(gray ** 2, s) - g ** 2]
X = np.stack(layers, axis=-1).reshape(-1, len(layers))

# Automatic training pixels: clearly above or below the Otsu threshold
t = filters.threshold_otsu(gray)
margin = 0.15 * np.ptp(gray)
y = np.full(gray.size, -1)
y[(gray > t + margin).ravel()] = 1
y[(gray < t - margin).ravel()] = 0
train = np.flatnonzero(y >= 0)
rng = np.random.default_rng(0)
train = rng.choice(train, size=min(len(train), 20000), replace=False)

forest = RandomForestClassifier(n_estimators=60, max_depth=12, n_jobs=-1, random_state=0)
forest.fit(X[train], y[train])
probability = forest.predict_proba(X)[:, 1].reshape(gray.shape).astype("float32")
classes = (probability > 0.5).astype("uint8") * 255

bio7.show_image(probability, "Foreground probability (random forest)")
bio7.show_image(classes, "Classes (random forest)")

fig, axes = plt.subplots(1, 3, figsize=(13, 4), constrained_layout=True)
axes[0].imshow(gray, cmap="gray")
axes[0].set_title("Image")
axes[1].imshow(probability, cmap="RdBu_r", vmin=0, vmax=1)
axes[1].set_title("Probability")
names = ["Intensity"] + ["%s s=%d" % (k, s) for s in SCALES for k in ("Gauss", "Sobel", "LoG", "Variance")]
order = np.argsort(forest.feature_importances_)[-8:]
axes[2].barh([names[i] for i in order], forest.feature_importances_[order], color="#55A868")
axes[2].set_title("The most important features")
for ax in axes[:2]:
    ax.set_axis_off()
plt.show()
