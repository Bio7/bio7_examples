package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoAgent;
import com.eco.bio7.simulation.GeoAgentModel;
import com.eco.bio7.simulation.Param;

/**
 * Hot air balloons over Cappadocia (Turkey): the balloons start at sunrise near
 * Göreme, climb to their cruising altitude, drift with the wind and land after
 * their flight time. The wind turns and becomes stronger with the altitude:
 * the pilots steer by choosing the altitude, so the balloons spread into
 * different directions.
 * <p>
 * The model runs only on the WorldWind globe of Bio7 (no ImageJ): the balloons
 * are OpenGL 3D shapes (lighted ellipsoids) in their altitude above the
 * terrain, seen in an oblique view. Compile the class (the WorldWind
 * perspective opens and the globe flies to Cappadocia), start it with the
 * Play/Pause button (one step is one minute) and export it with "Export
 * Simulation as HTML..." (a map with points, online).
 */
public class HotAirBalloons extends GeoAgentModel<HotAirBalloons.Balloon> {

	@Param(label = "Number of balloons", min = 5, max = 150, restart = true, group = "Balloons")
	int balloons = 60;

	@Param(label = "Maximum cruising altitude (m)", min = 200, max = 2000, step = 50, restart = true, group = "Balloons")
	double maxAltitude = 1200;

	@Param(label = "Flight time (minutes)", min = 20, max = 120, group = "Balloons")
	int flightTime = 60;

	@Param(label = "Wind from at the ground (degrees, 0 north)", min = 0, max = 360, step = 5, group = "Wind")
	double windFrom = 40;

	@Param(label = "Wind speed at the ground (m/s)", min = 0, max = 8, step = 0.1, group = "Wind")
	double windSpeed = 2;

	@Param(label = "Turning of the wind (degrees per 1000 m)", min = -120, max = 120, step = 5, group = "Wind")
	double veering = 60;

	static final double START_LON = 34.829, START_LAT = 38.643;
	static final String[] COLORS = { "#e63946", "#f4a261", "#e9c46a", "#2a9d8f", "#457b9d", "#9b5de5", "#f15bb5", "#00bbf9" };

	/** A balloon: its cruising altitude, the minutes in the air, landed. */
	static class Balloon extends GeoAgent {
		double cruise;
		int minutes;
		boolean landed;

		Balloon(double lon, double lat, double cruise) {
			super(lon, lat);
			this.cruise = cruise;
		}
	}

	public void init() {
		area(38.50, 34.65, 38.80, 35.05);
		agentShape = "sphere";
		agentSizeMeters = 120;
		viewPitch = 65;
		agentSize = 5;
		for (int i = 0; i < balloons; i++) {
			Balloon b = new Balloon(START_LON + random(-0.03, 0.03), START_LAT + random(-0.02, 0.02), random(150, maxAltitude));
			b.color = COLORS[i % COLORS.length];
			/* The start is spread over 15 minutes */
			b.minutes = -randomInt(15);
			agents.add(b);
		}
	}

	public void step() {
		int flying = 0;
		double altitudes = 0;
		for (Balloon b : agents) {
			b.minutes++;
			if (b.landed || b.minutes < 0) {
				continue;
			}
			/* Climb to the cruising altitude (2 m/s), descend at the end of the flight */
			if (b.minutes > flightTime) {
				b.altitude = Math.max(0, b.altitude - 90);
				if (b.altitude == 0) {
					b.landed = true;
					b.color = "#8d8d8d";
					continue;
				}
			} else if (b.altitude < b.cruise) {
				b.altitude = Math.min(b.cruise, b.altitude + 120);
			}
			/* The wind in the altitude of the balloon: it turns and becomes stronger */
			double from = windFrom + veering * b.altitude / 1000;
			double speed = windSpeed * (1 + b.altitude / 800) + randomGaussian(0, 0.3);
			double km = speed * 60 / 1000;
			/* The wind blows from "from": the movement is in the opposite direction */
			move(b, -sinDeg(from) * km, -cosDeg(from) * km);
			flying++;
			altitudes += b.altitude;
		}
		chart("Balloons in the air", flying);
		chart("Mean altitude (m)", flying == 0 ? 0 : altitudes / flying);
	}

	@Action(label = "Start a new group of balloons")
	public void newGroup() {
		for (int i = 0; i < 10; i++) {
			Balloon b = new Balloon(START_LON + random(-0.03, 0.03), START_LAT + random(-0.02, 0.02), random(150, maxAltitude));
			b.color = COLORS[randomInt(COLORS.length)];
			agents.add(b);
		}
	}
}
