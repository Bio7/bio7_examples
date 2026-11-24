"""Compares the exported pages with Bio7: each page is stepped to the time step of the Bio7 run
(bio7/run_bio7.sh) in headless Chrome and its state is compared with the state of Bio7.

    python3 page/compare.py [OUT]        (default: tests/work/out)

OUT contains <name>.json (the state in Bio7) and <name>.html (the page) of each example.
Writes OUT/compare.json and prints a line for each example; the exit code is 1 if a page differs.
"""
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cdp import Chrome  # noqa: E402

OUT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "work", "out"))

# The state of the page in the order of Bio7 (cells, values, x y z of the agents or lon lat)
STATE = {
    "java": "(() => { const c = model.cells || model.values; return JSON.stringify(c ? Array.from(c) : model.agents.reduce(function (a, g) {"
            " if (g.lon !== undefined) a.push(g.lon, g.lat); else a.push(g.x, g.y, g.z); return a; }, [])); })()",
    "py": "(() => { const s = bio7sim._model._state().toJs({dict_converter: Object.fromEntries}); const k = s.cells ? 'cells' : s.values ? 'values' : s.xyz ? 'xyz' : null;"
          " let v = k ? Array.from(s[k]) : []; if (!k) { v = []; for (let i = 0; i < s.lon.length; i++) v.push(s.lon[i], s.lat[i]); } return JSON.stringify(v); })()",
    "r": "(async () => { const s = bio7Plain(await (await bio7webR.evalR('.bio7sim_model$.state()')).toJs()); const k = s.cells ? 'cells' : s.values ? 'values' : s.xyz ? 'xyz' : null;"
         " let v = k ? Array.from(s[k]) : []; if (!k) { v = []; for (let i = 0; i < s.lon.length; i++) v.push(s.lon[i], s.lat[i]); } return JSON.stringify(v); })()",
}
STATUS = "document.getElementById('status').textContent"


def compare(c, b):
    page = "file://" + os.path.join(OUT, b["name"] + ".html").replace(os.sep, "/")
    c.open(page)
    if not c.wait(STATUS + ".startsWith('Step')", 240):
        try:
            status = c.eval(STATUS)
        except Exception as e:
            status = str(e)
        return {"result": "not started", "detail": str(status) + (" " + c.logs[-1] if c.logs else "")}
    # one step after the other (an asynchronous page: the next step after the counter has changed)
    for k in range(1, b["t"] + 1):
        c.eval("document.getElementById('step').click()")
        if not c.wait(STATUS + " === 'Step " + str(k) + "'", 120):
            return {"result": "stuck", "detail": "at " + str(c.eval(STATUS))}
    v = json.loads(c.eval(STATE[b["lang"]]))
    w = b["values"]
    if len(v) != len(w):
        return {"result": "different", "detail": "%d values in the page, %d in Bio7" % (len(v), len(w))}
    diff = max((abs(float(x) - float(y)) for x, y in zip(v, w)), default=0.0)
    return {"result": "identical" if diff == 0 else "different", "detail": "%d values, t=%d" % (len(v), b["t"]) + ("" if diff == 0 else ", max. difference %.3g" % diff)}


def main():
    results = []
    with Chrome() as c:
        for f in sorted(glob.glob(os.path.join(OUT, "*.json"))):
            if os.path.basename(f) in ("compare.json", "input.json"):
                continue
            b = json.load(open(f))
            if not os.path.exists(os.path.join(OUT, b["name"] + ".html")):
                continue
            try:
                r = compare(c, b)
            except Exception as e:
                r = {"result": "error", "detail": str(e)[:300]}
            r.update({"name": b["name"], "lang": b["lang"], "kind": b["kind"], "t": b["t"]})
            results.append(r)
            print("%-28s %-9s %s" % (b["name"], r["result"], r["detail"]), flush=True)
    json.dump(results, open(os.path.join(OUT, "compare.json"), "w"), indent=1)
    same = sum(r["result"] == "identical" for r in results)
    print("%d of %d pages identical with Bio7" % (same, len(results)))
    sys.exit(0 if same == len(results) else 1)


if __name__ == "__main__":
    main()
