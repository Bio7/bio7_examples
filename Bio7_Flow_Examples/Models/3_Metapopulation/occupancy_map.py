"""Step 4 of the flow Metapopulation.flow: the metapopulation plotted in Python.

Left: the map of the patches, the size of a circle is the area, the color the incidence
fitted in R (the share of years the patch is occupied).
Right: the share of occupied patches over 200 years in each run, the equilibrium of R.

Requires: pandas, matplotlib
"""
import numpy as np
import matplotlib.pyplot as plt

import bio7


def num(value):
    """A single number of R (R returns vectors: the first element)."""
    return float(np.asarray(value, dtype=float).ravel()[0])


patches = bio7.r("patchOccupancy")
time = bio7.r("occupancyTime")
result = bio7.r("metapopulationResult")

fig, (left, right) = plt.subplots(1, 2, figsize=(11, 4.8), constrained_layout=True)
points = left.scatter(patches["x"], patches["y"], s=40 + 60 * patches["area"], c=patches["fitted"],
                      cmap="viridis", vmin=0, vmax=1, edgecolor="black", linewidth=0.5)
fig.colorbar(points, ax=left, label="Incidence (fitted in R)")
left.set(xlabel="x (km)", ylabel="y (km)", title="Patches: area (size) and incidence (color)", aspect="equal")

for run, d in time.groupby("run"):
    right.plot(d["year"], d["share"], color="#1f6feb", alpha=0.25, lw=1)
right.axhline(num(result["equilibrium"]), color="#c8651b", lw=2, label="Equilibrium")
right.axvspan(100, 200, color="gray", alpha=0.08, label="Analysed years")
right.set(xlabel="Year", ylabel="Share of occupied patches", ylim=(0, 1), title="Occupancy in 20 runs")
right.legend(frameon=False)
plt.show()
