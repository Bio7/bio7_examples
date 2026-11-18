package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A surface plot of a function z = f(x, y) with the contour lines on the floor
 * (two layers of the same grid).
 */
public class SurfacePlotApi {

	public static void main(String[] args) {
		int n = 60;
		double[] x = new double[n], y = new double[n];
		double[][] z = new double[n][n];
		for (int i = 0; i < n; i++) {
			x[i] = -3 + 6.0 * i / (n - 1);
			y[i] = -3 + 6.0 * i / (n - 1);
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				z[i][j] = Math.sin(x[i]) * Math.cos(y[j]) * Math.exp(-(x[i] * x[i] + y[j] * y[j]) / 8);
			}
		}
		new Plot3D().title("z = sin(x) · cos(y) · e^{−r²/8}").axisTitles("x", "y", "z")
				.add(Plot3D.surface(x, y, z).colorMap(PlotLegend.RAINBOW))
				.add(Plot3D.contour(x, y, z).levels(12).onFloor(true).colorMap(PlotLegend.RAINBOW))
				.show();
	}
}
