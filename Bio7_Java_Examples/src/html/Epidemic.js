/*
 * An epidemic on a grid (an SIR model as a cellular automaton) for an HTML
 * simulation of Bio7 (see EpidemicScriptHtml). Edit the model in an editor of
 * Bio7 and export it again: the page loads this file or the export merges it
 * into one HTML file (embedScript).
 *
 * The states: 0 = susceptible, 1 = infected, 2 = recovered (immune), 3 = empty.
 * The settings (sim.p): size, density, infection, recovery, immunity, moore,
 * vaccination.
 */

function setup(sim) {
  const n = Math.max(20, Math.min(400, Math.round(sim.p.size)));
  sim.width = n;
  sim.height = n;
  sim.cells = new Uint8Array(n * n);
  sim.next = new Uint8Array(n * n);
  /* The days since the infection or the recovery */
  sim.age = new Uint16Array(n * n);
  sim.palette = ['#9ecae1', '#d62728', '#2ca02c', '#f0f0f0'];
  for (let i = 0; i < n * n; i++) {
    sim.cells[i] = sim.random() < sim.p.density ? 0 : 3;
    /* Vaccinated individuals are immune */
    if (sim.cells[i] === 0 && sim.random() < sim.p.vaccination) sim.cells[i] = 2;
  }
  infectCenter(sim);
  count(sim);
}

/* Infects a few individuals in the center (a button, too) */
function infectCenter(sim) {
  const n = sim.width, c = Math.floor(n / 2);
  for (let dy = -1; dy <= 1; dy++) {
    for (let dx = -1; dx <= 1; dx++) {
      const i = (c + dy) * n + c + dx;
      if (sim.cells[i] === 0) { sim.cells[i] = 1; sim.age[i] = 0; }
    }
  }
}

function step(sim) {
  const n = sim.width, c = sim.cells, next = sim.next, age = sim.age;
  const moore = sim.p.moore;
  for (let y = 0; y < n; y++) {
    for (let x = 0; x < n; x++) {
      const i = y * n + x, s = c[i];
      let t = s;
      if (s === 0) {
        /* The probability of an infection grows with the infected neighbors */
        let infected = 0;
        for (let dy = -1; dy <= 1; dy++) {
          for (let dx = -1; dx <= 1; dx++) {
            if ((dx === 0 && dy === 0) || (!moore && dx !== 0 && dy !== 0)) continue;
            const nx = (x + dx + n) % n, ny = (y + dy + n) % n;
            if (c[ny * n + nx] === 1) infected++;
          }
        }
        if (infected > 0 && sim.random() < 1 - Math.pow(1 - sim.p.infection, infected)) { t = 1; age[i] = 0; }
      } else if (s === 1) {
        age[i]++;
        if (age[i] >= sim.p.recovery) { t = 2; age[i] = 0; }
      } else if (s === 2) {
        /* The immunity is lost after a time (0 = permanent) */
        age[i]++;
        if (sim.p.immunity > 0 && age[i] >= sim.p.immunity) { t = 0; age[i] = 0; }
      }
      next[i] = t;
    }
  }
  sim.cells = next;
  sim.next = c;
  count(sim);
  /* Stops if the epidemic is over (return false) */
  if (sim.infected === 0 && sim.p.immunity === 0) return false;
}

/* The numbers of the states for the chart */
function count(sim) {
  let s = 0, i = 0, r = 0;
  for (let k = 0; k < sim.cells.length; k++) {
    const v = sim.cells[k];
    if (v === 0) s++; else if (v === 1) i++; else if (v === 2) r++;
  }
  sim.infected = i;
  sim.chart('Susceptible', s);
  sim.chart('Infected', i);
  sim.chart('Recovered', r);
}
