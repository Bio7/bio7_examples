"""The Game of Life of J. H. Conway: a dead cell with 3 living neighbors is born, a living cell
with 2 or 3 living neighbors survives, all other cells die or stay dead.

Run the script in Bio7 (the grid opens in ImageJ with the settings on the left), start it with
the Play/Pause button and export it with "Export Simulation as HTML..." (the same script runs
in the web browser with Pyodide).
"""
import numpy as np
from bio7sim import GridModel, param, action, simulation


class GameOfLife(GridModel):
    """The Game of Life of J. H. Conway: a dead cell with 3 living neighbors is born, a living cell with 2 or 3 living neighbors survives, all other cells die or stay dead."""

    grid_size = param(120, min=20, max=300, label="Grid size (cells)", restart=True)
    density = param(0.3, min=0, max=1, step=0.01, label="Start density", restart=True)
    torus = param(True, label="Torus world (wrap at the borders)")

    def init(self):
        self.size(self.grid_size, self.grid_size)      # a new grid, all states 0
        self.palette("#ffffff", "#1f3a93")              # the colors of the states 0 and 1
        self.cells[:] = self.random_array() < self.density

    def step(self):
        self.wrap = self.torus
        n = self.count_neighbors(1)                     # the living neighbors of all cells (an array)
        alive = self.cells == 1
        self.cells = ((n == 3) | (alive & (n == 2))).astype(np.int32)
        self.chart("Living cells", self.count(1))

    @action("Add a glider")
    def glider(self):
        x, y = self.width // 2, self.height // 2
        for dx, dy in [(1, 0), (2, 1), (0, 2), (1, 2), (2, 2)]:
            self.set(x + dx, y + dy, 1)


simulation(GameOfLife)
