// The forest fire model (Drossel and Schwabl, a cellular automaton): the JavaScript of
// the WindowBuilder JavaScript example (the design: src/webgui/ForestFireGui.java, the
// page: index.html created from it). Bio7 writes this file only once: this code stays
// when index.html is created again.
//
// A time step: a burning tree becomes an empty cell; a tree burns if one of its four
// neighbours burns or (with the probability f) by lightning; a tree grows on an empty
// cell with the probability p.

// The elements of the page (the ids are the names of the variables of the GUI class)
const scaleGrowth = document.getElementById("scaleGrowth");
const scaleLightning = document.getElementById("scaleLightning");
const comboSize = document.getElementById("comboSize");
const spinnerSpeed = document.getElementById("spinnerSpeed");
const btnStart = document.getElementById("btnStart");
const btnStop = document.getElementById("btnStop");
const btnReset = document.getElementById("btnReset");
const canvasForest = document.getElementById("canvasForest");
const canvasSeries = document.getElementById("canvasSeries");
const outStatus = document.getElementById("outStatus");

const EMPTY = 0, TREE = 1, FIRE = 2;
/* The colors of the cells (r, g, b): ground, tree, fire */
const COLORS = [[60, 42, 25], [46, 125, 50], [255, 152, 0]];
const MAX_SERIES = 600;

let size, cells, next, image, offscreen;
let running = false, step = 0, series = [];

function reset() {
  size = Number(comboSize.value);
  cells = new Uint8Array(size * size);
  next = new Uint8Array(size * size);
  for (let k = 0; k < cells.length; k++) {
    cells[k] = Math.random() < 0.5 ? TREE : EMPTY;
  }
  offscreen = document.createElement("canvas");
  offscreen.width = size;
  offscreen.height = size;
  image = offscreen.getContext("2d").createImageData(size, size);
  step = 0;
  series = [];
  draw();
}

// One time step of the automaton (the border: no neighbours outside)
function update() {
  const p = Number(scaleGrowth.value) / 1000;
  const f = Number(scaleLightning.value) / 100000;
  let trees = 0, fires = 0;
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const k = y * size + x;
      const c = cells[k];
      let n;
      if (c === FIRE) {
        n = EMPTY;
      } else if (c === TREE) {
        const burning = (x > 0 && cells[k - 1] === FIRE) || (x < size - 1 && cells[k + 1] === FIRE)
          || (y > 0 && cells[k - size] === FIRE) || (y < size - 1 && cells[k + size] === FIRE);
        n = burning || Math.random() < f ? FIRE : TREE;
      } else {
        n = Math.random() < p ? TREE : EMPTY;
      }
      next[k] = n;
      if (n === TREE) trees++; else if (n === FIRE) fires++;
    }
  }
  [cells, next] = [next, cells];
  step++;
  series.push([trees / cells.length, fires / cells.length]);
  if (series.length > MAX_SERIES) series.shift();
}

// The forest (one pixel per cell, scaled to the canvas) and the time series
function draw() {
  const d = image.data;
  for (let k = 0; k < cells.length; k++) {
    const col = COLORS[cells[k]];
    d[4 * k] = col[0]; d[4 * k + 1] = col[1]; d[4 * k + 2] = col[2]; d[4 * k + 3] = 255;
  }
  offscreen.getContext("2d").putImageData(image, 0, 0);
  const g = canvasForest.getContext("2d");
  g.imageSmoothingEnabled = false;
  g.drawImage(offscreen, 0, 0, canvasForest.width, canvasForest.height);
  drawSeries();
  const last = series.length ? series[series.length - 1] : [0, 0];
  outStatus.textContent = "Step " + step + ": " + (100 * last[0]).toFixed(1) + " % trees, "
    + Math.round(last[1] * cells.length) + " burning cells" + (running ? "" : " (stopped)");
}

// The part of the trees (green) and of the burning cells (orange, x 10) over the last steps
function drawSeries() {
  const g = canvasSeries.getContext("2d");
  const w = canvasSeries.width, h = canvasSeries.height;
  g.clearRect(0, 0, w, h);
  [[0, "#2e7d32", 1], [1, "#ff9800", 10]].forEach(([col, color, factor]) => {
    g.strokeStyle = color; g.lineWidth = 1.5; g.beginPath();
    series.forEach((v, k) => {
      const x = k / (MAX_SERIES - 1) * w, y = h - 4 - Math.min(1, v[col] * factor) * (h - 8);
      if (k === 0) g.moveTo(x, y); else g.lineTo(x, y);
    });
    g.stroke();
  });
}

function loop() {
  if (!running) return;
  const n = Number(spinnerSpeed.value);
  for (let k = 0; k < n; k++) update();
  draw();
  requestAnimationFrame(loop);
}

btnStart.addEventListener("click", () => { if (!running) { running = true; loop(); } });
btnStop.addEventListener("click", () => { running = false; draw(); });
btnReset.addEventListener("click", () => { running = false; reset(); });
comboSize.addEventListener("change", () => { running = false; reset(); });
reset();
