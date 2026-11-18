# Records the camera (webcam) to image files on disk and opens them as an
# ImageJ virtual stack: ImageJ loads only the frame shown, so the recording is
# limited by the disk, not by the memory (4K: 33 MB per frame in memory, only
# about 7 seconds fit in 8 GB of Java memory; as JPEG files 1.8 MB per frame).
#
# Several threads write the frames while the camera is read (OpenCV encodes
# in parallel): measured with 4K frames on an M1 Max, JPEG with 4 threads
# writes about 150 frames/s, faster than a camera delivers.
#
#   FORMAT = "jpg"   small files (4K: about 1.8 MB, 1 minute at 30 frames/s
#                    about 3 GB), lossy (JPEG_QUALITY)
#   FORMAT = "tif"   lossless, uncompressed (4K: 24 MB per frame, 1 minute
#                    about 43 GB: needs a fast disk; about 100 frames/s)
#
# The files (frame_000001.jpg, ...) and times.csv (the time of each frame) are
# in FOLDER: None: a folder selected in the directory dialog of Bio7 (canceled:
# the temporary folder). The stop button of the job (Progress view) ends the
# recording early. The virtual stack can be saved later as one file, or
# converted (Image > Duplicate: into memory, e.g. a part of it).
#
# SOURCE: 0 is the first camera (1, 2, ... others), or the path of a video
# file. macOS: Bio7 needs the permission for the camera (System Settings >
# Privacy & Security > Camera).
#
# Requires: opencv-python-headless (Python Packages: the bundle "OpenCV")

import os
import queue
import sys
import threading
import time

import cv2

import bio7

sys.modules.pop("camera_capture", None)      # the modules of this folder, imported again after changes
sys.modules.pop("cv_samples", None)
import camera_capture as cam  # noqa: E402
import cv_samples  # noqa: E402

SOURCE = 0                  # the camera (0, 1, ...) or a video file
WIDTH, HEIGHT = 1920, 1080  # requested from the camera (4K: 3840, 2160)
SECONDS = 60                # the length of the recording
GRAY = False                # 8-bit frames (smaller, faster)
FORMAT = "jpg"              # "jpg" (small, lossy) or "tif" (lossless, large)
JPEG_QUALITY = 90
WRITERS = 4                 # threads writing the files
PREVIEW = True              # a live view in ImageJ while recording
PREVIEW_EVERY = 10          # show every n-th frame (a 4K frame costs time to show)
FOLDER = None               # None: the directory dialog of Bio7

# The folder of the frames (a new subfolder for each recording)
base = FOLDER or cv_samples.choose_folder("Select the folder for the recorded frames") or cv_samples.folder()
folder = os.path.join(base, time.strftime("camera_%Y%m%d_%H%M%S"))
os.makedirs(folder)
params = [cv2.IMWRITE_JPEG_QUALITY, JPEG_QUALITY] if FORMAT == "jpg" else [cv2.IMWRITE_TIFF_COMPRESSION, 1]
cv2.setNumThreads(1)        # one encoding thread per frame, the writers in parallel

width, height, fps = cam.open_camera(SOURCE, WIDTH, HEIGHT)
print("Source %r: %d x %d, %s frames/s (reported)" % (SOURCE, width, height, "%.0f" % fps if fps else "?"))
print("Frames:", folder)

# The writers: frames from a queue (limited: if the disk is too slow, the recording waits)
frames = queue.Queue(maxsize=8 * WRITERS)
errors = []
waits = [0]


def writer():
    while True:
        item = frames.get()
        if item is None:
            break
        index, frame = item
        if not cv2.imwrite(os.path.join(folder, "frame_%06d.%s" % (index, FORMAT)), frame, params):
            errors.append(index)


threads = [threading.Thread(target=writer, daemon=True) for _ in range(WRITERS)]
for t in threads:
    t.start()

times = []
start = time.perf_counter()
try:
    with bio7.task("Recording to %s files" % FORMAT.upper()) as task:
        while time.perf_counter() - start < SECONDS:
            frame = cam.read_bgr()
            if frame is None:                       # the end of a video file
                break
            times.append(time.perf_counter() - start)
            if GRAY:
                frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            if frames.full():
                waits[0] += 1                       # the writers are behind (a slow disk)
            frames.put((len(times), frame))
            if PREVIEW and len(times) % PREVIEW_EVERY == 1:
                bio7.update_image(frame if GRAY else cv2.cvtColor(frame, cv2.COLOR_BGR2RGB), "Camera (live)")
            task.update(text="%d frames, %.1f s" % (len(times), times[-1]))
finally:
    cam.close()
    for _ in threads:
        frames.put(None)
    for t in threads:
        t.join()                                    # all frames written

if not times:
    raise RuntimeError("No frames were recorded")
with open(os.path.join(folder, "times.csv"), "w") as f:
    f.write("frame,file,seconds\n")
    for i, t in enumerate(times, start=1):
        f.write("%d,frame_%06d.%s,%.4f\n" % (i, i, FORMAT, t))

n = len(times)
elapsed = times[-1] if n > 1 else 1.0
rate = (n - 1) / elapsed if n > 1 else 0.0
size = sum(os.path.getsize(os.path.join(folder, f)) for f in os.listdir(folder)) / 2**20
print("Recorded: %d frames in %.1f s = %.1f frames/s, %.0f MB on disk (%.1f MB per frame)" % (n, elapsed, rate, size, size / n))
if waits[0]:
    print("  the writers were behind %d times: the disk is too slow for %s at this size (use jpg, GRAY or a smaller size)" % (waits[0], FORMAT))
if errors:
    print("  %d frames could not be written (disk full?)" % len(errors))

# The virtual stack: only the frame shown is loaded
imp = bio7.java.ij.plugin.FolderOpener.open(folder, "virtual file=frame_")
imp.setTitle("Camera recording (%d frames, virtual)" % n)
cal = imp.getCalibration()
cal.fps = rate
cal.frameInterval = 1.0 / rate if rate else 0.0
cal.setTimeUnit("s")
imp.setDimensions(1, 1, imp.getStackSize())     # the slices are frames (time)
bio7.java.ui(imp).show()
print("Virtual stack: %d frames - Image > Stacks > Animation (\\); the times of the frames: times.csv" % imp.getStackSize())
