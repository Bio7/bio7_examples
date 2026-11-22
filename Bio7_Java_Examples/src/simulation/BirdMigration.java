package simulation;

import com.eco.bio7.simulation.GeoAgent;
import com.eco.bio7.simulation.GeoAgentModel;
import com.eco.bio7.simulation.Param;

/**
 * The autumn migration of white storks from Central Europe to Africa: the
 * storks of the west fly over Gibraltar to the Sahel, the storks of the east
 * over the Bosporus, the Levant and the Nile valley to East and southern
 * Africa (the migratory divide of the species). Each stork flies a daily
 * distance with some scatter and a drift by the wind, rests on some days and
 * can die on the way. The waypoints are rough positions of the routes.
 * <p>
 * Compile the class (the WorldWind perspective opens and the globe flies to the
 * area), start it with the Play/Pause button (one step is one day) and export
 * it with "Export Simulation as HTML..." (a map, online).
 */
public class BirdMigration extends GeoAgentModel<BirdMigration.Stork> {

	@Param(label = "Number of storks", min = 20, max = 600, restart = true, group = "Population")
	int storks = 240;

	@Param(label = "Divide (longitude between west and east)", min = 8, max = 16, step = 0.5, restart = true, group = "Population")
	double divide = 11;

	@Param(label = "Distance per day (km)", min = 50, max = 400, step = 10, group = "Flight")
	double daily = 180;

	@Param(label = "Rest days (probability per day)", min = 0, max = 0.5, step = 0.01, group = "Flight")
	double rest = 0.15;

	@Param(label = "Wind drift (km per day)", min = 0, max = 80, step = 5, group = "Flight")
	double drift = 25;

	@Param(label = "Mortality (per day)", min = 0, max = 0.02, step = 0.0005, group = "Flight")
	double mortality = 0.002;

	/* The waypoints (longitude, latitude) of the routes */
	static final double[] WEST = { -5.6, 36.0, -6.5, 30.5, -4.0, 20.0, -3.0, 13.5 };
	static final double[] EAST = { 29.0, 41.1, 35.5, 33.0, 33.6, 29.9, 31.2, 24.0, 32.5, 12.0, 33.0, 2.0, 30.0, -12.0, 28.0, -26.0 };

	/** A stork: the route, the next waypoint, alive or arrived. */
	static class Stork extends GeoAgent {
		boolean east, alive = true, arrived;
		int waypoint;

		Stork(double lon, double lat, boolean east) {
			super(lon, lat);
			this.east = east;
			color = east ? "#1f77b4" : "#ff7f0e";
		}
	}

	public void init() {
		area(-30, -20, 60, 45);
		agentSize = 3;
		for (int i = 0; i < storks; i++) {
			/* The breeding area: Central Europe (from France to Poland) */
			double lon = random(3, 22), lat = random(47, 55);
			agents.add(new Stork(lon, lat, lon > divide));
		}
	}

	public void step() {
		int enRoute = 0, arrived = 0, dead = 0;
		for (Stork s : agents) {
			if (!s.alive) {
				dead++;
				continue;
			}
			if (s.arrived) {
				arrived++;
				continue;
			}
			if (chance(mortality)) {
				s.alive = false;
				s.color = "#7f7f7f";
				dead++;
				continue;
			}
			enRoute++;
			if (chance(rest)) {
				continue;
			}
			double[] route = s.east ? EAST : WEST;
			double km = Math.max(0, randomGaussian(daily, daily * 0.25));
			if (moveTowards(s, route[s.waypoint * 2], route[s.waypoint * 2 + 1], km)) {
				s.waypoint++;
				if (s.waypoint * 2 >= route.length) {
					s.arrived = true;
					s.color = s.east ? "#0b4f8a" : "#b35400";
				}
			}
			/* The drift by the wind */
			move(s, randomGaussian(0, drift), randomGaussian(0, drift));
		}
		chart("On the way", enRoute);
		chart("Arrived in Africa", arrived);
		chart("Dead", dead);
	}
}
