/*
 * Ants which forage for food with pheromone trails for the HTML simulation of
 * AntsScriptHtml. Edit the model in an editor of Bio7 (e.g., the generic
 * editor) and export it again (the export merges this file into the page).
 *
 * The ants leave the nest (the center), search food at random and follow the
 * pheromone trails. An ant with food walks home and lays a trail which
 * evaporates and diffuses. The view shows the pheromone (color map), the food
 * (the brightest color), the nest and the ants (dark = searching, red = with
 * food).
 *
 * The settings (sim.p): size, ants, sources, evaporation, diffusion,
 * deposit, follow, randomness.
 */

function setup(sim) {
  const n = Math.max(40, Math.min(300, Math.round(sim.p.size)));
  sim.width = n;
  sim.height = n;
  sim.pheromone = new Float32Array(n * n);
  sim.buffer = new Float32Array(n * n);
  sim.food = new Uint8Array(n * n);
  sim.cells = new Float32Array(n * n);
  sim.nest = [n / 2, n / 2];
  sim.collected = 0;
  sim.min = 0;
  sim.max = 1.2;
  for (let s = 0; s < sim.p.sources; s++) addFood(sim);
  const count = Math.max(1, Math.min(3000, Math.round(sim.p.ants)));
  sim.ants = [];
  for (let i = 0; i < count; i++) {
    sim.ants.push({ x: sim.nest[0], y: sim.nest[1], a: sim.random() * 2 * Math.PI, carry: false });
  }
  update(sim);
}

/* A patch of food at a random position (a button, too) */
function addFood(sim) {
  const n = sim.width, r = Math.max(2, Math.round(n * 0.05));
  let cx, cy;
  /* Not too close to the nest */
  do {
    cx = r + Math.floor(sim.random() * (n - 2 * r));
    cy = r + Math.floor(sim.random() * (n - 2 * r));
  } while (Math.hypot(cx - sim.nest[0], cy - sim.nest[1]) < n * 0.25);
  for (let y = cy - r; y <= cy + r; y++) {
    for (let x = cx - r; x <= cx + r; x++) {
      if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) sim.food[y * n + x] = 5;
    }
  }
}

function pheromoneAt(sim, x, y) {
  const n = sim.width, i = Math.floor(x), j = Math.floor(y);
  if (i < 0 || j < 0 || i >= n || j >= n) return -1;
  return sim.pheromone[j * n + i];
}

function step(sim) {
  const n = sim.width, p = sim.pheromone, food = sim.food, nest = sim.nest;
  const sense = 3, turn = 0.6;
  for (const ant of sim.ants) {
    if (ant.carry) {
      /* Home to the nest (with a little noise) and lays a trail */
      const home = Math.atan2(nest[1] - ant.y, nest[0] - ant.x);
      ant.a = home + (sim.random() - 0.5) * 0.6;
      const i = Math.floor(ant.y) * n + Math.floor(ant.x);
      p[i] = Math.min(1, p[i] + sim.p.deposit);
      if (Math.hypot(nest[0] - ant.x, nest[1] - ant.y) < 2) {
        ant.carry = false;
        ant.a += Math.PI;
        sim.collected++;
      }
    } else {
      /* Searches: follows the strongest pheromone of three directions */
      const left = pheromoneAt(sim, ant.x + sense * Math.cos(ant.a - turn), ant.y + sense * Math.sin(ant.a - turn));
      const ahead = pheromoneAt(sim, ant.x + sense * Math.cos(ant.a), ant.y + sense * Math.sin(ant.a));
      const right = pheromoneAt(sim, ant.x + sense * Math.cos(ant.a + turn), ant.y + sense * Math.sin(ant.a + turn));
      if (sim.random() < sim.p.follow && Math.max(left, ahead, right) > 0.01) {
        if (left > ahead && left > right) ant.a -= turn * 0.5;
        else if (right > ahead && right > left) ant.a += turn * 0.5;
      }
      ant.a += (sim.random() - 0.5) * sim.p.randomness;
    }
    let nx = ant.x + Math.cos(ant.a), ny = ant.y + Math.sin(ant.a);
    /* Turns at the border */
    if (nx < 0 || ny < 0 || nx >= n || ny >= n) {
      ant.a += Math.PI * (0.5 + sim.random());
      nx = Math.max(0, Math.min(n - 0.01, ant.x));
      ny = Math.max(0, Math.min(n - 0.01, ant.y));
    }
    ant.x = nx;
    ant.y = ny;
    const i = Math.floor(ant.y) * n + Math.floor(ant.x);
    if (!ant.carry && food[i] > 0) {
      food[i]--;
      ant.carry = true;
    }
  }
  /* The pheromone evaporates and diffuses */
  const b = sim.buffer, e = 1 - sim.p.evaporation, d = sim.p.diffusion;
  for (let y = 0; y < n; y++) {
    for (let x = 0; x < n; x++) {
      const i = y * n + x;
      const sum = (x > 0 ? p[i - 1] : p[i]) + (x < n - 1 ? p[i + 1] : p[i]) + (y > 0 ? p[i - n] : p[i]) + (y < n - 1 ? p[i + n] : p[i]);
      b[i] = e * ((1 - d) * p[i] + d * sum / 4);
    }
  }
  sim.pheromone = b;
  sim.buffer = p;
  let left = 0;
  for (let i = 0; i < n * n; i++) left += food[i];
  sim.chart('Collected food', sim.collected);
  sim.chart('Food left', left);
  update(sim);
  /* Stops when all food is collected */
  if (left === 0 && sim.ants.every(function (ant) { return !ant.carry; })) return false;
}

/* The view: the pheromone, the food, the nest and the ants */
function update(sim) {
  const n = sim.width, c = sim.cells;
  for (let i = 0; i < n * n; i++) c[i] = sim.food[i] > 0 ? 1.2 : sim.pheromone[i];
  const nx = Math.floor(sim.nest[0]), ny = Math.floor(sim.nest[1]);
  for (let y = ny - 2; y <= ny + 2; y++) for (let x = nx - 2; x <= nx + 2; x++) c[y * n + x] = 0.9;
  const x = [], y = [], colors = [];
  for (const ant of sim.ants) {
    x.push(ant.x);
    y.push(ant.y);
    colors.push(ant.carry ? '#e41a1c' : '#202020');
  }
  sim.agents = { x: x, y: y, colors: colors, radius: 0.6 };
}

/* Removes the pheromone (a button) */
function clearTrails(sim) {
  sim.pheromone.fill(0);
  update(sim);
}
