# Optical flow: the movement of every pixel between two frames (Farneback,
# dense) as a color image (the color: the direction, the brightness: the
# speed) and the movement of corner points (Lucas-Kanade, sparse) as tracks.
# The flow images are a stack in ImageJ, the mean speed per frame a plot.
#
# VIDEO: the path of a video file; None: a video selected in the file dialog
# of Bio7 (canceled: the synthetic video, moving cells on a slowly turning
# background).
#
# Requires: opencv-python-headless, matplotlib

import sys

import cv2
import matplotlib.pyplot as plt
import numpy as np

import bio7

sys.modules.pop("cv_samples", None)
import cv_samples  # noqa: E402

VIDEO = None
MAX_FRAMES = 100

frames, fps = cv_samples.read_frames(cv_samples.choose_video(VIDEO), max_frames=MAX_FRAMES)
grays = [cv2.cvtColor(f, cv2.COLOR_BGR2GRAY) for f in frames]

# Dense optical flow (Farneback): the flow as HSV color
flow_images, speed = [], []
hsv = np.zeros_like(frames[0])
hsv[..., 1] = 255
for prev, cur in bio7.task("Optical flow", list(zip(grays[:-1], grays[1:]))):
    flow = cv2.calcOpticalFlowFarneback(prev, cur, None, 0.5, 3, 15, 3, 5, 1.2, 0)
    mag, ang = cv2.cartToPolar(flow[..., 0], flow[..., 1])
    hsv[..., 0] = ang * 180 / np.pi / 2
    hsv[..., 2] = np.clip(mag * 40, 0, 255).astype(np.uint8)
    flow_images.append(cv2.cvtColor(hsv, cv2.COLOR_HSV2RGB))
    speed.append(mag.mean() * fps)
bio7.show_image(np.stack(flow_images), "Optical flow (Farneback)")

# Sparse optical flow (Lucas-Kanade): good corners followed through the video
points = cv2.goodFeaturesToTrack(grays[0], maxCorners=80, qualityLevel=0.05, minDistance=8)
tracks = np.zeros_like(frames[0])
colors = np.random.default_rng(1).integers(100, 255, (len(points), 3)).tolist()
alive = np.ones(len(points), bool)
for prev, cur in zip(grays[:-1], grays[1:]):
    new, status, _ = cv2.calcOpticalFlowPyrLK(prev, cur, points, None, winSize=(15, 15), maxLevel=2)
    for i, (p, q) in enumerate(zip(points.reshape(-1, 2), new.reshape(-1, 2))):
        if alive[i] and status[i, 0]:
            cv2.line(tracks, tuple(int(v) for v in p), tuple(int(v) for v in q), colors[i], 1, cv2.LINE_AA)
    alive &= status[:, 0].astype(bool)
    points = new
result = cv2.addWeighted(frames[-1], 0.6, tracks, 1.0, 0)
bio7.show_image(cv_samples.to_rgb(result), "Tracks of corners (Lucas-Kanade)")
print("Corners followed to the end: %d of %d" % (alive.sum(), len(alive)))

fig, ax = plt.subplots(figsize=(8, 3.5), constrained_layout=True)
ax.plot(np.arange(1, len(speed) + 1) / fps, speed, color="#4C72B0")
ax.set(title="Mean movement of the pixels", xlabel="Time (s)", ylabel="Pixels/s")
plt.show()
