package python.gui;

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.pybridge.PythonUtil;
import com.eco.bio7.pyffm.PyValue;

import gui.swt.BackgroundJob;
import python.PythonExamples;

/*
 * A Java GUI for Python and OpenCV: the colors of the current ImageJ image
 * (RGB, also gray) are clustered with k-means (cv2.kmeans) in a color space
 * (RGB, Lab, HSV). The image of the cluster colors and the label image are
 * shown in ImageJ, the share of each cluster is listed in the GUI (with its
 * color). Without an open image the synthetic cells of the OpenCV examples.
 *
 * The GUI is in ColorSegmentationGui (WindowBuilder: Open With > WindowBuilder Editor).
 * Requires: opencv-python-headless (Python Packages: the bundle "OpenCV").
 */
public class ColorSegmentation extends com.eco.bio7.compile.Model {

	/** A cluster: its color (RGB) and its share of the pixels (percent). */
	public record Cluster(int r, int g, int b, double percent) {
	}

	private ColorSegmentationGui gui;

	public ColorSegmentation() {
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Color Segmentation");
			parent.setLayout(new FillLayout());
			gui = new ColorSegmentationGui(parent, ColorSegmentation.this, SWT.NONE);
			parent.layout(true);
		});
	}

	public void segment(int k, String colorSpace, double blur, boolean showLabels) {
		gui.setBusy(true);
		BackgroundJob.run("k-means", () -> {
			PythonUtil.exec(PythonExamples.addToPath(PythonExamples.folder("OpenCV")) + code(k, colorSpace, blur, showLabels));
			double[][] rows = ((PyValue.Array) PythonUtil.fromPython("_result")).asDoubleMatrix();
			List<Cluster> clusters = java.util.Arrays.stream(rows).map(r -> new Cluster((int) r[0], (int) r[1], (int) r[2], r[3])).toList();
			Display.getDefault().asyncExec(() -> gui.setClusters(clusters));
			return k + " clusters in " + colorSpace;
		}, status -> {
			if (!gui.isDisposed()) {
				gui.setStatus(status);
				gui.setBusy(false);
			}
		});
	}

	/** The Python code of the segmentation (the result: _result, one row per cluster: r, g, b, percent). */
	public static String code(int k, String colorSpace, double blur, boolean showLabels) {
		return String.format(Locale.ROOT, """
					import sys
					import cv2
					import numpy as np
					import bio7
					sys.modules.pop("cv_samples", None)
					import cv_samples
					_img = cv_samples.image()                       # BGR (the current ImageJ image or the cells)
					if %f > 0:
					    _img = cv2.GaussianBlur(_img, (0, 0), %f)
					_space = %s
					_conv = {"RGB": None, "Lab": cv2.COLOR_BGR2LAB, "HSV": cv2.COLOR_BGR2HSV}[_space]
					_data = (cv2.cvtColor(_img, _conv) if _conv is not None else _img).reshape(-1, 3).astype(np.float32)
					_crit = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 50, 0.2)
					cv2.setRNGSeed(1)
					_, _labels, _ = cv2.kmeans(_data, %d, None, _crit, 3, cv2.KMEANS_PP_CENTERS)
					_labels = _labels.ravel()
					# The mean color (RGB) of each cluster, sorted by size
					_rgb = cv2.cvtColor(_img, cv2.COLOR_BGR2RGB).reshape(-1, 3)
					_order = np.argsort(-np.bincount(_labels, minlength=%d))
					_rank = np.empty_like(_order); _rank[_order] = np.arange(len(_order))
					_labels = _rank[_labels]
					_colors = np.array([_rgb[_labels == c].mean(axis=0) if (_labels == c).any() else [0, 0, 0] for c in range(%d)])
					_share = 100.0 * np.bincount(_labels, minlength=%d) / _labels.size
					_h, _w = _img.shape[:2]
					bio7.show_image(_colors[_labels].reshape(_h, _w, 3).astype(np.uint8), "k-means colors (k = %d, %s)")
					if %s:
					    bio7.show_image((_labels + 1).reshape(_h, _w).astype(np.uint8), "k-means labels", display_range="auto")
					_result = np.column_stack([_colors, _share])
					""", blur, blur, PythonExamples.quote(colorSpace), k, k, k, k, k, colorSpace, showLabels ? "True" : "False");
	}
}
