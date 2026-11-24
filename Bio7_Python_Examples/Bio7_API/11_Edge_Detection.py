# Edge detection with several operators (scikit-image): Sobel, Scharr,
# Prewitt, Laplacian of Gaussian and Canny, compared in the plot view.
#
# Open a grey image in ImageJ (e.g., File > Open Samples > Boats). Without an
# open image a sample is used. The Canny edges and the Sobel magnitude are
# shown in ImageJ.
#
# Requires: numpy, scipy, scikit-image, matplotlib

import numpy as np
import matplotlib.pyplot as plt
from scipy import ndimage as ndi
from skimage import filters, feature
import bio7

SIGMA = 2.0   # the smoothing of Canny and the LoG

try:
    img = bio7.image()
except RuntimeError:
    from skimage import data
    img = data.camera()
    bio7.show_image(img, "Camera (sample)")

gray = img.astype("float64")
if gray.ndim == 3 and gray.shape[-1] == 3:
    gray = gray @ np.array([0.299, 0.587, 0.114])
elif gray.ndim == 3:
    gray = gray[0]
gray = (gray - gray.min()) / max(np.ptp(gray), 1e-12)

edges = {
    "Sobel": filters.sobel(gray),
    "Scharr": filters.scharr(gray),
    "Prewitt": filters.prewitt(gray),
    "Laplacian of Gaussian": np.abs(ndi.gaussian_laplace(gray, sigma=SIGMA)),
    "Canny": feature.canny(gray, sigma=SIGMA),
}
bio7.show_image((edges["Canny"] * 255).astype("uint8"), "Canny edges")
bio7.show_image(edges["Sobel"].astype("float32"), "Sobel magnitude")

fig, axes = plt.subplots(2, 3, figsize=(12, 8), constrained_layout=True)
axes = axes.ravel()
axes[0].imshow(gray, cmap="gray")
axes[0].set_title("Image")
for ax, (name, e) in zip(axes[1:], edges.items()):
    ax.imshow(e, cmap="gray" if name == "Canny" else "magma")
    ax.set_title(name)
for ax in axes:
    ax.set_axis_off()
plt.show()
