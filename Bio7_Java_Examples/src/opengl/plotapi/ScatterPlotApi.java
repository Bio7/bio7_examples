package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;

/**
 * A 3D scatter plot with the plot API: three groups of measurements (each group
 * a layer with a label for the legend). Run the class (main method) to display
 * the plot in the 3D view.
 */
public class ScatterPlotApi {

	public static void main(String[] args) {
		Random random = new Random(42);
		double[][] centers = { { 4.5, 3.2, 1.5 }, { 6.0, 2.8, 4.3 }, { 6.6, 3.0, 5.6 } };
		String[] names = { "Species A", "Species B", "Species C" };
		Plot3D plot = new Plot3D().title("Three Species\nSepal length, sepal width and petal length")
				.axisTitles("Sepal length [cm]", "Sepal width [cm]", "Petal length [cm]");
		for (int g = 0; g < 3; g++) {
			int n = 60;
			double[] x = new double[n], y = new double[n], z = new double[n];
			for (int i = 0; i < n; i++) {
				x[i] = centers[g][0] + 0.4 * random.nextGaussian();
				y[i] = centers[g][1] + 0.3 * random.nextGaussian();
				z[i] = centers[g][2] + 0.4 * random.nextGaussian();
			}
			/* One layer for each group (the colors of the categories and the legend) */
			plot.add(Plot3D.scatter(x, y, z).label(names[g]).size(0.15));
		}
		plot.show();
	}
}
