# Template matching with OpenCV: finds all copies of one or more templates
# (small images of a shape) in an ImageJ image and selects them in ImageJ
# (ROI Manager, one color per template, a combined selection on the image).
#
# The functions are used by 09_Template_Matching.py and by the Java GUI
# Bio7_Java_Examples/src/python/templatematching (the GUI calls them with
# PythonUtil). The templates are kept in TEMPLATES (name: gray image).
#
#   test_image()                    a test image with shapes, a template of each shape
#   add_template(name)              the selection of the current ImageJ image as template
#   detect(threshold, method, ...)  the matches: (name, x, y, width, height, score)
#   select_in_imagej(matches)       the matches as ROIs in ImageJ
#   run(...)                        detect + select, a numpy array for Java
#
# Requires: opencv-python-headless

import cv2
import numpy as np

import bio7
from bio7 import java

TEMPLATES = {}
METHODS = ["TM_CCOEFF_NORMED", "TM_CCORR_NORMED", "TM_SQDIFF_NORMED"]
COLORS = [(255, 60, 60), (60, 200, 255), (255, 210, 0), (120, 255, 90), (255, 90, 255), (255, 150, 40)]
TEST_TITLE = "Shapes (test)"


def _gray(img):
    """An ImageJ image (any type) as 8-bit gray image."""
    if img.ndim == 3 and img.shape[-1] == 3:
        img = cv2.cvtColor(img, cv2.COLOR_RGB2GRAY)
    elif img.ndim == 3:                    # a stack: the first slice
        img = img[0]
    if img.dtype != np.uint8:
        img = cv2.normalize(img.astype(np.float32), None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)
    return img


def _shape(kind, size, angle=0.0):
    """A white shape on black (a square image of about 1.4 * size)."""
    n = int(size * 1.4) | 1
    img = np.zeros((n, n), np.uint8)
    c, r = n // 2, size / 2
    if kind == "circle":
        cv2.circle(img, (c, c), int(r), 255, -1, cv2.LINE_AA)
    elif kind == "square":
        cv2.rectangle(img, (int(c - r * 0.85), int(c - r * 0.85)), (int(c + r * 0.85), int(c + r * 0.85)), 255, -1)
    elif kind == "triangle":
        pts = np.array([[c, c - r], [c - r * 0.95, c + r * 0.7], [c + r * 0.95, c + r * 0.7]], np.int32)
        cv2.fillPoly(img, [pts], 255, cv2.LINE_AA)
    elif kind == "cross":
        t = max(2, int(r * 0.35))
        cv2.rectangle(img, (int(c - r), c - t), (int(c + r), c + t), 255, -1)
        cv2.rectangle(img, (c - t, int(c - r)), (c + t, int(c + r)), 255, -1)
    elif kind == "ring":
        cv2.circle(img, (c, c), int(r), 255, max(2, int(r * 0.3)), cv2.LINE_AA)
    if angle:
        M = cv2.getRotationMatrix2D((c, c), angle, 1.0)
        img = cv2.warpAffine(img, M, (n, n))
    return img


