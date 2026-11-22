package simulation;

import com.eco.bio7.simulation.Agent;
import com.eco.bio7.simulation.AgentModel;
import com.eco.bio7.simulation.Param;

/**
 * A swarm of boids in a box: each boid steers away from close boids
 * (separation), into the mean direction of its neighbors (alignment) and to
 * their center (cohesion), and turns back softly at the walls.
 * <p>
 * Compile the class (the boids are displayed in the 3D view), start the
 * simulation with the Play/Pause button of the toolbar and create a new swarm
 * with the Setup button. The menu of the Play/Pause button exports the model as
 * an HTML page with a WebGL view ("Export Simulation as HTML...").
 */
public class Swarm extends AgentModel<Swarm.Boid> {

	@Param(label = "Number of boids", min = 10, max = 500, restart = true)
	int boids = 150;

	@Param(label = "Neighbor radius", min = 2, max = 40)
	double radius = 12;

	@Param(label = "Separation", min = 0, max = 3)
	double separation = 1.5;

	@Param(label = "Alignment", min = 0, max = 3)
	double alignment = 1.0;

	@Param(label = "Cohesion", min = 0, max = 3)
	double cohesion = 1.0;

	@Param(label = "Maximum speed", min = 0.5, max = 4)
	double maxSpeed = 2;

	/* The distance from the walls at which the boids turn back */
	static final double MARGIN = 8;

	/** A boid: a position (Agent), a velocity and the steering of the step. */
	static class Boid extends Agent {
		double vx, vy, vz;
		double ax, ay, az;

		Boid(double x, double y, double z, double vx, double vy, double vz) {
			super(x, y, z);
			this.vx = vx;
			this.vy = vy;
			this.vz = vz;
		}

		double speed() {
			return Math.sqrt(vx * vx + vy * vy + vz * vz);
		}
	}

	public void init() {
		box(100, 100, 60);
		agentRadius = 1.2;
		for (int i = 0; i < boids; i++) {
			agents.add(new Boid(random() * boxWidth, random() * boxHeight, random() * boxDepth, random() - 0.5, random() - 0.5, random() - 0.5));
		}
		/* The first boid is red */
		agents.get(0).color = "#d62728";
	}

	public void step() {
		double r2 = radius * radius;
		/* All boids decide ... */
		for (Boid b : agents) {
			double sx = 0, sy = 0, sz = 0, mx = 0, my = 0, mz = 0, cx = 0, cy = 0, cz = 0;
			int n = 0;
			for (Boid o : agents) {
				if (o == b) {
					continue;
				}
				double dx = o.x - b.x, dy = o.y - b.y, dz = o.z - b.z;
				double d2 = dx * dx + dy * dy + dz * dz;
				if (d2 < r2 && d2 > 0) {
					n++;
					sx -= dx / d2;
					sy -= dy / d2;
					sz -= dz / d2;
					mx += o.vx;
					my += o.vy;
					mz += o.vz;
					cx += o.x;
					cy += o.y;
					cz += o.z;
				}
			}
			b.ax = 0;
			b.ay = 0;
			b.az = 0;
			if (n > 0) {
				b.ax = separation * sx + alignment * 0.05 * (mx / n - b.vx) + cohesion * 0.005 * (cx / n - b.x);
				b.ay = separation * sy + alignment * 0.05 * (my / n - b.vy) + cohesion * 0.005 * (cy / n - b.y);
				b.az = separation * sz + alignment * 0.05 * (mz / n - b.vz) + cohesion * 0.005 * (cz / n - b.z);
			}
			b.ax += wall(b.x, boxWidth);
			b.ay += wall(b.y, boxHeight);
			b.az += wall(b.z, boxDepth);
		}
		/* ... then all move */
		for (Boid b : agents) {
			b.vx += b.ax;
			b.vy += b.ay;
			b.vz += b.az;
			double s = b.speed();
			double limit = s > maxSpeed ? maxSpeed : s < maxSpeed / 2 ? maxSpeed / 2 : s;
			if (s > 0) {
				b.vx *= limit / s;
				b.vy *= limit / s;
				b.vz *= limit / s;
			}
			b.x = clamp(b.x + b.vx, boxWidth);
			b.y = clamp(b.y + b.vy, boxHeight);
			b.z = clamp(b.z + b.vz, boxDepth);
		}
	}

	/* The steering away from a wall (0 in the middle of the box) */
	double wall(double p, double size) {
		if (p < MARGIN) {
			return 0.1 * (MARGIN - p) / MARGIN;
		}
		if (p > size - MARGIN) {
			return -0.1 * (p - size + MARGIN) / MARGIN;
		}
		return 0;
	}

	static double clamp(double p, double size) {
		return Math.max(0, Math.min(size, p));
	}
}
