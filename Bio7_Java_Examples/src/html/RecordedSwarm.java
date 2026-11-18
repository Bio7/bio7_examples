package html;

import java.util.Random;

import com.eco.bio7.spatial.HtmlSimulation;
import com.eco.bio7.spatial.PlotLegend;
import com.eco.bio7.spatial.SimulationRecorder;

/**
 * A 3D swarm model (boids) computed in Java and recorded with
 * SimulationRecorder: the positions of the agents (colored by their height) and
 * the order of the swarm. The page plays the recording in a 3D view (WebGL).
 */
public class RecordedSwarm {

	public static SimulationRecorder record(int steps) {
		int n = 300;
		double[] x = new double[n], y = new double[n], z = new double[n], vx = new double[n], vy = new double[n], vz = new double[n];
		Random random = new Random(2);
		for (int i = 0; i < n; i++) {
			x[i] = 0.2 + 0.6 * random.nextDouble();
			y[i] = 0.2 + 0.6 * random.nextDouble();
			z[i] = 0.2 + 0.6 * random.nextDouble();
			vx[i] = (random.nextDouble() - 0.5) * 0.01;
			vy[i] = (random.nextDouble() - 0.5) * 0.01;
			vz[i] = (random.nextDouble() - 0.5) * 0.01;
		}
		SimulationRecorder recorder = new SimulationRecorder("Swarm (recorded in Java)")
				.description("Boids with separation, alignment and cohesion. The colors show the height.").view(HtmlSimulation.View.SCENE_3D)
				.points(0.008, 0x1F77B4).chart("Order of the swarm").speed(30);
		double radius = 0.12, maxSpeed = 0.006;
		for (int t = 0; t < steps; t++) {
			double[] nvx = new double[n], nvy = new double[n], nvz = new double[n];
			for (int i = 0; i < n; i++) {
				double sx = 0, sy = 0, sz = 0, ax = 0, ay = 0, az = 0, cx = 0, cy = 0, cz = 0;
				int count = 0;
				for (int j = 0; j < n; j++) {
					double dx = x[j] - x[i], dy = y[j] - y[i], dz = z[j] - z[i], d2 = dx * dx + dy * dy + dz * dz;
					if (i == j || d2 > radius * radius || d2 == 0) {
						continue;
					}
					count++;
					sx -= dx / d2;
					sy -= dy / d2;
					sz -= dz / d2;
					ax += vx[j];
					ay += vy[j];
					az += vz[j];
					cx += dx;
					cy += dy;
					cz += dz;
				}
				double ux = vx[i], uy = vy[i], uz = vz[i];
				if (count > 0) {
					ux += 1.5e-5 * sx + 0.05 * (ax / count - ux) + 0.006 * cx / count;
					uy += 1.5e-5 * sy + 0.05 * (ay / count - uy) + 0.006 * cy / count;
					uz += 1.5e-5 * sz + 0.05 * (az / count - uz) + 0.006 * cz / count;
				}
				/* A soft wall */
				ux += x[i] < 0.1 ? 5e-4 : x[i] > 0.9 ? -5e-4 : 0;
				uy += y[i] < 0.1 ? 5e-4 : y[i] > 0.9 ? -5e-4 : 0;
				uz += z[i] < 0.1 ? 5e-4 : z[i] > 0.9 ? -5e-4 : 0;
				double s = Math.sqrt(ux * ux + uy * uy + uz * uz);
				if (s > maxSpeed) {
					ux *= maxSpeed / s;
					uy *= maxSpeed / s;
					uz *= maxSpeed / s;
				}
				nvx[i] = ux;
				nvy[i] = uy;
				nvz[i] = uz;
			}
			double ox = 0, oy = 0, oz = 0;
			int[] colors = new int[n];
			for (int i = 0; i < n; i++) {
				vx[i] = nvx[i];
				vy[i] = nvy[i];
				vz[i] = nvz[i];
				x[i] = Math.max(0, Math.min(1, x[i] + vx[i]));
				y[i] = Math.max(0, Math.min(1, y[i] + vy[i]));
				z[i] = Math.max(0, Math.min(1, z[i] + vz[i]));
				double s = Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i] + vz[i] * vz[i]);
				if (s > 0) {
					ox += vx[i] / s;
					oy += vy[i] / s;
					oz += vz[i] / s;
				}
				colors[i] = PlotLegend.rampColor(PlotLegend.VIRIDIS, 0, 1, z[i]);
			}
			recorder.frame().points(x, y, z, colors).chart("Order", Math.sqrt(ox * ox + oy * oy + oz * oz) / n);
		}
		return recorder;
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/RecordedSwarm.html";
		record(500).export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
