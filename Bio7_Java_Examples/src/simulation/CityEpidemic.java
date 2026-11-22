package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoAgent;
import com.eco.bio7.simulation.GeoAgentModel;
import com.eco.bio7.simulation.Param;

/**
 * An epidemic in 30 European cities (an SIR model for each city): the
 * infections in a city come from its own infected people and, by travel, from
 * the other cities (weighted by the distance: near cities are more connected).
 * The color shows the share of the infected people (green: none, red: many),
 * the size the population (approximate numbers of the metropolitan areas, in
 * millions).
 * <p>
 * Compile the class (the WorldWind perspective opens and the globe flies to
 * Europe), start it with the Play/Pause button (one step is one day) and export
 * it with "Export Simulation as HTML..." (a map, online).
 */
public class CityEpidemic extends GeoAgentModel<CityEpidemic.City> {

	@Param(label = "Start in", options = { "Milan", "London", "Madrid", "Berlin", "Istanbul" }, restart = true, group = "Start")
	String start = "Milan";

	@Param(label = "Infection rate (per day)", min = 0.05, max = 1, step = 0.01, group = "Disease")
	double beta = 0.3;

	@Param(label = "Recovery rate (per day)", min = 0.02, max = 0.5, step = 0.01, group = "Disease")
	double gamma = 0.1;

	@Param(label = "Travel (coupling of the cities)", min = 0, max = 0.2, step = 0.005, group = "Travel")
	double travel = 0.03;

	@Param(label = "Travel distance (km)", min = 100, max = 2000, step = 50, group = "Travel")
	double travelKm = 600;

	static final String[] NAMES = { "London", "Paris", "Berlin", "Madrid", "Rome", "Vienna", "Warsaw", "Prague", "Budapest", "Stockholm", "Oslo", "Copenhagen", "Amsterdam", "Brussels", "Lisbon",
			"Barcelona", "Milan", "Munich", "Hamburg", "Athens", "Istanbul", "Bucharest", "Helsinki", "Dublin", "Zurich", "Lyon", "Kyiv", "Belgrade", "Sofia", "Riga" };
	static final double[] LON = { -0.128, 2.352, 13.405, -3.704, 12.496, 16.373, 21.012, 14.438, 19.040, 18.069, 10.752, 12.568, 4.904, 4.352, -9.139, 2.173, 9.190, 11.582, 9.993, 23.728, 28.978, 26.103,
			24.938, -6.260, 8.541, 4.835, 30.523, 20.457, 23.322, 24.105 };
	static final double[] LAT = { 51.507, 48.857, 52.520, 40.417, 41.903, 48.208, 52.230, 50.076, 47.498, 59.329, 59.914, 55.676, 52.368, 50.847, 38.722, 41.385, 45.464, 48.135, 53.551, 37.984, 41.008, 44.426,
			60.169, 53.350, 47.377, 45.764, 50.450, 44.787, 42.698, 56.950 };
	static final double[] MILLIONS = { 9.0, 11.1, 3.7, 6.7, 4.3, 1.9, 1.8, 1.3, 1.8, 1.6, 1.0, 1.3, 1.2, 1.2, 2.9, 5.6, 3.1, 1.5, 1.9, 3.2, 15.5, 1.8, 1.3, 1.4, 1.4, 2.3, 2.9, 1.4, 1.3, 0.6 };

	/** A city: the population (millions) and the susceptible, infected and recovered people. */
	static class City extends GeoAgent {
		double n, s, i, r;

		City(String name, double lon, double lat, double millions) {
			super(lon, lat);
			label = name;
			n = millions;
			s = millions;
		}
	}

	/* The distances of the cities (km) */
	double[][] distances;

	public void init() {
		area(35, -12, 62, 32);
		for (int k = 0; k < NAMES.length; k++) {
			City c = new City(NAMES[k], LON[k], LAT[k], MILLIONS[k]);
			c.size = 3 + 2.5 * Math.sqrt(c.n);
			agents.add(c);
		}
		int m = agents.size();
		distances = new double[m][m];
		for (int a = 0; a < m; a++) {
			for (int b = 0; b < m; b++) {
				distances[a][b] = distanceKm(agents.get(a), agents.get(b));
			}
		}
		infect(start, 0.001);
		colors();
	}

	void infect(String name, double millions) {
		for (City c : agents) {
			if (c.label.equals(name)) {
				double x = Math.min(c.s, millions);
				c.s -= x;
				c.i += x;
			}
		}
	}

	public void step() {
		int m = agents.size();
		double[] force = new double[m];
		for (int a = 0; a < m; a++) {
			City c = agents.get(a);
			double f = c.i / c.n;
			for (int b = 0; b < m; b++) {
				if (b != a) {
					City o = agents.get(b);
					double d = distances[a][b] / travelKm;
					f += travel * (o.i / o.n) / (1 + d * d);
				}
			}
			force[a] = f;
		}
		double infected = 0, recovered = 0;
		for (int a = 0; a < m; a++) {
			City c = agents.get(a);
			double newInfections = Math.min(c.s, beta * c.s * force[a]);
			double recoveries = gamma * c.i;
			c.s -= newInfections;
			c.i += newInfections - recoveries;
			c.r += recoveries;
			infected += c.i;
			recovered += c.r;
		}
		colors();
		chart("Infected (millions)", infected);
		chart("Recovered (millions)", recovered);
	}

	/* The color of a city: the share of the infected people */
	void colors() {
		String[] scale = { "#2ca02c", "#bcbd22", "#ff7f0e", "#d62728", "#7b0000" };
		for (City c : agents) {
			double share = c.i / c.n;
			int k = share < 0.001 ? 0 : share < 0.01 ? 1 : share < 0.05 ? 2 : share < 0.15 ? 3 : 4;
			c.color = scale[k];
		}
	}

	@Action(label = "New case in Istanbul")
	public void caseIstanbul() {
		infect("Istanbul", 0.001);
		colors();
	}
}
