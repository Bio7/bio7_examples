"""A rabbit: an Animal which eats the grass of its cell."""

from animal import Animal


class Rabbit(Animal):

    max_age = 30
    birth_probability = 0.2

    def eat(self, world):
        self.energy += world.graze(self.x, self.y)
