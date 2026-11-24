# ImageJ images as numpy arrays in the embedded Python of Bio7.
#
# Open an image in ImageJ first (e.g., File > Open Samples > Blobs), then run
# this script. bio7.image() returns the current ImageJ image as numpy array:
#   8-bit: uint8 (height, width), 16-bit: uint16, 32-bit: float32,
#   RGB: uint8 (height, width, 3), a stack: (slices, height, width).
# bio7.show_image() creates a new ImageJ image from an array.
#
# Requires: numpy, matplotlib

import numpy as np
import matplotlib.pyplot as plt
import bio7

img = bio7.image()
print("Image:", img.dtype, img.shape)

# A grey image (the mean of the colors of an RGB image, the first slice of a stack)
gray = img.astype("float64")
if gray.ndim == 3 and gray.shape[-1] == 3:
    gray = gray.mean(axis=-1)
elif gray.ndim == 3:
    gray = gray[0]

# A smoothing with the FFT (a Gaussian filter, numpy only)
sigma = 2.0
fy = np.fft.fftfreq(gray.shape[0])[:, None]
fx = np.fft.fftfreq(gray.shape[1])[None, :]
kernel = np.exp(-2 * (np.pi * sigma) ** 2 * (fx ** 2 + fy ** 2))
smooth = np.real(np.fft.ifft2(np.fft.fft2(gray) * kernel))

# A threshold (the mean) and the share of the foreground
mask = smooth > smooth.mean()
print("Foreground: %.1f %%" % (100 * mask.mean()))

# Back to ImageJ: the smoothed image (32-bit) and the mask (8-bit)
bio7.show_image(smooth.astype("float32"), "Smoothed (Python)")
bio7.show_image((mask * 255).astype("uint8"), "Mask (Python)")

# The histogram of the intensities in the plot view
fig, ax = plt.subplots(constrained_layout=True)
ax.hist(gray.ravel(), bins=64, color="#4C72B0", alpha=0.8)
ax.axvline(smooth.mean(), color="#C44E52", lw=2, label="Threshold")
ax.set(title="Intensities", xlabel="Grey value", ylabel="Pixels")
ax.legend(frameon=False)
plt.show()
