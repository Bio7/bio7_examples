package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * Sheep and wolves on a field (a continuous model): the grass is a biomass
 * (0 - 1) which grows logistically, the animals move with a heading and a
 * speed. The sheep prefer the directions with more grass, the wolves hunt the
 * nearest sheep in their view. A fenced pasture (a polygon) protects the sheep:
 * the wolves turn back at the fence.
 * <p>
 * The drawing of each step: the animals as images rotated to their heading
 * (drawImage), the hunting lines (drawLine), the view of the first wolf
 * (drawCircle), the fence (drawPolygon) and a text (drawText). Mathematics: the
 * field starts with fractal noise, the sheep choose a direction with
 * randomWeighted, the chart shows the median of the energies. Charts: the
 * populations, the grass (a second chart), a phase plot (chartXY) and the
 * energies of the sheep (chartBars).
 */
public class SheepWolvesField extends ValueGridModel {

	@Param(label = "Sheep at the start", min = 10, max = 150, restart = true, group = "Start")
	int sheep = 60;

	@Param(label = "Wolves at the start", min = 0, max = 30, restart = true, group = "Start")
	int wolves = 8;

	@Param(label = "Growth of the grass", min = 0, max = 0.2, step = 0.005, group = "Field")
	double growth = 0.04;

	@Param(label = "Speed of the wolves", min = 0.2, max = 1.5, step = 0.05, group = "Animals")
	double wolfSpeed = 0.75;

	@Param(label = "View of the wolves (cells)", min = 2, max = 20, step = 0.5, group = "Animals")
	double view = 9;

	@Param(label = "Fenced pasture (a refuge)", group = "Field")
	boolean fence = true;

	/* The corners of the fenced pasture (cells) */
	static final double[] FENCE_X = { 6, 22, 26, 10 }, FENCE_Y = { 8, 5, 20, 24 };

	/** An animal: the position, the heading (degrees, 0 up), the energy and its prey (wolves). */
	static class Animal {
		double x, y, heading, energy;
		boolean wolf;
		Animal prey;

		Animal(double x, double y, double heading, double energy, boolean wolf) {
			this.x = x;
			this.y = y;
			this.heading = heading;
			this.energy = energy;
			this.wolf = wolf;
		}
	}

	ArrayList<Animal> animals = new ArrayList<>();

