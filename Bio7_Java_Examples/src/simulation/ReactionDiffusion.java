package simulation;

import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * A reaction-diffusion model (Gray-Scott): two substances U and V diffuse, V
 * converts U into V (U + 2V -> 3V), U is fed and V is removed. Depending on
 * the feed and kill rates spots, stripes or mazes appear. The view shows V.
 * <p>
 * Compile the class (the grid opens in an ImageJ image and the settings in the
 * "Settings" view), start the simulation with the Play/Pause button of the
 * toolbar and create a new start with the Setup button. The menu of the
 * Play/Pause button (or the context menu of the Java editor) exports the model
 * as an HTML page ("Export Simulation as HTML...").
 */
public class ReactionDiffusion extends ValueGridModel {

	@Param(label = "Grid size (cells)", min = 50, max = 300, restart = true)
	int gridSize = 150;

	@Param(label = "Feed rate F", min = 0.01, max = 0.08, step = 0.001)
	double feed = 0.037;

	@Param(label = "Kill rate k", min = 0.04, max = 0.07, step = 0.001)
	double kill = 0.06;

	@Param(label = "Iterations per step", min = 1, max = 30)
	int iterations = 10;

	@Param(label = "Seeds", min = 1, max = 40, restart = true)
	int seeds = 12;

	/* The diffusion rates of U and V */
	static final double DU = 0.16, DV = 0.08;

	/* The substance U (V is in values) */
	double[] u, uNext;

	public void init() {
		size(gridSize, gridSize);
		range(0, 0.4);
		colorMap("#0b0b2b", "#2b3a8c", "#21908c", "#fde725", "#ffffff");
		u = new double[width * height];
		uNext = new double[width * height];
		for (int i = 0; i < u.length; i++) {
			u[i] = 1;
		}
		/* Squares with V at random places */
		for (int s = 0; s < seeds; s++) {
			int cx = randomInt(width), cy = randomInt(height);
			for (int y = -3; y <= 3; y++) {
				for (int x = -3; x <= 3; x++) {
					int i = index(cx + x, cy + y);
					u[i] = 0.5;
					values[i] = 0.25 + 0.05 * random();
				}
			}
		}
	}

	public void step() {
		for (int n = 0; n < iterations; n++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int i = y * width + x;
					double a = u[i], b = values[i];
					double lapU = u[index(x - 1, y)] + u[index(x + 1, y)] + u[index(x, y - 1)] + u[index(x, y + 1)] - 4 * a;
					double lapV = sum4(x, y) - 4 * b;
					double abb = a * b * b;
					uNext[i] = a + DU * lapU - abb + feed * (1 - a);
					next[i] = b + DV * lapV + abb - (feed + kill) * b;
				}
			}
			double[] t = u;
			u = uNext;
			uNext = t;
			swap();
		}
		chart("Mean of V", mean());
	}

	/* The index of a cell on the torus */
	int index(int x, int y) {
		x = (x + width) % width;
		y = (y + height) % height;
		return y * width + x;
	}
}
