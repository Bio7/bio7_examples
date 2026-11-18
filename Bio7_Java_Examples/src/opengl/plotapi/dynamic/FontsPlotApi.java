package opengl.plotapi.dynamic;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * The fonts of the texts of a plot: the title, the axis numbers, the axis
 * titles and the legend (also in the HTML and the Blender export). The
 * installed font families are listed in the console.
 */
public class FontsPlotApi {

	public static void main(String[] args) {
		String[] fonts = Plot3D.availableFonts();
		System.out.println(fonts.length + " fonts are available, e.g.: "
				+ String.join(", ", java.util.Arrays.copyOf(fonts, Math.min(10, fonts.length))));
		int n = 40;
		double[] x = new double[n], y = new double[n];
		double[][] z = new double[n][n];
		for (int i = 0; i < n; i++) {
			x[i] = -3 + 6.0 * i / (n - 1);
			y[i] = x[i];
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				z[i][j] = Math.sin(x[i]) * Math.cos(y[j]);
			}
		}
		/* title, axis numbers, axis titles, legend (null = the default font) */
		new Plot3D().title("Fonts of a Plot\nGeorgia, Courier New, Times New Roman")
				.axisTitles("Length [cm]", "Width [cm]", "Height")
				.fonts("Georgia", "Courier New", "Times New Roman", "Courier New")
				.add(Plot3D.surface(x, y, z).colorMap(PlotLegend.VIRIDIS))
				.colorBarTitle("Height")
				.exportAction(true)
				.show();
	}
}
