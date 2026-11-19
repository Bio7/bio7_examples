package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * A radar chart: the soil properties of three sites on six axes (each site a
 * filled polygon, the sites a little above each other). The export action of
 * the 3D view is enabled (HTML or Blender).
 */
public class RadarPlotApi {

	public static void main(String[] args) {
		String[] properties = { "pH", "Carbon", "Nitrogen", "Phosphorus", "Moisture", "Clay" };
		double[][] sites = { { 6.5, 7.8, 5.2, 4.1, 6.0, 3.5 }, { 4.2, 9.1, 7.4, 2.8, 8.5, 6.2 }, { 7.6, 3.2, 3.0, 6.9, 3.4, 8.1 } };
		new Plot3D().title("Soil Properties\nThree sites (scores 0 - 10)")
				.add(Plot3D.radar(sites, properties).names("Forest", "Bog", "Field").max(10).rings(5))
				.exportAction(true)
				.show();
	}
}
