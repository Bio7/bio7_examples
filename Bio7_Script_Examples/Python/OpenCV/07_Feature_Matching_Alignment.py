# Aligning two images with features: ORB key points are found in both images
# and matched, a homography is estimated (RANSAC: robust against wrong
# matches) and the second image is transformed onto the first. Used for
# shifted, rotated or scaled images (e.g., a drifting microscope, two photos of
# the same plot).
#
# The current ImageJ image (or an image file selected in the file dialog of
# Bio7, canceled: the synthetic cells) and a copy of it which is
# rotated, scaled, shifted and noisy (as a second recording).
#
# Requires: opencv-python-headless

import sys

import cv2
import numpy as np

import bio7

sys.modules.pop("cv_samples", None)
import cv_samples  # noqa: E402

img = cv_samples.image()
h, w = img.shape[:2]

# The second "recording": rotated 12 degrees, scaled 0.9, shifted, darker, noisy
M = cv2.getRotationMatrix2D((w / 2, h / 2), 12, 0.9)
M[:, 2] += (25, -15)
moved = cv2.warpAffine(img, M, (w, h), borderMode=cv2.BORDER_REFLECT)
moved = np.clip(moved * 0.85 + np.random.default_rng(0).normal(0, 5, moved.shape), 0, 255).astype(np.uint8)
bio7.show_image(cv_samples.to_rgb(moved), "Second recording")

# Key points and descriptors (ORB: fast, free of patents)
orb = cv2.ORB_create(nfeatures=1500)
k1, d1 = orb.detectAndCompute(cv2.cvtColor(img, cv2.COLOR_BGR2GRAY), None)
k2, d2 = orb.detectAndCompute(cv2.cvtColor(moved, cv2.COLOR_BGR2GRAY), None)
if d1 is None or d2 is None:
    raise RuntimeError("No features found (the image has too little structure)")

# Matching with the ratio test (the best match clearly better than the second)
matcher = cv2.BFMatcher(cv2.NORM_HAMMING)
good = [m for m, n in (p for p in matcher.knnMatch(d2, d1, k=2) if len(p) == 2) if m.distance < 0.75 * n.distance]
print("Key points: %d and %d, good matches: %d" % (len(k1), len(k2), len(good)))
if len(good) < 4:
    raise RuntimeError("Too few matches for a homography")

src = np.float32([k2[m.queryIdx].pt for m in good]).reshape(-1, 1, 2)
dst = np.float32([k1[m.trainIdx].pt for m in good]).reshape(-1, 1, 2)
H, inliers = cv2.findHomography(src, dst, cv2.RANSAC, 3.0)
print("RANSAC: %d of %d matches are consistent" % (inliers.sum(), len(good)))
print("Homography:\n", np.round(H, 3))

aligned = cv2.warpPerspective(moved, H, (w, h))
inl = [g for g, ok in zip(good, inliers.ravel()) if ok][:60]
matches = cv2.drawMatches(moved, k2, img, k1, inl, None, matchColor=(0, 255, 0), flags=cv2.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS)
bio7.show_image(cv_samples.to_rgb(matches), "Matches (60 inliers)")

# The check: the aligned image over the first (green: first, magenta: aligned)
g1 = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
g2 = cv2.cvtColor(aligned, cv2.COLOR_BGR2GRAY)
bio7.show_image(np.dstack([g2, g1, g2]), "Overlay (gray where aligned)")
valid = cv2.warpPerspective(np.ones((h, w), np.uint8), H, (w, h)) > 0
print("Mean difference after the alignment: %.1f gray values" % np.abs(g1[valid].astype(int) - g2[valid]).mean())
