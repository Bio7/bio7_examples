"""An experiment with the model of network_epidemic.py (batch runs without a display).

The share of the infected people at the end for some infection rates and two networks,
5 repetitions each (the seeds 42 to 46: the same for all values). experiment() runs the model
class directly and shows its progress in Bio7 (bio7.progress: a job in the Progress view, its stop
button interrupts the experiment); the result is a pandas DataFrame (a list of dicts without pandas). The same
runs can be started in Bio7 with the button "Experiment..." of the settings of the model.
"""
import os

from bio7sim import experiment

# The model: the script beside this one (it also opens the model in Bio7)
here = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() else os.getcwd()
ns = {"__name__": "network_epidemic"}
with open(os.path.join(here, "network_epidemic.py"), encoding="utf-8") as f:
    exec(compile(f.read(), "network_epidemic.py", "exec"), ns)
NetworkEpidemic = ns["NetworkEpidemic"]

d = experiment(NetworkEpidemic,
               params={"infection": [0.01, 0.02, 0.03, 0.05, 0.08], "network": ["Moving contacts", "Small world"]},
               steps=300, repetitions=5, every=300)   # the progress: a job in the Progress view of Bio7

rows = d.to_dict("records") if hasattr(d, "to_dict") else d
# the last row of each run (a run ends early when the epidemic is over: stop())
last = {}
for r in rows:
    last[r["run"]] = r
final = list(last.values())
print("\nThe mean share of the infected people at the end of the runs:")
print("%-10s %-16s %s" % ("infection", "network", "infected share"))
for inf in sorted({r["infection"] for r in final}):
    for net in ("Moving contacts", "Small world"):
        v = [r["Infected share"] for r in final if r["infection"] == inf and r["network"] == net]
        print("%-10.2f %-16s %.2f" % (inf, net, sum(v) / len(v)))
