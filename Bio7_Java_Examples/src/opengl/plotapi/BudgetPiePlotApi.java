package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;

/**
 * A 3D pie chart of the carbon budget of a forest (the shares of the carbon
 * pools) with exploded slices. The export action of the 3D view is enabled
 * (HTML or Blender, e.g., to render the pie with materials).
 */
public class BudgetPiePlotApi {

	public static void main(String[] args) {
		double[] carbon = { 42, 23, 18, 9, 5, 3 };
		String[] pool = { "Soil", "Stems", "Roots", "Litter", "Branches", "Leaves" };
		int[] colors = { 0x6D4C41, 0xA1887F, 0xD7CCC8, 0xFFB74D, 0x8D6E63, 0x66BB6A };
		new Plot3D().title("Carbon Pools of a Forest\nShares of the carbon stock")
				.add(Plot3D.pie(carbon, pool).colors(colors).explode(0.2).height(1.6))
				.exportAction(true)
				.show();
	}
}
