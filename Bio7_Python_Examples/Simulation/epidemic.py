"""An epidemic on a grid (SIR with immunity): all kinds of settings (sliders, a number field,
a checkbox, a choice, a text field), groups and buttons.

Run the script in Bio7, start it with Play/Pause and export it with "Export Simulation as HTML...".
"""
import numpy as np
from bio7sim import GridModel, param, action, simulation

SUSCEPTIBLE, INFECTED, RECOVERED, VACCINATED = 0, 1, 2, 3


class Epidemic(GridModel):
    """An epidemic on a grid: infected cells infect their susceptible neighbors, recover after some days and lose their immunity again."""

    # World: a slider, a checkbox, a choice
    grid_size = param(120, min=40, max=250, label="Grid size (cells)", restart=True, group="World")
    torus = param(True, label="Torus world (wrap at the borders)", group="World")
    neighborhood = param("Moore (8 cells)", options=["Moore (8 cells)", "von Neumann (4 cells)"], label="Neighborhood", group="World")
    # Disease: a slider and number fields (no min and max)
    infection = param(0.25, min=0, max=1, step=0.01, label="Infection probability (per infected neighbor)", group="Disease")
    recovery_days = param(7, label="Days until the recovery", group="Disease")
    immunity_days = param(60, label="Days of immunity (0: for ever)", group="Disease")
    # Start: a text field and a choice
    initial_infected = param("60,60; 20,100", label="Infected cells at the start (x,y; x,y)", restart=True, group="Start")
    colors = param("Classic", options=["Classic", "Pastel"], label="Colors", group="Start")
    # Interventions: a slider and the buttons below
    vaccine_share = param(0.1, min=0, max=0.5, step=0.01, label="Share of a vaccination", group="Interventions")

    def init(self):
        self.size(self.grid_size, self.grid_size)
        self.days = np.zeros((self.height, self.width), dtype=np.int32)   # the days in the state
        self.apply_colors()
        for part in self.initial_infected.split(";"):
            xy = part.split(",")
            if len(xy) == 2:
                x, y = int(xy[0]), int(xy[1])
                if 0 <= x < self.width and 0 <= y < self.height:
                    self.set(x, y, INFECTED)

    def step(self):
        self.wrap = self.torus
        self.apply_colors()
        c = self.cells
        k = self.count_neighbors(INFECTED) if self.neighborhood.startswith("Moore") else self.count4(INFECTED)
        # the probability of an infection by k infected neighbors: 1 - (1 - p)^k
        infected = (c == SUSCEPTIBLE) & (self.random_array() < 1 - (1 - self.infection) ** k)
        days = self.days + 1
        recovered = (c == INFECTED) & (days >= self.recovery_days)
        susceptible = (c == RECOVERED) & (self.immunity_days > 0) & (days >= self.immunity_days)
        new = c.copy()
        new[infected] = INFECTED
        new[recovered] = RECOVERED
        new[susceptible] = SUSCEPTIBLE
        self.days = np.where(new != c, 0, days)
        self.cells = new
        self.chart("Susceptible", self.count(SUSCEPTIBLE))
        self.chart("Infected", self.count(INFECTED))
        self.chart("Recovered", self.count(RECOVERED))

    def apply_colors(self):
        if self.colors == "Pastel":
            self.palette("#e8f1fb", "#f28b82", "#a8dab5", "#c5b3e6")
        else:
            self.palette("#f2f2f2", "#d62728", "#2ca02c", "#1f77b4")

    @action("Infect a random cell", group="Interventions")
    def infect_random(self):
        self.set(self.random_int(self.width), self.random_int(self.height), INFECTED)

    @action("Vaccinate the susceptible cells (share)", group="Interventions")
    def vaccinate(self):
        chosen = (self.cells == SUSCEPTIBLE) & (self.random_array() < self.vaccine_share)
        self.cells[chosen] = VACCINATED


simulation(Epidemic)