def test_image(width=640, height=480, seed=3):
    """A test image with circles, squares, triangles, crosses and rings of
    slightly different size and brightness on an uneven, noisy background
    (shown in ImageJ). A template of each shape (except the rings) is added to
    TEMPLATES. Returns the number of shapes."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:height, 0:width]
    img = 50 + 40 * xx / width + 15 * np.sin(yy / 50.0)
    kinds = ["circle", "square", "triangle", "cross", "ring"]
    placed = []
    clean = {}
    for kind in kinds:
        k = 0
        while k < 7:
            x, y = rng.uniform(30, width - 30), rng.uniform(30, height - 30)
            if any(np.hypot(x - px, y - py) < 52 for px, py in placed):
                continue
            size = 28 * rng.uniform(0.9, 1.1)
            s = _shape(kind, size, angle=rng.uniform(-8, 8) if kind != "circle" else 0).astype(np.float64)
            n = s.shape[0]
            x0, y0 = int(x) - n // 2, int(y) - n // 2
            img[y0:y0 + n, x0:x0 + n] += s / 255 * rng.uniform(90, 150)
            placed.append((x, y))
            if k == 0:
                clean[kind] = (x0, y0, n)
            k += 1
    img = cv2.GaussianBlur(img, (0, 0), 1.0) + rng.normal(0, 7, img.shape)
    img = np.clip(img, 0, 255).astype(np.uint8)
    bio7.show_image(img, TEST_TITLE)
    TEMPLATES.clear()
    for kind in kinds[:-1]:                # the rings are not searched (similar to circles: a test)
        x0, y0, n = clean[kind]
        TEMPLATES[kind] = img[y0:y0 + n, x0:x0 + n].copy()
    return len(placed)


def add_template(name, title=None):
    """The rectangle around the selection of the current (or the named) ImageJ
    image as template."""
    imp = java.ij.WindowManager.getImage(title) if title else java.ij.WindowManager.getCurrentImage()
    if imp is None:
        raise RuntimeError("No ImageJ image is open")
    roi = imp.getRoi()
    if roi is None:
        raise RuntimeError("Please select a shape with a rectangle (or any selection) in the image")
    b = roi.getBounds()
    img = _gray(bio7.image(imp.getTitle()))
    t = img[b.y:b.y + b.height, b.x:b.x + b.width].copy()
    if t.size < 16:
        raise RuntimeError("The selection is too small")
    TEMPLATES[name] = t
    imp.deleteRoi()
    return t.shape


def clear_templates():
    TEMPLATES.clear()


def template_names():
    return list(TEMPLATES)


def _overlap(a, b):
    """The intersection over union of two boxes (x, y, w, h)."""
    x1, y1 = max(a[0], b[0]), max(a[1], b[1])
    x2, y2 = min(a[0] + a[2], b[0] + b[2]), min(a[1] + a[3], b[1] + b[3])
    inter = max(0, x2 - x1) * max(0, y2 - y1)
    return inter / float(a[2] * a[3] + b[2] * b[3] - inter)


def detect(threshold=0.7, method="TM_CCOEFF_NORMED", scales=(0.9, 1.0, 1.1), max_overlap=0.3, title=None):
    """All matches of all templates in the current (or the named) ImageJ image
    with a score >= threshold (0..1): a list of (name, x, y, width, height,
    score), the best first. Overlapping matches (of any template) are reduced to
    the best (non-maximum suppression). scales: the template sizes searched."""
    if not TEMPLATES:
        raise RuntimeError("No templates: call test_image() or add_template(name)")
    img = _gray(bio7.image(title))
    m = getattr(cv2, method)
    candidates = []
    for name, templ in TEMPLATES.items():
        for s in scales:
            t = templ if s == 1.0 else cv2.resize(templ, None, fx=s, fy=s, interpolation=cv2.INTER_LINEAR)
            h, w = t.shape
            if h >= img.shape[0] or w >= img.shape[1]:
                continue
            res = cv2.matchTemplate(img, t, m)
            score = 1.0 - res if m == cv2.TM_SQDIFF_NORMED else res
            ys, xs = np.where(score >= threshold)
            for y, x in zip(ys, xs):
                candidates.append((name, int(x), int(y), w, h, float(score[y, x])))
    candidates.sort(key=lambda c: -c[5])
    kept = []
    for c in candidates:
        if all(_overlap(c[1:5], k[1:5]) <= max_overlap for k in kept):
            kept.append(c)
    return kept


def select_in_imagej(matches, title=None):
    """The matches as rectangles in the ROI Manager (named "name n (score)",
    one color per template) and all selected on the image (a combined
    selection: measure, crop, clear, ... all at once)."""
    imp = java.ij.WindowManager.getImage(title) if title else java.ij.WindowManager.getCurrentImage()
    RoiManager = java.ij.plugin.frame.RoiManager
    rm = RoiManager.getInstance() or RoiManager()
    rm.reset()
    names = list(TEMPLATES)
    counts = {}
    for name, x, y, w, h, score in matches:
        counts[name] = counts.get(name, 0) + 1
        roi = java.ij.gui.Roi(x, y, w, h)
        roi.setName("%s %d (%.2f)" % (name, counts[name], score))
        r, g, b = COLORS[names.index(name) % len(COLORS)] if name in names else (255, 255, 255)
        roi.setStrokeColor(java.java.awt.Color(r, g, b))
        roi.setStrokeWidth(2)
        rm.addRoi(roi)
    if matches and imp is not None:
        rm.runCommand(imp, "Show All with labels")
        rm.setSelectedIndexes(np.arange(len(matches), dtype=np.int32))
        rm.runCommand(imp, "Combine")          # one selection of all matches on the image
    return counts


def run(threshold=0.7, method="TM_CCOEFF_NORMED", multiscale=True, title=None):
    """detect() and select_in_imagej(): for Java a numpy array with one row
    per match: template index (in template_names()), x, y, width, height, score."""
    matches = detect(threshold, method, (0.9, 1.0, 1.1) if multiscale else (1.0,), title=title)
    counts = select_in_imagej(matches, title)
    print("Matches:", ", ".join("%s %d" % kv for kv in counts.items()) or "none")
    names = list(TEMPLATES)
    return np.array([[names.index(n), x, y, w, h, s] for n, x, y, w, h, s in matches], np.float64).reshape(-1, 6)
