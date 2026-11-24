# Modern plots with matplotlib in the embedded Python of Bio7.
#
# Run this script in the Python editor ("Interpret Python" or the run action).
# The figures appear in the "Python Plot" view beside the console: resize the
# view and the figures are drawn again in the new size. With the buttons of
# the view you can browse the figures, save them (PNG, SVG, PDF), copy them or
# open them as ImageJ image.
#
# Requires: numpy, matplotlib (pip install numpy matplotlib)

import matplotlib.pyplot as plt
import numpy as np

rng = np.random.default_rng(42)

# A clean, modern style for all figures of this script
plt.rcParams.update({
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.grid": True,
    "grid.alpha": 0.25,
    "axes.titleweight": "bold",
    "font.size": 10,
})
colors = ["#4C72B0", "#DD8452", "#55A868", "#C44E52"]

# 1. Smoothed time series with a confidence band
x = np.linspace(0, 10, 200)
fig, ax = plt.subplots(constrained_layout=True)
for i, c in enumerate(colors[:3]):
    y = np.sin(x + i) * np.exp(-x / 8) + rng.normal(0, 0.05, x.size)
    ax.plot(x, y, color=c, lw=2, label=f"Population {i + 1}")
    ax.fill_between(x, y - 0.15, y + 0.15, color=c, alpha=0.15)
ax.set(title="Population dynamics", xlabel="Time (years)", ylabel="Relative abundance")
ax.legend(frameon=False)

# 2. Distributions: violin plot with the points
groups = [rng.normal(m, s, 80) for m, s in [(5, 1), (6.5, 1.5), (4, 0.7), (7, 1.2)]]
fig, ax = plt.subplots(constrained_layout=True)
parts = ax.violinplot(groups, showmedians=True)
for body, c in zip(parts["bodies"], colors):
    body.set_facecolor(c)
    body.set_alpha(0.4)
for i, (g, c) in enumerate(zip(groups, colors), start=1):
    ax.scatter(rng.normal(i, 0.04, g.size), g, s=8, color=c, alpha=0.6)
ax.set_xticks(range(1, 5), ["Forest", "Meadow", "Wetland", "Field"])
ax.set(title="Body mass in four habitats", ylabel="Body mass (g)")

# 3. A heatmap of a 2-d density with a colorbar
xy = rng.multivariate_normal([0, 0], [[1, 0.6], [0.6, 1]], 5000)
fig, ax = plt.subplots(constrained_layout=True)
h = ax.hist2d(xy[:, 0], xy[:, 1], bins=40, cmap="viridis")
fig.colorbar(h[3], ax=ax, label="Count")
ax.set(title="Joint distribution", xlabel="Temperature (scaled)", ylabel="Growth (scaled)")
ax.grid(False)

# 4. Small multiples
fig, axes = plt.subplots(2, 2, sharex=True, constrained_layout=True)
for ax, c, k in zip(axes.flat, colors, range(4)):
    data = rng.gamma(2 + k, 1.0, 400)
    ax.hist(data, bins=25, color=c, alpha=0.8)
    ax.set_title(f"Site {k + 1}", fontsize=9)
fig.suptitle("Gamma distributed measurements")

plt.show()
print("Figures:", plt.get_fignums())
