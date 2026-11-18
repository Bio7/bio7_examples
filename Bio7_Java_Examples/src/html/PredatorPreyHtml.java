package html;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports an individual based predator prey model (wolves and sheep on
 * regrowing grass) as an interactive HTML page: the grass is a grid, the
 * animals are agents which move, eat, reproduce and die. The chart shows the
 * populations (the typical oscillations of predators and prey).
 */
public class PredatorPreyHtml {

	static final String MODEL = """
			function setup(sim) {
			  const n = 80;
			  sim.width = n; sim.height = n;
			  sim.cells = new Float32Array(n * n).fill(1);
			  sim.min = 0; sim.max = 1;
			  sim.regrow = new Float32Array(n * n);
			  sim.sheep = []; sim.wolves = [];
			  for (let i = 0; i < sim.p.sheep; i++) sim.sheep.push(animal(sim, sim.random() * 2 * sim.p.sheepGain));
			  for (let i = 0; i < sim.p.wolves; i++) sim.wolves.push(animal(sim, sim.random() * 2 * sim.p.wolfGain));
			  update(sim);
			}

			function animal(sim, energy) {
			  return { x: sim.random() * sim.width, y: sim.random() * sim.height, a: sim.random() * 2 * Math.PI, e: energy };
			}

			function move(sim, an) {
			  an.a += (sim.random() - 0.5) * 1.2;
			  an.x = (an.x + Math.cos(an.a) + sim.width) % sim.width;
			  an.y = (an.y + Math.sin(an.a) + sim.height) % sim.height;
			  an.e -= 1;
			}

			function step(sim) {
			  const w = sim.width, g = sim.cells;
			  /* The grass grows back after a time */
			  if (sim.p.grass) {
			    for (let i = 0; i < g.length; i++) {
			      if (g[i] < 1) { sim.regrow[i]++; if (sim.regrow[i] >= sim.p.regrowth) { g[i] = 1; sim.regrow[i] = 0; } }
			    }
			  }
			  const sheep = [], wolves = [];
			  sim.sheep.forEach(function (s) {
			    move(sim, s);
			    if (sim.p.grass) {
			      const i = Math.floor(s.y) * w + Math.floor(s.x);
			      if (g[i] >= 1) { g[i] = 0; s.e += sim.p.sheepGain; }
			    } else {
			      s.e += 1;
			    }
			    if (s.e < 0) return;
			    if (sim.random() < sim.p.sheepBirth) { s.e /= 2; sheep.push({ x: s.x, y: s.y, a: sim.random() * 6.28, e: s.e }); }
			    sheep.push(s);
			  });
			  /* The wolves eat a sheep at their cell */
			  const grid = new Map();
			  sheep.forEach(function (s, k) { const key = Math.floor(s.y) * w + Math.floor(s.x); if (!grid.has(key)) grid.set(key, []); grid.get(key).push(k); });
			  const eaten = new Set();
			  sim.wolves.forEach(function (wo) {
			    move(sim, wo);
			    const list = grid.get(Math.floor(wo.y) * w + Math.floor(wo.x));
			    if (list) { const k = list.find(function (q) { return !eaten.has(q); }); if (k !== undefined) { eaten.add(k); wo.e += sim.p.wolfGain; } }
			    if (wo.e < 0) return;
			    if (sim.random() < sim.p.wolfBirth) { wo.e /= 2; wolves.push({ x: wo.x, y: wo.y, a: sim.random() * 6.28, e: wo.e }); }
			    wolves.push(wo);
			  });
			  sim.sheep = sheep.filter(function (s, k) { return !eaten.has(k); }).slice(0, 4000);
			  sim.wolves = wolves.slice(0, 2000);
			  let grass = 0; for (let i = 0; i < g.length; i++) grass += g[i];
			  sim.chart('Sheep', sim.sheep.length);
			  sim.chart('Wolves', sim.wolves.length);
			  sim.chart('Grass / 4', grass / 4);
			  update(sim);
			}

			function update(sim) {
			  const x = [], y = [], colors = [];
			  sim.sheep.forEach(function (s) { x.push(s.x); y.push(s.y); colors.push('#fafafa'); });
			  sim.wolves.forEach(function (wo) { x.push(wo.x); y.push(wo.y); colors.push('#212121'); });
			  sim.agents = { x: x, y: y, colors: colors, radius: 0.6 };
			}

			function addWolves(sim) {
			  for (let i = 0; i < 20; i++) sim.wolves.push(animal(sim, 2 * sim.p.wolfGain));
			  update(sim);
			}
			""";

	public static HtmlSimulation create() {
		return new HtmlSimulation("Wolves, Sheep and Grass")
				.description("Sheep (white) eat grass (green, it regrows), wolves (black) eat sheep. The animals need energy and reproduce with a probability.")
				.view(HtmlSimulation.View.GRID_2D)
				.colorMap(new int[] { 0x8D6E63, 0x66BB6A })
				.heading("Start")
				.number("sheep", "Sheep", 120)
				.number("wolves", "Wolves", 40)
				.restartOn("sheep", "wolves")
				.heading("Sheep")
				.slider("sheepGain", "Energy from grass", 1, 10, 1, 4)
				.slider("sheepBirth", "Reproduction", 0, 0.1, 0.005, 0.04)
				.heading("Wolves")
				.slider("wolfGain", "Energy from a sheep", 1, 40, 1, 20)
				.slider("wolfBirth", "Reproduction", 0, 0.1, 0.005, 0.05)
				.heading("Grass")
				.checkbox("grass", "Grass (else unlimited food)", true)
				.slider("regrowth", "Regrowth time (steps)", 1, 100, 1, 30)
				.button("Add 20 wolves", "addWolves")
				.chart("Populations")
				.speed(15)
				.code(MODEL);
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/PredatorPrey.html";
		create().export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
