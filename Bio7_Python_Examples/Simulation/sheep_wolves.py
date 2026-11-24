"""Sheep and wolves on a field (a continuous model) with images, shapes and several charts.

The grass is a biomass (0 - 1, a numpy array) which grows logistically, the animals move with a
heading and a speed: the sheep prefer the directions with more grass, the wolves hunt the nearest
sheep in their view. A fenced pasture (a polygon) protects the sheep.

The drawing of each step: the animals as images rotated to their heading (draw_image), the hunting
lines (draw_line), the view of the first wolf (draw_circle), the fence (draw_polygon) and a text
(draw_text). Charts: the populations, the grass (a second chart), a phase plot (chart_xy) and the
energies of the sheep (chart_bars). The images are in data/sprites of this project.
"""
import numpy as np
from bio7sim import ValueGridModel, param, action, simulation, fractal_noise, heading_of, cos_deg, sin_deg

FENCE_X, FENCE_Y = [6, 22, 26, 10], [8, 5, 20, 24]


class Animal:
    def __init__(self, x, y, heading, energy, wolf):
        self.x, self.y, self.heading, self.energy, self.wolf = x, y, heading, energy, wolf
        self.prey = None


def inside(x, y):
    """The point in the fenced pasture (a ray crossing test)."""
    result, n = False, len(FENCE_X)
    for i in range(n):
        j = (i + n - 1) % n
        if (FENCE_Y[i] > y) != (FENCE_Y[j] > y) and x < (FENCE_X[j] - FENCE_X[i]) * (y - FENCE_Y[i]) / (FENCE_Y[j] - FENCE_Y[i]) + FENCE_X[i]:
            result = not result
    return result


