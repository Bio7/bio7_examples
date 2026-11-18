package python;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.pybridge.PythonUtil;
import com.eco.bio7.pyffm.PyValue;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;

/*
 * An ImageJ image from Java to Python (numpy), processed in Python and the
 * result back to ImageJ and to the ImageJ results table (Java).
 * Needs numpy and scipy.
 */
public class ImageJToPythonAndBack {

	public static void main(String[] args) throws Exception {
		/* An ImageJ sample image (or use the current image: PythonUtil.imageToPython("img")) */
		ImagePlus imp = IJ.openImage("https://imagej.net/images/blobs.gif");
		if (imp == null) {
			System.out.println("The sample image could not be loaded");
			return;
		}
		Display.getDefault().syncExec(imp::show);

		/* Java (ImageJ) -> Python: a numpy array (height x width) */
		PythonUtil.imageToPython("img", imp);

		PythonUtil.exec("""
				import numpy as np
				from scipy import ndimage as ndi
				img = img.astype(float)
				# The blobs are dark in the inverted LUT of the sample: the higher values are objects
				smooth = ndi.gaussian_filter(img, 2)
				mask = smooth > smooth.mean()
				labels, n = ndi.label(mask)
				index = np.arange(1, n + 1)
				areas = ndi.sum(mask, labels, index)
				centers = np.array(ndi.center_of_mass(mask, labels, index))
				edges = np.hypot(ndi.sobel(smooth, 0), ndi.sobel(smooth, 1))
				""");

		/* Python -> Java (ImageJ): new images */
		PythonUtil.imageFromPython("labels.astype(np.uint16)", "Labels (Python)");
		PythonUtil.imageFromPython("edges.astype(np.float32)", "Edges (Python)");

		/* Python -> Java: the measurements in the ImageJ results table */
		double[] areas = PythonUtil.fromPythonDoubles("areas");
		double[][] centers = ((PyValue.Array) PythonUtil.fromPython("centers")).asDoubleMatrix();
		ResultsTable rt = new ResultsTable();
		for (int i = 0; i < areas.length; i++) {
			rt.incrementCounter();
			rt.addValue("Area", areas[i]);
			rt.addValue("Y", centers[i][0]);
			rt.addValue("X", centers[i][1]);
		}
		Display.getDefault().asyncExec(() -> rt.show("Objects (Python)"));
		System.out.println(areas.length + " objects measured in Python");
	}
}
