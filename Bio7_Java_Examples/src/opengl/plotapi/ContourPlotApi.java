package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A 3D contour plot: the contour lines of a landscape with two hills at their
 * heights (with given levels).
 */
public class ContourPlotApi {

	public static void main(String[] args) {
		int n = 80;
		double[] x = new double[n], y = new double[n];
		double[][] z = new double[n][n];
		for (int i = 0; i < n; i++) {
			x[i] = 1000.0 * i / (n - 1);
			y[i] = 1000.0 * i / (n - 1);
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double a = Math.pow((x[i] - 300) / 180, 2) + Math.pow((y[j] - 350) / 220, 2);
				double b = Math.pow((x[i] - 700) / 150, 2) + Math.pow((y[j] - 650) / 150, 2);
				z[i][j] = 100 + 250 * Math.exp(-a) + 180 * Math.exp(-b);
			}
		}
		double[] levels = new double[14];
		for (int k = 0; k < levels.length; k++) {
			levels[k] = 120 + 20 * k;
		}
		new Plot3D().title("Contour Lines of a Landscape\nEvery 20 m").axisTitles("Easting [m]", "Northing [m]", "Elevation [m]")
				.add(Plot3D.contour(x, y, z).levels(levels).colorMap(PlotLegend.TERRAIN))
				.colorBarTitle("Elevation [m]")
				.show();
	}
}
