"""Tests the mouse and the keyboard in the exported pages of HeatDiffusion (a grid: click, drag, key c)
and NetworkEpidemic (the 3D view: a click infects the nearest person) of Java, Python and R.

    python3 page/input.py [OUT]        (default: tests/work/out; the pages of bio7/run_bio7.sh)

Writes OUT/input.json; the exit code is 1 if a test fails.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cdp import Chrome  # noqa: E402

OUT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "work", "out"))

COMMON = r"""
const wait = ms => new Promise(r => setTimeout(r, ms));
const fire = (el, type, p, extra) => el.dispatchEvent(new PointerEvent(type, Object.assign({ clientX: p[0], clientY: p[1], button: 0, buttons: 1, bubbles: true, pointerId: 1, isPrimary: true }, extra || {})));
const LANG = typeof bio7webR !== 'undefined' ? 'r' : typeof bio7py !== 'undefined' ? 'py' : 'java';
async function rstr(code) { const r = await bio7webR.evalR(code); const j = await r.toJs(); return j.values[0]; }
async function cells() {
  if (LANG === 'java') return Array.from(model.values);
  if (LANG === 'py') return JSON.parse(bio7py.runPython("import json, bio7sim; json.dumps(bio7sim._model.values.ravel().tolist())"));
  return JSON.parse('[' + await rstr("paste(as.vector(t(.bio7sim_model$values)), collapse = ',')") + ']');
}
async function agents() {
  if (LANG === 'java') return model.agents.map(a => [a.x, a.y, typeof a.state === 'number' ? String.fromCharCode(a.state) : String(a.state)]);
  if (LANG === 'py') return JSON.parse(bio7py.runPython("import json, bio7sim; json.dumps([[a.x, a.y, a.state] for a in bio7sim._model.agents])"));
  return JSON.parse(await rstr(".bio7sim_model$agents -> g; paste0('[', paste(sprintf('[%.6f,%.6f,\"%s\"]', g$x, g$y, g$state), collapse = ','), ']')"));
}
/* The default camera of the page (theta -0.95, phi 0.45, distance 3.2 x the size, 30 degrees) */
function camera(box) {
  const view3d = document.getElementById('view3d'), rect = view3d.getBoundingClientRect();
  const center = [box[0] / 2, box[1] / 2, box[2] / 2], size = Math.max(box[0], box[1], box[2]), dist = size * 3.2, th = -0.95, ph = 0.45;
  const e = [center[0] + dist * Math.cos(ph) * Math.cos(th), center[1] + dist * Math.cos(ph) * Math.sin(th), center[2] + dist * Math.sin(ph)];
  let z = [e[0] - center[0], e[1] - center[1], e[2] - center[2]]; let l = Math.hypot(...z); z = z.map(a => a / l);
  const up = [0, 0, 1]; let x = [up[1] * z[2] - up[2] * z[1], up[2] * z[0] - up[0] * z[2], up[0] * z[1] - up[1] * z[0]]; l = Math.hypot(...x); x = x.map(a => a / l);
  const y = [z[1] * x[2] - z[2] * x[1], z[2] * x[0] - z[0] * x[2], z[0] * x[1] - z[1] * x[0]];
  const f = 1 / Math.tan(15 * Math.PI / 180), aspect = view3d.width / view3d.height;
  return (px, py) => {
    const d = [px - e[0], py - e[1], -e[2]], cx = d[0] * x[0] + d[1] * x[1] + d[2] * x[2], cy = d[0] * y[0] + d[1] * y[1] + d[2] * y[2], cz = d[0] * z[0] + d[1] * z[1] + d[2] * z[2];
    return [rect.left + (f / aspect * cx / -cz + 1) / 2 * rect.width, rect.top + (1 - f * cy / -cz) / 2 * rect.height];
  };
}
"""

GRID = COMMON + r"""
(async () => {
  const view = document.getElementById('view'), rect = view.getBoundingClientRect(), k = view.width / rect.width, W = 120;
  const scale = Math.min(view.width / W, view.height / W), ox = (view.width - W * scale) / 2, oy = (view.height - W * scale) / 2;
  const pt = (cx, cy) => [rect.left + (ox + (cx + 0.5) * scale) / k, rect.top + (oy + (cy + 0.5) * scale) / k];
  let c = await cells(); const v = (x, y) => c[y * W + x];
  let cx = 5; while (cx < 60 && !(v(cx, 30) < 0.01 && v(cx + 2, 30) < 0.01 && v(cx - 2, 30) < 0.01)) cx++;
  fire(view, 'pointerdown', pt(cx, 30)); fire(view, 'pointerup', pt(cx, 30), { buttons: 0 }); await wait(2500);
  c = await cells(); const click = v(cx, 30) === 1 && v(cx + 2, 30) === 1 && v(cx + 5, 30) < 1;
  fire(view, 'pointerdown', pt(15, 90));
  for (let i = 1; i <= 40; i++) { fire(view, 'pointermove', pt(15 + 30 * i / 40, 90)); await wait(20); }
  fire(view, 'pointerup', pt(45, 90), { buttons: 0 }); await wait(3500);
  c = await cells(); const drag = [20, 30, 40].every(x => v(x, 90) === 1) && v(30, 95) < 1;
  document.body.dispatchEvent(new KeyboardEvent('keydown', { key: 'c', bubbles: true })); await wait(2500);
  c = await cells(); let s = 0; for (const a of c) s += a;
  /* c cools the plate (Java: the sources stay hot) */
  const key = LANG === 'java' ? v(cx, 30) === 1 && v(30, 90) === 0 : s === 0;
  return JSON.stringify({ click: click, drag: drag, key: key });
})()
"""

SCENE = COMMON + r"""
(async () => {
  const P = await agents(), n = P.length, screen = camera([100, 100, 4]);
  let pick = -1;
  for (let i = 0; i < n && pick < 0; i++) {
    if (P[i][2] !== 'S') continue;
    let best = 1e9; for (let j = 0; j < n; j++) if (j !== i) best = Math.min(best, Math.hypot(P[i][0] - P[j][0], P[i][1] - P[j][1]));
    if (best > 4 && P[i][0] > 15 && P[i][0] < 85 && P[i][1] > 15 && P[i][1] < 85) pick = i;
  }
  if (pick < 0) return JSON.stringify({ click: false, detail: 'no free person' });
  const p = screen(P[pick][0], P[pick][1]), v3 = document.getElementById('view3d');
  fire(v3, 'pointerdown', p); fire(v3, 'pointerup', p, { buttons: 0 }); await wait(3500);
  const Q = await agents(); let newly = 0; for (let i = 0; i < n; i++) if (P[i][2] !== 'I' && Q[i][2] === 'I') newly++;
  return JSON.stringify({ click: Q[pick][2] === 'I' && newly === 1 });
})()
"""

PAGES = [("HeatDiffusion_java", GRID), ("heat_diffusion_py", GRID), ("heat_diffusion_r", GRID),
         ("NetworkEpidemic_java", SCENE), ("network_epidemic_py", SCENE), ("network_epidemic_r", SCENE)]


def main():
    results = []
    with Chrome() as c:
        for name, js in PAGES:
            page = os.path.join(OUT, name + ".html")
            if not os.path.exists(page):
                continue
            c.open("file://" + page.replace(os.sep, "/"))
            if not c.wait("document.getElementById('status').textContent.startsWith('Step')", 240):
                r = {"error": "the page did not start"}
            else:
                try:
                    r = json.loads(c.eval(js))
                except Exception as e:
                    r = {"error": str(e)[:300]}
            for test, ok in r.items():
                if test in ("detail",):
                    continue
                results.append({"page": name, "test": test, "ok": ok is True, "detail": "" if ok in (True, False) else str(ok)})
                print("%-22s %-6s %s" % (name, test, "pass" if ok is True else "FAIL " + ("" if ok is False else str(ok))), flush=True)
    json.dump(results, open(os.path.join(OUT, "input.json"), "w"), indent=1)
    passed = sum(r["ok"] for r in results)
    print("%d of %d input tests passed" % (passed, len(results)))
    sys.exit(0 if results and passed == len(results) else 1)


if __name__ == "__main__":
    main()
