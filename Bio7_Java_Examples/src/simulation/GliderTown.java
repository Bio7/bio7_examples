package simulation;

import java.util.ArrayList;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Agent;
import com.eco.bio7.simulation.AgentModel;
import com.eco.bio7.simulation.Param;

/**
 * A flock of gliders flies through a small town (the Java version of
 * glider_town.py and glider_town.R).
 * <p>
 * The town is built of 3D models and 3D shapes: houses and trees (OBJ files
 * with their materials, the roofs with a texture) and the church tower (an STL
 * file) are objects of the environment (loadModel, placeModel), the office
 * towers are boxes (drawBox), the streets and the square flat polygons
 * (drawPolygon3) with dashed centre lines (drawPath3), a hot air balloon drifts
 * over the town (drawSphere, its basket a box, the ropes lines).
 * <p>
 * The flock flies along a route: the waypoints are markers (drawPoint3) joined
 * by a line (drawPath3), a ring at the height of the next waypoint (drawCircle
 * with z) shows where the flock goes, the labels stand above the buildings
 * (drawText with z). The boids keep the rules of C. Reynolds (separation,
 * alignment, cohesion), steer to the next waypoint and fly around the buildings
 * and the trees (only below their roofs: above them the way is free). The
 * walls, the floor and the roofs push the closer, the stronger (the gliders
 * glide smoothly, they do not hop).
 * <p>
 * The custom camera follows the red boid (in Bio7, the global view beside it).
 * The charts: the order of the flock (polarization), the mean speed and the
 * height of the flock. Export the model with "Export Simulation as HTML..." (the
 * OBJ and STL models are converted to glTF).
 */
public class GliderTown extends AgentModel<Agent> {

	@Param(label = "Number of boids", min = 20, max = 400, restart = true)
	int boids = 150;

	@Param(label = "Maximum speed", min = 1, max = 12, step = 0.1, group = "Flight")
	double maxSpeed = 6.5;

	@Param(label = "Maximum force (turns)", min = 0.005, max = 0.2, step = 0.005, group = "Flight")
	double maxForce = 0.05;

	@Param(label = "Follow the route", min = 0, max = 3, step = 0.1, group = "Flight")
	double route = 1.0;

	@Param(label = "Separation", min = 0, max = 3, step = 0.1, group = "Rules")
	double separation = 1.5;

	@Param(label = "Alignment", min = 0, max = 3, step = 0.1, group = "Rules")
	double alignment = 1.0;

	@Param(label = "Cohesion", min = 0, max = 3, step = 0.1, group = "Rules")
	double cohesion = 1.0;

	@Param(label = "Number of trees", min = 0, max = 120, group = "Town", restart = true)
	int trees = 40;

	@Param(label = "Show the route", group = "View")
	boolean showRoute = true;

	@Param(label = "Labels", group = "View")
	boolean labels = true;

	@Param(label = "Glider size", min = 2, max = 15, step = 0.5, group = "View")
	double gliderSize = 6.0;

	@Param(label = "Camera follows the red boid", group = "View")
	boolean follow = true;

	/* The box: 2000 x 2000, the height; the centre of the town; the lowest height of the flight */
	static final double WIDTH = 2000, TOP = 760, C = WIDTH / 2, FLOOR_Z = 70;
	static final double RADIUS = 150, CLOSE = 45, AVOID = 90, STREET = 70;

	/* The route of the flock (x, y, z): along the main street, around the church, past the offices and the balloon */
	static final double[][] ROUTE = { { 320, 1000, 170 }, { 780, 1000, 150 }, { 1000, 680, 300 }, { 1560, 700, 260 }, { 1680, 1160, 430 },
			{ 1420, 1640, 620 }, { 900, 1560, 400 }, { 430, 1430, 300 }, { 330, 720, 230 } };

