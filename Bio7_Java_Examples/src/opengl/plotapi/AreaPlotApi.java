package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * A stacked area chart: the land use of a region over the years (the areas of
 * the classes stacked). The export action of the 3D view is enabled (HTML or
 * Blender).
 */
public class AreaPlotApi {

	public static void main(String[] args) {
		int n = 31;
		double[] year = new double[n];
		String[] classes = { "Forest", "Grassland", "Fields", "Settlements" };
		double[][] area = new double[classes.length][n];
		for (int i = 0; i < n; i++) {
			year[i] = 1990 + i;
			double t = i / (double) (n - 1);
			area[0][i] = 380 + 40 * t;
			area[1][i] = 300 - 90 * t;
			area[2][i] = 250 - 20 * t + 15 * Math.sin(i * 0.6);
			area[3][i] = 70 + 70 * t * t;
		}
		new Plot3D().title("Land Use over Time\nStacked areas").axisTitles("Year", "", "Area [km^2]")
				.add(Plot3D.area(year, area).names(classes).colors(0x2E7D32, 0x9CCC65, 0xFBC02D, 0x9E9E9E))
				.exportAction(true)
				.show();
	}
}
