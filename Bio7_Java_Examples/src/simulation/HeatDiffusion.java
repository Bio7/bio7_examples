package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * Heat diffusion on a plate: heat sources keep their temperature, the heat
 * spreads to the neighbor cells (diffusion) and the plate cools down at a
 * cold border and by the air.
 * <p>
 * Compile the class, start the simulation with the Play/Pause button and add
 * heat sources with the buttons of the "Settings" view.
 */
public class HeatDiffusion extends ValueGridModel {

	@Param(label = "Grid size (cells)", min = 40, max = 250, restart = true, group = "Plate")
	int gridSize = 120;

	@Param(label = "Diffusion", min = 0, max = 0.25, step = 0.005, group = "Plate")
	double diffusion = 0.2;

	@Param(label = "Cooling by the air", min = 0, max = 0.01, step = 0.0001, group = "Plate")
	double cooling = 0.001;

	@Param(label = "Iterations per step", min = 1, max = 20, group = "Plate")
	int iterations = 5;

	@Param(label = "Color map", options = { "Heat", "Viridis", "Gray" }, group = "View")
	String colors = "Heat";

	/* The heat sources (1: a source) */
	int[] source;

	public void init() {
		size(gridSize, gridSize);
		wrap = false;
		range(0, 1);
		source = new int[width * height];
		for (int i = 0; i < 4; i++) {
			addSource();
		}
	}

	public void step() {
		applyColors();
		for (int n = 0; n < iterations; n++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int i = y * width + x;
					double v = values[i];
					next[i] = source[i] == 1 ? 1 : v + diffusion * (sum4(x, y) - 4 * v) - cooling * v;
				}
			}
			swap();
		}
		chart("Mean temperature", mean());
	}

	void applyColors() {
		if (colors.equals("Heat")) {
			colorMap("#000004", "#57106e", "#bc3754", "#f98e09", "#fcffa4");
		} else if (colors.equals("Gray")) {
			colorMap("#000000", "#ffffff");
		} else {
			colorMap(VIRIDIS);
		}
	}

	@Action(label = "Add a heat source", group = "Sources")
	public void addSource() {
		int cx = 5 + randomInt(width - 10), cy = 5 + randomInt(height - 10);
		for (int y = -2; y <= 2; y++) {
			for (int x = -2; x <= 2; x++) {
				source[(cy + y) * width + cx + x] = 1;
				values[(cy + y) * width + cx + x] = 1;
			}
		}
		applyColors();
	}

	@Action(label = "Remove the sources", group = "Sources")
	public void removeSources() {
		for (int i = 0; i < source.length; i++) {
			source[i] = 0;
		}
	}
}
