"""Last step of the flow Adaptive_Management.flow: the 30 years of the adaptive management
plotted in Python (the data frame "deerHistory" and the list "deerResult" of R).

- the true population, the survey estimate with its 90 % interval and the target range;
  the markers show the decision of each year
- the harvest quota and the deer shot per year
- the browsing damage over the density with the density of 40 % damage

Requires: pandas, matplotlib
"""
import numpy as np
import matplotlib.pyplot as plt

import bio7


def num(value):
    """A single number of R (R returns vectors: the first element)."""
    return float(np.asarray(value, dtype=float).ravel()[0])


h = bio7.r("deerHistory")
result = bio7.r("deerResult")
lower, upper = num(bio7.r("deerPar$lower")), num(bio7.r("deerPar$upper"))
area = num(bio7.r("deerPar$area"))
colors = {"more hunting": "#c8651b", "sustainable hunting": "#2e9e47", "no hunting": "#1f6feb"}

fig = plt.figure(figsize=(11, 7.5), constrained_layout=True)
grid = fig.add_gridspec(2, 2, height_ratios=[1.3, 1])

ax = fig.add_subplot(grid[0, :])
ax.axhspan(lower, upper, color="#2e9e47", alpha=0.08, label="Target range")
ax.fill_between(h["year"], h["est_lower"], h["est_upper"], color="#888888", alpha=0.2, label="Survey estimate (90 %)")
ax.plot(h["year"], h["estimate"], color="#888888", lw=1, ls="--")
ax.plot(h["year"], h["N"], color="#222222", lw=2, label="True population")
for action, color in colors.items():
    sel = h["action"] == action
    ax.scatter(h["year"][sel], h["estimate"][sel], color=color, s=40, zorder=3, label=f"Decision: {action}")
wolves = num(bio7.r("deerPar$wolvesFrom"))
ax.axvline(wolves - 0.5, color="#7a4fbf", lw=1.5)
ax.text(wolves - 0.3, ax.get_ylim()[0] + 8, "wolves return", color="#7a4fbf", va="bottom", fontsize=9)
ax.set(xlabel="Year", ylabel="Red deer", title="Adaptive management: monitor, decide, act - every year")
ax.legend(frameon=False, ncol=3, loc="upper right", fontsize=9)

bars = fig.add_subplot(grid[1, 0])
bars.bar(h["year"] - 0.2, h["quota"], width=0.4, color=[colors[a] for a in h["action"]], label="Quota (next year)")
bars.bar(h["year"] + 0.2, h["harvest"], width=0.4, color="#555555", label="Shot")
bars.set(xlabel="Year", ylabel="Deer", title=f"Hunting (total {int(num(result['total_harvest']))} deer)")
bars.legend(frameon=False)

dmg = fig.add_subplot(grid[1, 1])
density = h["N"] / area
dmg.scatter(density, h["damage"] * 100, c=h["year"], cmap="viridis", s=30)
d40 = num(result["density_damage40"])
dmg.axvline(d40, color="#c8651b", ls="--", lw=1, label=f"40 % damage at {d40:.1f} deer/km2")
dmg.axvspan(lower / area, upper / area, color="#2e9e47", alpha=0.08, label="Target range")
dmg.set(xlabel="Density (deer per km2)", ylabel="Young trees browsed (%)", title="Browsing damage (colour: year)")
dmg.legend(frameon=False, fontsize=9)
plt.show()
