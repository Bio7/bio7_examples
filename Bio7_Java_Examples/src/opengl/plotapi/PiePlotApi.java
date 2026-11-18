package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * A 3D pie chart (without axes): the land use of a region with the percentages
 * at the slices and the labels in the legend.
 */
public class PiePlotApi {

	public static void main(String[] args) {
		double[] area = { 35, 25, 20, 12, 8 };
		String[] use = { "Forest", "Grassland", "Fields", "Settlements", "Water" };
		int[] colors = { 0x2E7D32, 0x9CCC65, 0xFBC02D, 0x9E9E9E, 0x1E88E5 };
		new Plot3D().title("Land Use\nShare of the area")
				.add(Plot3D.pie(area, use).colors(colors).explode(0.12).height(1.3))
				.show();
	}
}
