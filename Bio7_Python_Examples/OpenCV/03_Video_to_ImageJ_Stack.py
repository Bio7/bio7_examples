# A video read with OpenCV as stack in ImageJ (one slice per frame), with the
# frame rate and the time of each frame. The stack can then be analyzed in
# ImageJ (Image > Stacks: animation, projections, Plot Z-axis Profile).
#
# VIDEO: the path of a video file (mp4, avi, mov, ...); None: a video selected
# in the file dialog of Bio7 (canceled: the synthetic video).
#
# Long or large videos: a 1920 x 1200 RGB frame needs 9 MB in ImageJ, so one
# minute at 30 frames/s is 16 GB. Choose:
#   SCALE = 0.5            half the width and height (a quarter of the memory)
#   EVERY = 10             every 10th frame (also START and MAX_FRAMES)
#   GRAY = True            8-bit (a quarter of the memory of RGB in ImageJ)
#   VIRTUAL = True         the frames are written as JPEG files and opened as
#                          virtual stack: ImageJ loads only the frame shown, so
#                          any length fits (a bit slower to browse)
# VIRTUAL = None chooses automatically: virtual if the stack would need more
# than half of the Java memory of Bio7.
#
# Requires: opencv-python-headless

import os
import sys

import cv2
import numpy as np

import bio7

sys.modules.pop("cv_samples", None)
import cv_samples  # noqa: E402

VIDEO = None          # None: the file dialog of Bio7, or e.g. "/Users/me/Movies/experiment.mp4"
START = 0.0           # the start (seconds)
EVERY = 1             # every n-th frame
MAX_FRAMES = 300      # the most frames (None: all from START)
SCALE = 1.0           # the size of the frames (0.5: half width and height)
GRAY = False          # True: an 8-bit stack
VIRTUAL = None        # None: automatic, True: a virtual stack (JPEG files), False: in memory
JPEG_QUALITY = 92     # of the files of a virtual stack

path = cv_samples.choose_video(VIDEO)
cap = cv2.VideoCapture(path)
if not cap.isOpened():
    raise RuntimeError("OpenCV can't open the video: " + path)
w0, h0 = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)), int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
total, fps = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)), cap.get(cv2.CAP_PROP_FPS) or 25.0
codec = int(cap.get(cv2.CAP_PROP_FOURCC)).to_bytes(4, "little").decode(errors="replace")
print("Video:", path)
print("  %d x %d, %d frames (%.0f s), %.1f frames/s, codec %s" % (w0, h0, total, total / fps, fps, codec))

# The frames to read and the memory they need in ImageJ (RGB: 4 bytes per pixel)
w, h = max(1, round(w0 * SCALE)), max(1, round(h0 * SCALE))
first = int(START * fps)
available = max(0, (total - first + EVERY - 1) // EVERY) if total > 0 else (MAX_FRAMES or 1000)
n = min(available, MAX_FRAMES) if MAX_FRAMES else available
needed = n * w * h * (1 if GRAY else 4)
heap = bio7.java.java.lang.Runtime.getRuntime().maxMemory()
virtual = VIRTUAL if VIRTUAL is not None else needed > heap / 2
print("  reading %d frames (every %d. from %.1f s) of %d x %d: %.1f GB in ImageJ (Java memory %.1f GB) -> %s"
      % (n, EVERY, START, w, h, needed / 2**30, heap / 2**30, "virtual stack (files)" if virtual else "in memory"))
if not virtual and needed > heap * 0.8:
    raise RuntimeError("The stack doesn't fit in the Java memory: use VIRTUAL = True, SCALE, EVERY, MAX_FRAMES or GRAY")

if first:
    cap.set(cv2.CAP_PROP_POS_FRAMES, first)
name = os.path.splitext(os.path.basename(path))[0]
if virtual:
    folder = os.path.join(cv_samples.folder(), name + "_frames")
    os.makedirs(folder, exist_ok=True)
    for f in os.listdir(folder):                 # the frames of a former run
        if f.endswith(".jpg"):
            os.remove(os.path.join(folder, f))
else:
    frames = np.empty((n, h, w) if GRAY else (n, h, w, 3), np.uint8)   # filled in place (no copy at the end)

count, index = 0, first
with bio7.task("Reading the video", total=n) as task:
    while count < n:
        ok, frame = cap.read()
        if not ok:
            break
        if (index - first) % EVERY == 0:
            if SCALE != 1.0:
                frame = cv2.resize(frame, (w, h), interpolation=cv2.INTER_AREA)
            if GRAY:
                frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            if virtual:
                cv2.imwrite(os.path.join(folder, "frame_%06d.jpg" % count), frame, [cv2.IMWRITE_JPEG_QUALITY, JPEG_QUALITY])
            else:
                frames[count] = frame if GRAY else cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            count += 1
            task.update()
        index += 1
cap.release()
if count == 0:
    raise RuntimeError("No frames were read")

title = "Video: %s" % os.path.basename(path)
if virtual:
    # ImageJ opens the folder as virtual stack: only the frame shown is in memory
    imp = bio7.java.ij.plugin.FolderOpener.open(folder, "virtual")
    imp.setTitle(title)
    bio7.java.ui(imp).show()
else:
    bio7.show_image(frames[:count], title)       # large stacks are transferred in parts
    imp = bio7.java.ij.WindowManager.getImage(title)

# The frame rate and the time per frame for ImageJ (Image > Properties, animation)
cal = imp.getCalibration()
cal.fps = fps / EVERY
cal.frameInterval = EVERY / fps
cal.setTimeUnit("s")
imp.setDimensions(1, 1, count)                   # the slices are frames (time)
if not virtual:
    for i in range(count):
        imp.getStack().setSliceLabel("%.2f s" % (START + i * EVERY / fps), i + 1)
imp.updateAndDraw()
print("Stack: %d frames%s - Image > Stacks > Animation > Start Animation (\\)" % (count, " (virtual, files in %s)" % folder if virtual else ""))
