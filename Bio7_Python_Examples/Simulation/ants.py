"""Ants collect food (agents on a grid).

The grid is the pheromone (a value which diffuses and evaporates), the ants are agents on it
(GridAgent, drawn as arrows in their heading). A searching ant (red) follows the pheromone ahead
of it and wiggles; when it finds a pile of food it takes a piece and turns home (green). On the
way to the nest it lays pheromone, so the trails to the piles grow while the food lasts. The
simulation stops when all food is in the nest (stop()).

The agents: add_agent, forward (in the heading, a torus), face (to the nest), cell_x/cell_y (the
cell under the ant). Charts: the food in the nest, the food of the piles and the ants which carry
food.
"""
import math

from bio7sim import ValueGridModel, GridAgent, param, action, simulation, cos_deg, sin_deg

SIZE = 80
NEST_X, NEST_Y, NEST_R, PILE_R, FOOD = 40.0, 40.0, 4.0, 5.0, 200.0
PILE_X, PILE_Y = [14.0, 66.0, 58.0], [16.0, 22.0, 68.0]


class Ant(GridAgent):
    """An ant: carries food or searches."""

    def __init__(self, x, y, heading):
        super().__init__(x, y, heading, color="#e53935", size=1.8, shape="arrow")
        self.carrying = False


class Ants(ValueGridModel):
    """Ants collect food: agents on a grid of pheromone which diffuses and evaporates."""

    ants = param(150, min=10, max=400, label="Ants", restart=True)
    evaporation = param(0.02, min=0, max=0.2, step=0.005, label="Evaporation of the pheromone", group="Pheromone")
    diffusion = param(0.15, min=0, max=0.5, step=0.01, label="Diffusion of the pheromone", group="Pheromone")
    wiggle = param(35.0, min=0, max=90, label="Wiggle (degrees)", group="Ants")

    def init(self):
        self.size(SIZE, SIZE)
        self.range(0, 30)
        self.color_map("#1b1b1b", "#3b528b", "#21908c", "#5dc963", "#fde725")
        self.food = [FOOD, FOOD, FOOD]
        self.delivered = 0
        for _ in range(int(self.ants)):
            self.add_agent(Ant(NEST_X, NEST_Y, self.random(0, 360)))
        self.show()

    def step(self):
        # the pheromone diffuses and evaporates (all cells at once)
        self.values = (self.values + self.diffusion * self.laplacian() / 4) * (1 - self.evaporation)
        for a in self.agents:
            if a.carrying:
                self.home(a)
            else:
                self.search(a)
        self.show()

    def search(self, a):
        """Takes food at a pile, else follows the pheromone ahead (left, ahead, right) and wiggles."""
        k = self.pile_at(a.x, a.y)
        if k >= 0:
            self.food[k] -= 1
            a.carrying, a.color = True, "#43a047"
            a.heading = (a.heading + 180) % 360
            return
        best, turn = -1.0, 0
        for t in (-45, 0, 45):
            v = self.scent(a, a.heading + t)
            if v > best:
                best, turn = v, t
        if best > 0.05:
            a.heading += turn
        a.heading += self.random(-self.wiggle, self.wiggle)
        self.forward(a, 1)

    def scent(self, a, h):
        """The pheromone two cells ahead in the direction h."""
        return self.get(int(math.floor(a.x + 2 * sin_deg(h))), int(math.floor(a.y - 2 * cos_deg(h))))

    def home(self, a):
        """An ant with food lays pheromone on its way to the nest."""
        if self.distance(a.x, a.y, NEST_X, NEST_Y) < NEST_R:
            a.carrying, a.color = False, "#e53935"
            self.delivered += 1
            a.heading = (a.heading + 180) % 360
            return
        x, y = a.cell_x(), a.cell_y()
        self.values[y, x] = min(100.0, self.values[y, x] + 25)
        self.face(a, NEST_X, NEST_Y)
        a.heading += self.random(-self.wiggle / 3, self.wiggle / 3)
        self.forward(a, 1)

    def pile_at(self, x, y):
        for k in range(3):
            if self.food[k] > 0 and self.distance(x, y, PILE_X[k], PILE_Y[k]) < PILE_R:
                return k
        return -1

    def show(self):
        """The nest, the piles (smaller with less food) and the charts; the end when all food is in the nest."""
        self.draw_circle(NEST_X, NEST_Y, NEST_R, "#8d6e63cc", "#4e342e")
        carrying = sum(1 for a in self.agents if a.carrying)
        for k in range(3):
            if self.food[k] > 0:
                self.draw_circle(PILE_X[k], PILE_Y[k], PILE_R * math.sqrt(self.food[k] / FOOD), "#ffca28cc", "#ff8f00")
            self.chart("Pile %d" % (k + 1), self.food[k], chart="Food of the piles")
        self.chart("Food in the nest", self.delivered)
        self.chart("Ants carrying food", carrying)
        if sum(self.food) == 0 and carrying == 0:
            self.stop()

    @action("New food")
    def new_food(self):
        self.food = [FOOD, FOOD, FOOD]
        self.show()


simulation(Ants)
