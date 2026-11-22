package simulation.api;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * All methods of a ValueGridModel (a grid of double values, displayed with a
 * color map in ImageJ and in the 2D view of the HTML page): a diffusion with
 * sources and a decay.
 * <p>
 * The fields: width, height, values (index y * width + x), next, wrap and the
 * color map VIRIDIS. The methods: size, range, colorMap, get, set, setNext,
 * swap, sum4, sum8, laplacian, mean.
 */
public class ApiValueGrid extends ValueGridModel {

	@Param(label = "Grid size (cells)", min = 20, max = 250, restart = true, group = "Grid")
	int gridSize = 120;

	@Param(label = "Torus world (wrap)", group = "Grid")
	boolean torus = false;

	@Param(label = "Diffusion", min = 0, max = 0.25, step = 0.005, group = "Process")
	double diffusion = 0.2;

	@Param(label = "Decay", min = 0, max = 0.02, step = 0.0005, group = "Process")
	double decay = 0.003;

	@Param(label = "Neighbors", options = { "4 (laplacian)", "8 (sum8)" }, group = "Process")
	String stencil = "4 (laplacian)";

	@Param(label = "Color map", options = { "Viridis", "Heat", "Gray" }, group = "View")
	String colors = "Viridis";

	public void init() {
		/* size(w, h): a new grid (all values 0) */
		size(gridSize, gridSize);
		/* range(min, max): the values of the first and the last color */
		range(0, 1);
		applyColors();
		source();
	}

	void applyColors() {
		/* colorMap(colors...): the colors from min to max (VIRIDIS: the default) */
		if (colors.equals("Heat")) {
			colorMap("#000004", "#57106e", "#bc3754", "#f98e09", "#fcffa4");
		} else if (colors.equals("Gray")) {
			colorMap("#000000", "#ffffff");
		} else {
			colorMap(VIRIDIS);
		}
	}

	public void step() {
		wrap = torus;
		applyColors();
		boolean four = stencil.startsWith("4");
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				/* get(x, y): the value (at the borders: wrap or 0) */
				double v = get(x, y);
				/* laplacian(x, y) = sum4(x, y) - 4 * value; sum8: the 8 neighbors */
				double change = four ? laplacian(x, y) : (sum8(x, y) - 8 * v) / 2;
				/* setNext(x, y, value): the next value */
				setNext(x, y, v + diffusion * change - decay * v);
			}
		}
		/* swap(): the next values become the values */
		swap();
		/* mean(): the mean of all values */
		chart("Mean value", mean());
		/* values: the array of the values (index y * width + x) */
		chart("Center value", values[(height / 2) * width + width / 2]);
	}

	@Action(label = "A source in the center")
	public void source() {
		for (int y = -3; y <= 3; y++) {
			for (int x = -3; x <= 3; x++) {
				/* set(x, y, value): a value now */
				set(width / 2 + x, height / 2 + y, 1);
			}
		}
	}
}
