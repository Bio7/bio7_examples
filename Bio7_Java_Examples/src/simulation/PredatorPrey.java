package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.Agent;
import com.eco.bio7.simulation.AgentModel;
import com.eco.bio7.simulation.Param;

/**
 * A predator-prey model with agents: rabbits move and reproduce (up to a
 * limit of the habitat), foxes move, hunt the nearest rabbit within their
 * radius, reproduce after a meal and starve without food. The populations
 * oscillate (see the chart of the HTML export).
 * <p>
 * Compile the class (the animals are displayed in the 3D view), start the
 * simulation with the Play/Pause button and change the settings in the
 * "Settings" view.
 */
public class PredatorPrey extends AgentModel<PredatorPrey.Animal> {

	@Param(label = "Rabbits at the start", min = 10, max = 400, restart = true, group = "Start")
	int rabbits = 200;

	@Param(label = "Foxes at the start", min = 1, max = 100, restart = true, group = "Start")
	int foxes = 15;

	@Param(label = "Birth rate", min = 0, max = 0.2, step = 0.005, group = "Rabbits")
	double rabbitBirth = 0.04;

	@Param(label = "Maximum number (habitat)", min = 100, max = 1500, group = "Rabbits")
	int rabbitLimit = 700;

	@Param(label = "Birth after a meal (probability)", min = 0, max = 1, step = 0.01, group = "Foxes")
	double foxBirth = 0.35;

	@Param(label = "Steps without food", min = 5, max = 100, group = "Foxes")
	int foxEnergy = 40;

	@Param(label = "Hunting radius", min = 0.5, max = 8, step = 0.1, group = "Foxes")
	double huntRadius = 2.5;

	/** A rabbit or a fox: the direction of the movement, the energy of a fox. */
	static class Animal extends Agent {
		boolean fox, alive = true;
		int energy;
		double dx, dy;

		Animal(double x, double y, boolean fox, int energy) {
			super(x, y, 1);
			this.fox = fox;
			this.energy = energy;
			color = fox ? "#e4572e" : "#9aa4b2";
		}
	}

	public void init() {
		box(100, 100, 4);
		agentRadius = 0.9;
		for (int i = 0; i < rabbits; i++) {
			agents.add(newAnimal(random() * boxWidth, random() * boxHeight, false, 0));
		}
		for (int i = 0; i < foxes; i++) {
			agents.add(newAnimal(random() * boxWidth, random() * boxHeight, true, foxEnergy));
		}
	}

	Animal newAnimal(double x, double y, boolean fox, int energy) {
		Animal a = new Animal(x, y, fox, energy);
		a.dx = random() - 0.5;
		a.dy = random() - 0.5;
		return a;
	}

	public void step() {
		ArrayList<Animal> born = new ArrayList<>();
		int nRabbits = 0;
		/* The movement: a random turn, a constant speed, the torus */
		for (Animal a : agents) {
			a.dx += (random() - 0.5) * 0.6;
			a.dy += (random() - 0.5) * 0.6;
			double len = Math.sqrt(a.dx * a.dx + a.dy * a.dy);
			double speed = a.fox ? 1.1 : 0.7;
			if (len > 0) {
				a.dx = a.dx / len;
				a.dy = a.dy / len;
			}
			a.x = ((a.x + a.dx * speed) % boxWidth + boxWidth) % boxWidth;
			a.y = ((a.y + a.dy * speed) % boxHeight + boxHeight) % boxHeight;
			if (!a.fox) {
				nRabbits++;
			}
		}
		/* The rabbits reproduce */
		for (Animal a : agents) {
			if (!a.fox && nRabbits + born.size() < rabbitLimit && random() < rabbitBirth) {
				born.add(newAnimal(a.x, a.y, false, 0));
			}
		}
		/* The foxes hunt the nearest rabbit */
		double r2 = huntRadius * huntRadius;
		for (Animal f : agents) {
			if (!f.fox) {
				continue;
			}
			f.energy--;
			Animal prey = null;
			double best = r2;
			for (Animal a : agents) {
				if (!a.fox && a.alive) {
					double ddx = a.x - f.x, ddy = a.y - f.y, d2 = ddx * ddx + ddy * ddy;
					if (d2 < best) {
						best = d2;
						prey = a;
					}
				}
			}
			if (prey != null) {
				prey.alive = false;
				f.energy = foxEnergy;
				if (random() < foxBirth) {
					born.add(newAnimal(f.x, f.y, true, foxEnergy));
				}
			}
		}
		agents.removeIf(a -> !a.alive || (a.fox && a.energy <= 0));
		agents.addAll(born);
		int r = 0, fx = 0;
		for (Animal a : agents) {
			if (a.fox) {
				fx++;
			} else {
				r++;
			}
		}
		chart("Rabbits", r);
		chart("Foxes", fx);
	}
}
