# Motion detection and tracking: the background is learned (MOG2), the moving
# objects are found as contours in each frame, their centroids are connected
# to tracks (the nearest object of the previous frame). The frames with the
# boxes and tracks are played in ImageJ, the tracks are a pandas DataFrame
# (speed, distance) and a plot.
#
# VIDEO: the path of a video file; None: a video selected in the file dialog
# of Bio7 (canceled: the synthetic video of moving cells).
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

VIDEO = None
MIN_AREA = 40          # the smallest object (pixels)
MAX_STEP = 25          # the largest movement between two frames (pixels)
MAX_GAP = 8            # a track continues after this many frames without its object (e.g., two touching objects)

frames, fps = cv_samples.read_frames(cv_samples.choose_video(VIDEO))
subtractor = cv2.createBackgroundSubtractorMOG2(history=500, varThreshold=16, detectShadows=False)
kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))

tracks = {}            # id: list of (frame, x, y, area)
last = {}              # id: (x, y, frame) the last position of the track
next_id = 1
colors = np.random.default_rng(0).integers(80, 255, (1000, 3)).tolist()
masks = []

for t, frame in enumerate(bio7.task("Tracking", frames)):
    fg = subtractor.apply(frame)
    fg = cv2.morphologyEx(fg, cv2.MORPH_OPEN, kernel)
    fg = cv2.morphologyEx(fg, cv2.MORPH_CLOSE, kernel)
    masks.append(fg)
    contours, _ = cv2.findContours(fg, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    found = []
    for c in contours:
        area = cv2.contourArea(c)
        if area >= MIN_AREA:
            m = cv2.moments(c)
            found.append((m["m10"] / m["m00"], m["m01"] / m["m00"], area, cv2.boundingRect(c)))
    # the nearest track of the previous frame (each track once)
    free = {tid: p for tid, p in last.items() if t - p[2] <= MAX_GAP}
    current = []
    for x, y, area, box in found:
        # the allowed distance grows with the frames since the track was seen
        best = min(free.items(), key=lambda kv: (kv[1][0] - x) ** 2 + (kv[1][1] - y) ** 2, default=None)
        if best is not None and np.hypot(best[1][0] - x, best[1][1] - y) <= MAX_STEP * (t - best[1][2]):
            tid = best[0]
            del free[tid]
        else:
            tid, next_id = next_id, next_id + 1
            tracks[tid] = []
        tracks[tid].append((t, x, y, area))
        last[tid] = (x, y, t)
        current.append(tid)
        bx, by, bw, bh = box
        cv2.rectangle(frame, (bx, by), (bx + bw, by + bh), colors[tid % 1000], 1)
        cv2.putText(frame, str(tid), (bx, by - 3), cv2.FONT_HERSHEY_SIMPLEX, 0.4, colors[tid % 1000], 1, cv2.LINE_AA)
    # the tracks so far
    for tid in current:
        pts = np.array([(p[1], p[2]) for p in tracks[tid][-30:]], np.int32)
        cv2.polylines(frame, [pts], False, colors[tid % 1000], 1, cv2.LINE_AA)
    bio7.update_image(cv_samples.to_rgb(frame), "Tracking (OpenCV)")

bio7.show_image(np.stack(masks), "Foreground (MOG2)")

rows = [(tid, f, x, y, a) for tid, pts in tracks.items() for f, x, y, a in pts]
df = pd.DataFrame(rows, columns=["track", "frame", "x", "y", "area"])
df["time"] = df["frame"] / fps
# the speed in pixels per second (the step to the previous point of the track)
df["speed"] = np.hypot(df.groupby("track")["x"].diff(), df.groupby("track")["y"].diff()) * fps
summary = df.groupby("track").agg(frames=("frame", "size"), speed=("speed", "mean"), area=("area", "mean"))
summary = summary[summary["frames"] >= 10]           # short tracks are noise
print("Tracks (at least 10 frames):\n", summary.round(1))

fig, axes = plt.subplots(1, 2, figsize=(11, 4.5), constrained_layout=True)
h, w = frames[0].shape[:2]
for tid in summary.index:
    tr = df[df["track"] == tid]
    axes[0].plot(tr["x"], tr["y"], lw=1.5, label=str(tid))
axes[0].set(title="Tracks", xlim=(0, w), ylim=(h, 0), aspect="equal")
axes[0].legend(fontsize=7, ncol=2, frameon=False)
for tid in summary.index:
    tr = df[df["track"] == tid]
    axes[1].plot(tr["time"], tr["speed"].rolling(5, min_periods=1).mean(), lw=1)
axes[1].set(title="Speed (mean of 5 frames)", xlabel="Time (s)", ylabel="Pixels/s")
plt.show()
