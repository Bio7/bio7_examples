// Epidemic (the SIR model): the JavaScript of the WindowBuilder JavaScript example
// (the design: src/webgui/EpidemicGui.java, the page: index.html created from it).
// Bio7 writes this file only once: this code stays when index.html is created again.
//
//   dS/dt = -beta S I     dI/dt = beta S I - gamma I     dR/dt = gamma I
//
// S, I and R are parts of the population; the vaccinated are immune from the start.

// The elements of the page (the ids are the names of the variables of the GUI class)
const scaleBeta = document.getElementById("scaleBeta");
const scaleGamma = document.getElementById("scaleGamma");
const scaleVaccinated = document.getElementById("scaleVaccinated");
const spinnerDays = document.getElementById("spinnerDays");
const chkRecovered = document.getElementById("chkRecovered");
const btnRun = document.getElementById("btnRun");
const btnAnimate = document.getElementById("btnAnimate");
const canvasChart = document.getElementById("canvasChart");
const outSummary = document.getElementById("outSummary");
const tableResults = document.getElementById("tableResults");

const COLORS = { S: "#4C72B0", I: "#C44E52", R: "#55A868" };
let data = [];
let animation = null;

function params() {
  return {
    beta: Number(scaleBeta.value) / 100,
    gamma: Number(scaleGamma.value) / 100,
    vaccinated: Number(scaleVaccinated.value) / 100,
    days: Number(spinnerDays.value)
  };
}

// The model with Runge-Kutta 4 (a step of 0.1 days); the result: one row per day [day, S, I, R]
function simulate(p) {
  const dt = 0.1, perDay = 10, steps = p.days * perDay;
  let i = 0.001;
  let s = Math.max(0, 1 - p.vaccinated - i);
  const f = (s, i) => [-p.beta * s * i, p.beta * s * i - p.gamma * i];
  const rows = [[0, s, i, 1 - s - i]];
  for (let k = 1; k <= steps; k++) {
    const [a1, b1] = f(s, i);
    const [a2, b2] = f(s + dt / 2 * a1, i + dt / 2 * b1);
    const [a3, b3] = f(s + dt / 2 * a2, i + dt / 2 * b2);
    const [a4, b4] = f(s + dt * a3, i + dt * b3);
    s += dt / 6 * (a1 + 2 * a2 + 2 * a3 + a4);
    i += dt / 6 * (b1 + 2 * b2 + 2 * b3 + b4);
    if (k % perDay === 0) {
      rows.push([k / perDay, s, i, 1 - s - i]);
    }
  }
  return rows;
}

// The chart: the days and the parts of the population (0 - 100 %), up to the day "upto"
function draw(upto) {
  const g = canvasChart.getContext("2d");
  const w = canvasChart.width, h = canvasChart.height;
  const left = 50, right = 120, top = 15, bottom = 35;
  const pw = w - left - right, ph = h - top - bottom;
  const days = params().days;
  const dark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  const ink = dark ? "#ddd" : "#333", grid = dark ? "#444" : "#e5e5e5";
  g.clearRect(0, 0, w, h);
  g.font = "12px system-ui, sans-serif";
  g.lineWidth = 1;
  /* The grid and the axes */
  for (let k = 0; k <= 4; k++) {
    const y = top + ph - k / 4 * ph;
    g.strokeStyle = grid; g.beginPath(); g.moveTo(left, y); g.lineTo(left + pw, y); g.stroke();
    g.fillStyle = ink; g.textAlign = "right"; g.fillText((k * 25) + " %", left - 6, y + 4);
  }
  for (let k = 0; k <= 5; k++) {
    const x = left + k / 5 * pw;
    g.fillStyle = ink; g.textAlign = "center"; g.fillText(Math.round(k / 5 * days), x, top + ph + 16);
  }
  g.fillText("Days", left + pw / 2, h - 4);
  g.strokeStyle = ink; g.strokeRect(left, top, pw, ph);
  /* The curves */
  const n = Math.min(upto, data.length - 1);
  const curves = [["S", 1, "Susceptible"], ["I", 2, "Infected"]];
  if (chkRecovered.checked) curves.push(["R", 3, "Immune"]);
  curves.forEach(([key, col, label], c) => {
    g.strokeStyle = COLORS[key]; g.lineWidth = 2; g.beginPath();
    for (let k = 0; k <= n; k++) {
      const x = left + data[k][0] / days * pw, y = top + ph - data[k][col] * ph;
      if (k === 0) g.moveTo(x, y); else g.lineTo(x, y);
    }
    g.stroke();
    /* The legend */
    const ly = top + 10 + c * 18;
    g.fillStyle = COLORS[key]; g.fillRect(left + pw + 12, ly - 8, 14, 4);
    g.fillStyle = ink; g.textAlign = "left"; g.fillText(label, left + pw + 32, ly - 3);
  });
}

// The summary and the table of the results
function results() {
  const p = params();
  let peak = 0, peakDay = 0;
  data.forEach(([day, s, i]) => { if (i > peak) { peak = i; peakDay = day; } });
  const last = data[data.length - 1];
  const r0 = p.beta / p.gamma;
  const infected = Math.max(0, 1 - p.vaccinated - last[1]);
  const pct = x => (100 * x).toFixed(1) + " %";
  outSummary.textContent = "R0 = " + r0.toFixed(2) + ", effective R = " + (r0 * (1 - p.vaccinated)).toFixed(2)
    + ": the peak is " + pct(peak) + " infected on day " + peakDay + ", " + pct(infected) + " are infected in total.";
  const rows = [["Quantity", "Value"],
    ["Basic reproduction number R0", r0.toFixed(2)],
    ["Effective reproduction number", (r0 * (1 - p.vaccinated)).toFixed(2)],
    ["Herd immunity threshold", r0 > 1 ? pct(1 - 1 / r0) : "-"],
    ["Peak of the infected", pct(peak)],
    ["Day of the peak", String(peakDay)],
    ["Infected in total", pct(infected)]];
  tableResults.innerHTML = rows.map((r, i) => "<tr>" + r.map(v => i === 0 ? "<th>" + v + "</th>" : "<td>" + v + "</td>").join("") + "</tr>").join("");
}

function run() {
  if (animation) { cancelAnimationFrame(animation); animation = null; }
  data = simulate(params());
  draw(data.length);
  results();
}

// The curves drawn day by day
function animate() {
  if (animation) cancelAnimationFrame(animation);
  data = simulate(params());
  results();
  let day = 0;
  const frame = () => {
    day += Math.max(1, Math.round(data.length / 240));
    draw(day);
    animation = day < data.length ? requestAnimationFrame(frame) : null;
  };
  frame();
}

btnRun.addEventListener("click", run);
btnAnimate.addEventListener("click", animate);
[scaleBeta, scaleGamma, scaleVaccinated, spinnerDays].forEach(e => e.addEventListener("input", run));
chkRecovered.addEventListener("change", () => draw(data.length));
run();
