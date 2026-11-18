package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A surface on logarithmic axes: the species area relation S = c A^z with an
 * exponent z which increases with the isolation of islands (the theory of
 * island biogeography). The area and the number of the species are
 * logarithmic (the surface is a plane for each isolation). The export action
 * of the 3D view is enabled (HTML or Blender).
 */
public class SpeciesAreaLogPlotApi {

	public static void main(String[] args) {
		int n = 40;
		double[] area = new double[n], isolation = new double[n];
		double[][] species = new double[n][n];
		for (int i = 0; i < n; i++) {
			/* 0.01 - 10000 km^2 (logarithmic steps) */
			area[i] = Math.pow(10, -2 + 6.0 * i / (n - 1));
			isolation[i] = 1000.0 * i / (n - 1);
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double z = 0.15 + 0.2 * isolation[j] / 1000;
				species[i][j] = 20 * Math.pow(area[i], z);
			}
		}
		new Plot3D().title("Species Area Relation\nS = c · A^z, z increases with the isolation")
				.axisTitles("Area [km^2]", "Isolation [km]", "Species").logAxes(true, false, true).numberFormats("10^", null, null)
				.add(Plot3D.surface(area, isolation, species).colorMap(PlotLegend.VIRIDIS))
				.colorBarTitle("Species")
				.exportAction(true)
				.show();
	}
}
