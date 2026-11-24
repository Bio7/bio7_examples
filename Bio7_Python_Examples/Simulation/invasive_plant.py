"""A neobiota: the invasion of the Himalayan balsam (Impatiens glandulifera) along a river.

An annual plant from the Himalaya which escaped from gardens and invades river banks in Central
Europe. One step is one year, one cell 5 x 5 m. The landscape (generated from the seed): a river
which flows from the top to the bottom, its banks, floodplain forest, meadows, a road and a
village at the bridge; the balsam starts in a garden of the village. Each year the plants throw
their seeds a few meters (the cell and its neighbors), seeds which fall into the river drift
downstream (an exponential distance) onto a bank, seeds at the road jump far with vehicles and
mowing, the seeds rest in a short-lived seed bank and the cover of a cell grows with the
germinating seeds up to the suitability of its habitat (banks 1, floodplain forest 0.6, road
verges 0.3, the village 0.15, meadows 0.04: the dense grass). The invasion runs fast downstream
and slowly upstream.

Control: a click (or a drag) pulls the plants and the seeds in a 5 x 5 cells square, the button
"Control campaign" pulls half of the invaded banks. Charts: the invaded area (ha), the invaded
share of the banks, the seed bank and the invaded cells of each habitat. All cells of a step are
computed with numpy arrays.
"""
import numpy as np
from bio7sim import GridModel, param, action, simulation, sin_deg, fractal_noise

WATER, BANK, FOREST, MEADOW, ROAD, VILLAGE, SPARSE, DENSE = range(8)
HABITATS = ["Water", "Bank", "Floodplain forest", "Meadow", "Road verge", "Village"]
SUITABILITY = np.array([0, 1.0, 0.6, 0.04, 0.3, 0.15])
W, H = 160, 100


