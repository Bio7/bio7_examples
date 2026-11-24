// The JavaScript of the food chain app (WindowBuilder JavaScript with WebGL; the
// design: src/gui/windowbuilder/FoodChainGlGui.java, index.html is created from it). Bio7 writes
// this file only once: this code stays when index.html is created again.
//
// The canvas glAttractor of the design (its name starts with "gl") is a canvas with
// WebGL; drag it with the mouse to rotate the view. The canvas canvasSeries is a
// normal 2D canvas.
//
// The food chain of Hastings and Powell (1991): prey x, predator y, top predator z.

// The elements of the page (the ids are the names of the variables of the GUI class)
const scaleB1 = document.getElementById("scaleB1");
const spinnerTime = document.getElementById("spinnerTime");
const scaleTransient = document.getElementById("scaleTransient");
const comboColor = document.getElementById("comboColor");
const btnRun = document.getElementById("btnRun");
const glAttractor = document.getElementById("glAttractor");
const canvasSeries = document.getElementById("canvasSeries");
const outSummary = document.getElementById("outSummary");

/* ---------------- The model (Runge-Kutta 4, a step of 0.1) */
function simulate(b1, time, transient) {
  const a1 = 5, a2 = 0.1, b2 = 2, d1 = 0.4, d2 = 0.01, dt = 0.1;
  const f = s => {
    const f1 = a1 * s[0] / (1 + b1 * s[0]), f2 = a2 * s[1] / (1 + b2 * s[1]);
    return [s[0] * (1 - s[0]) - f1 * s[1], f1 * s[1] - f2 * s[2] - d1 * s[1], f2 * s[2] - d2 * s[2]];
  };
  const add = (s, k, h) => [s[0] + h * k[0], s[1] + h * k[1], s[2] + h * k[2]];
  const n = Math.round(time / dt), first = Math.floor((n + 1) * transient);
  const data = { time: [], x: [], y: [], z: [] };
  let s = [0.8, 0.2, 8];
  for (let i = 0; i <= n; i++) {
    if (i >= first) { data.time.push(i * dt); data.x.push(s[0]); data.y.push(s[1]); data.z.push(s[2]); }
    const k1 = f(s), k2 = f(add(s, k1, dt / 2)), k3 = f(add(s, k2, dt / 2)), k4 = f(add(s, k3, dt));
    s = [0, 1, 2].map(j => s[j] + dt / 6 * (k1[j] + 2 * k2[j] + 2 * k3[j] + k4[j]));
  }
  return data;
}

// The local maxima of a series (a cycle has few different maxima, chaos many)
function maxima(z) {
  const m = [];
  for (let i = 1; i < z.length - 1; i++) if (z[i] > z[i - 1] && z[i] >= z[i + 1]) m.push(z[i]);
  return m;
}

/* A color scale from blue to red (v from 0 to 1) */
function color(v) {
  const stops = [[0.27, 0.46, 0.71], [0.4, 0.76, 0.65], [0.99, 0.88, 0.55], [0.84, 0.19, 0.15]];
  const p = Math.min(0.9999, Math.max(0, v)) * (stops.length - 1), i = Math.floor(p), t = p - i;
  return [0, 1, 2].map(k => stops[i][k] + t * (stops[i + 1][k] - stops[i][k]));
}

/* ---------------- WebGL: sharp on HiDPI screens (the size of the design stays the CSS size) */
const dpr = window.devicePixelRatio || 1;
glAttractor.style.width = glAttractor.width + "px";
glAttractor.width = Math.round(glAttractor.width * dpr);
glAttractor.height = Math.round(glAttractor.height * dpr);
const gl = glAttractor.getContext("webgl2") || glAttractor.getContext("webgl");