	/* The office towers (drawBox): the corner x, y, the width, the depth and the height */
	static final double[][] OFFICES = { { 1260, 1250, 130, 130, 460 }, { 1450, 1270, 110, 110, 380 }, { 1290, 1440, 120, 120, 540 } };

	/* The positions and the velocities of the boids */
	double[] px, py, pz, vx, vy, vz;
	/* The obstacles: x, y, radius, height (the buildings and the trees) */
	ArrayList<double[]> obstacles;
	int target, reached;

	public void init() {
		box(WIDTH, WIDTH, TOP);
		agentShape = "glider";
		floor = "plane";
		shadows = true;
		loadModel("house", "data/models/house.obj");
		loadModel("tree", "data/models/tree.obj");
		loadModel("tower", "data/models/tower.stl");
		buildTown();
		int n = boids;
		px = new double[n];
		py = new double[n];
		pz = new double[n];
		vx = new double[n];
		vy = new double[n];
		vz = new double[n];
		/* The flock starts at the west end of the main street, flying east */
		for (int i = 0; i < n; i++) {
			px[i] = 180 + random(0, 160);
			py[i] = C + random(-150, 150);
			pz[i] = 200 + random(0, 160);
			double angle = random(-0.4, 0.4), climb = random(-0.1, 0.1), s = 0.75 * maxSpeed;
			vx[i] = s * Math.cos(angle) * Math.cos(climb);
			vy[i] = s * Math.sin(angle) * Math.cos(climb);
			vz[i] = s * Math.sin(climb);
			Agent a = new Agent(px[i], py[i], pz[i]);
			/* Blue to turquoise, the first boid red */
			a.color = i == 0 ? "#f2261f" : hsv(random(0.5, 0.62), 0.75, 0.95);
			agents.add(a);
		}
		target = 0;
		reached = 0;
		view();
	}

	/* The objects of the environment (they stay) and the obstacles of the flock (x, y, radius, height) */
	void buildTown() {
		ArrayList<double[]> obs = new ArrayList<>();
		/* The church tower in the middle of the square */
		placeModel("tower", C, C, 0, 0, 420);
		obs.add(new double[] { C, C, 60, 420 });
		/* The houses along the main street (west - east) and the cross street (south - north), the front to the street */
		for (double x = 300; x < 1750; x += 150) {
			if (Math.abs(x - C) < 240) {
				continue;
			}
			for (int side = -1; side <= 1; side += 2) {
				double s = 100 + 40 * random();
				placeModel("house", x, C + side * 125, 0, side < 0 ? 0 : 180, s);
				obs.add(new double[] { x, C + side * 125, 85, s });
			}
		}
		for (double y = 300; y < 1750; y += 150) {
			if (Math.abs(y - C) < 240) {
				continue;
			}
			for (int side = -1; side <= 1; side += 2) {
				if (side > 0 && y > C) {
					continue; /* the office quarter */
				}
				double s = 100 + 40 * random();
				placeModel("house", C + side * 125, y, 0, side < 0 ? 90 : 270, s);
				obs.add(new double[] { C + side * 125, y, 85, s });
			}
		}
		for (double[] o : OFFICES) {
			obs.add(new double[] { o[0] + o[2] / 2, o[1] + o[3] / 2, Math.max(o[2], o[3]) * 0.72, o[4] });
		}
		/* The trees in the gardens (away from the streets, the houses and the offices) */
		int planted = 0;
		for (int k = 0; k < trees * 20 && planted < trees; k++) {
			double x = random(150, WIDTH - 150), y = random(150, WIDTH - 150);
			if (Math.abs(x - C) < 260 || Math.abs(y - C) < 260 || (x > 1180 && y > 1180)) {
				continue;
			}
			boolean free = true;
			for (double[] o : obs) {
				if (Math.hypot(x - o[0], y - o[1]) < o[2] + 50) {
					free = false;
					break;
				}
			}
			if (!free) {
				continue;
			}
			double s = 80 + 50 * random();
			placeModel("tree", x, y, 0, random(0, 360), s);
			obs.add(new double[] { x, y, 35, s });
			planted++;
		}
		obstacles = obs;
	}

