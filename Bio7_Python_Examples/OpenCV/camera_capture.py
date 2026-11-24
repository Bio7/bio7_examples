# The camera (or a video file) with OpenCV for the examples of this folder and
# the Java GUI Bio7_Java_Examples/src/python/camera (CameraRecorder): open the
# source, grab frames as numpy arrays for ImageJ (RGB or gray), close it.
#
#   open_camera(0)            the first camera (1, 2, ...: others), or a video file path
#   frame = grab()            the next frame: RGB uint8 (h, w, 3) or gray (h, w); None at the end
#   frame = read_bgr()        the next frame as OpenCV has it (BGR, for cv2.imwrite)
#   info()                    width, height and frame rate
#   close()
#
# macOS: Bio7 needs the permission for the camera. The first time OpenCV
# requests it: macOS shows its dialog (allow it), but this first attempt fails
# (OpenCV can't wait for the answer outside of the main thread): open the
# camera again. Then Bio7 is listed in System Settings > Privacy & Security >
# Camera. Bio7.app must be prepared for this (com.eco.bio7.product/macos/
# prepare_macos_app.sh: usage description, signature with camera entitlement).
#
# Requires: opencv-python-headless

import sys

import cv2

_cap = None
_source = None


def open_camera(source=0, width=None, height=None, fps=None):
    """Opens a camera (its index) or a video file. width, height, fps: requested
    from the camera (it may choose the nearest values it supports)."""
    global _cap, _source
    close()
    if isinstance(source, str) and source.strip().isdigit():
        source = int(source)
    if isinstance(source, int) and sys.platform == "win32":
        cap = cv2.VideoCapture(source, cv2.CAP_DSHOW)      # faster to open on Windows
    else:
        cap = cv2.VideoCapture(source)
    if not cap.isOpened():
        hint = ""
        if isinstance(source, int):
            hint = (" (macOS: the first time macOS asks for the permission: allow it and open the camera again; "
                    "if Bio7 is denied: System Settings > Privacy & Security > Camera)"
                    if sys.platform == "darwin" else " (is a camera connected and not used by another program?)")
        raise RuntimeError("The camera/video %r could not be opened%s" % (source, hint))
    if isinstance(source, int):
        if width:
            cap.set(cv2.CAP_PROP_FRAME_WIDTH, int(width))
        if height:
            cap.set(cv2.CAP_PROP_FRAME_HEIGHT, int(height))
        if fps:
            cap.set(cv2.CAP_PROP_FPS, float(fps))
        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)                # the newest frame, not an old one of a buffer
    _cap, _source = cap, source
    return info()


def info():
    """(width, height, frames per second) of the open source."""
    if _cap is None:
        raise RuntimeError("No camera is open (open_camera())")
    fps = _cap.get(cv2.CAP_PROP_FPS)
    return int(_cap.get(cv2.CAP_PROP_FRAME_WIDTH)), int(_cap.get(cv2.CAP_PROP_FRAME_HEIGHT)), fps if 0 < fps < 500 else 0.0


def grab(gray=False):
    """The next frame as array for ImageJ: RGB uint8 (h, w, 3), or gray uint8
    (h, w). None at the end of a video (or if the camera delivers nothing)."""
    if _cap is None:
        raise RuntimeError("No camera is open (open_camera())")
    ok, frame = _cap.read()
    if not ok:
        return None
    return cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY if gray else cv2.COLOR_BGR2RGB)


def read_bgr():
    """The next frame as OpenCV has it (BGR uint8 (h, w, 3), no conversion: for
    writing image files with cv2.imwrite). None at the end of a video."""
    if _cap is None:
        raise RuntimeError("No camera is open (open_camera())")
    ok, frame = _cap.read()
    return frame if ok else None


def is_camera():
    """True if a camera (not a video file) is open."""
    return isinstance(_source, int)


def close():
    global _cap, _source
    if _cap is not None:
        _cap.release()
    _cap, _source = None, None
