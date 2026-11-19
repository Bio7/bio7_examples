package opengl.plotapi;

import java.util.Random;

import com.eco.bio7.spatial.Plot3D;

/**
 * A violin plot: the distributions of the leaf length of four populations (one
 * with two modes). The density is a solid of revolution with the quartiles and
 * the median inside. The export action of the 3D view is enabled (HTML or
 * Blender).
 */
public class ViolinPlotApi {

	public static void main(String[] args) {
		Random random = new Random(12);
		String[] populations = { "North", "South", "East", "West" };
		double[][] length = new double[populations.length][200];
		for (int i = 0; i < 200; i++) {
			length[0][i] = 50 + 6 * random.nextGaussian();
			length[1][i] = 62 + 9 * random.nextGaussian();
			/* Two modes (two types of leaves) */
			length[2][i] = random.nextBoolean() ? 42 + 4 * random.nextGaussian() : 66 + 5 * random.nextGaussian();
			/* Skewed */
			length[3][i] = 40 + 20 * Math.exp(0.4 * random.nextGaussian());
		}
		new Plot3D().title("Leaf Length of Four Populations").axisTitles("Population", "", "Leaf length [mm]")
				.add(Plot3D.violin(length, populations).width(0.8))
				.exportAction(true)
				.show();
	}
}
