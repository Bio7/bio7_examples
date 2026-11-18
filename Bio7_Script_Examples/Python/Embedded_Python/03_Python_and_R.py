# Data between the embedded Python and R.
#
# Start R first (toolbar action "Start R"), then run this script.
#   bio7.to_r(name, value)  assigns a value in R: numpy arrays (matrices keep
#                           their shape), pandas DataFrames (data frames),
#                           dicts (named lists), strings
#   bio7.r(code)            evaluates R code and returns the value: vectors and
#                           matrices as numpy arrays, data frames as pandas
#                           DataFrames, named lists as dicts
#   bio7.from_r(name)       the value of an R object
#
# Requires: numpy, pandas, matplotlib

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import bio7

rng = np.random.default_rng(1)

# A data frame from Python to R
n = 60
df = pd.DataFrame({
    "temperature": rng.uniform(5, 25, n),
    "habitat": rng.choice(["forest", "meadow"], n),
})
df["growth"] = 0.8 * df["temperature"] + np.where(df["habitat"] == "forest", 2.0, 0.0) + rng.normal(0, 1.5, n)
bio7.to_r("growthData", df)

# A linear model in R, the coefficients back to Python
coef = bio7.r("coef(lm(growth ~ temperature + habitat, data = growthData))")
print("Coefficients of R:", coef)

# A matrix: R and numpy index the same element (m[1, 2] in R is m[0, 1] here)
m = bio7.r("matrix(1:6, nrow = 2)")
print("R matrix as numpy:\n", m)

# An R data set as pandas DataFrame
iris = bio7.r("iris")
print(iris.groupby("Species").mean())

# A plot of the data and the fitted lines of R
fig, ax = plt.subplots(constrained_layout=True)
for habitat, color in [("forest", "#55A868"), ("meadow", "#DD8452")]:
    d = df[df["habitat"] == habitat]
    ax.scatter(d["temperature"], d["growth"], color=color, alpha=0.7, label=habitat)
    t = np.linspace(5, 25, 50)
    offset = coef[2] if habitat == "meadow" else 0.0
    ax.plot(t, coef[0] + coef[1] * t + offset, color=color, lw=2)
ax.set(title="Growth model fitted in R", xlabel="Temperature (°C)", ylabel="Growth")
ax.legend(frameon=False)
plt.show()
