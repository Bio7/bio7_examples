package simulation.api;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * All methods of a GridModel (a grid of cells with integer states, displayed
 * in ImageJ and in the 2D view of the HTML page): a Game of Life with a third
 * state (cells which were alive and died).
 * <p>
 * The fields: width, height (the size), cells (the states, index y * width +
 * x), next (the next states), wrap (a torus). The methods: size, palette, get,
 * set, setNext, swap, countNeighbors (8 neighbors), count4 (4 neighbors),
 * count.
 */
public class ApiGrid extends GridModel {

	static final int DEAD = 0, ALIVE = 1, FORMER = 2;

	@Param(label = "Grid size (cells)", min = 20, max = 300, restart = true)
	int gridSize = 100;

	@Param(label = "Torus world (wrap)")
	boolean torus = true;

	@Param(label = "Neighborhood", options = { "Moore (8 cells)", "von Neumann (4 cells)" })
	String neighborhood = "Moore (8 cells)";

	public void init() {
		/* size(w, h): a new grid (all states 0), width and height are its size */
		size(gridSize, gridSize);
		/* palette(colors...): the colors of the states 0, 1, 2 (#rrggbbaa: with alpha in the browser) */
		palette("#ffffff", "#1d3557", "#a8dadc");
		/* cells: the states (index y * width + x) */
		for (int i = 0; i < cells.length; i++) {
			cells[i] = random() < 0.3 ? ALIVE : DEAD;
		}
	}

	public void step() {
		/* wrap: true, the neighbors at the borders are on the other side (a torus), false: 0 */
		wrap = torus;
		boolean moore = neighborhood.startsWith("Moore");
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				/* get(x, y): the state (at the borders: wrap or 0) */
				int s = get(x, y);
				/* countNeighbors(x, y, state): the 8 neighbors with the state, count4: the 4 neighbors */
				int n = moore ? countNeighbors(x, y, ALIVE) : count4(x, y, ALIVE);
				int born = moore ? 3 : 2;
				int state;
				if (s == ALIVE) {
					state = moore ? (n == 2 || n == 3 ? ALIVE : FORMER) : (n == 1 || n == 2 ? ALIVE : FORMER);
				} else {
					state = n == born ? ALIVE : s;
				}
				/* setNext(x, y, state): the next state (the cells are read unchanged in this step) */
				setNext(x, y, state);
			}
		}
		/* swap(): the next states become the states (all cells change at the same time) */
		swap();
		/* count(state): the number of the cells with the state */
		chart("Alive", count(ALIVE));
		chart("Former", count(FORMER));
	}

	@Action(label = "Add a glider in the center")
	public void glider() {
		int x0 = width / 2, y0 = height / 2;
		/* set(x, y, state): a state now */
		set(x0 + 1, y0, ALIVE);
		set(x0 + 2, y0 + 1, ALIVE);
		set(x0, y0 + 2, ALIVE);
		set(x0 + 1, y0 + 2, ALIVE);
		set(x0 + 2, y0 + 2, ALIVE);
	}
}