function shader(type, source) {
  const s = gl.createShader(type);
  gl.shaderSource(s, source);
  gl.compileShader(s);
  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
  return s;
}
const program = gl.createProgram();
gl.attachShader(program, shader(gl.VERTEX_SHADER, `
attribute vec3 aPos; attribute vec3 aColor; uniform mat4 uMatrix; varying vec3 vColor;
void main() { gl_Position = uMatrix * vec4(aPos, 1.0); vColor = aColor; }`));
gl.attachShader(program, shader(gl.FRAGMENT_SHADER, `
precision mediump float; varying vec3 vColor;
void main() { gl_FragColor = vec4(vColor, 1.0); }`));
gl.linkProgram(program);
gl.useProgram(program);
const aPos = gl.getAttribLocation(program, "aPos"), aColor = gl.getAttribLocation(program, "aColor");
const uMatrix = gl.getUniformLocation(program, "uMatrix");
const posBuffer = gl.createBuffer(), colorBuffer = gl.createBuffer();
gl.enable(gl.DEPTH_TEST);

/* The camera: the angles (drag with the mouse), a slow rotation when not dragged */
let yaw = 0.8, pitch = 0.35, dragging = false, lastX = 0, lastY = 0;
glAttractor.addEventListener("pointerdown", e => { dragging = true; lastX = e.clientX; lastY = e.clientY; glAttractor.setPointerCapture(e.pointerId); });
glAttractor.addEventListener("pointerup", () => dragging = false);
glAttractor.addEventListener("pointermove", e => {
  if (!dragging) return;
  yaw += (e.clientX - lastX) * 0.01;
  pitch = Math.max(-1.4, Math.min(1.4, pitch + (e.clientY - lastY) * 0.01));
  lastX = e.clientX; lastY = e.clientY;
});

function matrix() {
  const aspect = glAttractor.width / glAttractor.height, f = 1 / Math.tan(Math.PI / 8), near = 0.1, far = 20, nf = 1 / (near - far);
  const p = [f / aspect, 0, 0, 0, 0, f, 0, 0, 0, 0, (far + near) * nf, -1, 0, 0, 2 * far * near * nf, 0];
  /* The view: rotations by pitch and yaw, the scene 4.5 units in front of the camera */
  const cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
  const v = [cy, sp * sy, -cp * sy, 0, 0, cp, sp, 0, sy, -sp * cy, cp * cy, 0, 0, 0, -4.5, 1];
  const r = new Array(16).fill(0);
  for (let c = 0; c < 4; c++) for (let row = 0; row < 4; row++) for (let k = 0; k < 4; k++) r[c * 4 + row] += p[k * 4 + row] * v[c * 4 + k];
  return r;
}

/* The box of the data (-1 to 1) */
const BOX = [];
for (const [a, b] of [[0, 1], [1, 3], [3, 2], [2, 0], [4, 5], [5, 7], [7, 6], [6, 4], [0, 4], [1, 5], [2, 6], [3, 7]]) {
  for (const k of [a, b]) BOX.push(k & 1 ? 1 : -1, k & 2 ? 1 : -1, k & 4 ? 1 : -1);
}

let curve = { positions: new Float32Array(0), colors: new Float32Array(0), count: 0 };

// The curve in the box: x, y, z scaled to -1 ... 1 (z: the height)
function prepare(d) {
  const range = a => { let lo = Infinity, hi = -Infinity; for (const v of a) { if (v < lo) lo = v; if (v > hi) hi = v; } return [lo, Math.max(hi - lo, 1e-9)]; };
  const [x0, xs] = range(d.x), [y0, ys] = range(d.y), [z0, zs] = range(d.z), [t0, ts] = range(d.time);
  const n = d.x.length, positions = new Float32Array(3 * n), colors = new Float32Array(3 * n);
  const byZ = comboColor.value === "Top predator";
  for (let i = 0; i < n; i++) {
    positions[3 * i] = 2 * (d.x[i] - x0) / xs - 1;
    positions[3 * i + 1] = 2 * (d.z[i] - z0) / zs - 1;
    positions[3 * i + 2] = 2 * (d.y[i] - y0) / ys - 1;
    colors.set(color(byZ ? (d.z[i] - z0) / zs : (d.time[i] - t0) / ts), 3 * i);
  }
  curve = { positions, colors, count: n };
}

