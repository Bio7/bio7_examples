package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoAgent;
import com.eco.bio7.simulation.GeoAgentModel;
import com.eco.bio7.simulation.Param;

/**
 * An ash cloud of Mount Etna (Sicily): the volcano emits ash particles which
 * drift with the wind, spread by turbulence and settle (the heavy particles
 * first). The color shows the age of a particle (from the hot red of a new one
 * to the gray of an old one).
 * <p>
 * Compile the class (the WorldWind perspective opens and the globe flies to
 * Sicily), start it with the Play/Pause button (one step is one hour) and
 * export it with "Export Simulation as HTML..." (a map, online).
 */
public class VolcanicAshCloud extends GeoAgentModel<VolcanicAshCloud.Particle> {

	@Param(label = "Emission (particles per hour)", min = 0, max = 60, group = "Eruption")
	int emission = 20;

	@Param(label = "Wind from", options = { "West", "North-west", "North", "South-west", "South" }, group = "Wind")
	String wind = "North-west";

	@Param(label = "Wind speed (km per hour)", min = 0, max = 80, step = 1, group = "Wind")
	double windSpeed = 30;

	@Param(label = "Turbulence (km per hour)", min = 0, max = 20, step = 0.5, group = "Wind")
	double turbulence = 6;

	@Param(label = "Settling (probability per hour)", min = 0, max = 0.1, step = 0.002, group = "Ash")
	double settling = 0.02;

	static final double ETNA_LON = 14.995, ETNA_LAT = 37.751;
	static final String[] AGE = { "#ff3b1f", "#ff8c42", "#c9a27e", "#8c8c8c", "#5a5a5a" };

	/** An ash particle: its age (hours) and its weight (the heavy ones settle faster). */
	static class Particle extends GeoAgent {
		int age;
		double weight;

		Particle(double lon, double lat, double weight) {
			super(lon, lat);
			this.weight = weight;
		}
	}

	public void init() {
		area(33, 8, 42, 24);
		agentSize = 3;
	}

	public void step() {
		/* The direction to which the wind blows (east and north components) */
		double we = 0, wn = 0;
		if (wind.equals("West")) {
			we = 1;
		} else if (wind.equals("North-west")) {
			we = 0.7071;
			wn = -0.7071;
		} else if (wind.equals("North")) {
			wn = -1;
		} else if (wind.equals("South-west")) {
			we = 0.7071;
			wn = 0.7071;
		} else {
			wn = 1;
		}
		for (int k = 0; k < emission; k++) {
			agents.add(new Particle(ETNA_LON + random(-0.02, 0.02), ETNA_LAT + random(-0.02, 0.02), random(0.2, 1)));
		}
		ArrayList<Particle> settled = new ArrayList<>();
		for (Particle p : agents) {
			p.age++;
			move(p, windSpeed * we + randomGaussian(0, turbulence), windSpeed * wn + randomGaussian(0, turbulence));
			p.color = AGE[Math.min(AGE.length - 1, p.age / 12)];
			if (chance(settling * (0.5 + p.weight)) || p.age > 120) {
				settled.add(p);
			}
		}
		agents.removeIf(p -> settled.contains(p));
		chart("Particles in the air", agents.size());
	}

	@Action(label = "Strong eruption (300 particles)")
	public void eruption() {
		for (int k = 0; k < 300; k++) {
			agents.add(new Particle(ETNA_LON + random(-0.05, 0.05), ETNA_LAT + random(-0.05, 0.05), random(0.2, 1)));
		}
	}
}
