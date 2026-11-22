"""A heat diffusion: the heat flows to the cooler neighbors (a ValueGridModel with a color map).

Run the script in Bio7 (ImageJ shows the values with the color map), start it with Play/Pause
and export it with "Export Simulation as HTML..." (the page runs the script with Pyodide).
"""
from bio7sim import ValueGridModel, param, action, simulation


class HeatDiffusion(ValueGridModel):
    """A heat diffusion: the heat flows to the cooler neighbors, the plate loses heat at its surface. Add heat sources with the button."""

    diffusion = param(0.2, min=0, max=0.25, step=0.005, label="Diffusion")
    cooling = param(0.002, min=0, max=0.02, step=0.001, label="Cooling per step")
    sources = param(3, min=1, max=10, label="Heat sources at the start", restart=True)
    torus = param(False, label="Torus (the heat flows over the borders)")

    def init(self):
        self.size(120, 120)
        self.range(0, 1)                                     # the values of the first and the last color
        self.color_map("#000004", "#51127c", "#b73779", "#fc8961", "#fcfdbf")
        for _ in range(self.sources):
            self.add_source()

    def step(self):
        self.wrap = self.torus
        # the diffusion of all cells with numpy: laplacian() is the sum of the 4 neighbors - 4 * value
        self.values = (self.values + self.diffusion * self.laplacian()) * (1 - self.cooling)
        self.chart("Mean temperature", self.mean())
        self.chart("Maximum temperature", float(self.values.max()))

    @action("Add a heat source")
    def add_source(self):
        x, y = self.random_int(10, self.width - 10), self.random_int(10, self.height - 10)
        self.values[y - 3:y + 4, x - 3:x + 4] = 1.0


simulation(HeatDiffusion)
