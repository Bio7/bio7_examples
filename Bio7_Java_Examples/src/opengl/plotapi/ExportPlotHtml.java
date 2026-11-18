package opengl.plotapi;

import com.eco.bio7.spatial.HtmlSimulation;
import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * Exports a plot to an HTML page with WebGL (one file without dependencies)
 * which can be opened in a web browser (rotate with the mouse, zoom with the
 * mouse wheel) and displays the plot in the 3D view, too.
 */
public class ExportPlotHtml {

	public static void main(String[] args) throws Exception {
		int n = 50;
		double[] x = new double[n], y = new double[n];
		double[][] z = new double[n][n];
		for (int i = 0; i < n; i++) {
			x[i] = -2 + 4.0 * i / (n - 1);
			y[i] = -2 + 4.0 * i / (n - 1);
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				/* Two peaks of a density (a mixture of two normal distributions) */
				z[i][j] = Math.exp(-((x[i] - 0.7) * (x[i] - 0.7) + (y[j] - 0.5) * (y[j] - 0.5)) * 2)
						+ 0.7 * Math.exp(-((x[i] + 0.8) * (x[i] + 0.8) + (y[j] + 0.6) * (y[j] + 0.6)) * 1.5);
			}
		}
		Plot3D plot = new Plot3D().title("Density of a Mixture\nTwo normal distributions").axisTitles("x", "y", "Density")
				.add(Plot3D.surface(x, y, z).colorMap(PlotLegend.VIRIDIS))
				.add(Plot3D.contour(x, y, z).levels(10).onFloor(true).colorMap(PlotLegend.VIRIDIS));
		plot.show();
		String file = System.getProperty("user.home") + "/Plot3D.html";
		plot.exportHtml(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