function drawArrays(positions, colors, mode, count) {
  gl.bindBuffer(gl.ARRAY_BUFFER, posBuffer);
  gl.bufferData(gl.ARRAY_BUFFER, positions, gl.DYNAMIC_DRAW);
  gl.enableVertexAttribArray(aPos);
  gl.vertexAttribPointer(aPos, 3, gl.FLOAT, false, 0, 0);
  gl.bindBuffer(gl.ARRAY_BUFFER, colorBuffer);
  gl.bufferData(gl.ARRAY_BUFFER, colors, gl.DYNAMIC_DRAW);
  gl.enableVertexAttribArray(aColor);
  gl.vertexAttribPointer(aColor, 3, gl.FLOAT, false, 0, 0);
  gl.drawArrays(mode, 0, count);
}

// Draws the 3D view (the box and the curve)
function drawGlAttractor() {
  gl.viewport(0, 0, glAttractor.width, glAttractor.height);
  gl.clearColor(0.1, 0.1, 0.14, 1);
  gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  gl.uniformMatrix4fv(uMatrix, false, new Float32Array(matrix()));
  drawArrays(new Float32Array(BOX), new Float32Array(BOX.length).fill(0.45), gl.LINES, BOX.length / 3);
  drawArrays(curve.positions, curve.colors, gl.LINE_STRIP, curve.count);
}

// The normal 2D canvas: the top predator over time
function drawCanvasSeries(d) {
  const g = canvasSeries.getContext("2d"), w = canvasSeries.width, h = canvasSeries.height, m = 40;
  let lo = Infinity, hi = -Infinity;
  for (const v of d.z) { if (v < lo) lo = v; if (v > hi) hi = v; }
  g.clearRect(0, 0, w, h);
  g.fillStyle = "#888"; g.font = "12px system-ui, sans-serif";
  g.fillText("The top predator over time", m, 18);
  g.fillText(hi.toFixed(1), 4, m); g.fillText(lo.toFixed(1), 4, h - m);
  g.strokeStyle = "#ccc"; g.strokeRect(m, m - 10, w - m - 10, h - 2 * m + 10);
  g.strokeStyle = "#C44E52"; g.lineWidth = 1; g.beginPath();
  const n = d.z.length;
  for (let i = 0; i < n; i++) {
    const x = m + i / (n - 1) * (w - m - 10), y = h - m - (d.z[i] - lo) / Math.max(hi - lo, 1e-9) * (h - 2 * m);
    if (i === 0) g.moveTo(x, y); else g.lineTo(x, y);
  }
  g.stroke();
}

let data;
function run() {
  const b1 = Number(scaleB1.value) / 10;
  data = simulate(b1, Number(spinnerTime.value), Number(scaleTransient.value) / 100);
  prepare(data);
  drawCanvasSeries(data);
  const m = maxima(data.z), k = new Set(m.map(v => v.toFixed(2))).size;
  const kind = m.length < 3 ? "a stable state" : k === 1 ? "a cycle (one maximum of the top predator)"
    : k <= 4 ? "a cycle (" + k + " different maxima of the top predator)"
    : "chaos (" + k + " different maxima of the top predator)";
  outSummary.textContent = "b1 = " + b1.toFixed(1) + ": " + kind + "; the top predator varies between "
    + Math.min(...data.z).toFixed(2) + " and " + Math.max(...data.z).toFixed(2) + ".";
}

function loop() {
  if (!dragging) yaw += 0.003;
  drawGlAttractor();
  requestAnimationFrame(loop);
}

btnRun.addEventListener("click", run);
[scaleB1, spinnerTime, scaleTransient].forEach(e => e.addEventListener("change", run));
comboColor.addEventListener("change", () => prepare(data));
run();
loop();
