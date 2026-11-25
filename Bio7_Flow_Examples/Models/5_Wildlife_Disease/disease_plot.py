"""Step 3 of the flow Wildlife_Disease.flow: the epidemics plotted in Python.

Left: the infected per day of 20 runs without vaccination and with a coverage of 50 %.
Right: the probability of a major outbreak (more than 10 % infected) and the mean final
size per vaccination coverage; the dashed line is the critical coverage 1 - 1/R0 of R.

Requires: pandas, matplotlib
"""
import numpy as np
import matplotlib.pyplot as plt

import bio7


def num(value):
    """A single number of R (R returns vectors: the first element)."""
    return float(np.asarray(value, dtype=float).ravel()[0])


curves = bio7.r("epidemicCurves")
outbreaks = bio7.r("outbreaks")
result = bio7.r("diseaseResult")

fig, (left, right) = plt.subplots(1, 2, figsize=(11, 4.5), constrained_layout=True)
for coverage, color, label in [(0.0, "#c8651b", "No vaccination"), (0.5, "#1f6feb", "50 % vaccinated")]:
    d = curves[curves["coverage"] == coverage]
    for k, (run, r) in enumerate(d.groupby("run")):
        left.plot(r["day"], r["infected"], color=color, alpha=0.35, lw=1, label=label if k == 0 else None)
left.set(xlabel="Day", ylabel="Infected animals", title="Epidemic curves (20 runs each)")
left.legend(frameon=False)

right.plot(outbreaks["coverage"], outbreaks["major"], "o-", color="#c8651b", lw=2, label="Major outbreak")
right.plot(outbreaks["coverage"], outbreaks["final_size"], "s-", color="#1f6feb", lw=2, label="Mean final size")
right.axvline(num(result["critical_coverage"]), color="gray", ls="--", lw=1,
              label=f"1 - 1/R0 (R0 = {num(result['r0_estimated']):.2f})")
right.set(xlabel="Vaccination coverage", ylabel="Probability / share infected", ylim=(-0.02, 1.02),
          title="Herd immunity")
right.legend(frameon=False)
plt.show()
