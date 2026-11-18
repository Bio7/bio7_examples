package html;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports the forest fire model (Drossel and Schwabl) as an interactive HTML
 * page: trees grow on empty cells, lightning ignites trees and the fire spreads
 * to the neighbors. The settings are the growth, the lightning, the
 * neighborhood and the size of the forest. The chart shows the number of the
 * trees and of the burning trees.
 */
public class ForestFireHtml {

	static final String MODEL = """
			/* The states: 0 = empty, 1 = tree, 2 = fire, 3 = ash (burnt in the last step) */
			function setup(sim) {
			  const n = Math.max(20, Math.min(500, Math.round(sim.p.size)));
			  sim.width = n; sim.height = n;
			  sim.cells = new Uint8Array(n * n);
			  sim.next = new Uint8Array(n * n);
			  sim.palette = ['#8d6e63', '#2e7d32', '#ff6f00', '#424242'];
			  for (let i = 0; i < n * n; i++) sim.cells[i] = sim.random() < sim.p.start ? 1 : 0;
			}

			function step(sim) {
			  const w = sim.width, h = sim.height, c = sim.cells, next = sim.next;
			  const moore = sim.p.neighborhood === 'Moore (8)';
			  let trees = 0, fires = 0;
			  for (let y = 0; y < h; y++) {
			    for (let x = 0; x < w; x++) {
			      const i = y * w + x, s = c[i];
			      let t = s;
			      if (s === 2) { t = 3; }
			      else if (s === 3) { t = 0; }
			      else if (s === 0) { t = sim.random() < sim.p.growth ? 1 : 0; }
			      else {
			        let burning = false;
			        for (let dy = -1; dy <= 1 && !burning; dy++) for (let dx = -1; dx <= 1; dx++) {
			          if ((dx === 0 && dy === 0) || (!moore && dx !== 0 && dy !== 0)) continue;
			          const nx = x + dx, ny = y + dy;
			          if (nx >= 0 && ny >= 0 && nx < w && ny < h && c[ny * w + nx] === 2) { burning = true; break; }
			        }
			        t = burning || sim.random() < sim.p.lightning ? 2 : 1;
			      }
			      next[i] = t;
			      if (t === 1) trees++; else if (t === 2) fires++;
			    }
			  }
			  sim.cells = next; sim.next = c;
			  sim.chart('Trees', trees);
			  sim.chart('Fire', fires);
			}

			function ignite(sim) {
			  const w = sim.width, x0 = Math.floor(w / 2), y0 = Math.floor(sim.height / 2);
			  for (let dy = -2; dy <= 2; dy++) for (let dx = -2; dx <= 2; dx++) sim.cells[(y0 + dy) * w + x0 + dx] = 2;
			}
			""";

	public static HtmlSimulation create() {
		return new HtmlSimulation("Forest Fire")
				.description("Trees grow with the probability p, lightning ignites a tree with the probability f and the fire spreads to the neighbors.")
				.view(HtmlSimulation.View.GRID_2D)
				.heading("Forest")
				.number("size", "Grid size (cells)", 200)
				.slider("start", "Start density of the trees", 0, 1, 0.01, 0.55)
				.restartOn("size", "start")
				.heading("Model")
				.slider("growth", "Growth p", 0, 0.1, 0.001, 0.01)
				.slider("lightning", "Lightning f", 0, 0.001, 0.00001, 0.00005)
				.choice("neighborhood", "Neighborhood", new String[] { "von Neumann (4)", "Moore (8)" }, "von Neumann (4)")
				.button("Ignite the center", "ignite")
				.chart("Trees and fire")
				.speed(20)
				.code(MODEL);
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/ForestFire.html";
		create().export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
