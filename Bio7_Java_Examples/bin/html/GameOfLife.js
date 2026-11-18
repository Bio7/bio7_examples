/*
 * The Game of Life for the HTML simulation of GameOfLifeScriptHtml (edit it in an
 * editor of Bio7, the page loads this file).
 */
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
