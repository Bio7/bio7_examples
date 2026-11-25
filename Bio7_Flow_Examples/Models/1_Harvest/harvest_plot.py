"""Step 3 of the flow Harvest.flow: the results of R plotted in Python.

The mean total catch per harvest rate (from the data frame "harvestSummary" of R) with the
rate of the highest catch, and the collapse risk of the logistic regression with the share
of collapsed runs. A sustainable harvest balances the catch against the risk.

Requires: pandas, matplotlib
"""
import numpy as np
import matplotlib.pyplot as plt

import bio7


def num(value):
    """A single number of R (R returns vectors: the first element)."""
    return float(np.asarray(value, dtype=float).ravel()[0])


summary = bio7.r("harvestSummary")
result = bio7.r("harvestResult")
best = num(result["best_rate"])
safe = num(result["safe_rate"])

fig, ax = plt.subplots(figsize=(8, 4.5), constrained_layout=True)
ax.plot(summary["rate"], summary["catch"], color="#1f6feb", lw=2, label="Mean catch (50 years)")
ax.axvline(best, color="#1f6feb", ls="--", lw=1)
ax.set(xlabel="Harvest rate (share caught per year)", ylabel="Total catch",
       title="Harvesting a fluctuating fish stock")
risk = ax.twinx()
risk.plot(summary["rate"], summary["collapse_risk"], color="#c8651b", lw=2, label="Collapse risk (glm)")
risk.scatter(summary["rate"], summary["collapse_share"], color="#c8651b", s=14, alpha=0.6, label="Collapsed runs")
risk.set(ylabel="Probability of a collapse", ylim=(-0.02, 1.02))
if safe == safe:  # not NaN (NA in R)
    risk.axvspan(0, safe, color="#2e9e47", alpha=0.08, label="Risk below 10 %")
lines = ax.get_legend_handles_labels()
lines2 = risk.get_legend_handles_labels()
ax.legend(lines[0] + lines2[0], lines[1] + lines2[1], frameon=False, loc="upper left")
plt.show()
