package simulation.api;

import java.util.ArrayList;
import java.util.stream.IntStream;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * The basics of the simulation API: the life cycle (init, step), the settings
 * (@Param with all kinds of controls), the buttons (@Action), the random
 * numbers, the mathematical and statistical functions, differential equations
 * (rk4), charts and the parallel computation. The methods are inherited from
 * the class Simulation (the superclass of all models), this example uses a
 * ValueGridModel to display random numbers.
 * <p>
 * Compile the class, start it with the Play/Pause button and look at the
 * console: init() prints examples of the functions. Export it with "Export
 * Simulation as HTML...".
 */
public class ApiBasics extends ValueGridModel {

	/*
	 * ---------- @Param: the settings (in Bio7 and in the HTML page) ----------
	 * A number with min and max: a slider (step: the steps of the slider), a number
	 * without min and max: a number field, a boolean: a checkbox, a String with
	 * options: a choice, a String without options: a text field. restart = true:
	 * a change calls init() again, group = "...": a heading above the settings.
	 */
	@Param(label = "Grid size (cells)", min = 10, max = 200, restart = true, group = "Grid")
	int gridSize = 60;

	@Param(label = "Seed of the random numbers", restart = true, group = "Grid")
	int seedNumber = 42;

	@Param(label = "Distribution", options = { "Uniform", "Gaussian", "Exponential", "Poisson" }, group = "Random numbers")
	String distribution = "Gaussian";

	@Param(label = "Mixing of the new numbers", min = 0, max = 1, step = 0.01, group = "Random numbers")
	double mixing = 0.3;

	@Param(label = "Parallel computation (randomAt)", group = "Random numbers")
	boolean parallel = false;

	@Param(label = "Growth rate of the prey", group = "Differential equations")
	double preyGrowth = 0.6;

	@Param(label = "A text (printed by an action)", group = "Text")
	String note = "Hello from Bio7";

	/* The state of the predator-prey system (rk4) */
	double[] state;
	ArrayList<Integer> numbers;

	/* init(): the start state (after the compilation, the Setup button, a restart) */
	public void init() {
		/* seed(n): the sequence of the random numbers (the same in Bio7 and the browser) */
		seed(seedNumber);
		/* The grid of the values (ValueGridModel, see ApiValueGrid) */
		size(gridSize, gridSize);
		range(0, 1);
		colorMap("#0b1d51", "#3f88c5", "#f4f4f4", "#ffb000");
		state = new double[] { 10, 5 };
		numbers = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			numbers.add(i);
		}
		/* Examples of the functions (printed to the console of Bio7 and of the browser) */
		System.out.println("random() = " + random() + ", random(5, 10) = " + random(5, 10));
		System.out.println("randomInt(6) = " + randomInt(6) + ", randomInt(1, 7) = " + randomInt(1, 7) + ", chance(0.5) = " + chance(0.5));
		System.out.println("randomGaussian() = " + randomGaussian() + ", randomGaussian(100, 15) = " + randomGaussian(100, 15));
		System.out.println("randomExponential(2) = " + randomExponential(2) + ", randomPoisson(4) = " + randomPoisson(4));
		System.out.println("randomElement(numbers) = " + randomElement(numbers) + ", randomAt(7) = " + randomAt(7) + ", randomAt(7, 1) = " + randomAt(7, 1));
		System.out.println("clamp(1.5, 0, 1) = " + clamp(1.5, 0, 1) + ", lerp(0, 10, 0.25) = " + lerp(0, 10, 0.25) + ", mapRange(5, 0, 10, 100, 200) = " + mapRange(5, 0, 10, 100, 200));
		System.out.println("wrapAround(-1, 100) = " + wrapAround(-1, 100) + ", sq(3) = " + sq(3) + ", distance(0, 0, 3, 4) = " + distance(0, 0, 3, 4) + ", distance3(0, 0, 0, 1, 2, 2) = "
				+ distance3(0, 0, 0, 1, 2, 2));
		System.out.println("logistic(0) = " + logistic(0) + ", logisticGrowth(50, 0.1, 100) = " + logisticGrowth(50, 0.1, 100));
		double[] sample = { 2, 4, 4, 4, 5, 5, 7, 9 };
		int[] counts = { 1, 2, 3 };
		System.out.println("sum = " + sum(sample) + ", mean = " + mean(sample) + ", variance = " + variance(sample) + ", sd = " + sd(sample) + ", min = " + min(sample) + ", max = " + max(sample)
				+ ", sum(int[]) = " + sum(counts) + ", mean(int[]) = " + mean(counts));
	}

	/* A random number of the chosen distribution (0 - 1 for the grid) */
	double draw() {
		if (distribution.equals("Uniform")) {
			return random();
		}
		if (distribution.equals("Gaussian")) {
			return clamp(randomGaussian(0.5, 0.15), 0, 1);
		}
		if (distribution.equals("Exponential")) {
			return clamp(randomExponential(4), 0, 1);
		}
		return clamp(randomPoisson(3) / 10.0, 0, 1);
	}

	/* step(): one time step (the Play/Pause button calls it repeatedly), t is the number of the step */
	public void step() {
		if (parallel) {
			/*
			 * Parallel threads of Java (a loop in the browser): randomAt(index, k) is
			 * computed from the seed, t and the index, the same in any order of the
			 * threads. Each thread writes only its own cells.
			 */
			IntStream.range(0, height).parallel().forEach(y -> {
				for (int x = 0; x < width; x++) {
					int i = y * width + x;
					values[i] = lerp(values[i], randomAt(i), mixing);
				}
			});
		} else {
			/* random() and the distributions are one sequence: only in one thread */
			for (int i = 0; i < values.length; i++) {
				values[i] = lerp(values[i], draw(), mixing);
			}
		}
		/* rk4: one step of a system of differential equations (Lotka-Volterra) */
		for (int k = 0; k < 10; k++) {
			state = rk4((time, v) -> new double[] { preyGrowth * v[0] - 0.05 * v[0] * v[1], 0.02 * v[0] * v[1] - 0.4 * v[1] }, t + k * 0.1, state, 0.1);
		}
		/* chart(name, value): a series of the chart (one value per step) */
		chart("Mean of the grid", mean());
		chart("Standard deviation of the grid", sd(values));
		chart("Prey (rk4)", state[0] / 50);
		chart("Predators (rk4)", state[1] / 50);
	}

	/* @Action: a button (a public method without parameters) */
	@Action(label = "Shuffle and print the numbers")
	public void shuffleNumbers() {
		/* shuffle(list): a random order (with the random numbers of the model) */
		shuffle(numbers);
		System.out.println(note + ": " + numbers);
	}

	@Action(label = "Clear the grid")
	public void clearGrid() {
		for (int i = 0; i < values.length; i++) {
			values[i] = 0;
		}
	}

	/*
	 * An own settings panel in Bio7 (e.g., designed with the WindowBuilder): override
	 * settings(Composite parent) and return the panel, its widgets are bound to the
	 * Param fields and Action methods by their names (see Epidemic and
	 * EpidemicSettings in the package simulation). The HTML page creates the
	 * controls of the annotations.
	 */
}
