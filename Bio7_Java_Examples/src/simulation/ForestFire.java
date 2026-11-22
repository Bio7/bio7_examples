package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * A forest fire model (Drossel and Schwabl): trees grow on empty cells, a tree
 * burns if a neighbor burns or if a lightning strikes it, a burning tree
 * leaves an empty cell. The forest burns in fronts of very different sizes.
 * <p>
 * Compile the class, start the simulation with the Play/Pause button and
 * change the settings in the "Settings" view. Export the model with "Export
 * Simulation as HTML..." (the menu of the Play/Pause button).
 */
public class ForestFire extends GridModel {

	static final int EMPTY = 0, TREE = 1, FIRE = 2;

	@Param(label = "Grid size (cells)", min = 50, max = 300, restart = true, group = "Forest")
	int gridSize = 150;

	@Param(label = "Trees at the start", min = 0, max = 1, step = 0.01, restart = true, group = "Forest")
	double density = 0.55;

	@Param(label = "Growth (probability per step)", min = 0, max = 0.1, step = 0.001, group = "Forest")
	double growth = 0.02;

	@Param(label = "Lightning (probability per tree)", min = 0, max = 0.001, step = 0.00001, group = "Fire")
	double lightning = 0.00005;

	@Param(label = "Fire spreads diagonally", group = "Fire")
	boolean diagonal = true;

	public void init() {
		size(gridSize, gridSize);
		palette("#3b2f2a", "#2e8b3a", "#ff8c1a");
		wrap = false;
		for (int i = 0; i < cells.length; i++) {
			cells[i] = random() < density ? TREE : EMPTY;
		}
	}

	public void step() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int s = get(x, y);
				int n;
				if (s == FIRE) {
					n = EMPTY;
				} else if (s == TREE) {
					boolean burning = get(x - 1, y) == FIRE || get(x + 1, y) == FIRE || get(x, y - 1) == FIRE || get(x, y + 1) == FIRE;
					if (diagonal && !burning) {
						burning = get(x - 1, y - 1) == FIRE || get(x + 1, y - 1) == FIRE || get(x - 1, y + 1) == FIRE || get(x + 1, y + 1) == FIRE;
					}
					n = burning || random() < lightning ? FIRE : TREE;
				} else {
					n = random() < growth ? TREE : EMPTY;
				}
				setNext(x, y, n);
			}
		}
		swap();
		chart("Trees", count(TREE));
		chart("Fire", count(FIRE));
	}

	@Action(label = "Lightning strike (center)", group = "Fire")
	public void strike() {
		int x = width / 2, y = height / 2;
		for (int dy = -2; dy <= 2; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				if (get(x + dx, y + dy) == TREE) {
					set(x + dx, y + dy, FIRE);
				}
			}
		}
	}
}
