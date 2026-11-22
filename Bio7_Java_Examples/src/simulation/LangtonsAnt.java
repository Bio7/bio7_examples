package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * Langton's ant: an ant turns right on a white cell and left on a black cell,
 * flips the color of the cell and moves one cell forward. After about 10,000
 * steps of chaos one ant builds a "highway"; several ants interact.
 * <p>
 * Compile the class, start the simulation with the Play/Pause button and add
 * ants with the button of the "Settings" view.
 */
public class LangtonsAnt extends GridModel {

	@Param(label = "Grid size (cells)", min = 50, max = 300, restart = true)
	int gridSize = 160;

	@Param(label = "Number of ants", min = 1, max = 20, restart = true)
	int antCount = 1;

	@Param(label = "Moves per step", min = 1, max = 500)
	int moves = 100;

	@Param(label = "Rule (R = right, L = left per color)", options = { "RL", "RLR", "LLRR", "LRRRRRLLR" }, restart = true)
	String rule = "RL";

	/** An ant: the cell and the direction (0 up, 1 right, 2 down, 3 left). */
	static class Ant {
		int x, y, dir;

		Ant(int x, int y, int dir) {
			this.x = x;
			this.y = y;
			this.dir = dir;
		}
	}

	ArrayList<Ant> ants = new ArrayList<>();

	public void init() {
		size(gridSize, gridSize);
		String[] colors = { "#ffffff", "#1d2433", "#2f6fed", "#e4572e", "#2ca02c", "#9467bd", "#f2b134", "#17becf", "#8c564b" };
		String[] used = new String[rule.length()];
		for (int i = 0; i < used.length; i++) {
			used[i] = colors[i];
		}
		palette(used);
		ants = new ArrayList<>();
		ants.add(new Ant(width / 2, height / 2, 0));
		for (int i = 1; i < antCount; i++) {
			addAnt();
		}
	}

	public void step() {
		int states = rule.length();
		for (int m = 0; m < moves; m++) {
			for (Ant a : ants) {
				int s = get(a.x, a.y);
				a.dir = (a.dir + (rule.charAt(s) == 'R' ? 1 : 3)) % 4;
				set(a.x, a.y, (s + 1) % states);
				if (a.dir == 0) {
					a.y = (a.y - 1 + height) % height;
				} else if (a.dir == 1) {
					a.x = (a.x + 1) % width;
				} else if (a.dir == 2) {
					a.y = (a.y + 1) % height;
				} else {
					a.x = (a.x - 1 + width) % width;
				}
			}
		}
		chart("Colored cells", width * height - count(0));
	}

	@Action(label = "Add an ant")
	public void addAnt() {
		ants.add(new Ant(randomInt(width), randomInt(height), randomInt(4)));
	}
}
