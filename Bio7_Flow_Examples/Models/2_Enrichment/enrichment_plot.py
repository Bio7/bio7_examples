"""Step 3 of the flow Enrichment.flow: the paradox of enrichment plotted in Python.

Left: the minimum and maximum of the prey after the transient for each carrying capacity K
(a bifurcation diagram): one line while the equilibrium is stable, two lines when the
populations cycle; the dashed line is the Hopf bifurcation computed in R.
Right: the phase plane of prey and predator in the first 300 time units for K = 1.5 (a
spiral into the stable equilibrium) and K = 4 (away from the equilibrium into a limit cycle).

Requires: pandas, matplotlib
"""
import numpy as np
import matplotlib.pyplot as plt

import bio7


def num(value):
    """A single number of R (R returns vectors: the first element)."""
    return float(np.asarray(value, dtype=float).ravel()[0])


enrichment = bio7.r("enrichment")
trajectories = bio7.r("trajectories")
result = bio7.r("enrichmentResult")

fig, (left, right) = plt.subplots(1, 2, figsize=(11, 4.5), constrained_layout=True)
left.plot(enrichment["K"], enrichment["prey_max"], color="#1f6feb", lw=2, label="Prey (max)")
left.plot(enrichment["K"], enrichment["prey_min"], color="#1f6feb", lw=2, ls=":", label="Prey (min)")
left.plot(enrichment["K"], enrichment["predator_max"], color="#c8651b", lw=2, label="Predator (max)")
left.plot(enrichment["K"], enrichment["predator_min"], color="#c8651b", lw=2, ls=":", label="Predator (min)")
left.axvline(num(result["k_hopf"]), color="gray", ls="--", lw=1, label="Hopf bifurcation")
left.set(xlabel="Carrying capacity K of the prey", ylabel="Population density", title="Paradox of enrichment")
left.legend(frameon=False, fontsize=9)

for k, color in [(1.5, "#2e9e47"), (4.0, "#8a3fc4")]:
    t = trajectories[trajectories["K"] == k]
    right.plot(t["prey"], t["predator"], color=color, lw=1.5, label=f"K = {k:g}")
right.set(xlabel="Prey", ylabel="Predator", title="Phase plane (300 time units from the start)")
right.legend(frameon=False)
plt.show()
