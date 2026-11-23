# 3D plots (OpenGL) in the 3D view of Bio7 from Python: layers with
# bio7.scatter3d(), bio7.surface3d(), bio7.bars3d(), bio7.line3d(), ... shown
# with bio7.plot3d(). Rotate the plot with the mouse, zoom with the mouse wheel;
# the 3D view can be shown in fullscreen mode (F2, F3: second monitor).
#
#   bio7.plot3d(bio7.scatter3d(x, y, z), title="...", xlabel="...", colorbar="...")
#
# Grids: z[i, j] belongs to x[i], y[j] (np.meshgrid(x, y, indexing="ij")); a grid
# of np.meshgrid in the default order (len(y), len(x)) is transposed automatically,
# or pass a function f(X, Y).
#
# Each plot replaces the plot before: the script waits a few seconds between the
# plots (or run the sections one by one).
#
# Requires: numpy

import os
import tempfile
import time
import webbrowser

import numpy as np

import bio7

rng = np.random.default_rng(7)


def pause(seconds=4):
    time.sleep(seconds)


# 1. Scatter: three clusters as layers (legend), with lines down to the floor
centers = {"Cluster A": (0, 0, 0), "Cluster B": (4, 1, 3), "Cluster C": (1, 5, 6)}
colors = {"Cluster A": "#1F77B4", "Cluster B": "#FF7F0E", "Cluster C": "#2CA02C"}
layers = []
for name, c in centers.items():
    pts = rng.normal(c, 0.8, size=(60, 3))
    layers.append(bio7.scatter3d(pts, label=name, color=colors[name], size=0.12, drop_lines=True))
bio7.plot3d(layers, title="Three clusters", xlabel="x", ylabel="y", zlabel="z")
pause()

# 2. Surface of a function with its contour lines on the floor
x = np.linspace(-3, 3, 60)
y = np.linspace(-3, 3, 60)


def f(X, Y):
    return np.sin(X) * np.cos(Y) * np.exp(-(X**2 + Y**2) / 8)


bio7.plot3d(bio7.surface3d(x, y, f, cmap="viridis", mesh=True),
            bio7.contour3d(x, y, f, levels=12, on_floor=True, color="gray"),
            title="z = sin(x) cos(y) exp(-(x^2+y^2)/8)", colorbar="z", legend=False)
pause()

# 3. Bars: a 2D histogram (np.histogram2d: H[i, j] for the x bin i and the y bin j)
a = rng.normal(0, 1, 3000)
b = 0.6 * a + rng.normal(0, 0.8, 3000)
H, xe, ye = np.histogram2d(a, b, bins=(12, 12))
bio7.plot3d(bio7.bars3d((xe[:-1] + xe[1:]) / 2, (ye[:-1] + ye[1:]) / 2, H, cmap="rainbow", width=0.8),
            title="2D histogram", xlabel="a", ylabel="b", zlabel="Count", colorbar="Count", legend=False)
pause()

# 4. A line: the Lorenz attractor (Euler steps)
def lorenz(n=6000, dt=0.006, s=10.0, r=28.0, beta=8 / 3):
    p = np.empty((n, 3))
    p[0] = (1, 1, 1)
    for i in range(1, n):
        x_, y_, z_ = p[i - 1]
        p[i] = p[i - 1] + dt * np.array((s * (y_ - x_), x_ * (r - z_) - y_, x_ * y_ - beta * z_))
    return p


bio7.plot3d(bio7.line3d(lorenz(), label="Lorenz attractor", color="#C2255C"),
            title="Lorenz attractor", xlabel="x", ylabel="y", zlabel="z")
pause()

# 5. Points colored by a value (color bar): a noisy saddle with its model (wireframe)
px, py = rng.uniform(-2, 2, 400), rng.uniform(-2, 2, 400)
pz = px**2 - py**2 + rng.normal(0, 0.3, 400)
g = np.arange(-2, 2.01, 0.25)
bio7.plot3d(bio7.scatter3d(px, py, pz, values=pz - (px**2 - py**2), cmap="diverging", size=0.08),
            bio7.wireframe3d(g, g, lambda X, Y: X**2 - Y**2, color="gray", label="Model"),
            title="Saddle and residuals", colorbar="Residual")
pause()

# 6. A pie chart (shown alone)
bio7.plot3d(bio7.pie3d([45, 30, 15, 10], ["Forest", "Grassland", "Water", "Urban"], explode=0.1),
            title="Land cover")
pause()

# 7. Export: the plot as HTML page (WebGL, rotate it in any web browser), shown in
#    the browser view of Bio7
html = os.path.join(tempfile.gettempdir(), "clusters_3d.html")
bio7.plot3d(layers, title="Three clusters (HTML)", html=html)
webbrowser.open(html)

# bio7.plot3d_clear()   # removes the plot from the 3D view