	public void step() {
		int n = px.length;
		double ms = maxSpeed, maxTurn = maxForce * 12;
		double[] sx = new double[n], sy = new double[n], sz = new double[n];
		/* The next waypoint (the next one when the centre of the flock is near) */
		double[] goal = ROUTE[target];
		for (int i = 0; i < n; i++) {
			/* The rules of the flock: alignment, cohesion and separation of the neighbors */
			double ax = 0, ay = 0, az = 0, cx = 0, cy = 0, cz = 0, ex = 0, ey = 0, ez = 0;
			int count = 0;
			for (int j = 0; j < n; j++) {
				double dx = px[j] - px[i], dy = py[j] - py[i], dz = pz[j] - pz[i], d2 = dx * dx + dy * dy + dz * dz;
				if (d2 == 0 || d2 > RADIUS * RADIUS) {
					continue;
				}
				count++;
				ax += vx[j];
				ay += vy[j];
				az += vz[j];
				cx += dx;
				cy += dy;
				cz += dz;
				if (d2 < CLOSE * CLOSE) {
					ex -= dx / d2;
					ey -= dy / d2;
					ez -= dz / d2;
				}
			}
			if (count > 0) {
				double a = alignment * 0.06, c = cohesion * 0.0004, e = separation * 2.5;
				sx[i] = a * (ax / count - vx[i]) + c * cx / count + e * ex;
				sy[i] = a * (ay / count - vy[i]) + c * cy / count + e * ey;
				sz[i] = a * (az / count - vz[i]) + c * cz / count + e * ez;
			}
			/* The route */
			double gx = goal[0] - px[i], gy = goal[1] - py[i], gz = goal[2] - pz[i], gl = Math.max(Math.sqrt(gx * gx + gy * gy + gz * gz), 1e-9);
			sx[i] += route * 0.08 * gx / gl;
			sy[i] += route * 0.08 * gy / gl;
			sz[i] += route * 0.08 * gz / gl;
			/* At most the maximum turn */
			double s = Math.sqrt(sx[i] * sx[i] + sy[i] * sy[i] + sz[i] * sz[i]);
			if (s > maxTurn) {
				sx[i] *= maxTurn / s;
				sy[i] *= maxTurn / s;
				sz[i] *= maxTurn / s;
			}
		}
		if (Math.sqrt(sq(mean(px) - goal[0]) + sq(mean(py) - goal[1]) + sq(mean(pz) - goal[2])) < 220) {
			target = (target + 1) % ROUTE.length;
			reached++;
		}
		double margin = 120, wall = 0.1 * ms;
		for (int i = 0; i < n; i++) {
			/* The buildings and the trees: a boid below the roof looks ahead and steers away; up from a roof just below */
			double lift = 0;
			for (double[] o : obstacles) {
				double ax = px[i] + vx[i] * 15 - o[0], ay = py[i] + vy[i] * 15 - o[1];
				double la = Math.sqrt(ax * ax + ay * ay), gap = Math.min(la, Math.hypot(px[i] - o[0], py[i] - o[1])) - o[2];
				if (pz[i] >= o[3] + 30) {
					continue;
				}
				if (gap < AVOID) {
					double push = ms * 0.25 * (1 - Math.max(gap, 0) / AVOID);
					sx[i] += ax / Math.max(la, 1e-9) * push;
					sy[i] += ay / Math.max(la, 1e-9) * push;
				}
				if (gap < 0) {
					/* The closer to the roof, the stronger (no hopping) */
					lift = Math.max(lift, clamp((o[3] + 30 - pz[i]) / 30, 0, 1));
				}
			}
			sz[i] += lift * 0.05 * ms;
			/* The walls and the floor: the closer, the stronger (no sudden push, the gliders do not hop) */
			sx[i] += wall * (near(px[i], margin) - near(WIDTH - px[i], margin));
			sy[i] += wall * (near(py[i], margin) - near(WIDTH - py[i], margin));
			sz[i] += wall * (near(pz[i] - FLOOR_Z, 80) - near(TOP - pz[i], margin));
		}
		for (int i = 0; i < n; i++) {
			vx[i] += sx[i];
			vy[i] += sy[i];
			vz[i] += sz[i];
			/* The speed between half the maximum and the maximum */
			double speed = Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i] + vz[i] * vz[i]);
			double f = speed > ms ? ms / speed : speed < ms / 2 ? ms / 2 / Math.max(speed, 1e-9) : 1;
			vx[i] *= f;
			vy[i] *= f;
			vz[i] *= f;
			px[i] += vx[i];
			py[i] += vy[i];
			pz[i] += vz[i];
			/* Never inside a building or a tree (pushed out to its side) */
			for (double[] o : obstacles) {
				double dx = px[i] - o[0], dy = py[i] - o[1], d = Math.sqrt(dx * dx + dy * dy);
				if (d < o[2] + 2 && pz[i] < o[3]) {
					px[i] = o[0] + dx / Math.max(d, 1e-9) * (o[2] + 2);
					py[i] = o[1] + dy / Math.max(d, 1e-9) * (o[2] + 2);
				}
			}
			px[i] = clamp(px[i], 0, WIDTH);
			py[i] = clamp(py[i], 0, WIDTH);
			pz[i] = clamp(pz[i], FLOOR_Z * 0.5, TOP);
			Agent a = agents.get(i);
			a.x = px[i];
			a.y = py[i];
			a.z = pz[i];
		}
		/* The charts: the order of the flock, the mean speed, the height */
		double ux = 0, uy = 0, uz = 0, speeds = 0;
		for (int i = 0; i < n; i++) {
			double s = Math.max(Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i] + vz[i] * vz[i]), 1e-9);
			ux += vx[i] / s;
			uy += vy[i] / s;
			uz += vz[i] / s;
			speeds += s;
		}
		chart("Polarization", Math.sqrt(ux * ux + uy * uy + uz * uz) / n);
		chart("Speed", "Mean speed", speeds / n);
		chart("Height of the flock", "Mean height", mean(pz));
		view();
	}

	/* The settings of the view and the shapes of the town (drawn again in each step) */
	void view() {
		agentRadius = gliderSize;
		cameraFollow(follow ? 0 : -1);
		/* The streets and the square (flat polygons just above the floor), the dashed centre lines */
		double lo = 120, hi = WIDTH - 120, w = STREET / 2;
		drawPolygon3(new double[] { lo, hi, hi, lo }, new double[] { C - w, C - w, C + w, C + w }, new double[] { 1, 1, 1, 1 }, "#6b7078", null);
		drawPolygon3(new double[] { C - w, C + w, C + w, C - w }, new double[] { lo, lo, hi, hi }, new double[] { 1.2, 1.2, 1.2, 1.2 }, "#6b7078", null);
		double[] sqx = new double[8], sqy = new double[8], sqz = new double[8];
		for (int k = 0; k < 8; k++) {
			double a = 2 * Math.PI * k / 8 + Math.PI / 8;
			sqx[k] = C + 190 * Math.cos(a);
			sqy[k] = C + 190 * Math.sin(a);
			sqz[k] = 1.5;
		}
		drawPolygon3(sqx, sqy, sqz, "#c8bea6", "#8d846f");
		for (double x0 = lo; x0 < hi; x0 += 80) {
			if (Math.abs(x0 - C) > 200) {
				drawPath3(new double[] { x0, x0 + 40 }, new double[] { C, C }, new double[] { 2, 2 }, "#f2f2f2", 2);
				drawPath3(new double[] { C, C }, new double[] { x0, x0 + 40 }, new double[] { 2, 2 }, "#f2f2f2", 2);
			}
		}
		/* The office towers */
		for (double[] o : OFFICES) {
			drawBox(o[0], o[1], 0, o[2], o[3], o[4], "#8aa1bd", "#3d4a5c");
			drawBox(o[0] - 6, o[1] - 6, o[4], o[2] + 12, o[3] + 12, 12, "#56667c", null);
		}
		/* The balloon: it drifts over the west of the town */
		double bx = 560 + 160 * Math.sin(t / 300.0), by = 1480 + 60 * Math.cos(t / 210.0), bz = 470 + 25 * Math.sin(t / 40.0);
		drawSphere(bx, by, bz, 55, "#e0533c");
		drawBox(bx - 14, by - 14, bz - 105, 28, 28, 22, "#8b5a2b", null);
		for (int k = 0; k < 4; k++) {
			double dx = k == 0 || k == 3 ? -12 : 12, dy = k < 2 ? -12 : 12;
			drawLine3(bx + dx, by + dy, bz - 83, bx + dx * 2.6, by + dy * 2.6, bz - 40, "#5d4037", 1);
		}
		/* The route: the markers, the line between them, a ring at the next waypoint */
		if (showRoute) {
			int m = ROUTE.length;
			double[] rx = new double[m + 1], ry = new double[m + 1], rz = new double[m + 1];
			for (int k = 0; k <= m; k++) {
				rx[k] = ROUTE[k % m][0];
				ry[k] = ROUTE[k % m][1];
				rz[k] = ROUTE[k % m][2];
			}
			drawPath3(rx, ry, rz, "#ffb300aa", 2);
			for (int k = 0; k < m; k++) {
				drawPoint3(ROUTE[k][0], ROUTE[k][1], ROUTE[k][2], k == target ? 26 : 16, k == target ? "#43a047" : "#fb8c00");
			}
			double[] g = ROUTE[target];
			drawCircle(g[0], g[1], 70, null, "#43a047", g[2]);
		}
		if (labels) {
			drawText(C, C, "Church", "#3e2723", 1, 450);
			drawText(1320, 1500, "Offices", "#263238", 1, 580);
			drawText(bx, by, "Balloon", "#b71c1c", 1, bz + 75);
			if (showRoute) {
				double[] g = ROUTE[target];
				drawText(g[0], g[1], "Waypoint " + (target + 1), "#1b5e20", 1, g[2] + 45);
			}
		}
	}

	@Action(label = "Scatter the flock", group = "Flight")
	public void scatter() {
		for (int i = 0; i < px.length; i++) {
			double angle = random(0, 2 * Math.PI), climb = random(-0.6, 0.6);
			vx[i] = maxSpeed * Math.cos(angle) * Math.cos(climb);
			vy[i] = maxSpeed * Math.sin(angle) * Math.cos(climb);
			vz[i] = maxSpeed * Math.sin(climb);
		}
	}

	/* 1 at the wall, 0 from the distance width on */
	static double near(double distance, double width) {
		return clamp(1 - distance / width, 0, 1);
	}

	/* The color of hue, saturation and value (0 - 1) as #rrggbb */
	static String hsv(double h, double s, double v) {
		double f = h * 6 - Math.floor(h * 6), p = v * (1 - s), q = v * (1 - f * s), u = v * (1 - (1 - f) * s);
		int k = ((int) Math.floor(h * 6)) % 6;
		double r = k == 0 || k == 5 ? v : k == 1 ? q : k == 4 ? u : p;
		double g = k == 1 || k == 2 ? v : k == 0 ? u : k == 3 ? q : p;
		double b = k == 3 || k == 4 ? v : k == 2 ? u : k == 5 ? q : p;
		return "#" + hex(r) + hex(g) + hex(b);
	}

	/* Two hex digits of a value 0 - 1 */
	static String hex(double c) {
		int x = (int) Math.round(c * 255);
		String digits = "0123456789abcdef";
		return "" + digits.charAt(x / 16) + digits.charAt(x % 16);
	}
}