	public void init() {
		size(60, 45);
		range(0, 1);
		colorMap("#b39b62", "#9ccc65", "#33691e");
		/* A patchy field: fractal noise */
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				set(x, y, 0.2 + 0.8 * fractalNoise(x * 0.09, y * 0.09, 4));
			}
		}
		loadImage("sheep", "data/sprites/sheep.png");
		loadImage("wolf", "data/sprites/wolf.png");
		animals = new ArrayList<>();
		for (int i = 0; i < sheep; i++) {
			animals.add(new Animal(random(0, width), random(0, height), random(0, 360), random(5, 10), false));
		}
		for (int i = 0; i < wolves; i++) {
			animals.add(new Animal(random(30, width), random(0, height), random(0, 360), random(10, 20), true));
		}
		show();
	}

	public void step() {
		/* The logistic growth of the grass */
		for (int i = 0; i < values.length; i++) {
			values[i] = clamp(values[i] + growth * values[i] * (1 - values[i]) + 0.001, 0, 1);
		}
		ArrayList<Animal> born = new ArrayList<>();
		for (Animal a : animals) {
			if (a.energy <= 0) {
				continue;
			}
			if (a.wolf) {
				hunt(a, born);
			} else {
				graze(a, born);
			}
		}
		ArrayList<Animal> alive = new ArrayList<>();
		for (Animal a : animals) {
			if (a.energy > 0) {
				alive.add(a);
			}
		}
		for (Animal a : born) {
			alive.add(a);
		}
		animals = alive;
		show();
	}

	/* A sheep: three directions, weighted by the grass ahead; eats the grass of its cell */
	void graze(Animal a, ArrayList<Animal> born) {
		double[] turns = { -40, 0, 40 }, weights = new double[3];
		for (int k = 0; k < 3; k++) {
			double h = a.heading + turns[k];
			weights[k] = 0.05 + grass(a.x + 2 * sinDeg(h), a.y - 2 * cosDeg(h));
		}
		a.heading = (a.heading + turns[randomWeighted(weights)] + random(-10, 10) + 360) % 360;
		move(a, 0.35);
		int cx = (int) a.x, cy = (int) a.y;
		double eaten = Math.min(0.3, get(cx, cy));
		set(cx, cy, get(cx, cy) - eaten);
		a.energy += eaten * 3 - 0.45;
		if (a.energy > 12 && chance(0.06)) {
			a.energy /= 2;
			born.add(new Animal(a.x, a.y, random(0, 360), a.energy, false));
		}
	}

	/* A wolf: the nearest sheep in the view (outside the fence), else a random walk */
	void hunt(Animal w, ArrayList<Animal> born) {
		Animal best = null;
		double bestD = view;
		for (Animal a : animals) {
			if (!a.wolf && a.energy > 0 && !(fence && inside(a.x, a.y))) {
				double d = distance(w.x, w.y, a.x, a.y);
				if (d < bestD) {
					bestD = d;
					best = a;
				}
			}
		}
		w.prey = best;
		if (best != null) {
			w.heading = headingOf(best.x - w.x, w.y - best.y);
			if (bestD < 0.8) {
				best.energy = 0;
				w.energy += 10;
				w.prey = null;
			}
		} else {
			w.heading = (w.heading + random(-30, 30) + 360) % 360;
		}
		double ox = w.x, oy = w.y;
		move(w, best != null ? wolfSpeed : wolfSpeed * 0.6);
		/* The fence: the wolf turns back */
		if (fence && inside(w.x, w.y)) {
			w.x = ox;
			w.y = oy;
			w.heading = (w.heading + 180) % 360;
		}
		w.energy -= 0.25;
		if (w.energy > 30 && chance(0.04)) {
			w.energy /= 2;
			born.add(new Animal(w.x, w.y, random(0, 360), w.energy, true));
		}
	}

	/* Moves in the heading (0 up, clockwise; y down in the grid) and wraps around */
	void move(Animal a, double speed) {
		a.x = wrapAround(a.x + speed * sinDeg(a.heading), width);
		a.y = wrapAround(a.y - speed * cosDeg(a.heading), height);
	}

	double grass(double x, double y) {
		return get((int) Math.floor(wrapAround(x, width)), (int) Math.floor(wrapAround(y, height)));
	}

	/* The point in the fenced pasture (a ray crossing test) */
	boolean inside(double x, double y) {
		boolean in = false;
		int n = FENCE_X.length;
		for (int i = 0; i < n; i++) {
			int j = (i + n - 1) % n;
			if ((FENCE_Y[i] > y) != (FENCE_Y[j] > y) && x < (FENCE_X[j] - FENCE_X[i]) * (y - FENCE_Y[i]) / (FENCE_Y[j] - FENCE_Y[i]) + FENCE_X[i]) {
				in = !in;
			}
		}
		return in;
	}

	/* The drawing and the charts of the step */
	void show() {
		if (fence) {
			drawPolygon(FENCE_X, FENCE_Y, "#ffffff22", "#f5f0e6");
		}
		int nSheep = 0, nWolves = 0;
		ArrayList<Double> energies = new ArrayList<>();
		boolean first = true;
		for (Animal a : animals) {
			if (a.wolf) {
				nWolves++;
				if (a.prey != null) {
					drawLine(a.x, a.y, a.prey.x, a.prey.y, "#d62728aa", 2);
				}
				if (first) {
					drawCircle(a.x, a.y, view, null, "#ffffff88");
					first = false;
				}
			} else {
				nSheep++;
				energies.add(a.energy);
			}
		}
		for (Animal a : animals) {
			drawImage(a.wolf ? "wolf" : "sheep", a.x, a.y, a.wolf ? 2.6 : 2.0, a.heading);
		}
		drawText(1, 2.5, "Step " + t + ": " + nSheep + " sheep, " + nWolves + " wolves", "#ffffff", 1.6);
		double[] e = new double[energies.size()];
		for (int i = 0; i < e.length; i++) {
			e[i] = energies.get(i);
		}
		double[] bins = new double[5];
		for (double v : e) {
			bins[(int) clamp(Math.floor(v / 4), 0, 4)]++;
		}
		chart("Sheep", nSheep);
		chart("Wolves", nWolves);
		chart("Grass", "Mean biomass", mean());
		chart("Grass", "Median energy of the sheep / 20", e.length == 0 ? 0 : median(e) / 20);
		chartXY("Phase plot", "Sheep - wolves", nSheep, nWolves);
		chartBars("Energies of the sheep", new String[] { "0-4", "4-8", "8-12", "12-16", "16+" }, bins);
	}

	@Action(label = "Release 3 wolves")
	public void releaseWolves() {
		for (int i = 0; i < 3; i++) {
			animals.add(new Animal(random(30, width), random(0, height), random(0, 360), 15, true));
		}
	}
}
