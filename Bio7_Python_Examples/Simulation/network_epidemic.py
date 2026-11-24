"""An epidemic (SIR) on a network of contacts.

The people are agents; an infected person (red) infects each susceptible contact (blue) with
a probability per step and recovers (grey) with another probability. The contacts are the
links of a network (drawn as lines; a link which passed on the infection turns red):
- "Moving contacts": the people walk; in each step all pairs closer than the contact distance
  are linked (neighbor_pairs: a spatial index, fast for thousands of people),
- "Small world", "Scale-free" and "Random": a fixed network (link_small_world,
  link_scale_free, link_random), arranged with a spring layout (layout_spring).

Charts: the susceptible, infected and recovered people and the number of contacts. The
simulation stops when no one is infected (stop). The model records the share of the infected people (record) for experiments: the button "Experiment..."
of the settings (e.g. the infection 0.02:0.1:0.02 with 5 repetitions) or
network_epidemic_experiment.py.

A click on the floor of the 3D view (also in the page) infects the nearest person (click).
"""
from bio7sim import AgentModel, Agent, param, action, simulation

COLORS = {"S": "#2f6fed", "I": "#e53935", "R": "#9aa0a6"}


class NetworkEpidemic(AgentModel):
    """An epidemic (SIR) on a network of contacts: moving people (a spatial index) or a small-world, scale-free or random network."""

    people = param(400, min=50, max=3000, label="People", restart=True)
    network = param("Moving contacts", options=["Moving contacts", "Small world", "Scale-free", "Random"], label="Network", restart=True)
    contacts = param(6, min=2, max=20, label="Contacts of a person (fixed networks)", restart=True)
    distance = param(4.0, min=1, max=15, step=0.5, label="Contact distance (moving)", group="Moving")
    speed = param(0.8, min=0, max=3, step=0.1, label="Speed of the people", group="Moving")
    infection = param(0.05, min=0, max=0.5, step=0.005, label="Infection per contact and step", group="Disease")
    recovery = param(0.04, min=0, max=0.3, step=0.005, label="Recovery per step", group="Disease")
    initial = param(3, min=1, max=30, label="Infected at the start", group="Disease", restart=True)

    def init(self):
        self.box(100, 100, 4)
        self.floor = "grid"
        self.agent_radius = 0.9
        self.link_color = "#8a94a6"
        n = int(self.people)
        self.agents = [Agent(self.random(0, 100), self.random(0, 100), 2) for _ in range(n)]
        for a in self.agents:
            a.state, a.angle = "S", self.random(0, 360)
        if self.network == "Small world":
            self.link_small_world(self.contacts, 0.1)
        elif self.network == "Scale-free":
            self.link_scale_free(max(1, self.contacts // 2))
        elif self.network == "Random":
            self.link_random(self.contacts / max(1, n - 1))
        if self.network != "Moving contacts":
            self.layout_circle()
            self.layout_spring(80)
        for k in range(min(n, int(self.initial))):
            self.agents[self.random_int(n)].state = "I"
        self.peak = 0
        self.show()

    def step(self):
        ag = self.agents
        if self.network == "Moving contacts":
            # the people walk (a torus), the contacts of this step: all pairs within the distance
            for a in ag:
                a.angle += self.random(-30, 30)
                a.x += self.speed * self.cos_deg(a.angle)
                a.y += self.speed * self.sin_deg(a.angle)
                self.wrap_in_box(a)
            self.clear_links()
            i, j = self.neighbor_pairs(self.distance)
            for a, b in zip(i, j):
                self.link(ag[a], ag[b])
        # the infections along the links (all decide first, then all change)
        new = []
        for a in ag:
            if a.state == "I":
                for o in self.link_neighbors(a):
                    if o.state == "S" and self.chance(self.infection):
                        new.append(o)
                        self.set_link_color(a, o, "#e53935")
        recovered = [a for a in ag if a.state == "I" and self.chance(self.recovery)]
        for a in new:
            a.state = "I"
        for a in recovered:
            a.state = "R"
        self.show()
        # the epidemic is over: the simulation stops (an experiment ends the run)
        if not any(a.state == "I" for a in ag):
            self.stop()

    def show(self):
        n = len(self.agents)
        counts = {"S": 0, "I": 0, "R": 0}
        for a in self.agents:
            a.color = COLORS[a.state]
            counts[a.state] += 1
        self.peak = max(self.peak, counts["I"])
        self.chart("Susceptible", counts["S"])
        self.chart("Infected", counts["I"])
        self.chart("Recovered", counts["R"])
        self.chart("Contacts", len(self.links()), chart="Network")
        # for experiments: the share of the people who were infected and the peak
        self.record("Infected share", (counts["I"] + counts["R"]) / max(1, n))
        self.record("Peak share", self.peak / max(1, n))

    @action("Infect 5 people")
    def infect(self):
        for _ in range(5):
            self.agents[self.random_int(len(self.agents))].state = "I"
        self.show()

    def click(self, x, y):
        """A click on the floor of the view: the nearest person (within 8) is infected."""
        near = min(self.agents, key=lambda a: (a.x - x) ** 2 + (a.y - y) ** 2, default=None)
        if near is not None and (near.x - x) ** 2 + (near.y - y) ** 2 < 64:
            near.state = "I"
            self.show()


simulation(NetworkEpidemic)
