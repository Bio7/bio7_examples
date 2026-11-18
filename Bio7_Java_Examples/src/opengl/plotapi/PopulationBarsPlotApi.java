package opengl.plotapi;

import com.eco.bio7.spatial.Plot3D;
import com.eco.bio7.spatial.PlotLegend;

/**
 * A 3D bar plot of a population structure: the number of the individuals in
 * age classes over the years (a population with an aging structure). The
 * export action of the 3D view is enabled (HTML or Blender).
 */
public class PopulationBarsPlotApi {

	public static void main(String[] args) {
		double[] ageClass = new double[10];
		double[] year = new double[8];
		for (int a = 0; a < ageClass.length; a++) {
			ageClass[a] = 5 + 10 * a;
		}
		for (int y = 0; y < year.length; y++) {
			year[y] = 1990 + 5 * y;
		}
		double[][] count = new double[ageClass.length][year.length];
		for (int a = 0; a < ageClass.length; a++) {
			for (int y = 0; y < year.length; y++) {
				/* Fewer births and a longer life over the years */
				double peak = 30 + 3 * y;
				count[a][y] = 1000 * Math.exp(-Math.pow((ageClass[a] - peak) / 28, 2)) * (1 - 0.03 * y) + 150;
			}
		}
		new Plot3D().title("Population Structure\nIndividuals in age classes").axisTitles("Age [years]", "Year", "Individuals")
				.add(Plot3D.bars(ageClass, year, count).colorMap(PlotLegend.VIRIDIS).width(0.8))
				.colorBarTitle("Individuals")
				.exportAction(true)
				.show();
	}
}
