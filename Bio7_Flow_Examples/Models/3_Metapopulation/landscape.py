"""Step 1 of the flow Metapopulation.flow: a fragmented landscape created in Python.

60 habitat patches (e.g., meadows for a butterfly) in an area of 10 x 10 km: clustered
positions (patches in 5 groups) and log-normal areas (most patches small, a few large).
The next step (Java) reads the pandas DataFrame "patches" (x, y in km, area in ha) of the
embedded Python.

Requires: numpy, pandas
"""
import numpy as np
import pandas as pd

rng = np.random.default_rng(5)
centers = rng.uniform(1.5, 8.5, size=(5, 2))
group = rng.integers(0, len(centers), 60)
xy = centers[group] + rng.normal(0, 1.0, size=(60, 2))
patches = pd.DataFrame({
    "x": np.clip(xy[:, 0], 0, 10),
    "y": np.clip(xy[:, 1], 0, 10),
    "area": np.round(rng.lognormal(mean=0.0, sigma=0.9, size=60), 2),
})
print(f"Landscape: {len(patches)} patches, area {patches['area'].min():.2f} to {patches['area'].max():.2f} ha")
