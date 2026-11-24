"""The counts of each time step, a summary and the plot.

(Not named statistics.py: that is a module of the standard library.)
"""

from fox import Fox
from rabbit import Rabbit


class Statistics:

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
        ax1.set_title("Predator-prey model (classes in files)")
        ax1.legend()
        ax2.plot(self.grass, color="tab:olive")
        ax2.set_ylabel("Grass")
        ax2.set_xlabel("Time step")
        fig.tight_layout()
        plt.show()
