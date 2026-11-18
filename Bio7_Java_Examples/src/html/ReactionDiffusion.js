/*
 * Patterns of a reaction-diffusion system (the Gray-Scott model) for the HTML
 * simulation of ReactionDiffusionScriptHtml. Edit the model in an editor of
 * Bio7 (e.g., the generic editor): the exported page loads this file (it is
 * copied next to the page), reload the page to see the changes.
 *
 * Two substances U and V: U + 2V -> 3V, U is fed with the rate F, V is removed
 * with the rate F + k.
 *   du/dt = Du * lap(u) - u * v^2 + F * (1 - u)
 *   dv/dt = Dv * lap(v) + u * v^2 - (F + k) * v
 *
 * The settings (sim.p): size, preset, feed, kill, iterations, seeds.
 */

/* Typical values of the feed rate F and the kill rate k */
const PRESETS = {
  'Coral': [0.0545, 0.062],
  'Mitosis': [0.0367, 0.0649],
  'Spots': [0.030, 0.062],
  'Spirals': [0.014, 0.054],
  'Worms': [0.078, 0.061],
  'Maze': [0.029, 0.057]
};

function setup(sim) {
  const n = Math.max(40, Math.min(400, Math.round(sim.p.size)));
  sim.width = n;
  sim.height = n;
  sim.u = new Float32Array(n * n).fill(1);
  sim.v = new Float32Array(n * n);
  sim.u2 = new Float32Array(n * n);
  sim.v2 = new Float32Array(n * n);
  /* The view displays the concentration of V with the color map */
  sim.min = 0;
  sim.max = 0.45;
  for (let s = 0; s < sim.p.seeds; s++) seed(sim);
  sim.cells = sim.v;
}

/* The rates of the preset or of the sliders (Custom) */
function rates(sim) {
  const preset = PRESETS[sim.p.preset];
  return preset ? preset : [sim.p.feed, sim.p.kill];
}

function step(sim) {
  const n = sim.width, r = rates(sim), F = r[0], k = r[1], Du = 0.2097, Dv = 0.105;
  /* Several iterations per step: the patterns grow slowly */
  for (let it = 0; it < sim.p.iterations; it++) {
    const u = sim.u, v = sim.v, nu = sim.u2, nv = sim.v2;
    for (let y = 0; y < n; y++) {
      const ym = ((y - 1 + n) % n) * n, y0 = y * n, yp = ((y + 1) % n) * n;
      for (let x = 0; x < n; x++) {
        const xm = (x - 1 + n) % n, xp = (x + 1) % n, i = y0 + x;
        /* The Laplacian (a 3 x 3 stencil on a torus) */
        const lu = 0.2 * (u[ym + x] + u[yp + x] + u[y0 + xm] + u[y0 + xp])
          + 0.05 * (u[ym + xm] + u[ym + xp] + u[yp + xm] + u[yp + xp]) - u[i];
        const lv = 0.2 * (v[ym + x] + v[yp + x] + v[y0 + xm] + v[y0 + xp])
          + 0.05 * (v[ym + xm] + v[ym + xp] + v[yp + xm] + v[yp + xp]) - v[i];
        const uvv = u[i] * v[i] * v[i];
        nu[i] = u[i] + 4 * Du * lu - uvv + F * (1 - u[i]);
        nv[i] = v[i] + 4 * Dv * lv + uvv - (F + k) * v[i];
      }
    }
    sim.u = nu; sim.u2 = u;
    sim.v = nv; sim.v2 = v;
  }
  sim.cells = sim.v;
  let sum = 0;
  for (let i = 0; i < n * n; i++) sum += sim.v[i];
  sim.chart('Mean of V', sum / (n * n));
}

/* Adds a square with the substance V at a random position (a button, too) */
function seed(sim) {
  const n = sim.width, r = Math.max(2, Math.round(n * 0.03));
  const cx = Math.floor(sim.random() * n), cy = Math.floor(sim.random() * n);
  for (let y = cy - r; y <= cy + r; y++) {
    for (let x = cx - r; x <= cx + r; x++) {
      const i = ((y + n) % n) * n + (x + n) % n;
      sim.u[i] = 0.5;
      sim.v[i] = 0.25 + 0.05 * sim.random();
    }
  }
}

/* Removes the substance V (a button) */
function clearV(sim) {
  sim.u.fill(1);
  sim.v.fill(0);
  seed(sim);
  sim.cells = sim.v;
}
