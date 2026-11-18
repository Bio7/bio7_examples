package html;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports a dynamic 3D line plot as an interactive HTML page with WebGL: the
 * Lorenz system is integrated in each step and the trajectory grows. The
 * parameters can be changed while the simulation runs (e.g., ρ < 24.7: the
 * trajectory approaches a fixed point). Two trajectories with slightly
 * different starting points show the sensitivity to the initial conditions
 * (the chart shows their distance).
 */
public class LorenzHtml {

	static final String MODEL = """
			function setup(sim) {
			  sim.box = [1, 1, 1];
			  sim.a = [1, 1, 1];
			  sim.b = [1 + sim.p.offset, 1, 1];
			  sim.trailA = []; sim.trailB = [];
			  update(sim);
			}

			function derivative(sim, q) {
			  return [sim.p.sigma * (q[1] - q[0]), q[0] * (sim.p.rho - q[2]) - q[1], q[0] * q[1] - sim.p.beta * q[2]];
			}

			/* Runge-Kutta (4th order) */
			function rk4(sim, q, h) {
			  const k1 = derivative(sim, q);
			  const k2 = derivative(sim, q.map(function (v, i) { return v + h / 2 * k1[i]; }));
			  const k3 = derivative(sim, q.map(function (v, i) { return v + h / 2 * k2[i]; }));
			  const k4 = derivative(sim, q.map(function (v, i) { return v + h * k3[i]; }));
			  return q.map(function (v, i) { return v + h / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]); });
			}

			function step(sim) {
			  for (let k = 0; k < 4; k++) {
			    sim.a = rk4(sim, sim.a, 0.005); sim.b = rk4(sim, sim.b, 0.005);
			    sim.trailA.push(sim.a); sim.trailB.push(sim.b);
			  }
			  const max = Math.round(sim.p.trail);
			  while (sim.trailA.length > max) { sim.trailA.shift(); sim.trailB.shift(); }
			  sim.chart('Distance', Math.hypot(sim.a[0] - sim.b[0], sim.a[1] - sim.b[1], sim.a[2] - sim.b[2]));
			  update(sim);
			}

			/* The coordinates of the box: x, y -25 - 25, z 0 - 50 */
			function box(q) { return [(q[0] + 25) / 50, (q[1] + 25) / 50, q[2] / 50]; }

			function update(sim) {
			  const xyz = [], colors = [];
			  function trail(t, c) {
			    for (let i = 1; i < t.length; i++) {
			      const p0 = box(t[i - 1]), p1 = box(t[i]);
			      xyz.push(p0[0], p0[1], p0[2], p1[0], p1[1], p1[2]);
			      colors.push(c[0], c[1], c[2], c[0], c[1], c[2]);
			    }
			  }
			  trail(sim.trailA, [0.12, 0.47, 0.71]);
			  if (sim.p.second) trail(sim.trailB, [1.0, 0.5, 0.05]);
			  sim.lines = { xyz: xyz, colors: colors };
			  const a = box(sim.a), b = box(sim.b);
			  sim.points = sim.p.second ? { xyz: [a[0], a[1], a[2], b[0], b[1], b[2]], colors: [0.12, 0.47, 0.71, 1.0, 0.5, 0.05], radius: 0.015 }
			    : { xyz: a, color: '#1f77b4', radius: 0.015 };
			}
			""";

	public static HtmlSimulation create() {
		return new HtmlSimulation("Lorenz System")
				.description("dx/dt = σ(y − x), dy/dt = x(ρ − z) − y, dz/dt = xy − βz")
				.view(HtmlSimulation.View.SCENE_3D)
				.heading("Parameters")
				.slider("sigma", "σ", 0, 30, 0.1, 10)
				.slider("rho", "ρ", 0, 60, 0.1, 28)
				.slider("beta", "β", 0, 6, 0.01, 2.67)
				.heading("Trajectories")
				.slider("trail", "Length of the trail", 100, 20000, 100, 6000)
				.checkbox("second", "Second trajectory", true)
				.number("offset", "Start offset of the second trajectory", 0.00001)
				.restartOn("offset")
				.chart("Distance of the trajectories")
				.speed(60)
				.code(MODEL);
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/Lorenz.html";
		create().export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
