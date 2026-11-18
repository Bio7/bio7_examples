"""The grid: the grass of each cell and the animals."""

import random

from fox import Fox
from rabbit import Rabbit


class World:

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
