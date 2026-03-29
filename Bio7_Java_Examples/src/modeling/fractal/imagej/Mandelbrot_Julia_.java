package modeling.fractal.imagej;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import java.util.stream.IntStream;

/**
 * ImageJ Plugin: Mandelbrot & Julia Sets as an ImageJ stack. With manual
 * Coordinate Input, Parallel Processing and Smooth Coloring.
 */
public class Mandelbrot_Julia_ implements PlugIn {

	private String[] presets = { "Manual / Custom", "Mandelbrot: Mini-Mandel (-0.743643887, 0.131825904)",
			"Mandelbrot: Seahorse Valley (-0.75, 0.1)", "Julia: Classic Spiral (-0.7, 0.27015)",
			"Julia: Golden Dragon (-0.8, 0.156)" };

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Fractal Pro: Custom Coordinates");
		gd.addChoice("Fractal Type:", new String[] { "Mandelbrot", "Julia" }, "Mandelbrot");
		gd.addChoice("Start from Preset:", presets, presets[1]);

		// Manual Coordinate Input Fields (High Precision)
		gd.addNumericField("Target_X:", -0.743643887, 9);
		gd.addNumericField("Target_Y:", 0.131825904, 9);

		gd.addNumericField("Width (Pixels):", 800, 0);
		gd.addNumericField("Frames:", 40, 0);
		gd.addNumericField("Max_Iterations:", 1500, 0);
		gd.addNumericField("Zoom_Step (per Frame):", 1.15, 2);

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String type = gd.getNextChoice();
		int presetIndex = gd.getNextChoiceIndex();

		// Read manual inputs (they override presets if changed by user)
		double targetX = gd.getNextNumber();
		double targetY = gd.getNextNumber();

		int width = (int) gd.getNextNumber();
		int height = (int) (width * 0.75);
		int frames = (int) gd.getNextNumber();
		int maxIter = (int) gd.getNextNumber();
		double zoomStep = gd.getNextNumber();

		ImageStack stack = new ImageStack(width, height);
		double currentZoom = 1.0;
		final double LOG2 = Math.log(2.0);

		for (int f = 1; f <= frames; f++) {
			IJ.showStatus("Rendering Frame " + f + "/" + frames);
			IJ.showProgress(f, frames);

			FloatProcessor fp = new FloatProcessor(width, height);
			final double zoom = currentZoom;
			final double tx = targetX;
			final double ty = targetY;

			IntStream.range(0, height).parallel().forEach(y -> {
				for (int x = 0; x < width; x++) {
					double zx, zy, cx, cy;

					if (type.equals("Mandelbrot")) {
						zx = 0;
						zy = 0;
						cx = tx + (x - width / 2.0) * (4.0 / (width * zoom));
						cy = ty + (y - height / 2.0) * (4.0 / (width * zoom));
					} else {
						zx = (x - width / 2.0) * (4.0 / (width * zoom));
						zy = (y - height / 2.0) * (4.0 / (width * zoom));
						cx = tx;
						cy = ty;
					}

					int iter = 0;
					// Escape radius 256 for smooth coloring
					while (zx * zx + zy * zy < 256 && iter < maxIter) {
						double tmp = zx * zx - zy * zy + cx;
						zy = 2.0 * zx * zy + cy;
						zx = tmp;
						iter++;
					}

					if (iter < maxIter) {
						double mu = iter + 1 - Math.log(Math.log(Math.sqrt(zx * zx + zy * zy))) / LOG2;
						fp.setf(x, y, (float) mu);
					} else {
						// Prevent void: fill with maxIter instead of 0
						fp.setf(x, y, (float) maxIter);
					}
				}
			});

			stack.addSlice("Z:" + String.format("%.2f", currentZoom), fp);
			currentZoom *= zoomStep;
		}

		ImagePlus imp = new ImagePlus(type + " Custom Zoom", stack);
		imp.getProcessor().resetMinAndMax();
		IJ.run(imp, "Fire", "");
		imp.show();
		IJ.doCommand("Start Animation [\\]");
	}
}
