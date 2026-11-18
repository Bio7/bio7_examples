"""A fox: an Animal which eats one rabbit of its cell."""

from animal import Animal


class Fox(Animal):

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
