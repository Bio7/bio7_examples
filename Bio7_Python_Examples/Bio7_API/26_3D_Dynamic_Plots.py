# Dynamic 3D plots (OpenGL) in the 3D view of Bio7 from Python: a plot updated in
# each step (bio7.plot3d(..., plot=p): the camera and the zoom are kept), a recorded
# animation (bio7.plot3d_recorder(): an HTML page with Play/Pause and a timeline)
# and the charts (histogram, line graph, area, box plot, violin, radar, rose, treemap).
#
#   p = bio7.plot3d(bio7.histogram3d(x), zlim=(0, 100))         # shown once
#   for step in range(100): bio7.plot3d(bio7.histogram3d(x), plot=p)   # updated
#
# Fixed ranges (xlim, ylim, zlim) keep the axes steady while the data changes.
#
# Requires: numpy

import os
import tempfile
import time

import numpy as np

import bio7

rng = np.random.default_rng(3)


def pause(seconds=3):
    time.sleep(seconds)


# 1. An evolving trait: the body size in two populations with different optima
#    (selection and mutation), the histograms updated in each generation
n = 800
trait = {"Island": rng.normal(50, 5, n), "Mainland": rng.normal(50, 5, n)}
optimum = {"Island": 35, "Mainland": 65}


def generation(x, opt):
    a, b = rng.choice(x, len(x)), rng.choice(x, len(x))
    parent = np.where(np.abs(a - opt) < np.abs(b - opt), a, b)
    return np.clip(parent + rng.normal(0, 1.5, len(x)), 0, 100)


p = bio7.plot3d(bio7.histogram3d(trait, bin_width=2.5), title="Evolution of a trait\nGeneration 0",
                xlabel="Body size", ylabel="Population", zlabel="Individuals", xlim=(0, 100), zlim=(0, 300))
for g in range(1, 121):
    trait = {k: generation(v, optimum[k]) for k, v in trait.items()}
    bio7.plot3d(bio7.histogram3d(trait, bin_width=2.5), title="Evolution of a trait\nGeneration %d" % g, plot=p)
    time.sleep(0.03)
pause()

# 2. Growing line graphs: logistic growth of three populations (the series grow in
#    each step, each series at its own depth)
r = {"Fast": 0.25, "Medium": 0.15, "Slow": 0.08}
K = 100
pop = {k: [5.0] for k in r}
p = bio7.plot3d(bio7.linegraph3d([0], pop), title="Logistic growth", xlabel="Time", zlabel="Individuals",
                xlim=(0, 60), zlim=(0, 110))
for t in range(1, 61):
    for k, rate in r.items():
        last = pop[k][-1]
        pop[k].append(last + rate * last * (1 - last / K))
    bio7.plot3d(bio7.linegraph3d(np.arange(t + 1), pop, fill=True, marker_size=0.08), plot=p)
    time.sleep(0.05)
pause()

# 3. Recorded: random walkers of two species drifting to two centers, recorded as an
#    animation (without the 3D view) and opened as HTML page in the browser view
walkers = [rng.uniform(0, 100, (150, 3)) for _ in range(2)]
centers = [np.array((30, 30, 60)), np.array((70, 70, 40))]
rec = bio7.plot3d_recorder(speed=20)
for t in range(1, 121):
    for s in range(2):
        w = walkers[s]
        walkers[s] = np.clip(w + rng.normal(0, 2, w.shape) + (centers[s] - w) * 0.03, 0, 100)
    bio7.plot3d(bio7.scatter3d(walkers[0], label="Species A", size=0.12),
                bio7.scatter3d(walkers[1], label="Species B", size=0.12),
                title="Random walkers\nStep %d" % t, xlabel="x [m]", ylabel="y [m]", zlabel="z [m]",
                xlim=(0, 100), ylim=(0, 100), zlim=(0, 100), record=rec, show=False)
bio7.plot3d_export(rec, os.path.join(tempfile.gettempdir(), "random_walkers.html"))
pause(5)

# 4. The other charts
groups = {"Control": rng.normal(10, 2, 60), "Low dose": rng.normal(12, 2.5, 45), "High dose": rng.normal(15, 3, 70)}
bio7.plot3d(bio7.boxplot3d(groups), title="Treatments: box plots", zlabel="Growth [mm]")
pause()
bio7.plot3d(bio7.violin3d(groups), title="Treatments: violins", zlabel="Growth [mm]")
pause()
years = np.arange(1, 21)
bio7.plot3d(bio7.area3d(years, {"Grass": 20 + years, "Shrubs": 10 + np.sqrt(years) * 3, "Trees": years ** 1.3}),
            title="Land cover (stacked areas)", xlabel="Year", zlabel="Area [ha]")
pause()
bio7.plot3d(bio7.radar3d([[5.0, 3.4, 1.5, 0.2], [5.9, 2.8, 4.3, 1.3], [6.6, 3.0, 5.6, 2.0]],
                         axes=["Sepal length", "Sepal width", "Petal length", "Petal width"],
                         names=["Setosa", "Versicolor", "Virginica"], rings=4),
            title="Iris: the mean of the species")
pause()
wind = np.array([[4, 2, 1], [6, 3, 1], [3, 2, 1], [2, 1, 0], [5, 3, 1], [8, 5, 2], [6, 4, 2], [3, 2, 1]], dtype=float)
bio7.plot3d(bio7.rose3d(wind, sectors=["N", "NE", "E", "SE", "S", "SW", "W", "NW"],
                        classes=["< 3 m/s", "3-8 m/s", "> 8 m/s"]), title="Wind rose")
pause()
bio7.plot3d(bio7.treemap3d({"Forest": 45, "Grassland": 30, "Water": 15, "Urban": 10}, heights=True),
            title="Land cover (treemap)")
