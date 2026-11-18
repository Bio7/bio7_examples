# Objects with OpenCV: threshold (Otsu), the touching objects separated
# (distance transform and watershed), contours and measurements (area,
# perimeter, circularity, ellipse). The objects are drawn and numbered in an
# RGB image in ImageJ, the measurements are a pandas DataFrame (also in R).
#
# Open an image of bright objects in ImageJ (e.g., File > Open Samples > Blobs:
# Edit > Invert first, the blobs are dark), else an image file selected in the
# file dialog of Bio7 (canceled: a synthetic image of cells).
#
# Requires: opencv-python-headless, pandas, matplotlib

import sys

import cv2
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

import bio7

sys.modules.pop("cv_samples", None)
import cv_samples  # noqa: E402

img = cv_samples.image()
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# The uneven background removed (top hat), smoothed, then Otsu's threshold
flat = cv2.morphologyEx(gray, cv2.MORPH_TOPHAT, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (61, 61)))
flat = cv2.GaussianBlur(flat, (0, 0), 1.5)
t, mask = cv2.threshold(flat, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8), iterations=2)
print("Otsu threshold:", t)

# Watershed: the maxima of the distance transform are the seeds of the objects
dist = cv2.distanceTransform(mask, cv2.DIST_L2, 5)
_, seeds = cv2.threshold(dist, 0.45 * dist.max(), 255, cv2.THRESH_BINARY)
seeds = seeds.astype(np.uint8)
n, markers = cv2.connectedComponents(seeds)
background = cv2.dilate(mask, np.ones((3, 3), np.uint8), iterations=3)
markers = markers + 1
markers[(background > 0) & (seeds == 0)] = 0      # the unknown zone between the objects
markers = cv2.watershed(img, markers)
print("Objects (watershed):", n - 1)

rows = []
drawn = img.copy()
for label in range(2, n + 1):
    obj = np.uint8(markers == label) * 255
    contours, _ = cv2.findContours(obj, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    if not contours:
        continue
    c = max(contours, key=cv2.contourArea)
    area = cv2.contourArea(c)
    if area < 20:
        continue
    perimeter = cv2.arcLength(c, True)
    m = cv2.moments(c)
    cx, cy = m["m10"] / m["m00"], m["m01"] / m["m00"]
    row = {"x": cx, "y": cy, "area": area, "perimeter": perimeter,
           "circularity": 4 * np.pi * area / perimeter ** 2,
           "mean": cv2.mean(gray, mask=obj)[0]}
    if len(c) >= 5:                                  # the fitted ellipse
        (_, _), (d1, d2), angle = cv2.fitEllipse(c)
        row.update(major=max(d1, d2), minor=min(d1, d2), angle=angle)
    rows.append(row)
    cv2.drawContours(drawn, [c], -1, (0, 255, 255), 1, cv2.LINE_AA)
    cv2.putText(drawn, str(len(rows)), (int(cx) - 6, int(cy) + 4), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (0, 0, 255), 1, cv2.LINE_AA)

df = pd.DataFrame(rows)
df.index = df.index + 1
print(df.round(2).head(10))
print("...\n", df[["area", "circularity", "mean"]].describe().round(2))

bio7.show_image(cv_samples.to_rgb(drawn), "Objects (OpenCV)")
bio7.show_image(markers.astype(np.float32), "Labels (watershed)", display_range="auto")

# The measurements in R (if R is running)
try:
    bio7.to_r("objects", df)
    print("In R: the data frame 'objects'")
except Exception as e:
    print("R:", e)

fig, axes = plt.subplots(1, 2, figsize=(10, 4), constrained_layout=True)
axes[0].hist(df["area"], bins=20, color="#4C72B0")
axes[0].set(title="Area", xlabel="Pixels", ylabel="Objects")
axes[1].scatter(df["area"], df["circularity"], c=df["mean"], cmap="viridis")
axes[1].set(title="Shape", xlabel="Area", ylabel="Circularity")
plt.show()
