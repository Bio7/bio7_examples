# Filtering in the frequency domain (FFT, numpy): the power spectrum, a low
# pass (smoothing), a high pass (details) and a band pass.
#
# Open a grey image in ImageJ (e.g., File > Open Samples > Bridge). Without an
# open image a sample is used. The filtered images are shown in ImageJ.
#
# Requires: numpy, matplotlib

import numpy as np
import matplotlib.pyplot as plt
import bio7

LOW, HIGH = 0.02, 0.15   # the band (frequencies as a fraction of the sampling rate)

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

F = np.fft.fft2(gray)
fy = np.fft.fftfreq(gray.shape[0])[:, None]
fx = np.fft.fftfreq(gray.shape[1])[None, :]
radius = np.sqrt(fx ** 2 + fy ** 2)


def filtered(transfer):
    return np.real(np.fft.ifft2(F * transfer)).astype("float32")


# Smooth (Gaussian) transfer functions: no ringing
lowpass = np.exp(-(radius / HIGH) ** 2)
highpass = 1 - np.exp(-(radius / LOW) ** 2)
bandpass = lowpass * highpass

bio7.show_image(filtered(lowpass), "Low pass")
bio7.show_image(filtered(highpass), "High pass")
bio7.show_image(filtered(bandpass), "Band pass")

spectrum = np.log1p(np.abs(np.fft.fftshift(F)))
fig, axes = plt.subplots(1, 3, figsize=(13, 4), constrained_layout=True)
axes[0].imshow(spectrum, cmap="inferno")
axes[0].set_title("Power spectrum (log)")
axes[1].imshow(np.fft.fftshift(bandpass), cmap="gray")
axes[1].set_title("Band pass %.2f - %.2f" % (LOW, HIGH))
axes[2].imshow(filtered(bandpass), cmap="gray")
axes[2].set_title("Band-pass filtered")
for ax in axes:
    ax.set_axis_off()
plt.show()
