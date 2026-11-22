package simulation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * An epidemic on a grid (SIR): an infected cell infects each susceptible
 * neighbor with a probability, recovers after some days and is immune for a
 * time (or for ever). Vaccinated cells are immune. The example uses all the
 * kinds of settings: sliders, a number field, a checkbox, choices, a text
 * field, buttons (actions) and groups.
 * <p>
 * The settings of Bio7 are an own panel designed with the WindowBuilder
 * (EpidemicSettings, open it with the WindowBuilder editor): its widgets are
 * bound to the fields and actions by their names. The HTML export creates the
 * same controls from the annotations.
 * <p>
 * Compile the class, start the simulation with the Play/Pause button and
 * export it with "Export Simulation as HTML..." (the menu of the Play/Pause
 * button or the context menu of the editor).
 */
public class Epidemic extends GridModel {

	static final int SUSCEPTIBLE = 0, INFECTED = 1, RECOVERED = 2, VACCINATED = 3;

	/* World */
	@Param(label = "Grid size (cells)", min = 40, max = 250, restart = true, group = "World")
	int gridSize = 120;

	@Param(label = "Torus world (wrap at the borders)", group = "World")
	boolean torus = true;

	@Param(label = "Neighborhood", options = { "Moore (8 cells)", "von Neumann (4 cells)" }, group = "World")
	String neighborhood = "Moore (8 cells)";

	/* Disease */
	@Param(label = "Infection probability (per infected neighbor)", min = 0, max = 1, step = 0.01, group = "Disease")
	double infection = 0.25;

	@Param(label = "Days until the recovery", group = "Disease")
	int recoveryDays = 7;

	@Param(label = "Days of immunity (0: for ever)", group = "Disease")
	int immunityDays = 60;

	/* Start */
	@Param(label = "Infected cells at the start (x,y; x,y)", restart = true, group = "Start")
	String initialInfected = "60,60; 20,100";

	@Param(label = "Colors", options = { "Classic", "Pastel" }, group = "Start")
	String colors = "Classic";

	/* Interventions */
	@Param(label = "Share of a vaccination", min = 0, max = 0.5, step = 0.01, group = "Interventions")
	double vaccineShare = 0.1;

	/* The days in the state (infected or recovered) */
	int[] days, nextDays;

	public void init() {
		size(gridSize, gridSize);
		days = new int[width * height];
		nextDays = new int[width * height];
		applyColors();
		for (String part : initialInfected.split(";")) {
			String[] xy = part.trim().split(",");
			if (xy.length == 2) {
				int x = Integer.parseInt(xy[0].trim()), y = Integer.parseInt(xy[1].trim());
				if (x >= 0 && y >= 0 && x < width && y < height) {
					set(x, y, INFECTED);
				}
			}
		}
	}

	public void step() {
		wrap = torus;
		applyColors();
		boolean moore = neighborhood.startsWith("Moore");
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = y * width + x;
				int s = cells[i];
				int d = days[i] + 1;
				int n = s;
				if (s == SUSCEPTIBLE) {
					d = 0;
					int k = moore ? countNeighbors(x, y, INFECTED)
							: (get(x - 1, y) == INFECTED ? 1 : 0) + (get(x + 1, y) == INFECTED ? 1 : 0) + (get(x, y - 1) == INFECTED ? 1 : 0) + (get(x, y + 1) == INFECTED ? 1 : 0);
					for (int j = 0; j < k && n == SUSCEPTIBLE; j++) {
						if (random() < infection) {
							n = INFECTED;
						}
					}
				} else if (s == INFECTED && d >= recoveryDays) {
					n = RECOVERED;
					d = 0;
				} else if (s == RECOVERED && immunityDays > 0 && d >= immunityDays) {
					n = SUSCEPTIBLE;
					d = 0;
				}
				next[i] = n;
				nextDays[i] = d;
			}
		}
		swap();
		int[] t = days;
		days = nextDays;
		nextDays = t;
		chart("Susceptible", count(SUSCEPTIBLE));
		chart("Infected", count(INFECTED));
		chart("Recovered", count(RECOVERED));
	}

	void applyColors() {
		if (colors.equals("Pastel")) {
			palette("#e8f1fb", "#f28b82", "#a8dab5", "#c5b3e6");
		} else {
			palette("#f2f2f2", "#d62728", "#2ca02c", "#1f77b4");
		}
	}

	@Action(label = "Infect a random cell", group = "Interventions")
	public void infectRandom() {
		set(randomInt(width), randomInt(height), INFECTED);
	}

	@Action(label = "Vaccinate the susceptible cells (share)", group = "Interventions")
	public void vaccinate() {
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] == SUSCEPTIBLE && random() < vaccineShare) {
				cells[i] = VACCINATED;
			}
		}
	}

	/* The settings of Bio7: the panel of the WindowBuilder (not in the HTML export) */
	@Override
	public Composite settings(Composite parent) {
		return new EpidemicSettings(parent, SWT.NONE);
	}
}
