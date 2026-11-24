package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridAgent;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * Ants collect food (agents on a grid): the grid is the pheromone (a value
 * which diffuses and evaporates), the ants are agents on it (GridAgent, drawn
 * as arrows in their heading). A searching ant (red) follows the pheromone
 * ahead of it and wiggles; when it finds a pile of food it takes a piece and
 * turns home (green). On the way to the nest it lays pheromone, so the trails
 * to the piles grow while the food lasts. The simulation stops when all food
 * is in the nest (stop()).
 * <p>
 * The agents: addAgent, forward (in the heading, a torus), face (to the nest),
 * cellX/cellY (the cell under the ant). Charts: the food in the nest, the food
 * of the piles and the ants which carry food.
 */
public class Ants extends ValueGridModel {

	@Param(label = "Ants", min = 10, max = 400, restart = true)
	int ants = 150;

	@Param(label = "Evaporation of the pheromone", min = 0, max = 0.2, step = 0.005, group = "Pheromone")
	double evaporation = 0.02;

	@Param(label = "Diffusion of the pheromone", min = 0, max = 0.5, step = 0.01, group = "Pheromone")
	double diffusion = 0.15;

	@Param(label = "Wiggle (degrees)", min = 0, max = 90, group = "Ants")
	double wiggle = 35;

	static final int SIZE = 80;
	static final double NEST_X = 40, NEST_Y = 40, NEST_R = 4, PILE_R = 5, FOOD = 200;
	static final double[] PILE_X = { 14, 66, 58 }, PILE_Y = { 16, 22, 68 };

	/** An ant: carries food or searches. */
	static class Ant extends GridAgent {
		boolean carrying;

		Ant(double x, double y, double heading) {
			super(x, y);
			this.heading = heading;
			shape = "arrow";
			size = 1.8;
			color = "#e53935";
		}
	}

	double[] food = new double[3];
	int delivered;

	public void init() {
		size(SIZE, SIZE);
		range(0, 30);
		colorMap("#1b1b1b", "#3b528b", "#21908c", "#5dc963", "#fde725");
		food = new double[] { FOOD, FOOD, FOOD };
		delivered = 0;
		for (int i = 0; i < ants; i++) {
			addAgent(new Ant(NEST_X, NEST_Y, random(0, 360)));
		}
		show();
	}

	public void step() {
		/* The pheromone diffuses and evaporates */
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				setNext(x, y, (get(x, y) + diffusion * laplacian(x, y) / 4) * (1 - evaporation));
			}
		}
		swap();
		for (GridAgent g : agents) {
			Ant a = (Ant) g;
			if (a.carrying) {
				home(a);
			} else {
				search(a);
			}
		}
		show();
	}

	/* A searching ant: takes food at a pile, else follows the pheromone ahead (left, ahead, right) and wiggles */
	void search(Ant a) {
		int k = pileAt(a.x, a.y);
		if (k >= 0) {
			food[k]--;
			a.carrying = true;
			a.color = "#43a047";
			a.heading = (a.heading + 180) % 360;
			return;
		}
		double best = -1, turn = 0;
		for (int t = -45; t <= 45; t += 45) {
			double v = scent(a, a.heading + t);
			if (v > best) {
				best = v;
				turn = t;
			}
		}
		if (best > 0.05) {
			a.heading += turn;
		}
		a.heading += random(-wiggle, wiggle);
		forward(a, 1);
	}

	/* The pheromone two cells ahead in the direction h */
	double scent(Ant a, double h) {
		return get((int) Math.floor(a.x + 2 * sinDeg(h)), (int) Math.floor(a.y - 2 * cosDeg(h)));
	}

	/* An ant with food: lays pheromone on its way to the nest */
	void home(Ant a) {
		if (distance(a.x, a.y, NEST_X, NEST_Y) < NEST_R) {
			a.carrying = false;
			a.color = "#e53935";
			delivered++;
			a.heading = (a.heading + 180) % 360;
			return;
		}
		int x = a.cellX(), y = a.cellY();
		set(x, y, Math.min(100, get(x, y) + 25));
		face(a, NEST_X, NEST_Y);
		a.heading += random(-wiggle / 3, wiggle / 3);
		forward(a, 1);
	}

	int pileAt(double x, double y) {
		for (int k = 0; k < 3; k++) {
			if (food[k] > 0 && distance(x, y, PILE_X[k], PILE_Y[k]) < PILE_R) {
				return k;
			}
		}
		return -1;
	}

	/* The nest, the piles (smaller with less food) and the charts; the end when all food is in the nest */
	void show() {
		drawCircle(NEST_X, NEST_Y, NEST_R, "#8d6e63cc", "#4e342e");
		int carrying = 0;
		for (GridAgent g : agents) {
			if (((Ant) g).carrying) {
				carrying++;
			}
		}
		for (int k = 0; k < 3; k++) {
			if (food[k] > 0) {
				drawCircle(PILE_X[k], PILE_Y[k], PILE_R * Math.sqrt(food[k] / FOOD), "#ffca28cc", "#ff8f00");
			}
			chart("Food of the piles", "Pile " + (k + 1), food[k]);
		}
		chart("Food in the nest", delivered);
		chart("Ants carrying food", carrying);
		if (food[0] + food[1] + food[2] == 0 && carrying == 0) {
			stop();
		}
	}

	@Action(label = "New food")
	public void newFood() {
		food = new double[] { FOOD, FOOD, FOOD };
		show();
	}
}
