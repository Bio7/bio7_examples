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
 * <p>
 * The mouse and the keyboard in the view (also in the HTML export): a click
 * adds a heat source at the cell, a drag warms the plate along the way, the key
 * c cools the plate, r removes the sources (the methods click, drag and key).
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

	/** A click in the view: a heat source (5 x 5 cells) at the cell. */
	public void click(double x, double y) {
		heat((int) x, (int) y, 2, true);
	}

	/** A drag in the view: heat along the way (3 x 3 cells, no source). */
	public void drag(double x, double y) {
		heat((int) x, (int) y, 1, false);
	}

	/** A key in the view: c cools the plate (not the sources), r removes the sources. */
	public void key(String key) {
		if (key.equals("c")) {
			for (int i = 0; i < values.length; i++) {
				values[i] = source[i] == 1 ? 1 : 0;
			}
		} else if (key.equals("r")) {
			removeSources();
		}
	}

	/* The cells around (cx, cy) get the temperature 1 (a source: they keep it) */
	void heat(int cx, int cy, int r, boolean permanent) {
		for (int y = cy - r; y <= cy + r; y++) {
			for (int x = cx - r; x <= cx + r; x++) {
				if (x >= 0 && y >= 0 && x < width && y < height) {
					values[y * width + x] = 1;
					if (permanent) {
						source[y * width + x] = 1;
					}
				}
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
