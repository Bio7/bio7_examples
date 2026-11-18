package html;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports the Game of Life as an interactive HTML page (a 2D grid): Play/Pause,
 * the speed, the size of the grid, the start density, the rule (e.g., B3/S23
 * for Conway, B36/S23 for HighLife), a torus world and a chart of the
 * population. The simulation runs in the web browser (JavaScript).
 */
public class GameOfLifeHtml {

	/* The model in JavaScript: setup(sim) and step(sim) */
	static final String MODEL = """
			function setup(sim) {
			  const n = Math.max(10, Math.min(600, Math.round(sim.p.size)));
			  sim.width = n; sim.height = n;
			  sim.cells = new Uint8Array(n * n);
			  sim.next = new Uint8Array(n * n);
			  sim.palette = [sim.p.colors === 'Dark' ? '#111111' : '#ffffff', sim.p.colors === 'Dark' ? '#55ff55' : '#1f3a93'];
			  randomize(sim);
			}

			/* The numbers of the neighbors for a birth and for the survival of a rule like B3/S23 */
			function parseRule(text) {
			  const born = new Array(9).fill(false), survive = new Array(9).fill(false);
			  const parts = text.toUpperCase().split('/');
			  parts.forEach(function (part) {
			    const target = part.charAt(0) === 'B' ? born : part.charAt(0) === 'S' ? survive : null;
			    if (!target) return;
			    for (let i = 1; i < part.length; i++) { const d = part.charCodeAt(i) - 48; if (d >= 0 && d <= 8) target[d] = true; }
			  });
			  return { born: born, survive: survive };
			}

			function step(sim) {
			  const w = sim.width, h = sim.height, c = sim.cells, next = sim.next;
			  const rule = parseRule(sim.p.rule), wrap = sim.p.wrap;
			  let alive = 0;
			  for (let y = 0; y < h; y++) {
			    for (let x = 0; x < w; x++) {
			      let neighbors = 0;
			      for (let dy = -1; dy <= 1; dy++) {
			        for (let dx = -1; dx <= 1; dx++) {
			          if (dx === 0 && dy === 0) continue;
			          let nx = x + dx, ny = y + dy;
			          if (wrap) { nx = (nx + w) % w; ny = (ny + h) % h; }
			          else if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
			          neighbors += c[ny * w + nx];
			        }
			      }
			      const state = c[y * w + x] ? (rule.survive[neighbors] ? 1 : 0) : (rule.born[neighbors] ? 1 : 0);
			      next[y * w + x] = state;
			      alive += state;
			    }
			  }
			  sim.cells = next; sim.next = c;
			  sim.chart('Population', alive);
			}

			/* The buttons */
			function randomize(sim) {
			  for (let i = 0; i < sim.cells.length; i++) sim.cells[i] = sim.random() < sim.p.density ? 1 : 0;
			}
			function clearGrid(sim) {
			  sim.cells.fill(0);
			}
			function glider(sim) {
			  const w = sim.width, x0 = Math.floor(w / 2), y0 = Math.floor(sim.height / 2);
			  [[1, 0], [2, 1], [0, 2], [1, 2], [2, 2]].forEach(function (p) { sim.cells[(y0 + p[1]) * w + x0 + p[0]] = 1; });
			}
			""";

	public static HtmlSimulation create() {
		return new HtmlSimulation("Game of Life")
				.description("A cellular automaton of J. H. Conway: a dead cell with 3 living neighbors is born, a living cell with 2 or 3 neighbors survives.")
				.view(HtmlSimulation.View.GRID_2D)
				.heading("World")
				.number("size", "Grid size (cells)", 120)
				.slider("density", "Start density", 0, 1, 0.01, 0.3)
				.checkbox("wrap", "Torus world (wrap at the borders)", true)
				.choice("colors", "Colors", new String[] { "Light", "Dark" }, "Light")
				.restartOn("size", "density", "colors")
				.heading("Rule")
				.text("rule", "Rule (B = birth, S = survival)", "B3/S23")
				.heading("Cells")
				.button("Randomize", "randomize")
				.button("Clear", "clearGrid")
				.button("Add a glider", "glider")
				.chart("Population (living cells)")
				.speed(15)
				.code(MODEL);
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/GameOfLife.html";
		create().export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
