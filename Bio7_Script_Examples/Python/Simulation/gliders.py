"""A flock of gliders in 3D (as the Java example modeling3d/swarm/BoidSphereModel).

Each boid is a glider of triangles (agent_shape = "glider"): it points in its direction of
flight, climbs, dives and banks into turns. The flock follows the rules of C. Reynolds within
the neighbourhood of a boid - separation (away from close boids), alignment (the mean
direction) and cohesion (to their centre); the boids turn back softly at the walls of the
flight space above the floor, fly around pillars (obstacles) and fly between half and the
full maximum speed. All boids decide first, then all move (one time step), computed for all
boids at once with numpy arrays.

The 3D view shows a dark floor with soft shadows (floor = "plane", shadows = True), the
custom camera follows the red boid (camera_follow, the global view beside it in the split
view). The charts: the polarization of the flock (0: disordered, 1: all fly in parallel)
and the mean speed. Run the script in Bio7, export it with "Export Simulation as HTML..."
(WebGL in the page: the gliders, the pillars, the floor and the shadows; the orbit view).
"""
import colorsys

import numpy as np
from bio7sim import AgentModel, Agent, param, action, simulation

# The flight space in the box (as BoidSphereModel: a floor of 2000 x 2000)
WIDTH, FLOOR_Z, TOP = 2000.0, 120.0, 720.0
RADIUS, CLOSE, AVOID = 150.0, 45.0, 110.0   # the neighbourhood, close boids, the distance to avoid a pillar
HALF = WIDTH / 2 * 0.9                      # x and y within the centre +- HALF


