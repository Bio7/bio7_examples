package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * Sheep and wolves on a pasture (a discrete model): the sheep eat the grass of
 * their cell, the wolves eat the sheep of their cell, both move to a
 * neighboring cell in each step, lose energy and reproduce. The grass grows
 * again. The animals are drawn as images (drawImage) on the grid of the grass.
 * <p>
 * Charts: the populations (the first chart), the mean energy (a second chart),
 * a phase plot of the populations (chartXY) and the ages of the sheep (a bar
 * chart). The images are in data/sprites of the project (loadImage), the HTML
 * export embeds them.
 */
public class SheepWolvesGrid extends GridModel {

	static final int BARE = 0, GRASS = 1;

	@Param(label = "Sheep at the start", min = 10, max = 200, restart = true, group = "Start")
	int sheep = 70;

	@Param(label = "Wolves at the start", min = 0, max = 60, restart = true, group = "Start")
	int wolves = 12;

	@Param(label = "Regrowth of the grass", min = 0, max = 0.2, step = 0.005, group = "Pasture")
	double regrowth = 0.035;

	@Param(label = "Energy of the grass for a sheep", min = 1, max = 10, group = "Animals")
	int sheepGain = 4;

	@Param(label = "Energy of a sheep for a wolf", min = 5, max = 40, group = "Animals")
	int wolfGain = 20;

	@Param(label = "Reproduction of the sheep", min = 0, max = 0.2, step = 0.005, group = "Animals")
	double sheepBirth = 0.05;

	@Param(label = "Reproduction of the wolves", min = 0, max = 0.2, step = 0.005, group = "Animals")
	double wolfBirth = 0.04;

	/** An animal: its cell, the energy, the age and the direction of its last move. */
	static class Animal {
		int x, y, age;
		double energy, angle;
		boolean wolf;

		Animal(int x, int y, double energy, boolean wolf) {
			this.x = x;
			this.y = y;
			this.energy = energy;
			this.wolf = wolf;
		}
	}

	ArrayList<Animal> animals = new ArrayList<>();

	public void init() {
		size(40, 40);
		palette("#c8b17a", "#7cb342");
		for (int i = 0; i < cells.length; i++) {
			cells[i] = chance(0.6) ? GRASS : BARE;
		}
		loadImage("sheep", "data/sprites/sheep.png");
		loadImage("wolf", "data/sprites/wolf.png");
		animals = new ArrayList<>();
		for (int i = 0; i < sheep; i++) {
			animals.add(new Animal(randomInt(width), randomInt(height), random(2, 2 * sheepGain), false));
		}
		for (int i = 0; i < wolves; i++) {
			animals.add(new Animal(randomInt(width), randomInt(height), random(2, 2 * wolfGain), true));
		}
		show();
	}

	public void step() {
		/* The grass grows again */
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] == BARE && chance(regrowth)) {
				cells[i] = GRASS;
			}
		}
		shuffle(animals);
		ArrayList<Animal> born = new ArrayList<>();
		for (Animal a : animals) {
			if (a.energy <= 0) {
				continue;
			}
			/* A move to a neighboring cell (a torus) */
			int dx = randomInt(3) - 1, dy = randomInt(3) - 1;
			a.x = (a.x + dx + width) % width;
			a.y = (a.y + dy + height) % height;
			if (dx != 0 || dy != 0) {
				/* The angle of the image (y down in the grid) */
				a.angle = headingOf(dx, -dy);
			}
			a.energy -= 1;
			a.age++;
			if (!a.wolf && get(a.x, a.y) == GRASS) {
				set(a.x, a.y, BARE);
				a.energy += sheepGain;
			}
			if (a.wolf) {
				Animal prey = sheepAt(a.x, a.y);
				if (prey != null) {
					prey.energy = 0;
					a.energy += wolfGain;
				}
			}
			if (chance(a.wolf ? wolfBirth : sheepBirth) && a.energy > 2) {
				a.energy /= 2;
				born.add(new Animal(a.x, a.y, a.energy, a.wolf));
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

	Animal sheepAt(int x, int y) {
		for (Animal a : animals) {
			if (!a.wolf && a.energy > 0 && a.x == x && a.y == y) {
				return a;
			}
		}
		return null;
	}

	/* The images of the animals and the charts */
	void show() {
		int nSheep = 0, nWolves = 0;
		double eSheep = 0, eWolves = 0;
		double[] ages = new double[6];
		for (Animal a : animals) {
			drawImage(a.wolf ? "wolf" : "sheep", a.x + 0.5, a.y + 0.5, a.wolf ? 2.2 : 1.8, a.angle);
			if (a.wolf) {
				nWolves++;
				eWolves += a.energy;
			} else {
				nSheep++;
				eSheep += a.energy;
				ages[Math.min(5, a.age / 10)]++;
			}
		}
		chart("Sheep", nSheep);
		chart("Wolves", nWolves);
		chart("Grass / 10", count(GRASS) / 10.0);
		chart("Mean energy", "Sheep", nSheep == 0 ? 0 : eSheep / nSheep);
		chart("Mean energy", "Wolves", nWolves == 0 ? 0 : eWolves / nWolves);
		chartXY("Phase plot", "Sheep - wolves", nSheep, nWolves);
		chartBars("Ages of the sheep (steps)", new String[] { "0-9", "10-19", "20-29", "30-39", "40-49", "50+" }, ages);
	}

	@Action(label = "Release 5 wolves")
	public void releaseWolves() {
		for (int i = 0; i < 5; i++) {
			animals.add(new Animal(randomInt(width), randomInt(height), wolfGain, true));
		}
	}
}
