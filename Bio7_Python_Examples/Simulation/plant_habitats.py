"""Plant species settle in the habitats which fit their Ellenberg indicator values.

One step is one year, one cell 10 x 10 m. The landscape is a mosaic of six habitats with their
site factors on the scales of Ellenberg (light L 1 - 9, moisture F 1 - 12, reaction R 1 - 9,
nitrogen N 1 - 9; small differences within a patch): beech forest, calcareous grassland, wet
meadow, raised bog, ruderal field margin and river bank with reed. Twelve species with their
indicator values (None: indifferent) and their growth: clonal species (rhizomes: nettle, reed,
wood sorrel) grow in compact patches, species with light seeds (sundew, reed, heather) found new
patches far away, the beech grows slowly, reproduces after 15 years and shades its neighbors
(their light falls by 3).

The suitability of a cell for a species is the product of the fits of the four factors
(1 / (1 + (d / tolerance)^4), d the difference of the site and the indicator value). It decides
the establishment of a seed, the survival of a plant and the displacement of a much weaker
neighbor. Each year all seeds and shoots are collected; the first one which arrives in a cell (a
lottery) may establish there. Without disturbance the strongest competitor of a habitat takes
over (competitive exclusion); disturbance (gaps) keeps weaker species in the habitat. The panel
beside the map summarizes each habitat: the species which fit it by their indicator values (the
mean suitability at least 0.5: they can settle) and the species which grow there now.

The indicator values after Ellenberg et al. (Zeigerwerte von Pflanzen in Mitteleuropa, Scripta
Geobotanica 18) as commonly cited; check them against the original tables before a scientific use.
"""
import numpy as np
from bio7sim import GridModel, param, action, simulation, noise, fractal_noise, cos_deg, sin_deg

W, H, PW = 120, 80, 48
HABITATS = ["Beech forest", "Calcareous grassland", "Wet meadow", "Raised bog", "Ruderal margin", "River bank"]
SITE = np.array([[3, 5, 6, 5], [8, 3, 8, 3], [7, 8, 6, 5], [8, 9, 1, 1], [7, 5, 7, 8], [7, 10, 7, 7]], dtype=float)  # L, F, R, N
SHORT = ["Beech", "Wood sorrel", "Woodruff", "Upright brome", "Meadow clary", "Marsh marigold", "Meadowsweet", "Sundew", "Heather",
         "Nettle", "Reed", "Oat-grass"]
SPECIES = ["Fagus sylvatica", "Oxalis acetosella", "Galium odoratum", "Bromus erectus", "Salvia pratensis", "Caltha palustris",
           "Filipendula ulmaria", "Drosera rotundifolia", "Calluna vulgaris", "Urtica dioica", "Phragmites australis", "Arrhenatherum elatius"]
X = None
VALUES = [[3, 5, X, X], [1, 5, 4, 6], [2, 5, 6, 5], [8, 3, 8, 3], [8, 3, 8, 4], [7, 9, X, 6], [7, 8, X, 5], [8, 9, 1, 1],
          [8, X, 1, 1], [X, 6, 7, 8], [7, 10, 7, 7], [8, 5, 7, 7]]  # L, F, R, N (None: indifferent)
CLONAL = np.array([0, 0.4, 0.35, 0.2, 0, 0.15, 0.3, 0, 0.1, 0.45, 0.5, 0.15])
SEEDS = np.array([6, 3, 2, 10, 15, 10, 20, 30, 40, 25, 30, 25])
DISTANCE = np.array([1.5, 0.8, 1.2, 2, 1, 3, 2.5, 4, 3, 2, 8, 3])
MATURITY = np.array([15, 1, 1, 1, 2, 2, 2, 2, 3, 1, 2, 1])
MORTALITY = np.array([0.01, 0.06, 0.06, 0.05, 0.05, 0.05, 0.05, 0.08, 0.04, 0.04, 0.03, 0.05])
HEIGHT = np.array([30, 0.1, 0.2, 0.8, 0.5, 0.3, 1.5, 0.05, 0.5, 1.2, 3, 1.2])
MISFIT = 0.25
NH, NS = len(HABITATS), len(SHORT)
HABITAT_COLORS = ["#2e6b3a", "#d9c46a", "#7fc6a4", "#b6789a", "#c98a4b", "#5b8fc9"]
SPECIES_COLORS = ["#2f5d34", "#9fd36a", "#5fae5a", "#c9b458", "#7a5cc0", "#f2c230", "#b9a3d9", "#d1435b", "#b03a8a", "#3d6e3a", "#8a9a5b", "#cfa46b"]
# the directions of the seeds (whole degrees, the same digits in Bio7 and in the page)
COS = np.array([cos_deg(a) for a in range(360)])
SIN = np.array([sin_deg(a) for a in range(360)])


