# Records the camera (webcam) into an ImageJ stack: one slice per frame, with
# the time of each frame and the frame rate (Image > Properties), ready for
# the stack functions of ImageJ (animation, projections, Plot Z-axis Profile).
#
# PREVIEW = True:  a live view in ImageJ while recording (every PREVIEW_EVERY-th
#                  frame is shown; showing costs time on large frames).
# PREVIEW = False: only recording (the fastest; the progress is shown in the
#                  Progress view of Bio7).
# The stop button of the job (Progress view) ends the recording early; the
# frames recorded so far become the stack.
#
# SOURCE: 0 is the first camera (1, 2, ... others), or the path of a video file
# (to try it without a camera). macOS: Bio7 needs the permission for the camera
# (System Settings > Privacy & Security > Camera).
#
# Requires: opencv-python-headless (Python Packages: the bundle "OpenCV")

import sys
import time

import numpy as np

import bio7

sys.modules.pop("camera_capture", None)       # the module of this folder, imported again after changes
import camera_capture as cam  # noqa: E402

SOURCE = 0             # the camera (0, 1, ...) or a video file
SECONDS = 10           # the length of the recording
MAX_FRAMES = 600       # the most frames (memory: 640 x 480 RGB is 0.9 MB per frame)
WIDTH, HEIGHT = 640, 480   # requested from the camera (None: its default)
GRAY = False           # True: an 8-bit stack (a third of the memory, faster)
PREVIEW = True         # the live view in ImageJ while recording
PREVIEW_EVERY = 2      # show every n-th frame in the live view

width, height, fps = cam.open_camera(SOURCE, WIDTH, HEIGHT)
print("Source %r: %d x %d, %s frames/s (reported)" % (SOURCE, width, height, "%.0f" % fps if fps else "?"))

frames, times = [], []
start = time.perf_counter()
try:
    with bio7.task("Recording", total=MAX_FRAMES) as task:
        while len(frames) < MAX_FRAMES and time.perf_counter() - start < SECONDS:
            frame = cam.grab(GRAY)
            if frame is None:              # the end of a video file
                break
            times.append(time.perf_counter() - start)
            frames.append(frame)
            if PREVIEW and len(frames) % PREVIEW_EVERY == 0:
                bio7.update_image(frame, "Camera (live)")
            task.update(text="%d frames, %.1f s" % (len(frames), times[-1]))
finally:
    cam.close()                            # also when stopped: the camera is free again

if not frames:
    raise RuntimeError("No frames were recorded")
elapsed = times[-1] if len(times) > 1 else 1.0
rate = (len(frames) - 1) / elapsed if len(frames) > 1 else 0.0
intervals = np.diff(times)
print("Recorded: %d frames in %.1f s = %.1f frames/s (preview %s)" % (len(frames), elapsed, rate, "on" if PREVIEW else "off"))
if len(intervals):
    print("  frame interval: mean %.1f ms, max %.1f ms (a long one: a dropped frame or a busy computer)"
          % (1000 * intervals.mean(), 1000 * intervals.max()))

# The stack: the time of each frame as slice label, the frame rate for the animation
stack = np.stack(frames)
title = "Camera recording (%d frames)" % len(frames)
bio7.show_image(stack, title)
imp = bio7.java.ij.WindowManager.getImage(title)
cal = imp.getCalibration()
cal.fps = rate
cal.frameInterval = 1.0 / rate if rate else 0.0
cal.setTimeUnit("s")
imp.setDimensions(1, 1, len(frames))       # the slices are frames (time)
for i, t in enumerate(times):
    imp.getStack().setSliceLabel("%.3f s" % t, i + 1)
imp.updateAndDraw()
print("Stack: %s, %.0f MB - Image > Stacks > Animation > Start Animation (\\)" % (title, stack.nbytes / 2**20))
