"""Tests the Python module of the simulation API (bio7sim.py) without Bio7: the protocol of Bio7 and
of the page (setup, step, input), determinism, stop(), random_at, experiments and designs, with
the Python simulation examples.

    python3 offline/test_python.py [--bio7sim DIR]

DIR: the folder of bio7sim.py (the environment variable BIO7SIM, default: the repository bio7
beside bio7_examples). Requires numpy (pandas optional). Writes work/out/test_python.json; the
exit code is 1 if a test fails. Examples which need Bio7 (images, 3D models, geodata) are skipped.
"""
import argparse, glob, hashlib, json, os, sys, time
import numpy as np

TESTS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(TESTS)
ap = argparse.ArgumentParser()
ap.add_argument("--bio7sim", default=os.environ.get("BIO7SIM", os.path.join(os.path.dirname(REPO), "bio7", "com.eco.bio7", "src", "com", "eco", "bio7", "simulation")))
ap.add_argument("--out", default=os.path.join(TESTS, "work", "out"))
args = ap.parse_args()
if not os.path.exists(os.path.join(args.bio7sim, "bio7sim.py")):
    sys.exit("bio7sim.py was not found in " + args.bio7sim + " (use --bio7sim or BIO7SIM)")
sys.path.insert(0, args.bio7sim)
sys.dont_write_bytecode = True
import bio7sim as b  # noqa: E402
SIM = os.path.join(REPO, "Bio7_Python_Examples", "Simulation")
results = []
def check(name, ok, detail=""):
    results.append({"test": name, "ok": bool(ok), "detail": detail})
def load(path):
    holder = {}
    b.simulation = lambda cls, source=None: holder.setdefault("cls", cls)
    ns = {"__name__": "__main__", "__file__": path}
    exec(compile(open(path).read(), path, "exec"), ns)
    return holder["cls"]
def digest(s):
    h = hashlib.sha1()
    for k in sorted(s):
        v = s[k]
        if k in ("more_charts", "inputs", "chart_names"): continue
        h.update(k.encode()); h.update(np.asarray(v).tobytes() if isinstance(v, np.ndarray) else str(v).encode())
    return h.hexdigest()[:12]
def run(cls, seed, steps):
    b._model = cls()
    b._do("setup", str(seed))
    s = None
    for _ in range(steps):
        s = b._do("step")
        if s.get("stopped"): break
    return digest(s), b._model.t
for path in sorted(glob.glob(os.path.join(SIM, "*.py"))):
    name = os.path.basename(path)
    if "_experiment" in name or "_sensitivity" in name: continue
    try:
        cls = load(path); t0 = time.time()
        d1, t1 = run(cls, 42, 50); d2, _ = run(cls, 42, 50); d3, _ = run(cls, 7, 50)
        check(name + ": setup and 50 steps", True, "t=%d, %.1f s" % (t1, (time.time() - t0) / 3))
        check(name + ": same seed, same state", d1 == d2, d1)
        check(name + ": other seed, other state", d1 != d3 or name in ("heat_diffusion.py",), "seed 7: " + d3)
    except Exception as e:
        needs = "bio7" in repr(e) or name == "wildfire.py"
        results.append({"test": name + ": setup and 50 steps", "ok": None if needs else False, "detail": ("needs Bio7 (images, 3D models or geodata): " if needs else "") + repr(e)[:160]})
# stop(): network_epidemic stops when no one is infected
cls = load(os.path.join(SIM, "network_epidemic.py"))
b._model = cls(); b._model.recovery = 0.3; b._do("setup", "42"); n = 0
while not b._do("step").get("stopped") and n < 2000: n += 1
check("stop(): the run ends (network_epidemic, recovery 0.3)", n < 2000, "stopped at t=%d" % b._model.t)
# input
cls = load(os.path.join(SIM, "heat_diffusion.py"))
b._model = cls(); s = b._do("setup", "42")
check("inputs reported in the state", sorted(s["inputs"]) == ["click", "drag", "key"], str(s["inputs"]))
b._do("input", "click\x1f60.5\x1f60.5"); check("input click (heat_diffusion)", b._model.values[60, 60] == 1.0)
b._do("input", "drag\x1f10\x1f100"); check("input drag", b._model.values[100, 10] == 1.0)
b._do("input", "key\x1fc"); check("input key c", float(b._model.values.sum()) == 0.0)
# experiment
cls = load(os.path.join(SIM, "network_epidemic.py"))
t = b.experiment(cls, {"infection": [0.02, 0.05]}, steps=20, repetitions=2, every=10, as_frame=False)
check("experiment: runs = combinations x repetitions", len({r["run"] for r in t}) == 4, "%d rows" % len(t))
check("experiment: seeds 42, 43 per combination", sorted({r["seed"] for r in t}) == [42, 43])
t2 = b.experiment(cls, design={"infection": [0.02, 0.08], "people": [100.6, 200.2]}, steps=10, repetitions=1, every=10, as_frame=False)
check("experiment design: point column, int rounding", [r["point"] for r in t2 if r["step"] == 0] == [0, 1] and t2[0]["people"] == 101, str([(r["point"], r["people"]) for r in t2 if r["step"] == 0]))
first = [r for r in t if r["run"] == 1]
again = b.experiment(cls, {"infection": 0.02}, steps=20, repetitions=1, every=10, as_frame=False)
check("experiment: run 1 repeatable", [r.get("Infected share") for r in first] == [r.get("Infected share") for r in again])
try:
    b.experiment(cls, design={"infektion": [0.1]}, steps=1); check("experiment: unknown parameter -> error", False)
except ValueError as e:
    check("experiment: unknown parameter -> error", True, str(e))
# the aliases of the review: random_at (the values of Java), agent_distance, link_count, is_stopped
class _T(b.AgentModel):
    def init(self): pass
    def step(self): pass
m = _T(); m.seed(42); m.t = 3
JAVA = [0.8950467265676707, 0.9944779181387275, 0.13833033409900963, 0.7390363635495305]
check("random_at: the same numbers as randomAt of Java", [m.random_at(5), m.random_at(5, 2), m.random_at(6), m.random_at(7)] == JAVA)
check("random_at: an array of indices", list(m.random_at(np.array([5, 6, 7]))) == [JAVA[0], JAVA[2], JAVA[3]])
m.agents = [b.Agent(0, 0, 0), b.Agent(3, 4, 0), b.Agent(50, 50, 0)]
check("agent_distance (agent model)", m.agent_distance(m.agents[0], m.agents[1]) == 5.0)
m.link(m.agents[0], m.agents[1]); m.link(m.agents[1], m.agents[2])
check("link_count", m.link_count() == 2 and len(m.links()) == 2)
m._stopped = False; before = m.is_stopped(); m.stop()
check("is_stopped", before is False and m.is_stopped() is True)
os.makedirs(args.out, exist_ok=True)
json.dump(results, open(os.path.join(args.out, "test_python.json"), "w"), indent=1)
print(sum(r["ok"] is True for r in results), "of", len(results), "passed,", sum(r["ok"] is None for r in results), "skipped")
for r in results:
    if r["ok"] is False: print("FAILED", r)
sys.exit(1 if any(r["ok"] is False for r in results) else 0)
