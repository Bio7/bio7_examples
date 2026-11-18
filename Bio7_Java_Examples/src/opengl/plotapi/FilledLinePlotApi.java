package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * Filled line plots (ribbons): the light spectra in a lake at five depths (one
 * series for each depth).
 */
public class FilledLinePlotApi {

	public static void main(String[] args) {
		int n = 150;
		double[] wavelength = new double[n];
		for (int i = 0; i < n; i++) {
			wavelength[i] = 400 + 300.0 * i / (n - 1);
		}
		Plot3D plot = new Plot3D().title("Light Spectra in a Lake\nThe red light is absorbed first")
				.axisTitles("Wavelength [nm]", "Depth [m]", "Intensity");
		for (int k = 0; k < 5; k++) {
			double depth = 1 + k * 2;
			double[] y = new double[n], intensity = new double[n];
			for (int i = 0; i < n; i++) {
				y[i] = depth;
				double w = wavelength[i];
				/* The absorption increases with the wavelength */
				double absorption = 0.02 + 0.6 * Math.pow((w - 400) / 300, 3);
				intensity[i] = Math.exp(-Math.pow((w - 520) / 120, 2)) * Math.exp(-absorption * depth * 2);
			}
			plot.add(Plot3D.filledLine(wavelength, y, intensity).label(depth + " m"));
		}
		plot.show();
	}
}
