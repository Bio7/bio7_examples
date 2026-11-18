package html;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports a dynamic 3D plot as an interactive HTML page with WebGL: the
 * diffusion of heat on a plate with a moving heat source (a surface colored by
 * the temperature). The settings are the diffusion, the cooling, the strength
 * and the movement of the source and the boundary (open or insulated). The chart
 * shows the mean and the maximum temperature. The simulation runs in the web
 * browser (JavaScript).
 */
public class HeatDiffusionHtml {

	static final String MODEL = """
			function setup(sim) {
			  const n = Math.max(10, Math.min(150, Math.round(sim.p.grid)));
			  sim.n = n;
			  sim.u = new Float32Array(n * n);
			  sim.box = [1, 1, 0.45];
			  update(sim);
			}

			function step(sim) {
			  const n = sim.n, u = sim.u, next = new Float32Array(n * n), d = sim.p.diffusion, insulated = sim.p.boundary === 'Insulated';
			  function at(i, j) {
			    if (i < 0 || j < 0 || i >= n || j >= n) {
			      if (!insulated) return 0;
			      i = Math.max(0, Math.min(n - 1, i)); j = Math.max(0, Math.min(n - 1, j));
			    }
			    return u[i * n + j];
			  }
			  for (let i = 0; i < n; i++) {
			    for (let j = 0; j < n; j++) {
			      const c = u[i * n + j];
			      const lap = at(i + 1, j) + at(i - 1, j) + at(i, j + 1) + at(i, j - 1) - 4 * c;
			      next[i * n + j] = c + d * lap - sim.p.cooling * 0.01 * c;
			    }
			  }
			  /* The heat source (moves on a circle) */
			  if (sim.p.source) {
			    const a = sim.t * sim.p.motion * 0.02, ci = Math.round(n / 2 + n * 0.3 * Math.cos(a)), cj = Math.round(n / 2 + n * 0.3 * Math.sin(a));
			    const r = Math.max(1, Math.round(n * 0.04));
			    for (let i = ci - r; i <= ci + r; i++) for (let j = cj - r; j <= cj + r; j++) {
			      if (i >= 0 && j >= 0 && i < n && j < n && (i - ci) * (i - ci) + (j - cj) * (j - cj) <= r * r) next[i * n + j] += sim.p.power;
			    }
			  }
			  sim.u = next;
			  let sum = 0, max = 0;
			  for (let k = 0; k < n * n; k++) { sum += next[k]; max = Math.max(max, next[k]); }
			  sim.chart('Mean', sum / (n * n));
			  sim.chart('Maximum', max);
			  update(sim);
			}

			function update(sim) {
			  sim.surface = { nx: sim.n, ny: sim.n, z: sim.u, min: 0, max: sim.p.scale };
			}

			function coolDown(sim) { sim.u.fill(0); update(sim); }
			""";

	public static HtmlSimulation create() {
		return new HtmlSimulation("Heat Diffusion")
				.description("The temperature u of a plate: du/dt = D \u00B7 \u0394u \u2212 c \u00B7 u + source.")
				.view(HtmlSimulation.View.SURFACE_3D)
				.heading("Model")
				.number("grid", "Grid size", 80)
				.slider("diffusion", "Diffusion D", 0.01, 0.24, 0.01, 0.2)
				.slider("cooling", "Cooling c", 0, 5, 0.1, 0.5)
				.choice("boundary", "Boundary", new String[] { "Open (cold)", "Insulated" }, "Open (cold)")
				.restartOn("grid")
				.heading("Heat source")
				.checkbox("source", "Heat source on", true)
				.slider("power", "Power", 0, 2, 0.05, 0.5)
				.slider("motion", "Motion", 0, 5, 0.1, 1)
				.heading("Display")
				.slider("scale", "Height scale (maximum temperature)", 1, 30, 1, 10)
				.button("Cool down", "coolDown")
				.chart("Temperature")
				.colorMap(com.eco.bio7.spatial.PlotLegend.RAINBOW)
				.speed(30)
				.code(MODEL);
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/HeatDiffusion.html";
		create().export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
