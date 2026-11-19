package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A treemap: the number of the species of the orders of insects (rectangles
 * with the area of the numbers, blocks with the height of the numbers, colored
 * with a color map). The export action of the 3D view is enabled (HTML or
 * Blender).
 */
public class TreemapPlotApi {

	public static void main(String[] args) {
		String[] orders = { "Beetles", "Butterflies", "Flies", "Wasps, Bees", "Bugs", "Grasshoppers", "Dragonflies", "Lacewings", "Caddisflies",
				"Mayflies" };
		double[] species = { 6500, 3700, 9200, 9700, 900, 85, 81, 120, 320, 110 };
		new Plot3D().title("Insect Orders\nNumber of the species")
				.add(Plot3D.treemap(species, orders).colorMap(PlotLegend.VIRIDIS).heights(true))
				.colorBarTitle("Species")
				.exportAction(true)
				.show();
	}
}
