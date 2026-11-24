# Sample data of the OpenCV examples: the current ImageJ image (or a synthetic
# image of cells) and a synthetic video (moving cells), so that the examples
# run without own files, and the file dialogs of the Bio7 API (Bio7Dialog,
# called with the Java API of the embedded Python) to select own files.
# Imported by the examples of this folder.

import os
import tempfile

import cv2
import numpy as np

import bio7


VIDEO_FILES = ["*.mp4;*.avi;*.mov;*.mkv;*.m4v;*.wmv;*.mpg;*.webm", "*.*"]
IMAGE_FILES = ["*.png;*.jpg;*.jpeg;*.tif;*.tiff;*.bmp;*.webp", "*.*"]


def _dialog():
    """The dialogs of the Bio7 API (com.eco.bio7.batch.Bio7Dialog)."""
    return bio7.java.com.eco.bio7.batch.Bio7Dialog


def open_file(extensions=None):
    """A file selected in the file dialog of Bio7 (None if canceled).
    extensions: the filters, e.g. ["*.mp4;*.avi", "*.*"]."""
    path = _dialog().openFile(extensions) if extensions else _dialog().openFile()
    return str(path) if path else None


def open_files():
    """Several files selected in the file dialog of Bio7 (a list, empty if canceled)."""
    names = _dialog().openMultipleFiles()
    if not names:
        return []
    return [os.path.join(str(_dialog().getCurrentPath()), str(n)) for n in names]


def save_file(extension="*.*"):
    """A file name selected in the save dialog of Bio7 (it asks before an
    existing file is replaced), None if canceled. The extension is added if the
    name has none."""
    path = _dialog().saveFile(extension)
    if not path:
        return None
    path = str(path)
    ext = extension.lstrip("*")
    if ext not in ("", ".") and not os.path.splitext(path)[1]:
        path += ext
    return path


def choose_folder(text="Select a folder"):
    """A folder selected in the directory dialog of Bio7 (None if canceled)."""
    path = _dialog().directory(text)
    return str(path) if path else None


def choose_video(path=None):
    """The video to use: the given path, else a video selected in the file
    dialog of Bio7, canceled: the synthetic video."""
    if path:
        return path
    selected = open_file(VIDEO_FILES)
    if selected:
        return selected
    print("No video selected: the synthetic video is used")
    return video()


def folder():
    """The folder of the example data (in the temporary folder)."""
    d = os.path.join(tempfile.gettempdir(), "bio7_opencv_examples")
    os.makedirs(d, exist_ok=True)
    return d


def to_bgr(img):
    """An image of ImageJ (8, 16, 32-bit, RGB, the first slice of a stack) as
    8-bit BGR image of OpenCV (OpenCV orders the colors blue, green, red)."""
    if img.ndim == 3 and img.shape[-1] != 3:          # a stack: the first slice
        img = img[0]
    if img.ndim == 4:                                  # an RGB stack
        img = img[0]
    if img.dtype != np.uint8:                          # 16 or 32-bit: scaled to 0..255
        img = cv2.normalize(img.astype(np.float32), None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)
    if img.ndim == 2:
        return cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
    return cv2.cvtColor(img, cv2.COLOR_RGB2BGR)


