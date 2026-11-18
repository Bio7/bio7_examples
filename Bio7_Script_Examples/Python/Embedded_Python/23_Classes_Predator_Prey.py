# An object-oriented Python program with several classes and one main class:
# a predator-prey model (rabbits, foxes and grass on a grid). The classes are
# shown in the Outline view; set breakpoints in the methods to follow the
# simulation in the debugger, or profile it (Profile in the context menu).
#
#   Animal      the base class: position, energy, age, moving, dying
#   Rabbit      an Animal which eats grass
#   Fox         an Animal which hunts rabbits
#   World       the grid with the grass and the animals (one time step)
#   Statistics  the counts of each step, a summary and the plot
#   Main        the main class: the parameters, the simulation, the results
#
# Requires: matplotlib for the plot (else only the text output)

import random


class Animal:
    """The base class of the animals: position, energy and age."""

    max_age = 50
    move_cost = 1.0
    birth_energy = 20.0
    birth_probability = 0.1

    def __init__(self, x, y, energy):
        self.x = x
        self.y = y
        self.energy = energy
        self.age = 0

    def move(self, world):
        """A random step to a neighbour cell (the world is a torus)."""
        dx, dy = random.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        self.x = (self.x + dx) % world.width
        self.y = (self.y + dy) % world.height
        self.energy -= self.move_cost
        self.age += 1

    def is_alive(self):
        return self.energy > 0 and self.age < self.max_age

    def offspring(self):
        """A new animal of the same class (half of the energy), or None."""
        if self.energy > self.birth_energy and random.random() < self.birth_probability:
            self.energy /= 2
            return type(self)(self.x, self.y, self.energy)
        return None

    def eat(self, world):
        """Overridden by the subclasses."""

    def __repr__(self):
        return "%s(x=%d, y=%d, energy=%.1f, age=%d)" % (type(self).__name__, self.x, self.y, self.energy, self.age)


class Rabbit(Animal):
    """A rabbit eats the grass of its cell."""

    max_age = 30
    birth_probability = 0.2

    def eat(self, world):
        self.energy += world.graze(self.x, self.y)


class Fox(Animal):
    """A fox eats one rabbit of its cell."""

    max_age = 60
    move_cost = 1.0
    birth_energy = 30.0
    birth_probability = 0.08
    rabbit_energy = 20.0

    def eat(self, world):
        rabbit = world.rabbit_at(self.x, self.y)
        if rabbit is not None:
            world.remove(rabbit)
            self.energy += self.rabbit_energy


class World:
    """The grid: the grass of each cell and the animals."""

    def __init__(self, width, height, grass_growth=0.3, grass_max=5.0):
        self.width = width
        self.height = height
        self.grass_growth = grass_growth
        self.grass_max = grass_max
        self.grass = [[grass_max] * width for _ in range(height)]
        self.animals = []

    def populate(self, rabbits, foxes):
        for _ in range(rabbits):
            self.animals.append(Rabbit(random.randrange(self.width), random.randrange(self.height), 10.0))
        for _ in range(foxes):
            self.animals.append(Fox(random.randrange(self.width), random.randrange(self.height), 20.0))

    def graze(self, x, y):
        """The grass of a cell is eaten (its energy for the rabbit)."""
        food = min(self.grass[y][x], 4.0)
        self.grass[y][x] -= food
        return food

    def rabbit_at(self, x, y):
        for a in self.animals:
            if isinstance(a, Rabbit) and a.x == x and a.y == y:
                return a
        return None

    def remove(self, animal):
        animal.energy = 0

    def step(self):
        """One time step: the grass grows, the animals move, eat and reproduce."""
        for row in self.grass:
            for i, g in enumerate(row):
                row[i] = min(self.grass_max, g + self.grass_growth)
        random.shuffle(self.animals)
        born = []
        for a in self.animals:
            if not a.is_alive():
                continue
            a.move(self)
            a.eat(self)
            child = a.offspring()
            if child is not None:
                born.append(child)
        self.animals = [a for a in self.animals if a.is_alive()] + born

    def count(self, cls):
        return sum(1 for a in self.animals if isinstance(a, cls))

    def total_grass(self):
        return sum(sum(row) for row in self.grass)


class Statistics:
    """The counts of each time step."""

    def __init__(self):
        self.rabbits = []
        self.foxes = []
        self.grass = []

    def record(self, world):
        self.rabbits.append(world.count(Rabbit))
        self.foxes.append(world.count(Fox))
        self.grass.append(world.total_grass())

    def summary(self):
        return ("Steps: %d\nRabbits: max %d, last %d\nFoxes:   max %d, last %d"
                % (len(self.rabbits) - 1, max(self.rabbits), self.rabbits[-1], max(self.foxes), self.foxes[-1]))

    def plot(self):
        """The populations over the time (in the Python Plot view of Bio7)."""
        try:
            import matplotlib.pyplot as plt
        except ImportError:
            print("matplotlib is not installed: no plot")
            return
        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8, 6), sharex=True)
        ax1.plot(self.rabbits, label="Rabbits", color="tab:green")
        ax1.plot(self.foxes, label="Foxes", color="tab:red")
        ax1.set_ylabel("Animals")
        ax1.set_title("Predator-prey model")
        ax1.legend()
        ax2.plot(self.grass, color="tab:olive")
        ax2.set_ylabel("Grass")
        ax2.set_xlabel("Time step")
        fig.tight_layout()
        plt.show()


class Main:
    """The main class: the parameters, the simulation and the results."""

    def __init__(self, width=30, height=30, rabbits=200, foxes=20, steps=300, seed=1):
        random.seed(seed)
        self.steps = steps
        self.world = World(width, height)
        self.world.populate(rabbits, foxes)
        self.statistics = Statistics()

    def run(self):
        self.statistics.record(self.world)
        for step in range(1, self.steps + 1):
            self.world.step()
            self.statistics.record(self.world)
            if self.world.count(Rabbit) == 0 and self.world.count(Fox) == 0:
                print("All animals died at step", step)
                break
        print(self.statistics.summary())
        self.statistics.plot()


if __name__ == "__main__":
    Main().run()
