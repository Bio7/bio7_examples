package opengl.plotapi.charts;

import com.eco.bio7.spatial.Plot3D;

/**
 * A line graph: the monthly precipitation of four stations, each station a
 * line with markers in the depth (the y axis shows the names of the stations).
 * The export action of the 3D view is enabled (HTML or Blender).
 */
public class LineGraphPlotApi {

	public static void main(String[] args) {
		double[] month = new double[12];
		for (int i = 0; i < 12; i++) {
			month[i] = i + 1;
		}
		String[] stations = { "Coast", "Lowland", "Hills", "Mountains" };
		double[][] precipitation = new double[stations.length][12];
		for (int k = 0; k < stations.length; k++) {
			for (int i = 0; i < 12; i++) {
				/* More rain in the mountains, a summer maximum and a winter maximum at the coast */
				double summer = Math.exp(-Math.pow((month[i] - 7) / 2.2, 2));
				double winter = Math.exp(-Math.pow((month[i] - 12.5) / 2.5, 2)) + Math.exp(-Math.pow((month[i] + 0.5) / 2.5, 2));
				precipitation[k][i] = 40 + 25 * k + (20 + 30 * k) * summer + (k == 0 ? 45 : 10) * winter;
			}
		}
		new Plot3D().title("Monthly Precipitation\nFour stations").axisTitles("Month", "Station", "Precipitation [mm]")
				.add(Plot3D.lineGraph(month, precipitation).names(stations).markers(true, 0.1))
				.exportAction(true)
				.show();
	}
}
