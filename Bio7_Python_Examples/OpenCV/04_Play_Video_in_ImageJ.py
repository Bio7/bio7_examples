# Plays a video (or the camera) frame by frame in one ImageJ window: OpenCV
# reads and processes each frame, bio7.update_image() replaces the pixels of the
# open image. The progress is a job of Bio7: its stop button (Progress view)
# ends the playback.
#
# SOURCE: None (a video selected in the file dialog of Bio7, canceled: the
# synthetic video), the path of a video file, or 0 for the camera.
# The camera on macOS: Bio7 must be allowed to use it (System Settings >
# Privacy & Security > Camera), it asks at the first use.
#
# Requires: opencv-python-headless

import sys
import time

import cv2

import bio7

sys.modules.pop("cv_samples", None)
import cv_samples  # noqa: E402

SOURCE = None         # None: the file dialog of Bio7, a path, or 0 (the camera)
EFFECT = "none"       # "none" (the video as it is), "edges" (the Canny edges drawn in
                      # yellow on the frames), "cartoon", "gray"
SECONDS = 20          # the maximum time (the camera runs until stopped or this time)

src = 0 if SOURCE == 0 else cv_samples.choose_video(SOURCE)
cap = cv2.VideoCapture(src)
if not cap.isOpened():
    raise RuntimeError("OpenCV can't open: %r" % (src,))
fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
if fps > 120 or fps < 1:      # cameras sometimes report nonsense
    fps = 25.0
count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)) if SOURCE != 0 else 0


def effect(frame):
    if EFFECT == "edges":
        edges = cv2.Canny(cv2.GaussianBlur(frame, (0, 0), 1.2), 40, 100)
        frame = frame.copy()
        frame[edges > 0] = (0, 255, 255)
    elif EFFECT == "cartoon":
        color = cv2.bilateralFilter(frame, 9, 80, 80)
        edges = cv2.adaptiveThreshold(cv2.medianBlur(cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY), 7), 255,
                                      cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY, 9, 2)
        frame = cv2.bitwise_and(color, color, mask=edges)
    elif EFFECT == "gray":
        return cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    return cv_samples.to_rgb(frame)


shown, start = 0, time.perf_counter()
with bio7.task("Video", total=count or None) as task:
    while time.perf_counter() - start < SECONDS:
        ok, frame = cap.read()
        if not ok:
            break
        out = effect(frame)
        cv2.putText(out, "%d" % shown, (8, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255) if out.ndim == 3 else 255, 1, cv2.LINE_AA)
        bio7.update_image(out, "Video (OpenCV)")
        shown += 1
        task.update()
        # the speed of the video (a camera delivers in its own speed)
        wait = shown / fps - (time.perf_counter() - start)
        if SOURCE != 0 and wait > 0:
            time.sleep(wait)
cap.release()
elapsed = time.perf_counter() - start
print("%d frames in %.1f s (%.1f frames/s, the video: %.1f)" % (shown, elapsed, shown / elapsed, fps))
