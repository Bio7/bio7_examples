package opengl.plotapi.dynamic;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SpatialUtil;

/**
 * The colors of the texts of a plot: the title, the axis numbers, the axis
 * titles and the legend (also in the HTML and the Blender export) with a dark
 * background of the 3D view and light lines.
 */
public class TextColorsPlotApi {

	public static void main(String[] args) {
		int n = 50;
		double[] x = new double[n], y = new double[n];
		double[][] z = new double[n][n];
		for (int i = 0; i < n; i++) {
			x[i] = -3 + 6.0 * i / (n - 1);
			y[i] = x[i];
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				z[i][j] = Math.cos(Math.hypot(x[i], y[j]) * 2) * Math.exp(-(x[i] * x[i] + y[j] * y[j]) / 6);
			}
		}
		/* A dark background with light lines (the 3D view) */
		SpatialUtil.setBackgroundColor(25, 28, 38);
		SpatialUtil.setLinesColor(150, 160, 180);
		new Plot3D().title("Ripple\nColored texts").axisTitles("x", "y", "z")
				.textColors(0xFFD54F, 0xB0BEC5, 0x80DEEA, 0xECEFF1)
				.add(Plot3D.surface(x, y, z).colorMap(PlotLegend.VIRIDIS))
				.colorBarTitle("Height")
				.exportAction(true)
				.show();
	}
}
