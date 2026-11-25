"""Step 3 of the flow Sea_Turtle.flow: the matrix analysis and the scenarios plotted in Python.

Left: the elasticities of R per stage (fecundity, staying in the stage, growing to the
next): where a change of the rates has the largest effect on the growth rate.
Right: the risk of a quasi-extinction (below 500 turtles) over 100 years of the three
management scenarios simulated in Java.

Requires: numpy, pandas, matplotlib
"""
import numpy as np
import matplotlib.pyplot as plt

import bio7


def num(value):
    """A single number of R (R returns vectors: the first element)."""
    return float(np.asarray(value, dtype=float).ravel()[0])


elasticity = bio7.r("turtleElasticity")
scenarios = bio7.r("turtleScenarios")
lam = num(bio7.r("turtleLambda"))

fig, (left, right) = plt.subplots(1, 2, figsize=(12, 4.8), constrained_layout=True)
stages = [str(s) for s in elasticity["stage"]]
x = np.arange(len(stages))
width = 0.27
for k, (column, color) in enumerate([("fecundity", "#2e9e47"), ("stasis", "#1f6feb"), ("growth", "#c8651b")]):
    left.bar(x + (k - 1) * width, elasticity[column], width, color=color, label=column.capitalize())
left.set_xticks(x, stages, rotation=35, ha="right")
left.set(ylabel="Elasticity of lambda", title=f"Loggerhead sea turtle (lambda = {lam:.3f})")
left.legend(frameon=False)

colors = {"No change": "#5c6370", "Nest protection": "#2e9e47", "Turtle excluder devices": "#1f6feb"}
for name, d in scenarios.groupby("scenario"):
    right.plot(d["year"], d["quasi_extinction"], color=colors.get(name, "black"), lw=2, label=name)
right.set(xlabel="Year", ylabel="Probability below 500 turtles", ylim=(-0.02, 1.02),
          title="Quasi-extinction of the management scenarios")
right.legend(frameon=False)
plt.show()
