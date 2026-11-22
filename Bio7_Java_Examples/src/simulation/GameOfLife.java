package simulation;

import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * The Game of Life of J. H. Conway: a dead cell with 3 living neighbors is
 * born, a living cell with 2 or 3 living neighbors survives, all other cells
 * die or stay dead.
 * <p>
 * Compile the class (the grid opens in an ImageJ image), start the simulation
 * with the Play/Pause button of the toolbar and create a new start with the
 * Setup button. The menu of the Play/Pause button exports the model as an HTML
 * page ("Export Simulation as HTML..."): the same model runs in the web
 * browser, the fields with the annotation Param become the settings.
 */
public class GameOfLife extends GridModel {

	@Param(label = "Grid size (cells)", min = 20, max = 300, restart = true)
	int gridSize = 120;

	@Param(label = "Start density", min = 0, max = 1, step = 0.01, restart = true)
	double density = 0.3;

	@Param(label = "Torus world (wrap at the borders)")
	boolean torus = true;

	public void init() {
		size(gridSize, gridSize);
		palette("#ffffff", "#1f3a93");
		for (int i = 0; i < cells.length; i++) {
			cells[i] = random() < density ? 1 : 0;
		}
	}

	public void step() {
		wrap = torus;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int n = countNeighbors(x, y, 1);
				boolean alive = get(x, y) == 1;
				setNext(x, y, alive ? (n == 2 || n == 3 ? 1 : 0) : (n == 3 ? 1 : 0));
			}
		}
		swap();
		chart("Living cells", count(1));
	}
}