class InvasivePlant(GridModel):
    """The invasion of the Himalayan balsam along a river: seeds drift downstream, jump along the road and rest in a seed bank."""

    fecundity = param(400.0, min=50, max=2000, label="Seeds per cell (full cover)", group="Balsam")
    bank_survival = param(0.3, min=0, max=0.9, step=0.05, label="Seed bank survival per year", group="Balsam")
    germination = param(0.6, min=0.1, max=1, step=0.05, label="Germination", group="Balsam")
    river_drift = param(True, label="Drift of the seeds in the river", group="Dispersal")
    drift_distance = param(30.0, min=5, max=100, label="Mean drift distance (cells)", group="Dispersal")
    road_jumps = param(0.3, min=0, max=1, step=0.05, label="Road jumps (probability per year)", group="Dispersal")

    def init(self):
        self.size(W, H)
        self.wrap = False
        self.palette("#4a90d9", "#a8d5a2", "#3e7b47", "#dfe9b0", "#9e9e9e", "#d2c9be", "#ee9ad4", "#a2238f")
        phase, ox, oy = self.random(0, 360), self.random(0, 500), self.random(0, 500)
        ys, xs = np.mgrid[0:H, 0:W]
        self.river_x = np.array([W * 0.45 + 20 * sin_deg(phase + y * 3.6) + 10 * (fractal_noise(ox, oy + y * 0.04, 3) - 0.5) for y in range(H)])
        self.road_y = np.array([int(round(H * 0.68 + 3 * sin_deg(x * 2.5))) for x in range(W)])
        d = np.abs(xs - self.river_x[:, None])
        dy = np.abs(ys - self.road_y[None, :])
        forest = fractal_noise(ox + xs * 0.07, oy + ys * 0.07, 4) > 0.5
        h = np.full((H, W), MEADOW)
        h[(d < 30) & forest] = FOREST
        h[(dy < 12) & (d < 28)] = VILLAGE
        h[dy == 0] = ROAD
        h[d <= 4.5] = BANK
        h[d <= 1.5] = WATER
        self.habitat = h
        # the cells beside the water (their seeds can fall into the river)
        water = np.pad(h == WATER, 1)
        self.near_water = np.zeros((H, W), dtype=bool)
        for dy_ in (-1, 0, 1):
            for dx_ in (-1, 0, 1):
                self.near_water |= water[1 + dy_:1 + dy_ + H, 1 + dx_:1 + dx_ + W]
        self.cover = np.zeros((H, W))
        self.bank = np.zeros((H, W))
        # the garden: in the village, near the river, upstream of the bridge
        gy = self.road_y[int(self.river_x[0])] - 9
        gx = int(round(self.river_x[gy] + 9))
        self.cover[gy - 1:gy + 2, gx - 1:gx + 2] = 1.0
        self.show()

    def step(self):
        n = self.cover * self.fecundity
        seeds = np.zeros((H, W))
        # seeds which fall into the river drift downstream (three packets of each cell)
        if self.river_drift:
            ys, xs = np.nonzero((n > 0) & self.near_water)
            drift = n[ys, xs] * 0.3
            n[ys, xs] -= drift
            k = len(ys)
            if k:
                y2 = np.repeat(ys, 3) + np.round(self.rng.exponential(self.drift_distance, 3 * k)).astype(int)
                side = np.where(self.random_array(3 * k) < 0.5, 1, -1)
                ok = y2 < H
                y2 = y2[ok]
                x2 = np.round(self.river_x[y2] + side[ok] * (2 + self.rng.integers(0, 3, len(y2)))).astype(int)
                inside = (x2 >= 0) & (x2 < W)
                np.add.at(seeds, (y2[inside], x2[inside]), np.repeat(drift / 3, 3)[ok][inside])
        # rare jumps along the road
        ys, xs = np.nonzero((n > 0) & (self.habitat == ROAD))
        jump = self.random_array(len(ys)) < self.road_jumps
        x2 = xs[jump] + self.rng.integers(-60, 61, int(jump.sum()))
        x2 = x2[(x2 >= 0) & (x2 < W)]
        np.add.at(seeds, (self.road_y[x2], x2), 20.0)
        # the explosive capsules: the cell and its eight neighbors
        p = np.pad(n, 1)
        around = sum(p[1 + dy:1 + dy + H, 1 + dx:1 + dx + W] for dy in (-1, 0, 1) for dx in (-1, 0, 1) if dy or dx)
        seeds += 0.4 * n + 0.6 / 8 * around
        seeds[self.habitat == WATER] = 0
        # the seed bank, the germination in spring and the cover of the year
        self.bank = self.bank * self.bank_survival + seeds
        germinating = self.bank * self.germination
        self.bank -= germinating
        self.cover = SUITABILITY[self.habitat] * germinating / (germinating + 50)
        self.show()

    def show(self):
        invaded = self.cover > 0.1
        self.cells = np.where(self.cover > 0.5, DENSE, np.where(invaded, SPARSE, self.habitat)).astype(np.int32)
        banks = self.habitat == BANK
        self.chart("Invaded area (ha)", float(invaded.sum()) * 25 / 10000)
        self.chart("Invaded banks (%)", 100 * float((invaded & banks).sum()) / float(banks.sum()), chart="Banks")
        self.chart("Seeds (thousands)", float(self.bank.sum()) / 1000, chart="Seed bank")
        self.chart_bars("Invaded cells per habitat", HABITATS, [float((invaded & (self.habitat == k)).sum()) for k in range(len(HABITATS))])
        self.record("Invaded area (ha)", float(invaded.sum()) * 25 / 10000)

    def click(self, x, y):
        """A click: pulls the plants and the seeds in a 5 x 5 cells square."""
        self.pull(int(x), int(y))
        self.show()

    def drag(self, x, y):
        """A drag: pulls the plants along the way."""
        self.pull(int(x), int(y))
        self.show()

    def pull(self, cx, cy):
        y0, y1, x0, x1 = max(0, cy - 2), min(H, cy + 3), max(0, cx - 2), min(W, cx + 3)
        self.cover[y0:y1, x0:x1] = 0
        self.bank[y0:y1, x0:x1] = 0

    @action("Control campaign (half of the invaded banks)", group="Control")
    def campaign(self):
        hit = (self.habitat == BANK) & (self.cover > 0.1) & (self.random_array((H, W)) < 0.5)
        self.cover[hit] = 0
        self.bank[hit] *= 0.3
        self.show()


simulation(InvasivePlant)
