package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A digital elevation model (a synthetic landscape with hills and a valley) as
 * a surface with the terrain colors and the contour lines on the floor. The
 * export action in the toolbar of the 3D view is enabled: export the plot as an
 * HTML page (WebGL) or for Blender (glTF and a Python script).
 */
public class TerrainPlotApi {

	public static void main(String[] args) {
		int n = 90;
		double[] east = new double[n], north = new double[n];
		double[][] elevation = new double[n][n];
		for (int i = 0; i < n; i++) {
			east[i] = 5000.0 * i / (n - 1);
			north[i] = 5000.0 * i / (n - 1);
		}
		/* Random hills (sums of Gaussian bumps) and a valley along a sine */
		Random random = new Random(11);
		double[][] hills = new double[12][];
		for (int k = 0; k < hills.length; k++) {
			hills[k] = new double[] { random.nextDouble() * 5000, random.nextDouble() * 5000, 300 + random.nextDouble() * 900, 300 + random.nextDouble() * 600 };
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double h = 200;
				for (double[] hill : hills) {
					double dx = (east[i] - hill[0]) / hill[3], dy = (north[j] - hill[1]) / hill[3];
					h += hill[2] * Math.exp(-(dx * dx + dy * dy) / 2) * 0.6;
				}
				double valley = north[j] - (2500 + 800 * Math.sin(east[i] / 900));
				h -= 250 * Math.exp(-valley * valley / (2 * 250 * 250));
				elevation[i][j] = h;
			}
		}
		new Plot3D().title("Digital Elevation Model\nA synthetic landscape with a valley")
				.axisTitles("Easting [m]", "Northing [m]", "Elevation [m]")
				.aspect(1, 1, 0.35)
				.add(Plot3D.surface(east, north, elevation).colorMap(PlotLegend.TERRAIN).meshStep(6))
				.add(Plot3D.contour(east, north, elevation).levels(15).onFloor(true).colorMap(PlotLegend.TERRAIN))
				.colorBarTitle("Elevation [m]")
				/* The action "Export Plot (WebGL, Blender)" in the toolbar of the 3D view */
				.exportAction(true)
				.show();
	}
}
