"""A wildfire in the Harz mountains (Germany) with real elevation and land cover data.

load() reads the data with GDAL (only in Bio7): the elevation and the ESA WorldCover land cover,
resampled to a grid of 200 columns. The fire spreads by the land cover (forest, grassland and
fields, settlements), faster uphill and with the wind. The model runs in ImageJ and on the
WorldWind globe of Bio7; the exported page shows it on an online map (with the loaded data).
The data is in data/geo of this project (see its README.md for the sources and licenses).
"""
import numpy as np
from bio7sim import GeoGridModel, param, action, simulation

# The states: the fuel (by the land cover), burning, burnt, no fuel
FOREST, BURNING, BURNT, NO_FUEL, OPEN_LAND, SETTLEMENT = 0, 1, 2, 3, 4, 5
NEIGHBORS = [(-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)]


class Wildfire(GeoGridModel):
    """A wildfire in the Harz mountains: the fire spreads by the land cover (forest, grassland and fields, settlements), faster uphill and with the wind."""

    forest = param(0.35, min=0, max=1, step=0.01, label="Spread in the forest", group="Spread by the land cover")
    open_land = param(0.15, min=0, max=1, step=0.01, label="Spread over grassland and fields", group="Spread by the land cover")
    settlement = param(0.04, min=0, max=1, step=0.01, label="Spread over settlements", group="Spread by the land cover")
    slope_effect = param(4.0, min=0, max=10, step=0.1, label="Effect of the slope (uphill faster)", group="Terrain")
    wind = param("West", options=["No wind", "West", "East", "North", "South"], label="Wind from", group="Wind")
    wind_strength = param(0.6, min=0, max=1, step=0.05, label="Strength of the wind", group="Wind")

    def load(self):
        self.grid("data/geo/harz_dem.tif", 200)                          # the grid: extent, coordinate system, 200 columns
        self.add_layer("elevation", self.raster("data/geo/harz_dem.tif"))
        # ESA WorldCover classes (10 trees, 20 shrubs, 30 grassland, 40 cropland, 50 built-up, 60 bare, 80 water, ...)
        self.add_layer("landcover", self.raster("data/geo/harz_landcover.tif", 1, "near"))
        self.show_on_globe()                                              # also live on the WorldWind globe of Bio7
        self.attribution("Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data, ETOPO1); "
                         "Land cover: © ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium (CC BY 4.0)")

    def init(self):
        self.palette("#2e7d3240", "#ff5a00", "#2b2420", "#00000000", "#f2d16b30", "#7f7f7f80")   # with alpha: the map below
        self.wrap = False
        self.elevation = self.layer("elevation")
        lc = self.layer("landcover")
        self.fuel = np.full(lc.shape, NO_FUEL, dtype=np.int32)
        self.fuel[np.isin(lc, [10, 95])] = FOREST
        self.fuel[np.isin(lc, [20, 30, 40, 90])] = OPEN_LAND
        self.fuel[lc == 50] = SETTLEMENT
        self.cells = self.fuel.copy()
        self.ignite(10.668, 51.788)

    def step(self):
        wx, wy = {"West": (1, 0), "East": (-1, 0), "North": (0, 1), "South": (0, -1)}.get(self.wind, (0, 0))
        c = self.cells
        p = np.select([c == FOREST, c == OPEN_LAND, c == SETTLEMENT], [self.forest, self.open_land, self.settlement], 0.0)
        e = np.pad(self.elevation, 1, mode="edge")
        h, w = c.shape
        burning = np.pad(c == BURNING, 1)
        catch = np.zeros(c.shape, dtype=bool)
        for dy, dx in NEIGHBORS:
            # the burning neighbors in the direction (dy, dx) and the rise from them to the cell
            near = burning[1 + dy:1 + dy + h, 1 + dx:1 + dx + w]
            diagonal = 1.4142 if dx != 0 and dy != 0 else 1.0
            rise = (self.elevation - e[1 + dy:1 + dy + h, 1 + dx:1 + dx + w]) / (diagonal * self.cell_size_meters)
            slope = np.clip(1 + self.slope_effect * rise, 0.2, 3)
            wind = 1 + self.wind_strength * (-dx * wx - dy * wy) / diagonal
            catch |= near & (self.random_array() < p * slope * wind)
        new = c.copy()
        new[c == BURNING] = BURNT
        new[catch & (p > 0)] = BURNING
        self.cells = new
        ha = self.cell_size_meters ** 2 / 10000
        self.chart("Burning area (ha)", self.count(BURNING) * ha)
        self.chart("Burnt area (ha)", self.count(BURNT) * ha)
        self.chart("Burnt settlements (ha)", np.count_nonzero((new == BURNT) & (self.fuel == SETTLEMENT)) * ha)

    def ignite(self, lon, lat):
        """Ignites the burnable cells around the coordinates."""
        cx, cy = self.col_at(lon, lat), self.row_at(lon, lat)
        if cx < 0 or cy < 0:
            return
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < self.width and 0 <= y < self.height and self.get(x, y) in (FOREST, OPEN_LAND, SETTLEMENT):
                    self.set(x, y, BURNING)

    @action("Ignite near the Brocken")
    def ignite_brocken(self):
        self.ignite(10.630, 51.795)

    @action("Lightning at a random place")
    def lightning(self):
        self.ignite(self.lon(self.random_int(self.width)), self.lat(self.random_int(self.height)))


simulation(Wildfire)
