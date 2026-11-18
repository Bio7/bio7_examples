# Writes the current ImageJ stack (e.g., a time series, the result of a
# simulation or of 03_Video_to_ImageJ_Stack.py) as video file with OpenCV, with
# a time stamp in each frame. 16 and 32-bit stacks are scaled to 8 bit.
#
# Without an open stack a synthetic time series (a spreading wave) is used.
# The file is selected in the save dialog of Bio7 (canceled: the temporary
# folder bio7_opencv_examples).
#
# Requires: opencv-python-headless

import os
import sys

import cv2
import numpy as np

import bio7

sys.modules.pop("cv_samples", None)
import cv_samples  # noqa: E402

FPS = 15
FILE = None           # None: the save dialog of Bio7, or a path (.mp4: mp4v, .avi: MJPG)

FILE = FILE or cv_samples.save_file("*.mp4")
if not FILE:
    FILE = os.path.join(cv_samples.folder(), "imagej_stack.mp4")
    print("No file selected:", FILE)

try:
    stack = bio7.image()
except RuntimeError:
    yy, xx = np.mgrid[0:240, 0:320]
    r = np.hypot(xx - 160, yy - 120)
    stack = np.stack([np.sin(r / 8 - t / 3) * np.exp(-r / (40 + 4 * t)) for t in range(60)]).astype(np.float32)
    bio7.show_image(stack, "Wave (synthetic)")

rgb = stack.ndim == 4 or (stack.ndim == 3 and stack.shape[-1] == 3 and stack.dtype == np.uint8 and stack.shape[0] > 3)
if stack.ndim == 2 or (stack.ndim == 3 and rgb):          # a single image: one frame
    stack = stack[None]
if stack.dtype != np.uint8:                                # one scaling for all frames (no flicker)
    lo, hi = np.percentile(stack, [0.5, 99.5])
    stack = np.clip((stack - lo) / max(hi - lo, 1e-9) * 255, 0, 255).astype(np.uint8)

n, h, w = stack.shape[:3]
fourcc = cv2.VideoWriter_fourcc(*("mp4v" if FILE.endswith(".mp4") else "MJPG"))
writer = cv2.VideoWriter(FILE, fourcc, FPS, (w, h))
if not writer.isOpened():
    raise RuntimeError("OpenCV can't write: " + FILE)
for i in bio7.task("Writing the video", range(n)):
    frame = stack[i]
    frame = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR) if frame.ndim == 3 else cv2.applyColorMap(frame, cv2.COLORMAP_VIRIDIS)
    cv2.putText(frame, "%.2f s" % (i / FPS), (8, h - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1, cv2.LINE_AA)
    writer.write(frame)
writer.release()
print("Written: %s (%d frames, %d x %d, %.1f MB)" % (FILE, n, w, h, os.path.getsize(FILE) / 2**20))
print("Read it again with 03_Video_to_ImageJ_Stack.py (VIDEO = the path).")
