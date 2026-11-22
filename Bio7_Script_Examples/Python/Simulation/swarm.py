"""A swarm of boids in 3D (separation, alignment and cohesion of C. Reynolds).

The agents are Agent objects (x, y, z, color); the movement is computed for all boids at
once with numpy arrays. Run the script in Bio7 (the 3D view with Options3D and the settings
on the left), export it with "Export Simulation as HTML..." (WebGL in the page).
"""
import numpy as np
from bio7sim import AgentModel, Agent, param, action, simulation


class Swarm(AgentModel):
    """A swarm of boids in 3D: each boid keeps a distance to its neighbors (separation), flies in their direction (alignment) and to their center (cohesion)."""

    boids = param(150, min=10, max=400, label="Number of boids", restart=True)
    view = param(12.0, min=2, max=30, step=0.5, label="View radius", group="Rules")
    separation = param(0.04, min=0, max=0.2, step=0.005, label="Separation", group="Rules")
    alignment = param(0.06, min=0, max=0.2, step=0.005, label="Alignment", group="Rules")
    cohesion = param(0.006, min=0, max=0.05, step=0.001, label="Cohesion", group="Rules")
    max_speed = param(1.5, min=0.2, max=4, step=0.1, label="Maximum speed", group="Rules")

    def init(self):
        self.box(100, 100, 60)                       # the box: x, y and z (the height)
        self.agent_radius = 1.2
        n = self.boids
        size = np.array([self.box_width, self.box_height, self.box_depth])
        self.agents = [Agent(*p) for p in self.random_array((n, 3)) * size]
        self.agents[0].color = "#d62728"             # an own color (None: agent_color)
        self.velocity = self.random_array((n, 3), -1, 1)

    def step(self):
        p, v = self.positions(), self.velocity       # arrays (n, 3)
        d = p[None, :, :] - p[:, None, :]            # the vectors to the other boids
        dist = np.sqrt((d * d).sum(axis=2))
        np.fill_diagonal(dist, np.inf)
        near = dist < self.view
        count = np.maximum(near.sum(axis=1), 1)[:, None]
        center = (near[:, :, None] * p[None, :, :]).sum(axis=1) / count
        heading = (near[:, :, None] * v[None, :, :]).sum(axis=1) / count
        crowd = -((dist < self.view / 3)[:, :, None] * d).sum(axis=1)
        has = near.any(axis=1)[:, None]
        v = v + has * (self.cohesion * (center - p) + self.alignment * (heading - v)) + self.separation * crowd
        speed = np.sqrt((v * v).sum(axis=1))[:, None]
        v = np.where(speed > self.max_speed, v / np.maximum(speed, 1e-12) * self.max_speed, v)
        p = p + v
        # bounce at the floor and the ceiling
        v[:, 2] = np.where((p[:, 2] < 0) | (p[:, 2] > self.box_depth), -v[:, 2], v[:, 2])
        self.velocity = v
        for a, (x, y, z) in zip(self.agents, p):
            a.x, a.y, a.z = x, y, z
            self.wrap_in_box(a)                      # x and y wrap around, z stays in the box
        self.chart("Mean speed", float(np.sqrt((v * v).sum(axis=1)).mean()))

    @action("Scatter the swarm")
    def scatter(self):
        self.velocity = self.random_array((len(self.agents), 3), -3, 3)


simulation(Swarm)
