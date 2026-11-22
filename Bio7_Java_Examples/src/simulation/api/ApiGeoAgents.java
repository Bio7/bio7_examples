package simulation.api;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoAgent;
import com.eco.bio7.simulation.GeoAgentModel;
import com.eco.bio7.simulation.Param;

/**
 * All methods of a GeoAgentModel (agents on the Earth: points or 3D shapes on
 * the WorldWind globe of Bio7, points on the map of the HTML page): travelers
 * between European cities and drones which circle above them.
 * <p>
 * The fields of a GeoAgent: lon, lat, color, size (pixels), label, altitude
 * (meters), heading (degrees). The fields of the model: agents, south, west,
 * north, east, agentSize, agentColor, agentShape, agentSizeMeters, viewPitch.
 * The methods: area, distanceKm, move, moveTowards, neighborsKm and the
 * functions cosLat, cosDeg, sinDeg (the same digits in Java and JavaScript) and
 * EARTH_RADIUS.
 */
public class ApiGeoAgents extends GeoAgentModel<ApiGeoAgents.Traveler> {

	@Param(label = "Travelers", min = 5, max = 200, restart = true)
	int travelers = 40;

	@Param(label = "Speed (km per step)", min = 5, max = 200, step = 5)
	double speed = 60;

	@Param(label = "Meeting distance (km)", min = 10, max = 300, step = 10)
	double meeting = 80;

	static final String[] CITIES = { "Berlin", "Paris", "Rome", "Madrid", "Vienna", "Stockholm" };
	static final double[] LON = { 13.405, 2.352, 12.496, -3.704, 16.373, 18.069 };
	static final double[] LAT = { 52.520, 48.857, 41.903, 40.417, 48.208, 59.329 };

	/** A traveler: the index of the target city (the GeoAgent fields: lon, lat, color, size, label, altitude, heading). */
	static class Traveler extends GeoAgent {
		int target;
		boolean drone;

		Traveler(double lon, double lat) {
			super(lon, lat);
		}
	}

	public void init() {
		/* area(south, west, north, east): the area of the view (the globe flies to it, the map shows it) */
		area(36, -12, 62, 28);
		/* agentSize and agentColor: the points (pixels) of the agents without an own size or color */
		agentSize = 4;
		agentColor = "#1f77b4";
		/* agentShape: "point", "sphere", "cone", "box" or "cylinder" (3D shapes on the globe, agentSizeMeters) */
		agentShape = "point";
		agentSizeMeters = 4000;
		/* viewPitch: the angle of the view of the globe (0: from above) */
		viewPitch = 0;
		for (int i = 0; i < travelers; i++) {
			int c = randomInt(CITIES.length);
			Traveler a = new Traveler(LON[c] + random(-0.3, 0.3), LAT[c] + random(-0.3, 0.3));
			a.target = randomInt(CITIES.length);
			agents.add(a);
		}
		/* Three drones: a label, an altitude (meters) and a heading (degrees) */
		for (int k = 0; k < 3; k++) {
			Traveler d = new Traveler(LON[k], LAT[k]);
			d.drone = true;
			d.label = "Drone " + (k + 1);
			d.altitude = 2000;
			d.color = "#d62728";
			d.size = 6;
			agents.add(d);
		}
		/* distanceKm(lon1, lat1, lon2, lat2): the great circle distance; EARTH_RADIUS in km */
		System.out.println("Berlin - Paris: " + distanceKm(LON[0], LAT[0], LON[1], LAT[1]) + " km (Earth radius " + EARTH_RADIUS + " km)");
	}

	public void step() {
		int meetings = 0;
		for (Traveler a : agents) {
			if (a.drone) {
				/* cosDeg(deg), sinDeg(deg): a circle of 30 km around the city, heading along the circle */
				double angle = t * 12 + 120 * agents.indexOf(a);
				int c = agents.indexOf(a) - travelers;
				a.lon = LON[c] + 30 / (111.32 * cosLat(LAT[c])) * sinDeg(angle);
				a.lat = LAT[c] + 30 / 110.574 * cosDeg(angle);
				a.heading = angle + 90;
				continue;
			}
			/* moveTowards(agent, lon, lat, km): at most km towards the position, true if reached */
			if (moveTowards(a, LON[a.target], LAT[a.target], speed)) {
				a.target = randomInt(CITIES.length);
			}
			/* move(agent, eastKm, northKm): a random step */
			move(a, random(-5, 5), random(-5, 5));
			/* neighborsKm(agent, km): the other agents within the distance; distanceKm(a, b) of two agents */
			ArrayList<Traveler> near = neighborsKm(a, meeting);
			a.color = near.size() > 3 ? "#ff7f0e" : null;
			meetings += near.size();
		}
		chart("Mean number of travelers nearby", meetings / (double) travelers);
	}

	@Action(label = "Everybody to Vienna")
	public void toVienna() {
		for (Traveler a : agents) {
			a.target = 4;
		}
	}
}