def to_rgb(bgr):
    """A BGR image of OpenCV as RGB array for ImageJ (bio7.show_image)."""
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def cells(width=512, height=384, n=45, seed=1):
    """A synthetic micrograph (BGR): cells of different size and brightness,
    some touching, on an uneven background with noise."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:height, 0:width]
    img = 60 + 40 * xx / width + 20 * np.sin(yy / 60.0)        # uneven illumination
    for _ in range(n):
        cx, cy = rng.uniform(15, width - 15), rng.uniform(15, height - 15)
        a, b = rng.uniform(7, 18), rng.uniform(6, 14)
        angle = rng.uniform(0, np.pi)
        u = (xx - cx) * np.cos(angle) + (yy - cy) * np.sin(angle)
        v = -(xx - cx) * np.sin(angle) + (yy - cy) * np.cos(angle)
        inside = (u / a) ** 2 + (v / b) ** 2
        img += rng.uniform(70, 130) * np.exp(-inside ** 3)        # a soft edge
    img += rng.normal(0, 6, img.shape)
    gray = np.clip(img, 0, 255).astype(np.uint8)
    # a slight color: the cells green-blue (as a fluorescence stain)
    return cv2.merge([gray, (gray * 0.85).astype(np.uint8), (gray * 0.55).astype(np.uint8)])


def image(ask=True):
    """The current ImageJ image as BGR image. Without an open image: an image
    file selected in the file dialog of Bio7 (ask=True), canceled (or
    ask=False): the synthetic cells. The image is shown in ImageJ."""
    try:
        return to_bgr(bio7.image())
    except Exception:
        pass
    path = open_file(IMAGE_FILES) if ask else None
    if path:
        img = cv2.imread(path, cv2.IMREAD_COLOR)
        if img is None:
            raise RuntimeError("OpenCV can't read the image: " + path)
        bio7.show_image(to_rgb(img), os.path.basename(path))
        return img
    img = cells()
    bio7.show_image(to_rgb(img), "Cells (synthetic)")
    return img


def video(path=None, frames=150, width=320, height=240, fps=25.0, seed=2):
    """A synthetic video (AVI, Motion JPEG): cells which move, one divides and a
    slowly rotating background pattern. Returns the path (created once)."""
    path = path or os.path.join(folder(), "moving_cells.avi")
    if os.path.exists(path):
        return path
    rng = np.random.default_rng(seed)
    writer = cv2.VideoWriter(path, cv2.VideoWriter_fourcc(*"MJPG"), fps, (width, height))
    if not writer.isOpened():
        raise RuntimeError("OpenCV can't write the video: " + path)
    n = 7
    pos = rng.uniform([30, 30], [width - 30, height - 30], (n, 2))
    vel = rng.uniform(-1.6, 1.6, (n, 2))
    radius = rng.uniform(7, 13, n)
    color = rng.uniform(140, 255, (n, 3))
    yy, xx = np.mgrid[0:height, 0:width]
    for t in range(frames):
        # the background: a texture which turns slowly (for the optical flow)
        a = t * 0.004
        u = (xx - width / 2) * np.cos(a) + (yy - height / 2) * np.sin(a)
        v = -(xx - width / 2) * np.sin(a) + (yy - height / 2) * np.cos(a)
        bg = 45 + 12 * np.sin(u / 9.0) * np.cos(v / 11.0)
        frame = np.dstack([bg * 1.1, bg, bg * 0.8]).astype(np.uint8)
        for i in range(n):
            pos[i] += vel[i] + rng.normal(0, 0.25, 2)
            for k in range(2):                          # reflected at the border
                hi = (width, height)[k] - radius[i]
                if pos[i, k] < radius[i] or pos[i, k] > hi:
                    vel[i, k] = -vel[i, k]
                    pos[i, k] = np.clip(pos[i, k], radius[i], hi)
            cv2.circle(frame, (int(pos[i, 0]), int(pos[i, 1])), int(radius[i]), color[i].tolist(), -1, cv2.LINE_AA)
        noise = rng.normal(0, 4, frame.shape)
        writer.write(np.clip(frame + noise, 0, 255).astype(np.uint8))
    writer.release()
    return path


def read_frames(path, every=1, max_frames=None, gray=False):
    """The frames of a video (BGR or gray) as list and the frame rate."""
    cap = cv2.VideoCapture(path)
    if not cap.isOpened():
        raise RuntimeError("OpenCV can't open the video: " + path)
    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    frames, i = [], 0
    while max_frames is None or len(frames) < max_frames:
        ok, frame = cap.read()
        if not ok:
            break
        if i % every == 0:
            frames.append(cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY) if gray else frame)
        i += 1
    cap.release()
    return frames, fps
