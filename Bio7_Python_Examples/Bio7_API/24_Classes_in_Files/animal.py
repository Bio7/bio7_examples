"""The base class of the animals: position, energy and age."""

import random


class Animal:

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
