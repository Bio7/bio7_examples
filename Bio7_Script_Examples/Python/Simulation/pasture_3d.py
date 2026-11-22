"""Sheep and wolves on a pasture in 3D with animated glTF models.

The sheep graze (the animation "graze"), walk and run away from the wolves ("run"); the wolves walk
or hunt the nearest sheep outside the fenced pasture. A caught sheep returns into the fence. The
models are in data/models of this project (load_model; agent.model, agent.animation, agent.size),
the heading is the direction of the movement. The drawing: the fence on the floor (draw_polygon)
and the hunting lines in 3D (draw_line3). The page shows the models with WebGL.
"""
from bio7sim import AgentModel, Agent, param, action, simulation, heading_of, cos_deg, sin_deg

FENCE_X, FENCE_Y = [6, 26, 30, 8], [6, 4, 24, 28]


def inside(x, y):
    result, n = False, len(FENCE_X)
    for i in range(n):
        j = (i + n - 1) % n
        if (FENCE_Y[i] > y) != (FENCE_Y[j] > y) and x < (FENCE_X[j] - FENCE_X[i]) * (y - FENCE_Y[i]) / (FENCE_Y[j] - FENCE_Y[i]) + FENCE_X[i]:
            result = not result
    return result


class Animal(Agent):
    def __init__(self, x, y, wolf):
        super().__init__(x, y, 0, model="wolf" if wolf else "sheep", size=2.8 if wolf else 2.2, heading=0.0)
        self.wolf, self.rest, self.prey = wolf, 0, None


class Pasture3D(AgentModel):
    """Sheep and wolves on a pasture in 3D with animated glTF models: the sheep graze, walk and run away from the wolves, the wolves hunt the sheep outside the fence."""

    sheep = param(30, min=5, max=80, label="Sheep", restart=True)
    wolves = param(3, min=0, max=10, label="Wolves", restart=True)
    wolf_speed = param(0.42, min=0.1, max=1, step=0.05, label="Speed of the wolves")
    flight = param(9.0, min=2, max=20, step=0.5, label="Distance of the flight (sheep)")

    def init(self):
        self.box(60, 60, 12)
        self.load_model("sheep", "data/models/sheep.glb")
        self.load_model("wolf", "data/models/wolf.glb")
        self.catches = 0
        for i in range(self.sheep):
            # two thirds in the fence, a third on the open pasture
            a = Animal(self.random(32, 56), self.random(4, 56), False) if i % 3 == 0 else Animal(self.random(8, 26), self.random(8, 24), False)
            a.heading = self.random(0, 360)
            self.agents.append(a)
        for _ in range(self.wolves):
            w = Animal(self.random(40, 58), self.random(30, 58), True)
            w.heading = self.random(0, 360)
            self.agents.append(w)
        self.draw()

    def walk(self, a, speed):
        """Moves in the heading (0 the direction of y, clockwise)."""
        a.x += speed * sin_deg(a.heading)
        a.y += speed * cos_deg(a.heading)

    def step(self):
        running, distances, hunting = 0, 0.0, 0
        sheep = [a for a in self.agents if not a.wolf]
        wolves = [a for a in self.agents if a.wolf]
        for a in self.agents:
            if a.wolf:
                free = [s for s in sheep if not inside(s.x, s.y) and self.distance(a, s) < 25]
                a.prey = min(free, key=lambda s: self.distance(a, s), default=None)
                speed, a.animation = 0.15, "walk"
                if a.prey is not None:
                    a.heading = heading_of(a.prey.x - a.x, a.prey.y - a.y)
                    speed, a.animation = self.wolf_speed, "run"
                    d = self.distance(a, a.prey)
                    distances += d
                    hunting += 1
                    if d < 1.2:                                                   # a new sheep in the fence
                        a.prey.x, a.prey.y = self.random(10, 24), self.random(10, 22)
                        self.catches += 1
                else:
                    a.heading = (a.heading + self.random(-25, 25)) % 360
                ox, oy = a.x, a.y
                self.walk(a, speed)
                if inside(a.x, a.y):
                    a.x, a.y, a.heading = ox, oy, (a.heading + 180) % 360
            else:
                w = min(wolves, key=lambda w: self.distance(a, w), default=None)
                if w is not None and self.distance(a, w) < self.flight:
                    a.heading = heading_of(a.x - w.x, a.y - w.y)
                    a.animation, a.rest = "run", 0
                    self.walk(a, 0.4)
                    running += 1
                elif a.rest > 0:
                    a.animation = "graze"
                    a.rest -= 1
                else:
                    a.animation = "walk"
                    a.heading = (a.heading + self.random(-20, 20)) % 360
                    self.walk(a, 0.1)
                    if self.chance(0.08):
                        a.rest = self.random_int(5, 25)
            self.keep_in_box(a)
        self.draw()
        self.chart("Sheep running", running)
        self.chart("Catches", self.catches)
        self.chart("Mean distance wolf - prey", distances / hunting if hunting else 0, chart="Hunt")

    def draw(self):
        self.draw_polygon(FENCE_X, FENCE_Y, fill="#7cb34266", stroke="#f5f0e6")
        for a in self.agents:
            if a.wolf and a.prey is not None:
                self.draw_line3(a.x, a.y, 1, a.prey.x, a.prey.y, 1, "#d62728", 2)

    @action("Open the fence: all sheep out")
    def scatter(self):
        for a in self.agents:
            if not a.wolf:
                a.x, a.y = self.random(30, 58), self.random(2, 58)


simulation(Pasture3D)
