package html;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports a 3D swarm model (boids after C. Reynolds) as an interactive HTML
 * page with WebGL: the number of the agents, the weights of the separation, the
 * alignment and the cohesion, the radius of the view, the speed and the
 * velocity vectors. The chart shows the order (polarization) of the swarm. The
 * simulation runs in the web browser (JavaScript), the view can be rotated with
 * the mouse.
 */
public class SwarmHtml {

	static final String MODEL = """
			function setup(sim) {
			  const n = Math.max(1, Math.min(3000, Math.round(sim.p.agents)));
			  sim.box = [1, 1, 1];
			  sim.n = n;
			  sim.pos = new Float32Array(n * 3);
			  sim.vel = new Float32Array(n * 3);
			  for (let i = 0; i < n * 3; i++) {
			    sim.pos[i] = 0.2 + 0.6 * sim.random();
			    sim.vel[i] = (sim.random() - 0.5) * 0.01;
			  }
			  update(sim);
			}

			function step(sim) {
			  const n = sim.n, p = sim.pos, v = sim.vel, r = sim.p.radius, r2 = r * r, maxSpeed = sim.p.speed * 0.01;
			  const next = new Float32Array(n * 3);
			  for (let i = 0; i < n; i++) {
			    let sx = 0, sy = 0, sz = 0, ax = 0, ay = 0, az = 0, cx = 0, cy = 0, cz = 0, count = 0;
			    for (let j = 0; j < n; j++) {
			      if (i === j) continue;
			      const dx = p[j * 3] - p[i * 3], dy = p[j * 3 + 1] - p[i * 3 + 1], dz = p[j * 3 + 2] - p[i * 3 + 2];
			      const d2 = dx * dx + dy * dy + dz * dz;
			      if (d2 > r2 || d2 === 0) continue;
			      count++;
			      /* Separation: away from close neighbors */
			      sx -= dx / d2; sy -= dy / d2; sz -= dz / d2;
			      ax += v[j * 3]; ay += v[j * 3 + 1]; az += v[j * 3 + 2];
			      cx += dx; cy += dy; cz += dz;
			    }
			    let vx = v[i * 3], vy = v[i * 3 + 1], vz = v[i * 3 + 2];
			    if (count > 0) {
			      vx += sim.p.separation * sx * 1e-5 + sim.p.alignment * (ax / count - vx) * 0.05 + sim.p.cohesion * cx / count * 0.01;
			      vy += sim.p.separation * sy * 1e-5 + sim.p.alignment * (ay / count - vy) * 0.05 + sim.p.cohesion * cy / count * 0.01;
			      vz += sim.p.separation * sz * 1e-5 + sim.p.alignment * (az / count - vz) * 0.05 + sim.p.cohesion * cz / count * 0.01;
			    }
			    /* Back into the box (a soft wall) */
			    for (let k = 0; k < 3; k++) {
			      const x = p[i * 3 + k];
			      if (x < 0.1) { if (k === 0) vx += 0.0005; if (k === 1) vy += 0.0005; if (k === 2) vz += 0.0005; }
			      if (x > 0.9) { if (k === 0) vx -= 0.0005; if (k === 1) vy -= 0.0005; if (k === 2) vz -= 0.0005; }
			    }
			    const s = Math.hypot(vx, vy, vz);
			    if (s > maxSpeed) { vx *= maxSpeed / s; vy *= maxSpeed / s; vz *= maxSpeed / s; }
			    next[i * 3] = vx; next[i * 3 + 1] = vy; next[i * 3 + 2] = vz;
			  }
			  sim.vel = next;
			  let ox = 0, oy = 0, oz = 0;
			  for (let i = 0; i < n; i++) {
			    for (let k = 0; k < 3; k++) {
			      p[i * 3 + k] = Math.max(0, Math.min(1, p[i * 3 + k] + next[i * 3 + k]));
			    }
			    const s = Math.hypot(next[i * 3], next[i * 3 + 1], next[i * 3 + 2]) || 1;
			    ox += next[i * 3] / s; oy += next[i * 3 + 1] / s; oz += next[i * 3 + 2] / s;
			  }
			  /* The order: 1 = all agents fly in the same direction */
			  sim.chart('Order', Math.hypot(ox, oy, oz) / n);
			  update(sim);
			}

			/* The view: the agents colored by their height and the velocity vectors */
			function update(sim) {
			  const n = sim.n, colors = new Float32Array(n * 3);
			  for (let i = 0; i < n; i++) {
			    const c = sim.rampColor(sim.pos[i * 3 + 2]);
			    colors[i * 3] = c[0]; colors[i * 3 + 1] = c[1]; colors[i * 3 + 2] = c[2];
			  }
			  sim.points = { xyz: sim.pos, colors: colors, radius: sim.p.size * 0.001 };
			  if (sim.p.vectors) {
			    const l = new Float32Array(n * 6);
			    for (let i = 0; i < n; i++) {
			      for (let k = 0; k < 3; k++) {
			        l[i * 6 + k] = sim.pos[i * 3 + k];
			        l[i * 6 + 3 + k] = sim.pos[i * 3 + k] + sim.vel[i * 3 + k] * 6;
			      }
			    }
			    sim.lines = { xyz: l, color: '#444444' };
			  } else {
			    sim.lines = null;
			  }
			}

			function scatter(sim) {
			  for (let i = 0; i < sim.n * 3; i++) sim.vel[i] = (sim.random() - 0.5) * 0.02;
			}
			""";

	public static HtmlSimulation create() {
		return new HtmlSimulation("Swarm (Boids)")
				.description("Agents which avoid close neighbors (separation), fly in the direction of the neighbors (alignment) and to their center (cohesion).")
				.view(HtmlSimulation.View.SCENE_3D)
				.heading("Agents")
				.number("agents", "Number of agents", 400)
				.slider("radius", "Radius of the view", 0.02, 0.4, 0.01, 0.12)
				.slider("speed", "Maximum speed", 0.1, 2, 0.05, 0.6)
				.restartOn("agents")
				.heading("Behavior")
				.slider("separation", "Separation", 0, 5, 0.1, 1.5)
				.slider("alignment", "Alignment", 0, 2, 0.05, 1)
				.slider("cohesion", "Cohesion", 0, 2, 0.05, 0.6)
				.heading("Display")
				.slider("size", "Size of the agents", 1, 20, 1, 7)
				.checkbox("vectors", "Velocity vectors", false)
				.button("Scatter the swarm", "scatter")
				.chart("Order of the swarm (1 = same direction)")
				.colorMap(com.eco.bio7.spatial.PlotLegend.VIRIDIS)
				.speed(30)
				.code(MODEL);
	}

	public static void main(String[] args) throws Exception {
		String file = System.getProperty("user.home") + "/Swarm.html";
		create().export(file);
		System.out.println("Exported: " + file);
		HtmlSimulation.open(file);
	}
}
