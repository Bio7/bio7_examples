// Boids in 3D with WebGL: the JavaScript of the WindowBuilder JavaScript example
// (the design: src/webgui/BoidsGlGui.java, the page: index.html created from it).
// Bio7 writes this file only once: this code stays when index.html is created again.
//
// Each boid steers by three rules of its neighbours (Reynolds 1987): separation
// (away from close boids), alignment (the mean direction) and cohesion (to the
// centre). The boids fly in a cube; they turn back at its walls.

// The elements of the page (the ids are the names of the variables of the GUI class)
const scaleBoids = document.getElementById("scaleBoids");
const scaleSeparation = document.getElementById("scaleSeparation");
const scaleAlignment = document.getElementById("scaleAlignment");
const scaleCohesion = document.getElementById("scaleCohesion");
const scaleSpeed = document.getElementById("scaleSpeed");
const chkRotate = document.getElementById("chkRotate");
const btnStart = document.getElementById("btnStart");
const btnStop = document.getElementById("btnStop");
const btnReset = document.getElementById("btnReset");
const glView = document.getElementById("glView");
const outStatus = document.getElementById("outStatus");

/* ---------------- WebGL: sharp on HiDPI screens (the size of the design stays the CSS size) */
const dpr = window.devicePixelRatio || 1;
glView.style.width = glView.width + "px";
glView.width = Math.round(glView.width * dpr);
glView.height = Math.round(glView.height * dpr);
const gl = glView.getContext("webgl2") || glView.getContext("webgl");

const VERTEX = `
attribute vec3 aPos;
attribute vec3 aColor;
uniform mat4 uMatrix;
uniform float uPointSize;
varying vec3 vColor;
void main() {
  gl_Position = uMatrix * vec4(aPos, 1.0);
  gl_PointSize = uPointSize / gl_Position.w;
  vColor = aColor;
}`;
const FRAGMENT = `
precision mediump float;
uniform bool uRound;
varying vec3 vColor;
void main() {
  if (uRound && length(gl_PointCoord - vec2(0.5)) > 0.5) discard;
  gl_FragColor = vec4(vColor, 1.0);
}`;

function shader(type, source) {
  const s = gl.createShader(type);
  gl.shaderSource(s, source);
  gl.compileShader(s);
  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
  return s;
}

const program = gl.createProgram();
gl.attachShader(program, shader(gl.VERTEX_SHADER, VERTEX));
gl.attachShader(program, shader(gl.FRAGMENT_SHADER, FRAGMENT));
gl.linkProgram(program);
gl.useProgram(program);
const loc = {
  pos: gl.getAttribLocation(program, "aPos"),
  color: gl.getAttribLocation(program, "aColor"),
  matrix: gl.getUniformLocation(program, "uMatrix"),
  pointSize: gl.getUniformLocation(program, "uPointSize"),
  round: gl.getUniformLocation(program, "uRound")
};
const posBuffer = gl.createBuffer();
const colorBuffer = gl.createBuffer();
gl.enable(gl.DEPTH_TEST);

/* The edges of the cube (from -1 to 1) */
const CUBE = [];
for (const [a, b] of [[0, 1], [1, 3], [3, 2], [2, 0], [4, 5], [5, 7], [7, 6], [6, 4], [0, 4], [1, 5], [2, 6], [3, 7]]) {
  for (const k of [a, b]) CUBE.push(k & 1 ? 1 : -1, k & 2 ? 1 : -1, k & 4 ? 1 : -1);
}

/* ---------------- Matrices (column major, as WebGL) */
function perspective(fovy, aspect, near, far) {
  const f = 1 / Math.tan(fovy / 2), nf = 1 / (near - far);
  return [f / aspect, 0, 0, 0, 0, f, 0, 0, 0, 0, (far + near) * nf, -1, 0, 0, 2 * far * near * nf, 0];
}
function multiply(a, b) {
  const r = new Array(16).fill(0);
  for (let c = 0; c < 4; c++) for (let row = 0; row < 4; row++) for (let k = 0; k < 4; k++) r[c * 4 + row] += a[k * 4 + row] * b[c * 4 + k];
  return r;
}
/* The camera: on a circle around the cube (the angle), a little above it */
function view(angle) {
  const eye = [5 * Math.sin(angle), 2, 5 * Math.cos(angle)];
  const z = normalize(eye), x = normalize(cross([0, 1, 0], z)), y = cross(z, x);
  return [x[0], y[0], z[0], 0, x[1], y[1], z[1], 0, x[2], y[2], z[2], 0, -dot(x, eye), -dot(y, eye), -dot(z, eye), 1];
}
const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const normalize = a => { const l = Math.hypot(a[0], a[1], a[2]) || 1; return [a[0] / l, a[1] / l, a[2] / l]; };

/* ---------------- The boids */
let n = 0, pos, vel, running = true, frame = 0, angle = 0.6, polarization = 0;
const RADIUS = 0.3, CLOSE = 0.1;

function reset() {
  n = Number(scaleBoids.value);
  pos = new Float32Array(3 * n);
  vel = new Float32Array(3 * n);
  for (let i = 0; i < 3 * n; i++) {
    pos[i] = Math.random() * 1.6 - 0.8;
    vel[i] = (Math.random() - 0.5) * 0.02;
  }
  frame = 0;
}

