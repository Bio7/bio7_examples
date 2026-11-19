package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;

/**
 * A box plot: the body mass of a species in five habitats (the quartiles, the
 * median, the whiskers and the outliers). The export action of the 3D view is
 * enabled (HTML or Blender).
 */
public class BoxPlotApi {

	public static void main(String[] args) {
		Random random = new Random(8);
		String[] habitats = { "Forest", "Meadow", "Field", "Wetland", "Urban" };
		double[] means = { 42, 38, 31, 45, 27 }, sds = { 6, 5, 7, 4, 8 };
		double[][] mass = new double[habitats.length][];
		for (int k = 0; k < habitats.length; k++) {
			mass[k] = new double[60];
			for (int i = 0; i < mass[k].length; i++) {
				mass[k][i] = means[k] + sds[k] * random.nextGaussian();
				/* Some outliers */
				if (random.nextDouble() < 0.04) {
					mass[k][i] += 25 * (random.nextBoolean() ? 1 : -1);
				}
			}
		}
		new Plot3D().title("Body Mass in Five Habitats").axisTitles("Habitat", "", "Body mass [g]")
				.add(Plot3D.boxplot(mass, habitats).width(0.55))
				.exportAction(true)
				.show();
	}
}