class PlantHabitats(GridModel):
    """Plant species settle in the habitats which fit their Ellenberg indicator values: a mosaic of six habitats and twelve species."""

    tolerance = param(1.6, min=0.5, max=4, step=0.1, label="Tolerance (Ellenberg units)", group="Species")
    seed_rain = param(0.3, min=0, max=1, step=0.05, label="Seed rain from outside (per species and year)", group="Species")
    shade = param(True, label="Shade of the beech", group="Species")
    patches = param(16, min=6, max=40, label="Patches of the landscape", restart=True, group="Landscape")
    disturbance = param(0.05, min=0, max=0.1, step=0.005, label="Disturbance (share of the cells cleared per year)", group="Landscape")
    view = param("Species", options=["Species", "Habitats"], label="View", group="Landscape")

    def init(self):
        self.size(W + PW, H)
        self.wrap = False
        self.palette(*(["#e3eadf", "#f3efcf", "#dceee8", "#eadfe6", "#efe5d6", "#d8e6ef"] + SPECIES_COLORS + ["#ffffff"] + HABITAT_COLORS))
        # the habitats: patches around random centers (each habitat at least twice), the borders with noise
        n = int(self.patches)
        self.patch_x, self.patch_y = self.random_array(n, 0, W), self.random_array(n, 0, H)
        self.patch_habitat = np.arange(n) % NH
        ox, oy = self.random(0, 500), self.random(0, 500)
        ys, xs = np.mgrid[0:H, 0:W]
        jx = xs + 12 * (noise(ox + xs * 0.08, oy + ys * 0.08) - 0.5)
        jy = ys + 12 * (noise(ox + 40 + xs * 0.08, oy + ys * 0.08) - 0.5)
        d = (jx[None] - self.patch_x[:, None, None]) ** 2 + (jy[None] - self.patch_y[:, None, None]) ** 2
        self.habitat = self.patch_habitat[np.argmin(d, axis=0)].ravel()
        self.site = np.stack([SITE[self.habitat, f] + 1.2 * (fractal_noise(ox + 100 * f + xs * 0.1, oy + ys * 0.1, 3).ravel() - 0.5) for f in range(4)])
        self.species = np.full(W * H, -1)
        self.age = np.zeros(W * H, dtype=int)
        self.light = self.site[0].copy()
        fit = self.fits()
        # the species which can settle in each habitat (by the indicator values, without shade)
        self.fit_names = []
        for h in range(NH):
            mean = 100 * fit[:, self.habitat == h].mean(axis=1)
            self.fit_names.append(names(mean, 50, 4, False))
        # the seeds of all species everywhere: they establish where the site fits (adult plants)
        s = np.repeat(np.arange(NS), 300)
        self.settle(s, self.rng.integers(0, W * H, len(s)), fit)
        self.age = np.where(self.species >= 0, MATURITY[np.maximum(self.species, 0)], 0)
        self.shade_light()
        self.show()

    def fits(self):
        """The suitability of all cells for all species (species x cells)."""
        f = np.ones((NS, W * H))
        for s in range(NS):
            for k in range(4):
                v = VALUES[s][k]
                if v is not None:
                    d = ((self.light if k == 0 else self.site[k]) - v) / self.tolerance
                    f[s] *= 1 / (1 + d * d * d * d)
        return f

    def shade_light(self):
        """The light: the site, minus 3 under the crowns of the adult beeches (their 8 neighbors)."""
        self.light = self.site[0].copy()
        if not self.shade:
            return
        adult = ((self.species == 0) & (self.age >= MATURITY[0])).reshape(H, W)
        p = np.pad(adult, 1)
        under = np.zeros((H, W), dtype=bool)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dy or dx:
                    under |= p[1 + dy:1 + dy + H, 1 + dx:1 + dx + W]
        under = under.ravel()
        self.light[under] = np.maximum(1, self.site[0][under] - 3)

    def settle(self, s, target, fit):
        """The propagules s at the cells target: the first one in a cell (a lottery) establishes or displaces a weaker plant."""
        order = self.rng.permutation(len(s))
        s, target = s[order], target[order]
        target, first = np.unique(target, return_index=True)
        s = s[first]
        other = self.species[target]
        keep = other != s
        s, target, other = s[keep], target[keep], other[keep]
        chance = self.random_array(len(s))
        fit_s = fit[s, target]
        empty = other < 0
        strength = fit_s * HEIGHT[s] / (HEIGHT[s] + 1)
        o = np.maximum(other, 0)
        weaker = fit[o, target] * HEIGHT[o] / (HEIGHT[o] + 1)
        # an empty cell: the suitability; an occupied cell: only a much stronger plant displaces it
        new = np.where(empty, chance < fit_s, (strength > 2 * weaker) & (chance < 0.2 * (strength - weaker) / np.maximum(strength, 1e-12)))
        self.species[target[new]] = s[new]
        self.age[target[new]] = 0

    def step(self):
        fit = self.fits()
        cell = np.arange(W * H)
        occupied = self.species >= 0
        sp = np.maximum(self.species, 0)
        # mortality: the base rate and the misfit of the site
        dies = occupied & (self.random_array(W * H) < MORTALITY[sp] + MISFIT * (1 - fit[sp, cell]))
        self.species[dies] = -1
        self.age[self.species >= 0] += 1
        self.shade_light()
        fit = self.fits()
        sp = np.maximum(self.species, 0)
        mature = (self.species >= 0) & (self.age >= MATURITY[sp])
        props_s, props_t = [], []
        # clonal growth into the four neighbors
        x, y = cell % W, cell // W
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            go = mature & (self.random_array(W * H) < CLONAL[sp])
            tx, ty = x[go] + dx, y[go] + dy
            ok = (tx >= 0) & (tx < W) & (ty >= 0) & (ty < H)
            props_s.append(sp[go][ok]); props_t.append((ty * W + tx)[ok])
        # seeds: an exponential distance in a random direction
        src = np.repeat(cell[mature], SEEDS[sp[mature]])
        s = self.species[src]
        dist = self.rng.exponential(DISTANCE[s])
        a = self.rng.integers(0, 360, len(src))
        tx = np.round(x[src] + dist * COS[a]).astype(int)
        ty = np.round(y[src] + dist * SIN[a]).astype(int)
        ok = (tx >= 0) & (tx < W) & (ty >= 0) & (ty < H)
        props_s.append(s[ok]); props_t.append((ty * W + tx)[ok])
        # seeds from outside the landscape
        rain = np.repeat(np.nonzero(self.random_array(NS) < self.seed_rain)[0], 3)
        props_s.append(rain); props_t.append(self.rng.integers(0, W * H, len(rain)))
        self.settle(np.concatenate(props_s), np.concatenate(props_t), fit)
        # disturbance (e.g., windthrow, game, floods): gaps for new plants
        gaps = (self.species >= 0) & (self.random_array(W * H) < self.disturbance)
        self.species[gaps] = -1
        self.show()

    def show(self):
        grid = np.full((H, W + PW), NH + NS)
        land = np.where(self.species < 0, self.habitat, NH + self.species)
        if self.view == "Habitats":
            land = NH + NS + 1 + self.habitat
        grid[:, :W] = land.reshape(H, W)
        self.cells = grid.astype(np.int32)
        ty = 3.0
        self.draw_text(W + 2, ty, "Settled species (% of the cells)", "#111111", 2.4)
        ty += 4
        for h in range(NH):
            inh = self.habitat == h
            share = 100 * np.array([np.sum(inh & (self.species == s)) for s in range(NS)]) / max(1, inh.sum())
            self.chart_bars(HABITATS[h] + " (% of the cells)", SHORT, [float(v) for v in share])
            self.chart(HABITATS[h], int(np.sum(share >= 1)), chart="Species in the habitats")
            self.draw_rect(W + 2, ty - 1.6, 1.8, 1.8, HABITAT_COLORS[h], None)
            self.draw_text(W + 5, ty, HABITATS[h], "#111111", 2.2)
            self.draw_text(W + 5, ty + 2.5, "Fits: " + self.fit_names[h], "#2e6b3a", 1.9)
            self.draw_text(W + 5, ty + 4.8, "Now: " + names(share, 1, 3, True), "#444444", 1.9)
            ty += 8.3
        ty += 1
        self.draw_text(W + 2, ty, "Species", "#111111", 2.4)
        ty += 3.4
        for s in range(NS):
            lx, ly = W + 2 + (s % 2) * 23, ty + (s // 2) * 3
            self.draw_rect(lx, ly - 1.6, 1.8, 1.8, SPECIES_COLORS[s], None)
            self.draw_text(lx + 2.6, ly, SHORT[s], "#222222", 2)

    @action("Sow all species everywhere", group="Species")
    def sow_all(self):
        s = np.repeat(np.arange(NS), 40)
        self.settle(s, self.rng.integers(0, W * H, len(s)), self.fits())
        self.show()


def names(value, minimum, count, percent):
    """The species with the largest values (at least minimum, at most count): "Reed 80 %, Nettle 5 %" or "none"."""
    order = [s for s in np.argsort(-np.asarray(value), kind="stable") if value[s] >= minimum][:count]
    text = ", ".join(SHORT[s] + (" %d %%" % round(value[s]) if percent else "") for s in order)
    return text or "none"


simulation(PlantHabitats)