// One time step: the three rules within the radius, the walls, the speed limits
function update() {
  const ws = Number(scaleSeparation.value) / 100 * 0.004;
  const wa = Number(scaleAlignment.value) / 100 * 0.12;
  const wc = Number(scaleCohesion.value) / 100 * 0.004;
  const maxSpeed = Number(scaleSpeed.value) / 1000 * 2;
  const next = new Float32Array(vel);
  let sx = 0, sy = 0, sz = 0;
  for (let i = 0; i < n; i++) {
    const ix = 3 * i;
    let cnt = 0, ax = 0, ay = 0, az = 0, cx = 0, cy = 0, cz = 0, px = 0, py = 0, pz = 0;
    for (let j = 0; j < n; j++) {
      if (j === i) continue;
      const jx = 3 * j;
      const dx = pos[jx] - pos[ix], dy = pos[jx + 1] - pos[ix + 1], dz = pos[jx + 2] - pos[ix + 2];
      const d2 = dx * dx + dy * dy + dz * dz;
      if (d2 > RADIUS * RADIUS) continue;
      cnt++;
      ax += vel[jx]; ay += vel[jx + 1]; az += vel[jx + 2];
      cx += dx; cy += dy; cz += dz;
      if (d2 < CLOSE * CLOSE) { const f = 1 / Math.max(d2, 1e-4); px -= dx * f; py -= dy * f; pz -= dz * f; }
    }
    let vx = next[ix], vy = next[ix + 1], vz = next[ix + 2];
    if (cnt > 0) {
      vx += wa * (ax / cnt - vx) + wc * cx / cnt + ws * px * 0.01;
      vy += wa * (ay / cnt - vy) + wc * cy / cnt + ws * py * 0.01;
      vz += wa * (az / cnt - vz) + wc * cz / cnt + ws * pz * 0.01;
    }
    /* The walls: turn back near the faces of the cube */
    for (let k = 0; k < 3; k++) {
      const p = pos[ix + k];
      if (p > 0.8) (k === 0 ? vx -= 0.0012 : k === 1 ? vy -= 0.0012 : vz -= 0.0012);
      if (p < -0.8) (k === 0 ? vx += 0.0012 : k === 1 ? vy += 0.0012 : vz += 0.0012);
    }
    /* The speed between a half and the whole maximum */
    const s = Math.hypot(vx, vy, vz) || 1e-9;
    const f = s > maxSpeed ? maxSpeed / s : s < maxSpeed / 2 ? maxSpeed / 2 / s : 1;
    next[ix] = vx * f; next[ix + 1] = vy * f; next[ix + 2] = vz * f;
    sx += next[ix] / (s * f); sy += next[ix + 1] / (s * f); sz += next[ix + 2] / (s * f);
  }
  vel = next;
  for (let i = 0; i < 3 * n; i++) pos[i] = Math.max(-1, Math.min(1, pos[i] + vel[i]));
  /* The polarization: the length of the mean direction (0: disordered, 1: all fly in parallel) */
  polarization = Math.hypot(sx, sy, sz) / n;
  frame++;
}

// Draws the cube (lines) and the boids (round points, the color: their direction)
function drawGlView() {
  gl.viewport(0, 0, glView.width, glView.height);
  gl.clearColor(0.1, 0.1, 0.14, 1);
  gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  const matrix = multiply(perspective(Math.PI / 4, glView.width / glView.height, 0.1, 20), view(angle));
  gl.uniformMatrix4fv(loc.matrix, false, new Float32Array(matrix));

  const draw = (positions, colors, mode, count, round, size) => {
    gl.bindBuffer(gl.ARRAY_BUFFER, posBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, positions, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(loc.pos);
    gl.vertexAttribPointer(loc.pos, 3, gl.FLOAT, false, 0, 0);
    gl.bindBuffer(gl.ARRAY_BUFFER, colorBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, colors, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(loc.color);
    gl.vertexAttribPointer(loc.color, 3, gl.FLOAT, false, 0, 0);
    gl.uniform1i(loc.round, round ? 1 : 0);
    gl.uniform1f(loc.pointSize, size);
    gl.drawArrays(mode, 0, count);
  };
  draw(new Float32Array(CUBE), new Float32Array(CUBE.length).fill(0.45), gl.LINES, CUBE.length / 3, false, 1);
  const colors = new Float32Array(3 * n);
  for (let i = 0; i < n; i++) {
    const s = Math.hypot(vel[3 * i], vel[3 * i + 1], vel[3 * i + 2]) || 1;
    for (let k = 0; k < 3; k++) colors[3 * i + k] = 0.55 + 0.45 * vel[3 * i + k] / s;
  }
  draw(pos, colors, gl.POINTS, n, true, 24 * dpr);
  outStatus.textContent = "Frame " + frame + ": " + n + " boids, polarization " + polarization.toFixed(2)
    + " (0: disordered, 1: all fly in parallel)" + (running ? "" : " - stopped");
}

function loop() {
  if (running) update();
  if (chkRotate.checked) angle += 0.004;
  drawGlView();
  requestAnimationFrame(loop);
}

btnStart.addEventListener("click", () => running = true);
btnStop.addEventListener("click", () => running = false);
btnReset.addEventListener("click", reset);
scaleBoids.addEventListener("change", reset);
reset();
loop();
