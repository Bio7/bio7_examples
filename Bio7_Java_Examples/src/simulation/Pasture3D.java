package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Agent;
import com.eco.bio7.simulation.AgentModel;
import com.eco.bio7.simulation.Param;

/**
 * Sheep and wolves on a pasture in 3D with animated glTF models: the sheep
 * graze (the animation "graze"), walk and run away from the wolves ("run"), the
 * wolves walk or hunt the nearest sheep outside the fenced pasture. A caught
 * sheep returns into the fence (a new sheep).
 * <p>
 * The models are in data/models of the project (loadModel; agent.model,
 * agent.animation, agent.size); the heading is the direction of the movement.
 * The drawing: the fence on the floor (drawPolygon) and the hunting lines in 3D
 * (drawLine3). The HTML export embeds the models (WebGL).
 */
public class Pasture3D extends AgentModel<Pasture3D.Animal> {

	@Param(label = "Sheep", min = 5, max = 80, restart = true)
	int sheep = 30;

	@Param(label = "Wolves", min = 0, max = 10, restart = true)
	int wolves = 3;

	@Param(label = "Speed of the wolves", min = 0.1, max = 1, step = 0.05)
	double wolfSpeed = 0.42;

	@Param(label = "Distance of the flight (sheep)", min = 2, max = 20, step = 0.5)
	double flight = 9;

	/* The fence of the pasture (x, y of the box) */
	static final double[] FENCE_X = { 6, 26, 30, 8 }, FENCE_Y = { 6, 4, 24, 28 };

	/** An animal: a sheep or a wolf, the steps of grazing, the prey of a wolf. */
	static class Animal extends Agent {
		boolean wolf;
		int rest;
		Animal prey;

		Animal(double x, double y, boolean wolf) {
			super(x, y, 0);
			this.wolf = wolf;
			this.model = wolf ? "wolf" : "sheep";
			this.size = wolf ? 2.8 : 2.2;
			this.heading = 0;
		}
	}

	int catches;

	public void init() {
		box(60, 60, 12);
		loadModel("sheep", "data/models/sheep.glb");
		loadModel("wolf", "data/models/wolf.glb");
		catches = 0;
		for (int i = 0; i < sheep; i++) {
			/* Two thirds in the fence, a third on the open pasture */
			Animal a = i % 3 == 0 ? new Animal(random(32, 56), random(4, 56), false) : new Animal(random(8, 26), random(8, 24), false);
			a.heading = random(0, 360);
			agents.add(a);
		}
		for (int i = 0; i < wolves; i++) {
			Animal w = new Animal(random(40, 58), random(30, 58), true);
			w.heading = random(0, 360);
			agents.add(w);
		}
		draw();
	}

	public void step() {
		int running = 0;
		double distances = 0;
		int hunting = 0;
		for (Animal a : agents) {
			if (a.wolf) {
				Animal p = nearestPrey(a);
				a.prey = p;
				double speed = 0.15;
				a.animation = "walk";
				if (p != null) {
					a.heading = headingOf(p.x - a.x, p.y - a.y);
					speed = wolfSpeed;
					a.animation = "run";
					double d = distance(a, p);
					distances += d;
					hunting++;
					if (d < 1.2) {
						/* A new sheep in the fence */
						p.x = random(10, 24);
						p.y = random(10, 22);
						catches++;
					}
				} else {
					a.heading = (a.heading + random(-25, 25) + 360) % 360;
				}
				double ox = a.x, oy = a.y;
				walk(a, speed);
				if (inside(a.x, a.y)) {
					a.x = ox;
					a.y = oy;
					a.heading = (a.heading + 180) % 360;
				}
			} else {
				Animal w = nearestWolf(a);
				if (w != null && distance(a, w) < flight) {
					a.heading = headingOf(a.x - w.x, a.y - w.y);
					a.animation = "run";
					a.rest = 0;
					walk(a, 0.4);
					running++;
				} else if (a.rest > 0) {
					a.animation = "graze";
					a.rest--;
				} else {
					a.animation = "walk";
					a.heading = (a.heading + random(-20, 20) + 360) % 360;
					walk(a, 0.1);
					if (chance(0.08)) {
						a.rest = randomInt(5, 25);
					}
				}
			}
			keepInBox(a);
		}
		draw();
		chart("Sheep running", running);
		chart("Catches", catches);
		chart("Hunt", "Mean distance wolf - prey", hunting == 0 ? 0 : distances / hunting);
	}

	/* Moves in the heading (0 the direction of y, clockwise) */
	void walk(Animal a, double speed) {
		a.x += speed * sinDeg(a.heading);
		a.y += speed * cosDeg(a.heading);
	}

	Animal nearestPrey(Animal w) {
		Animal best = null;
		double bestD = 25;
		for (Animal a : agents) {
			if (!a.wolf && !inside(a.x, a.y)) {
				double d = distance(w, a);
				if (d < bestD) {
					bestD = d;
					best = a;
				}
			}
		}
		return best;
	}

	Animal nearestWolf(Animal s) {
		Animal best = null;
		double bestD = Double.MAX_VALUE;
		for (Animal a : agents) {
			if (a.wolf) {
				double d = distance(s, a);
				if (d < bestD) {
					bestD = d;
					best = a;
				}
			}
		}
		return best;
	}

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

	void draw() {
		drawPolygon(FENCE_X, FENCE_Y, "#7cb34266", "#f5f0e6");
		for (Animal a : agents) {
			if (a.wolf && a.prey != null) {
				drawLine3(a.x, a.y, 1, a.prey.x, a.prey.y, 1, "#d62728", 2);
			}
		}
	}

	@Action(label = "Open the fence: all sheep out")
	public void scatter() {
		for (Animal a : agents) {
			if (!a.wolf) {
				a.x = random(30, 58);
				a.y = random(2, 58);
			}
		}
	}
}
