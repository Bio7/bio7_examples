"""A sensitivity analysis of network_epidemic.py: which parameter decides the size of the epidemic?

The method of Morris (package SALib) samples 40 parameter sets (10 trajectories) of the infection
rate, the recovery rate and the contact distance. experiment(design=...) runs each set 3 times (the
seeds 42 to 44) without a display; the column "point" of the result is the row of the design.
The analysis is done in Python: the mean and the 95 % confidence interval of the infected share
over the repetitions of each set (pandas) and the elementary effects of Morris (mu*: the
influence of a parameter, sigma: nonlinear effects and interactions).

Requires: pandas, matplotlib, SALib (Python Packages of Bio7 or pip install SALib)
"""
import os

import matplotlib.pyplot as plt
import pandas as pd
from SALib.analyze import morris
from SALib.sample import morris as morris_sample
from scipy import stats

from bio7sim import experiment

# The model: the script beside this one (it also opens the model in Bio7)
here = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() else os.getcwd()
ns = {"__name__": "network_epidemic"}
with open(os.path.join(here, "network_epidemic.py"), encoding="utf-8") as f:
    exec(compile(f.read(), "network_epidemic.py", "exec"), ns)
NetworkEpidemic = ns["NetworkEpidemic"]

# The design: the trajectories of Morris in the ranges of the parameters (a row is a parameter set)
problem = {"num_vars": 3, "names": ["infection", "recovery", "distance"], "bounds": [[0.01, 0.1], [0.02, 0.1], [2, 8]]}
X = morris_sample.sample(problem, N=10, num_levels=4, seed=1)
design = pd.DataFrame(X, columns=problem["names"])

# 120 runs (the progress: a job in the Progress view of Bio7)
d = experiment(NetworkEpidemic, design=design, steps=300, repetitions=3, every=300)

# The last row of each run, then the mean and the 95 % confidence interval of each parameter set
final = d.groupby("run").tail(1)
summary = final.groupby("point")["Infected share"].agg(["mean", "std", "count"])
half = stats.t.ppf(0.975, summary["count"] - 1) * summary["std"] / summary["count"] ** 0.5
# (a share: the interval within 0 and 1)
summary["lower"], summary["upper"] = (summary["mean"] - half).clip(0, 1), (summary["mean"] + half).clip(0, 1)
result = design.join(summary[["mean", "lower", "upper"]])
print(result.sort_values("mean", ascending=False).head(10).round(3).to_string())

# The elementary effects of Morris (the means of the sets: the order of the design)
res = morris.analyze(problem, X, result["mean"].to_numpy(), num_levels=4, seed=1)
effects = pd.DataFrame({"mu_star": res["mu_star"], "mu_star_conf": res["mu_star_conf"], "sigma": res["sigma"]}, index=problem["names"])
print("\nMorris: mu* (influence) and sigma (nonlinear effects and interactions)")
print(effects.round(3).to_string())

fig, axes = plt.subplots(1, 4, figsize=(15, 3.8))
for ax, n in zip(axes, problem["names"]):
    ax.errorbar(result[n], result["mean"], yerr=[result["mean"] - result["lower"], result["upper"] - result["mean"]],
                fmt="o", color="#2f6fed", ecolor="#9aa0a6", markersize=4, capsize=2)
    ax.set_xlabel(n)
    ax.set_ylabel("Infected share (mean of 3 runs)")
    ax.set_ylim(0, 1.05)
axes[3].barh(effects.index, effects["mu_star"], xerr=effects["mu_star_conf"], color="#e53935")
axes[3].set_xlabel("mu* (Morris)")
fig.suptitle("Network epidemic: sensitivity of the infected share")
fig.tight_layout()
plt.show()
