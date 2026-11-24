# Projections and profiles of a stack (e.g., a time series): the mean, the
# maximum and the standard deviation over the slices and the mean intensity of
# each slice (in the selection, if there is one).
#
# Open a stack in ImageJ (e.g., File > Open Samples > T1 Head or a time
# series) and optionally draw a selection. Without an open stack a synthetic
# time series is used. The projections are shown in ImageJ.
#
# Requires: numpy, matplotlib (scikit-image for the selection mask)

import numpy as np
import matplotlib.pyplot as plt
import bio7

try:
    stack = bio7.image()
except RuntimeError:
    # a synthetic time series: a blob moving and pulsing on noise
    rng = np.random.default_rng(0)
    yy, xx = np.mgrid[0:128, 0:128]
    stack = np.stack([100 * np.exp(-((xx - 30 - 2 * t) ** 2 + (yy - 64) ** 2) / 200) * (1 + 0.5 * np.sin(t / 3)) + rng.normal(10, 3, (128, 128))
                      for t in range(40)]).astype("float32")
    bio7.show_image(stack, "Time series (synthetic)")

if stack.ndim == 4:                 # an RGB stack: the luminance
    stack = stack @ np.array([0.299, 0.587, 0.114])
if stack.ndim != 3:
    raise RuntimeError("Please open a stack (slices, height, width)")
stack = stack.astype("float32")

bio7.show_image(stack.mean(axis=0), "Mean projection")
bio7.show_image(stack.max(axis=0), "Maximum projection")
bio7.show_image(stack.std(axis=0), "Standard deviation projection")

# The profile in the selection (or of the whole image)
mask = None
try:
    x, y = bio7.roi()
    from skimage.draw import polygon
    rr, cc = polygon(np.asarray(y), np.asarray(x), shape=stack.shape[1:])
    mask = np.zeros(stack.shape[1:], dtype=bool)
    mask[rr, cc] = True
except Exception:
    pass
profile = stack[:, mask].mean(axis=1) if mask is not None and mask.any() else stack.mean(axis=(1, 2))

fig, axes = plt.subplots(1, 2, figsize=(11, 4), constrained_layout=True)
axes[0].plot(np.arange(1, len(profile) + 1), profile, "o-", color="#4C72B0")
axes[0].set(title="Mean intensity" + (" (selection)" if mask is not None else ""), xlabel="Slice", ylabel="Intensity")
axes[1].imshow(stack.std(axis=0), cmap="inferno")
axes[1].set_title("Variability (standard deviation)")
axes[1].set_axis_off()
plt.show()
