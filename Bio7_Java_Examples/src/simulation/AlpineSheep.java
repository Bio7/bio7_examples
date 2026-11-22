package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.GeoAgent;
import com.eco.bio7.simulation.GeoAgentModel;
import com.eco.bio7.simulation.Param;

/**
 * Sheep and wolves in the Alps (Kleine Scheidegg, Switzerland) on the WorldWind
 * globe: the sheep graze in a fenced pasture, some stray outside; two wolves
 * roam in their territory and hunt the sheep outside the fence. The animals
 * are animated glTF models on the globe (agent.model, agentSizeMeters), the
 * exported page shows them as images on the map (agent.image).
 * <p>
 * The drawing (longitude and latitude): the fence (drawPolygon), the territory
 * of the wolves (drawCircle, the radius in km), the tracks of the wolves
 * (drawPath) and the shepherd's hut (drawText).
 */
public class AlpineSheep extends GeoAgentModel<AlpineSheep.Animal> {

	@Param(label = "Sheep", min = 5, max = 80, restart = true)
	int sheep = 35;

	@Param(label = "Wolves", min = 0, max = 5, restart = true)
	int wolves = 2;

	@Param(label = "Sheep straying outside the fence", min = 0, max = 0.2, step = 0.005)
	double stray = 0.03;

	@Param(label = "Speed of the wolves (km per step)", min = 0.01, max = 0.2, step = 0.005)
	double wolfSpeed = 0.06;

	/* The pasture (longitude, latitude), the territory of the wolves (center, radius in km) and the hut */
	static final double[] FENCE_LON = { 7.948, 7.966, 7.970, 7.952 }, FENCE_LAT = { 46.580, 46.578, 46.590, 46.592 };
	static final double DEN_LON = 7.935, DEN_LAT = 46.570, TERRITORY = 2.2, HUT_LON = 7.9605, HUT_LAT = 46.5935;

	/** An animal: a sheep or a wolf, outside the fence, the track of a wolf. */
	static class Animal extends GeoAgent {
		boolean wolf, outside;
		ArrayList<Double> trackLon = new ArrayList<>(), trackLat = new ArrayList<>();

		Animal(double lon, double lat, boolean wolf) {
			super(lon, lat);
			this.wolf = wolf;
			this.model = wolf ? "wolf" : "sheep";
			this.image = wolf ? "wolf" : "sheep";
			this.animation = wolf ? "walk" : "graze";
			this.size = wolf ? 9 : 7;
		}
	}

	int lost;

	public void init() {
		area(46.560, 7.915, 46.602, 7.985);
		loadModel("sheep", "data/models/sheep.glb");
		loadModel("wolf", "data/models/wolf.glb");
		loadImage("sheep", "data/sprites/sheep.png");
		loadImage("wolf", "data/sprites/wolf.png");
		agentSizeMeters = 35;
		viewPitch = 55;
		lost = 0;
		for (int i = 0; i < sheep; i++) {
			agents.add(new Animal(random(7.953, 7.965), random(46.582, 46.589), false));
		}
		for (int i = 0; i < wolves; i++) {
			agents.add(new Animal(DEN_LON + random(-0.01, 0.01), DEN_LAT + random(-0.005, 0.005), true));
		}
		draw();
	}

	public void step() {
		int outside = 0;
		for (Animal a : agents) {
			if (a.wolf) {
				Animal prey = null;
				double best = 1.0;
				for (Animal s : agents) {
					if (!s.wolf && s.outside && distanceKm(a, s) < best) {
						best = distanceKm(a, s);
						prey = s;
					}
				}
				if (prey != null) {
					a.animation = "run";
					if (moveTowards(a, prey.lon, prey.lat, wolfSpeed)) {
						/* Caught: a new sheep in the fence */
						prey.lon = random(7.953, 7.965);
						prey.lat = random(46.582, 46.589);
						prey.outside = false;
						lost++;
					}
				} else {
					a.animation = "walk";
					double h = random(0, 360);
					move(a, sinDeg(h) * wolfSpeed * 0.5, cosDeg(h) * wolfSpeed * 0.5);
					if (distanceKm(a.lon, a.lat, DEN_LON, DEN_LAT) > TERRITORY) {
						moveTowards(a, DEN_LON, DEN_LAT, wolfSpeed);
					}
				}
				a.trackLon.add(a.lon);
				a.trackLat.add(a.lat);
				if (a.trackLon.size() > 40) {
					a.trackLon.remove(0);
					a.trackLat.remove(0);
				}
			} else {
				if (!a.outside && chance(stray)) {
					a.outside = true;
				}
				double h = random(0, 360);
				double km = chance(0.6) ? 0 : 0.012;
				a.animation = km == 0 ? "graze" : "walk";
				move(a, sinDeg(h) * km, cosDeg(h) * km);
				if (a.outside) {
					/* Away from the fence (to the west) */
					a.animation = "walk";
					move(a, -0.015, random(-0.01, 0.01));
					outside++;
				} else if (!inside(a.lon, a.lat)) {
					moveTowards(a, 7.959, 46.585, 0.03);
				}
			}
		}
		draw();
		chart("Sheep outside the fence", outside);
		chart("Sheep caught", lost);
	}

	boolean inside(double x, double y) {
		boolean in = false;
		int n = FENCE_LON.length;
		for (int i = 0; i < n; i++) {
			int j = (i + n - 1) % n;
			if ((FENCE_LAT[i] > y) != (FENCE_LAT[j] > y) && x < (FENCE_LON[j] - FENCE_LON[i]) * (y - FENCE_LAT[i]) / (FENCE_LAT[j] - FENCE_LAT[i]) + FENCE_LON[i]) {
				in = !in;
			}
		}
		return in;
	}

	void draw() {
		drawPolygon(FENCE_LON, FENCE_LAT, "#7cb34255", "#ffffff");
		drawCircle(DEN_LON, DEN_LAT, TERRITORY, "#d6272818", "#d62728aa");
		drawText(HUT_LON, HUT_LAT, "Shepherd's hut", "#ffffff", 14);
		for (Animal a : agents) {
			if (a.wolf && a.trackLon.size() > 1) {
				double[] xs = new double[a.trackLon.size()], ys = new double[a.trackLat.size()];
				for (int i = 0; i < xs.length; i++) {
					xs[i] = a.trackLon.get(i);
					ys[i] = a.trackLat.get(i);
				}
				drawPath(xs, ys, "#ffcc00", 2);
			}
		}
	}
}