class Gliders(AgentModel):
    """A flock of gliders in 3D: separation, alignment and cohesion, pillars as obstacles, a camera which follows a boid."""

    boids = param(200, min=20, max=500, label="Number of boids", restart=True)
    max_speed = param(6.2, min=1, max=12, step=0.1, label="Maximum speed", group="Flight")
    max_force = param(0.05, min=0.005, max=0.2, step=0.005, label="Maximum force (turns)", group="Flight")
    separation = param(1.5, min=0, max=3, step=0.1, label="Separation", group="Rules")
    alignment = param(1.0, min=0, max=3, step=0.1, label="Alignment", group="Rules")
    cohesion = param(1.0, min=0, max=3, step=0.1, label="Cohesion", group="Rules")
    obstacles = param(True, label="Pillars", group="Obstacles", restart=True)
    pillars = param(8, min=1, max=30, label="Number of pillars", group="Obstacles", restart=True)
    placement = param("Random", options=["Random", "Fixed (centre)"], label="Placement", group="Obstacles", restart=True)
    size = param(5.0, min=2, max=15, step=0.5, label="Glider size", group="View")
    floor_style = param("plane", options=["plane", "grid", "none"], label="Floor", group="View")
    show_shadows = param(True, label="Shadows", group="View")
    follow = param(True, label="Camera follows the red boid", group="View")

    def init(self):
        self.box(WIDTH, WIDTH, TOP + 40)
        self.agent_shape = "glider"
        n = self.boids
        c = WIDTH / 2
        # in the flight space, flying in random directions at three quarters of the maximum speed
        self.p = np.column_stack([c + self.random_array((n, 2), -0.65, 0.65) * HALF,
                                  FLOOR_Z + (0.2 + 0.6 * self.random_array((n, 1))) * (TOP - FLOOR_Z)])
        angle, climb = self.random_array(n, 0, 2 * np.pi), self.random_array(n, -0.3, 0.3)
        self.v = 0.75 * self.max_speed * np.column_stack([np.cos(angle) * np.cos(climb), np.sin(angle) * np.cos(climb), np.sin(climb)])
        # each boid a shade of blue-cyan, the first one (the camera) red
        colors = ["#%02x%02x%02x" % tuple(int(255 * v) for v in colorsys.hsv_to_rgb(h, 0.75, 0.95)) for h in self.random_array(n, 0.5, 0.62)]
        colors[0] = "#f2261f"
        self.agents = [Agent(*xyz, color=col) for xyz, col in zip(self.p, colors)]
        self.columns = self.make_pillars() if self.obstacles else np.zeros((0, 3))
        self.view()

    def make_pillars(self):
        """The pillars (x, y, radius): random (a little apart) or fixed (the centre, then rings)."""
        n, c, o = int(self.pillars), WIDTH / 2, []
        for i in range(n):
            if self.placement.startswith("Random"):
                r = 30 + self.random() * 50
                for _ in range(30):
                    x, y = c + self.random(-0.8, 0.8) * HALF, c + self.random(-0.8, 0.8) * HALF
                    if all(np.hypot(x - q[0], y - q[1]) >= r + q[2] + 120 for q in o):
                        break
                o.append((x, y, r))
            elif i == 0:
                o.append((c, c, 45.0))
            else:
                ring, first = 1, 1
                while i >= first + 6 * ring:
                    first += 6 * ring
                    ring += 1
                rings, m = 0, 1
                while m < n:
                    rings += 1
                    m += 6 * rings
                dist = ring * min(220, HALF * 0.85 / max(1, rings))
                a = 2 * np.pi * (i - first) / (6 * ring) + ring * 0.3
                o.append((c + dist * np.cos(a), c + dist * np.sin(a), 45.0))
        return np.array(o, dtype=float).reshape(-1, 3)

    def step(self):
        p, v, n = self.p, self.v, len(self.p)
        ms = self.max_speed
        # the other boids within the neighbourhood: d[i, j] the vector from boid i to boid j
        d = p[None, :, :] - p[:, None, :]
        d2 = (d * d).sum(axis=2)
        d2[d2 == 0] = np.inf                        # itself (and a boid at the same place): not a neighbour
        near = d2 <= RADIUS * RADIUS
        count = near.sum(axis=1)
        has = (count > 0)[:, None]
        cnt = np.maximum(count, 1)[:, None]
        align = (near[:, :, None] * v[None, :, :]).sum(axis=1) / cnt - v
        cohes = (near[:, :, None] * d).sum(axis=1) / cnt
        # separation: away from close boids, the closer the stronger (1 / distance)
        sep = -((d2 < CLOSE * CLOSE)[:, :, None] * d / d2[:, :, None]).sum(axis=1)
        steer = has * (self.alignment * 0.06 * align + self.cohesion * 0.0004 * cohes + self.separation * 2.5 * sep)
        # the steering limited by the maximum force
        s = np.linalg.norm(steer, axis=1)[:, None]
        max_turn = self.max_force * 12
        steer = np.where(s > max_turn, steer * max_turn / np.maximum(s, 1e-12), steer)
        # the pillars: a boid looks ahead (15 steps), the closer its path to a pillar the stronger it steers away
        for ox, oy, r in self.columns:
            f = p[:, :2] + v[:, :2] * 15 - (ox, oy)
            now = p[:, :2] - (ox, oy)
            gap = np.minimum(np.linalg.norm(f, axis=1), np.linalg.norm(now, axis=1)) - r
            push = np.where(gap < AVOID, ms * 0.25 * (1 - np.maximum(gap, 0) / AVOID), 0.0)
            steer[:, :2] += f / np.maximum(np.linalg.norm(f, axis=1), 1e-9)[:, None] * push[:, None]
        # the walls: turn back softly near the edges of the flight space
        c, margin, wall = WIDTH / 2, min(120.0, HALF * 0.2), 0.1 * ms
        steer[:, 0] -= wall * (p[:, 0] > c + HALF - margin) - wall * (p[:, 0] < c - HALF + margin)
        steer[:, 1] -= wall * (p[:, 1] > c + HALF - margin) - wall * (p[:, 1] < c - HALF + margin)
        steer[:, 2] -= wall * (p[:, 2] > TOP - margin) - wall * (p[:, 2] < FLOOR_Z + margin)
        v = v + steer
        # the speed between a half and the whole maximum
        speed = np.linalg.norm(v, axis=1)[:, None]
        v = v * np.where(speed > ms, ms / speed, np.where(speed < ms / 2, ms / 2 / np.maximum(speed, 1e-9), 1.0))
        # all move; never inside a pillar (pushed out to its surface), not far outside the flight space
        p = p + v
        for ox, oy, r in self.columns:
            now = p[:, :2] - (ox, oy)
            dist = np.linalg.norm(now, axis=1)
            inside = dist < r + 2
            p[inside, :2] = (ox, oy) + now[inside] / np.maximum(dist[inside], 1e-9)[:, None] * (r + 2)
        out = min(200.0, HALF * 0.1)
        p[:, :2] = np.clip(p[:, :2], c - HALF - out, c + HALF + out)
        p[:, 2] = np.clip(p[:, 2], FLOOR_Z * 0.5, TOP + out)
        self.p, self.v = p, v
        for a, (x, y, z) in zip(self.agents, p):
            a.x, a.y, a.z = x, y, z
        # the charts: the order of the flock and the mean speed
        unit = v / np.maximum(np.linalg.norm(v, axis=1), 1e-9)[:, None]
        self.chart("Polarization", float(np.linalg.norm(unit.mean(axis=0))))
        self.chart("Mean speed", float(np.linalg.norm(v, axis=1).mean()), chart="Speed")
        self.view()

    def view(self):
        """The settings of the view (changed while the model runs) and the pillars (drawn in each step)."""
        self.agent_radius = self.size
        self.floor = self.floor_style
        self.shadows = bool(self.show_shadows)
        self.camera_follow(0 if self.follow else None)
        for x, y, r in self.columns:
            self.draw_pillar(x, y, r, TOP * 0.95)

    @action("Scatter the flock", group="Flight")
    def scatter(self):
        n = len(self.p)
        angle, climb = self.random_array(n, 0, 2 * np.pi), self.random_array(n, -0.6, 0.6)
        self.v = self.max_speed * np.column_stack([np.cos(angle) * np.cos(climb), np.sin(angle) * np.cos(climb), np.sin(climb)])

    @action("Place the pillars again", group="Obstacles")
    def new_pillars(self):
        self.columns = self.make_pillars() if self.obstacles else np.zeros((0, 3))
        self.view()


simulation(Gliders)
