# The predator-prey model of 23_Classes_Predator_Prey.py with one class per
# file (modules of this folder). Run this file (main.py): Bio7 adds the folder
# of the script to the module path, so the other files can be imported.
#
#   animal.py                 Animal      the base class
#   rabbit.py                 Rabbit      an Animal which eats grass
#   fox.py                    Fox         an Animal which hunts rabbits
#   world.py                  World       the grid with the grass and the animals
#   population_statistics.py  Statistics  the counts, the summary and the plot
#   main.py                   Main        the main class (this file)
#
# Requires: matplotlib for the plot (else only the text output)

import importlib
import random
import sys

# The embedded Python keeps imported modules: the modules of this folder are
# imported again, so changes in their files are used at the next run
for name in ("animal", "rabbit", "fox", "world", "population_statistics"):
    sys.modules.pop(name, None)
importlib.invalidate_caches()

from fox import Fox  # noqa: E402
from population_statistics import Statistics  # noqa: E402
from rabbit import Rabbit  # noqa: E402
from world import World  # noqa: E402


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
