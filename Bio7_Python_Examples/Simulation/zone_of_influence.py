"""The zone of influence model of plant competition (after Schwinning and Weiner 1998 and Weiner et al. 2001).

Each plant takes up resources in a circle around it, its zone of influence, which grows with its
mass m (the area A = c * m^(2/3)). Where zones overlap, the plants share the resource of a cell:
complete symmetry (equal shares), size-symmetric (shares in proportion to the mass),
size-asymmetric (in proportion to m^4: larger plants get much more, e.g., light) or complete
asymmetry (the largest plant gets all). The growth per step: dm = r * (A_eff - c * m^2 / M^(4/3));
A_eff is the uptake area after the sharing, M the size of a plant without neighbors. A plant which
gets less than 5 % of the resources of its zone five steps in a row starves and dies (or when its
mass falls below 0.5): self-thinning. Asymmetric competition makes few large and many small plants
(a high Gini coefficient of the sizes), symmetric competition plants of similar sizes.

The grid shows the crowding (the number of zones over a cell), the circles the zones. A click plants
a seedling. Charts: the plants alive, the mean mass, the Gini coefficient and the size classes.
"""
import math

import numpy as np
from bio7sim import ValueGridModel, param, action, simulation

W, H = 100, 100


def cbrt(v):
    """The cube root with the same digits in Bio7 and in the page (Newton's method; also for arrays)."""
    v = np.asarray(v, dtype=float)
    r = np.where(v < 1, 1.0, v / 3 + 1)
    for _ in range(60):
        r = (2 * r + v / (r * r)) / 3
    return np.where(v <= 0, 0.0, r)


class ZoneOfInfluence(ValueGridModel):
    """The zone of influence model of plant competition: overlapping zones share the resources, symmetric or asymmetric."""

    plants = param(250, min=20, max=800, label="Plants at the start", restart=True, group="Plants")
    competition = param("Size-asymmetric", options=["Complete symmetry", "Size-symmetric", "Size-asymmetric", "Complete asymmetry"],
                        label="Competition", group="Plants")
    rate = param(0.6, min=0.1, max=2, step=0.05, label="Growth rate r", group="Plants")
    max_mass = param(1000.0, min=100, max=3000, step=50, label="Size without neighbors M", group="Plants")
    max_radius = param(8.0, min=3, max=15, step=0.5, label="Radius of the zone at M (cells)", restart=True, group="Plants")

    def init(self):
        self.size(W, H)
        self.wrap = False
        self.range(0, 6)
        self.color_map("#f7f4ea", "#e6dfb8", "#c9b97a", "#a3884a", "#6e5a2c")
        cm = float(cbrt(self.max_mass))
        # c: the area of the zone of a plant of the size M is pi * radius^2
        self.c = math.pi * self.max_radius * self.max_radius / (cm * cm)
        n = int(self.plants)
        self.px, self.py = self.random_array(n, 0, W), self.random_array(n, 0, H)
        self.mass = self.random_array(n, 0.8, 1.2)
        self.alive = np.ones(n, dtype=bool)
        self.starving = np.zeros(n, dtype=int)
        self.show()

    def plant(self, x, y, m):
        self.px, self.py = np.append(self.px, x), np.append(self.py, y)
        self.mass = np.append(self.mass, m)
        self.alive = np.append(self.alive, True)
        self.starving = np.append(self.starving, 0)

    def area(self, m):
        cm = cbrt(m)
        return self.c * cm * cm

    def zone(self, i, r):
        """The cells of the zone of plant i: its box and the mask (the centers within the radius, or the cell of the plant)."""
        x0, x1 = max(0, int(math.floor(self.px[i] - r))), min(W - 1, int(math.floor(self.px[i] + r)))
        y0, y1 = max(0, int(math.floor(self.py[i] - r))), min(H - 1, int(math.floor(self.py[i] + r)))
        ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1]
        dx, dy = xs + 0.5 - self.px[i], ys + 0.5 - self.py[i]
        mask = (dx * dx + dy * dy <= r * r) | ((xs == int(math.floor(self.px[i]))) & (ys == int(math.floor(self.py[i]))))
        return (slice(y0, y1 + 1), slice(x0, x1 + 1)), mask

    def step(self):
        p = {"Complete symmetry": 0, "Size-symmetric": 1}.get(self.competition, 4)
        complete = self.competition == "Complete asymmetry"
        # the weights as products (the same digits in Bio7 and in the page)
        w = np.ones(len(self.mass)) if p == 0 else self.mass if p == 1 else self.mass * self.mass * self.mass * self.mass
        radius = np.sqrt(self.area(self.mass) / math.pi)
        crowd, weights, largest = np.zeros((H, W)), np.zeros((H, W)), np.zeros((H, W))
        owner = np.full((H, W), -1)
        live = np.nonzero(self.alive)[0]
        zones = {}
        # the zones over the cells: the crowding, the sum of the weights, the largest plant
        for i in live:
            box, mask = self.zone(i, radius[i])
            zones[i] = (box, mask)
            crowd[box][mask] += 1
            weights[box][mask] += w[i]
            bigger = mask & (self.mass[i] > largest[box])
            largest[box][bigger] = self.mass[i]
            owner[box][bigger] = i
        # the uptake area after the sharing and the growth
        area = self.area(self.mass)
        cm = float(cbrt(self.max_mass))
        m43 = cm * cm * cm * cm
        effective = np.zeros(len(self.mass))
        for i in live:
            box, mask = zones[i]
            share = np.sum(owner[box][mask] == i) if complete else np.sum(w[i] / weights[box][mask])
            effective[i] = area[i] * share / mask.sum()
        growth = self.rate * (effective - self.c * self.mass * self.mass / m43)
        self.mass = np.where(self.alive, self.mass + growth, self.mass)
        self.starving = np.where(self.alive & (effective < 0.05 * area), self.starving + 1, 0)
        self.alive &= ~((self.starving >= 5) | (self.mass < 0.5))
        self.values = crowd
        self.show()

    def show(self):
        live = np.nonzero(self.alive)[0]
        m = self.mass[live]
        radius = np.sqrt(self.area(m) / math.pi)
        for k, i in enumerate(live):
            big = m[k] / self.max_mass > 0.5
            self.draw_circle(float(self.px[i]), float(self.py[i]), float(radius[k]), "#2e7d3240" if big else "#8bc34a33", "#1b5e20" if big else "#689f38")
        n = len(m)
        mean = float(m.mean()) if n else 0.0
        gini = float(np.abs(m[:, None] - m[None, :]).sum() / (2.0 * n * n * mean)) if n > 1 and mean > 0 else 0.0
        classes = np.bincount(np.minimum(9, np.floor(m / self.max_mass * 10).astype(int)), minlength=10)
        labels = ["< %d" % round((k + 1) * self.max_mass / 10) for k in range(10)]
        self.chart("Plants alive", n)
        self.chart("Mean mass", mean, chart="Size")
        self.chart("Gini coefficient", gini, chart="Inequality")
        self.chart_bars("Size classes (mass)", labels, [float(v) for v in classes])
        self.record("Gini coefficient", gini)
        self.record("Plants alive", n)

    def click(self, x, y):
        """A click: a seedling at the position."""
        self.plant(x, y, 1.0)
        self.show()

    @action("Sow 50 seedlings", group="Plants")
    def sow(self):
        for _ in range(50):
            self.plant(self.random(0, W), self.random(0, H), 1.0)
        self.show()


simulation(ZoneOfInfluence)