class SheepWolves(ValueGridModel):
    """Sheep and wolves on a field: the sheep prefer the directions with more grass, the wolves hunt the nearest sheep in their view, a fenced pasture protects the sheep."""

    sheep = param(60, min=10, max=150, label="Sheep at the start", restart=True, group="Start")
    wolves = param(8, min=0, max=30, label="Wolves at the start", restart=True, group="Start")
    growth = param(0.04, min=0, max=0.2, step=0.005, label="Growth of the grass", group="Field")
    fence = param(True, label="Fenced pasture (a refuge)", group="Field")
    wolf_speed = param(0.75, min=0.2, max=1.5, step=0.05, label="Speed of the wolves", group="Animals")
    view = param(9.0, min=2, max=20, step=0.5, label="View of the wolves (cells)", group="Animals")

    def init(self):
        self.size(60, 45)
        self.range(0, 1)
        self.color_map("#b39b62", "#9ccc65", "#33691e")
        y, x = np.mgrid[0:self.height, 0:self.width]
        self.values = 0.2 + 0.8 * fractal_noise(x * 0.09, y * 0.09, 4)          # a patchy field
        self.load_image("sheep", "data/sprites/sheep.png")
        self.load_image("wolf", "data/sprites/wolf.png")
        self.animals = [Animal(self.random(0, self.width), self.random(0, self.height), self.random(0, 360), self.random(5, 10), False) for _ in range(self.sheep)]
        self.animals += [Animal(self.random(30, self.width), self.random(0, self.height), self.random(0, 360), self.random(10, 20), True) for _ in range(self.wolves)]
        self.show()

    def step(self):
        v = self.values
        self.values = np.clip(v + self.growth * v * (1 - v) + 0.001, 0, 1)     # the logistic growth (all cells)
        born = []
        for a in self.animals:
            if a.energy > 0:
                (self.hunt if a.wolf else self.graze)(a, born)
        self.animals = [a for a in self.animals if a.energy > 0] + born
        self.show()

    def move(self, a, speed):
        """Moves in the heading (0 up, clockwise; y down in the grid) and wraps around."""
        a.x = (a.x + speed * sin_deg(a.heading)) % self.width
        a.y = (a.y - speed * cos_deg(a.heading)) % self.height

    def grass(self, x, y):
        return self.values[int(y % self.height), int(x % self.width)]

    def graze(self, a, born):
        turns = [-40, 0, 40]
        weights = [0.05 + self.grass(a.x + 2 * sin_deg(a.heading + t), a.y - 2 * cos_deg(a.heading + t)) for t in turns]
        a.heading = (a.heading + turns[self.random_weighted(weights)] + self.random(-10, 10)) % 360
        self.move(a, 0.35)
        cx, cy = int(a.x), int(a.y)
        eaten = min(0.3, self.values[cy, cx])
        self.values[cy, cx] -= eaten
        a.energy += eaten * 3 - 0.45
        if a.energy > 12 and self.chance(0.06):
            a.energy /= 2
            born.append(Animal(a.x, a.y, self.random(0, 360), a.energy, False))

    def hunt(self, w, born):
        free = [a for a in self.animals if not a.wolf and a.energy > 0 and not (self.fence and inside(a.x, a.y))]
        best = min(free, key=lambda a: self.distance(w.x, w.y, a.x, a.y), default=None)
        w.prey = best if best is not None and self.distance(w.x, w.y, best.x, best.y) < self.view else None
        if w.prey is not None:
            w.heading = heading_of(w.prey.x - w.x, w.y - w.prey.y)
            if self.distance(w.x, w.y, w.prey.x, w.prey.y) < 0.8:
                w.prey.energy, w.prey = 0, None
                w.energy += 10
        else:
            w.heading = (w.heading + self.random(-30, 30)) % 360
        ox, oy = w.x, w.y
        self.move(w, self.wolf_speed if w.prey is not None else self.wolf_speed * 0.6)
        if self.fence and inside(w.x, w.y):                                    # the fence: the wolf turns back
            w.x, w.y, w.heading = ox, oy, (w.heading + 180) % 360
        w.energy -= 0.25
        if w.energy > 30 and self.chance(0.04):
            w.energy /= 2
            born.append(Animal(w.x, w.y, self.random(0, 360), w.energy, True))

    def show(self):
        if self.fence:
            self.draw_polygon(FENCE_X, FENCE_Y, fill="#ffffff22", stroke="#f5f0e6")
        wolves = [a for a in self.animals if a.wolf]
        sheep = [a for a in self.animals if not a.wolf]
        for w in wolves:
            if w.prey is not None:
                self.draw_line(w.x, w.y, w.prey.x, w.prey.y, "#d62728aa", 2)
        if wolves:
            self.draw_circle(wolves[0].x, wolves[0].y, self.view, None, "#ffffff88")
        for a in self.animals:
            self.draw_image("wolf" if a.wolf else "sheep", a.x, a.y, 2.6 if a.wolf else 2.0, a.heading)
        self.draw_text(1, 2.5, "Step %d: %d sheep, %d wolves" % (self.t, len(sheep), len(wolves)), "#ffffff", 1.6)
        energies = np.array([a.energy for a in sheep])
        self.chart("Sheep", len(sheep))
        self.chart("Wolves", len(wolves))
        self.chart("Mean biomass", self.mean(), chart="Grass")
        self.chart("Median energy of the sheep / 20", float(np.median(energies)) / 20 if len(energies) else 0, chart="Grass")
        self.chart_xy("Phase plot", "Sheep - wolves", len(sheep), len(wolves))
        bins = np.bincount(np.clip((energies // 4).astype(int), 0, 4), minlength=5) if len(energies) else np.zeros(5)
        self.chart_bars("Energies of the sheep", ["0-4", "4-8", "8-12", "12-16", "16+"], bins)

    @action("Release 3 wolves")
    def release_wolves(self):
        for _ in range(3):
            self.animals.append(Animal(self.random(30, self.width), self.random(0, self.height), self.random(0, 360), 15, True))


simulation(SheepWolves)
