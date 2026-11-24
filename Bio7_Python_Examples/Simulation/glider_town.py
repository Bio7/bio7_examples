"""A flock of gliders flies through a small town (the gliders of gliders.py with a 3D scene).

The town is built of 3D models and 3D shapes:
- houses and trees (OBJ files with their materials, the roofs with a texture) and the church
  tower (an STL file) are objects of the environment (load_model, place_model),
- the office towers are boxes (draw_box), the streets and the square flat polygons
  (draw_polygon3) with dashed centre lines (draw_path3),
- a hot air balloon drifts over the town (draw_sphere, its basket a box, the ropes lines).

The flock flies along a route: the waypoints are markers (draw_point3) joined by a line
(draw_path3), a ring at the height of the next waypoint (draw_circle with z) shows where the
flock goes, the labels stand above the buildings (draw_text with z). The boids keep the rules
of C. Reynolds (separation, alignment, cohesion), steer to the next waypoint and fly around the
buildings and the trees (only below their roofs: above them the way is free).

The custom camera follows the red boid (in Bio7, the global view beside it). The charts: the
order of the flock (polarization), the mean speed and the height of the flock. Export the
model with "Export Simulation as HTML..." (the OBJ and STL models are converted to glTF).
"""
import colorsys

import numpy as np
from bio7sim import AgentModel, Agent, param, action, simulation

WIDTH, TOP = 2000.0, 760.0          # the box: 2000 x 2000, the height
C = WIDTH / 2                       # the centre of the town
FLOOR_Z = 70.0                      # the lowest height of the flight
RADIUS, CLOSE, AVOID = 150.0, 45.0, 90.0
STREET = 70.0                       # the width of the streets

# The route of the flock (x, y, z): along the main street, around the church, past the offices and the balloon
ROUTE = np.array([(320, 1000, 170), (780, 1000, 150), (1000, 680, 300), (1560, 700, 260), (1680, 1160, 430),
                  (1420, 1640, 620), (900, 1560, 400), (430, 1430, 300), (330, 720, 230)], dtype=float)

# The office towers (draw_box): the corner x, y, the width, the depth and the height
OFFICES = [(1260, 1250, 130, 130, 460), (1450, 1270, 110, 110, 380), (1290, 1440, 120, 120, 540)]


class GliderTown(AgentModel):
    """A flock of gliders flies along a route through a small town: houses, trees and a church tower (OBJ and STL), office towers, streets and a balloon (3D shapes)."""

    boids = param(150, min=20, max=400, label="Number of boids", restart=True)
    max_speed = param(6.5, min=1, max=12, step=0.1, label="Maximum speed", group="Flight")
    max_force = param(0.05, min=0.005, max=0.2, step=0.005, label="Maximum force (turns)", group="Flight")
    route = param(1.0, min=0, max=3, step=0.1, label="Follow the route", group="Flight")
    separation = param(1.5, min=0, max=3, step=0.1, label="Separation", group="Rules")
    alignment = param(1.0, min=0, max=3, step=0.1, label="Alignment", group="Rules")
    cohesion = param(1.0, min=0, max=3, step=0.1, label="Cohesion", group="Rules")
    trees = param(40, min=0, max=120, label="Number of trees", group="Town", restart=True)
    show_route = param(True, label="Show the route", group="View")
    labels = param(True, label="Labels", group="View")
    size = param(6.0, min=2, max=15, step=0.5, label="Glider size", group="View")
    follow = param(True, label="Camera follows the red boid", group="View")

    def init(self):
        self.box(WIDTH, WIDTH, TOP)
        self.agent_shape = "glider"
        self.floor = "plane"
        self.shadows = True
        self.load_model("house", "data/models/house.obj")
        self.load_model("tree", "data/models/tree.obj")
        self.load_model("tower", "data/models/tower.stl")
        self.build_town()
        n = self.boids
        # the flock starts at the west end of the main street, flying east
        self.p = np.column_stack([180 + self.random_array(n, 0, 160), C + self.random_array(n, -150, 150),
                                  200 + self.random_array(n, 0, 160)])
        angle, climb = self.random_array(n, -0.4, 0.4), self.random_array(n, -0.1, 0.1)
        self.v = 0.75 * self.max_speed * np.column_stack([np.cos(angle) * np.cos(climb), np.sin(angle) * np.cos(climb), np.sin(climb)])
        colors = ["#%02x%02x%02x" % tuple(int(255 * v) for v in colorsys.hsv_to_rgb(h, 0.75, 0.95)) for h in self.random_array(n, 0.5, 0.62)]
        colors[0] = "#f2261f"
        self.agents = [Agent(*xyz, color=col) for xyz, col in zip(self.p, colors)]
        self.target, self.reached = 0, 0
        self.view()

    def build_town(self):
        """The objects of the environment (they stay) and the obstacles of the flock (x, y, radius, height)."""
        obstacles = []
        # the church tower in the middle of the square
        self.place_model("tower", C, C, size=420)
        obstacles.append((C, C, 60, 420))
        # the houses along the main street (west - east) and the cross street (south - north), the front to the street
        for x in np.arange(300, 1750, 150):
            if abs(x - C) < 240:
                continue
            for side, heading in ((-1, 0), (1, 180)):
                size = 100 + 40 * self.random()
                self.place_model("house", x, C + side * 125, heading=heading, size=size)
                obstacles.append((x, C + side * 125, 85, size))
        for y in np.arange(300, 1750, 150):
            if abs(y - C) < 240:
                continue
            for side, heading in ((-1, 90), (1, 270)):
                if side > 0 and y > C:
                    continue                      # the office quarter
                size = 100 + 40 * self.random()
                self.place_model("house", C + side * 125, y, heading=heading, size=size)
                obstacles.append((C + side * 125, y, 85, size))
        for x, y, w, d, h in OFFICES:
            obstacles.append((x + w / 2, y + d / 2, max(w, d) * 0.72, h))
        # the trees in the gardens (away from the streets, the houses and the offices)
        planted = 0
        for _ in range(int(self.trees) * 20):
            if planted >= self.trees:
                break
            x, y = self.random(150, WIDTH - 150), self.random(150, WIDTH - 150)
            if abs(x - C) < 260 or abs(y - C) < 260 or (x > 1180 and y > 1180):
                continue
            if any(np.hypot(x - o[0], y - o[1]) < o[2] + 50 for o in obstacles):
                continue
            size = 80 + 50 * self.random()
            self.place_model("tree", x, y, heading=self.random(0, 360), size=size)
            obstacles.append((x, y, 35, size))
            planted += 1
        self.obstacles = np.array(obstacles, dtype=float)

    def step(self):
        p, v = self.p, self.v
        ms = self.max_speed
        d = p[None, :, :] - p[:, None, :]
        d2 = (d * d).sum(axis=2)
        d2[d2 == 0] = np.inf
        near = d2 <= RADIUS * RADIUS
        count = near.sum(axis=1)
        has = (count > 0)[:, None]
        cnt = np.maximum(count, 1)[:, None]
        align = (near[:, :, None] * v[None, :, :]).sum(axis=1) / cnt - v
        cohes = (near[:, :, None] * d).sum(axis=1) / cnt
        sep = -((d2 < CLOSE * CLOSE)[:, :, None] * d / d2[:, :, None]).sum(axis=1)
        steer = has * (self.alignment * 0.06 * align + self.cohesion * 0.0004 * cohes + self.separation * 2.5 * sep)
        # the route: to the next waypoint (the next one when the centre of the flock is near)
        goal = ROUTE[self.target]
        to_goal = goal - p
        steer += self.route * 0.08 * to_goal / np.maximum(np.linalg.norm(to_goal, axis=1), 1e-9)[:, None]
        if np.linalg.norm(p.mean(axis=0) - goal) < 220:
            self.target = (self.target + 1) % len(ROUTE)
            self.reached += 1
        s = np.linalg.norm(steer, axis=1)[:, None]
        max_turn = self.max_force * 12
        steer = np.where(s > max_turn, steer * max_turn / np.maximum(s, 1e-12), steer)
        # the buildings and the trees: a boid below the roof looks ahead and steers away (all obstacles at once)
        o = self.obstacles
        ahead = p[:, None, :2] + v[:, None, :2] * 15 - o[None, :, :2]          # (boids, obstacles, 2)
        now = p[:, None, :2] - o[None, :, :2]
        gap = np.minimum(np.linalg.norm(ahead, axis=2), np.linalg.norm(now, axis=2)) - o[None, :, 2]
        below = p[:, None, 2] < o[None, :, 3] + 30
        push = np.where(below & (gap < AVOID), ms * 0.25 * (1 - np.maximum(gap, 0) / AVOID), 0.0)
        steer[:, :2] += (ahead / np.maximum(np.linalg.norm(ahead, axis=2), 1e-9)[:, :, None] * push[:, :, None]).sum(axis=1)
        # up from a roof just below
        steer[:, 2] += (below & (gap < 0)).any(axis=1) * 0.05 * ms
        # the walls and the floor
        margin, wall = 120.0, 0.1 * ms
        steer[:, 0] -= wall * (p[:, 0] > WIDTH - margin) - wall * (p[:, 0] < margin)
        steer[:, 1] -= wall * (p[:, 1] > WIDTH - margin) - wall * (p[:, 1] < margin)
        steer[:, 2] -= wall * (p[:, 2] > TOP - margin) - wall * (p[:, 2] < FLOOR_Z + 40)
        v = v + steer
        speed = np.linalg.norm(v, axis=1)[:, None]
        v = v * np.where(speed > ms, ms / speed, np.where(speed < ms / 2, ms / 2 / np.maximum(speed, 1e-9), 1.0))
        p = p + v
        # never inside a building or a tree (pushed out to its side)
        now = p[:, None, :2] - o[None, :, :2]
        dist = np.linalg.norm(now, axis=2)
        inside = (dist < o[None, :, 2] + 2) & (p[:, None, 2] < o[None, :, 3])
        for i, k in zip(*np.nonzero(inside)):
            p[i, :2] = o[k, :2] + now[i, k] / max(dist[i, k], 1e-9) * (o[k, 2] + 2)
        p[:, :2] = np.clip(p[:, :2], 0, WIDTH)
        p[:, 2] = np.clip(p[:, 2], FLOOR_Z * 0.5, TOP)
        self.p, self.v = p, v
        for a, (x, y, z) in zip(self.agents, p):
            a.x, a.y, a.z = x, y, z
        unit = v / np.maximum(np.linalg.norm(v, axis=1), 1e-9)[:, None]
        self.chart("Polarization", float(np.linalg.norm(unit.mean(axis=0))))
        self.chart("Mean speed", float(np.linalg.norm(v, axis=1).mean()), chart="Speed")
        self.chart("Mean height", float(p[:, 2].mean()), chart="Height of the flock")
        self.view()

    def view(self):
        """The settings of the view and the shapes of the town (drawn again in each step)."""
        self.agent_radius = self.size
        self.camera_follow(0 if self.follow else None)
        # the streets and the square (flat polygons just above the floor), the dashed centre lines
        lo, hi, w = 120, WIDTH - 120, STREET / 2
        self.draw_polygon3([lo, hi, hi, lo], [C - w, C - w, C + w, C + w], [1, 1, 1, 1], "#6b7078")
        self.draw_polygon3([C - w, C + w, C + w, C - w], [lo, lo, hi, hi], [1.2, 1.2, 1.2, 1.2], "#6b7078")
        square = np.linspace(0, 2 * np.pi, 9)[:-1] + np.pi / 8
        self.draw_polygon3(C + 190 * np.cos(square), C + 190 * np.sin(square), np.full(8, 1.5), "#c8bea6", "#8d846f")
        for x0 in np.arange(lo, hi, 80):
            if abs(x0 - C) > 200:
                self.draw_path3([x0, x0 + 40], [C, C], [2, 2], "#f2f2f2", 2)
                self.draw_path3([C, C], [x0, x0 + 40], [2, 2], "#f2f2f2", 2)
        # the office towers
        for x, y, w_, d_, h in OFFICES:
            self.draw_box(x, y, 0, w_, d_, h, "#8aa1bd", "#3d4a5c")
            self.draw_box(x - 6, y - 6, h, w_ + 12, d_ + 12, 12, "#56667c")
        # the balloon: it drifts over the west of the town
        bx, by, bz = 560 + 160 * np.sin(self.t / 300), 1480 + 60 * np.cos(self.t / 210), 470 + 25 * np.sin(self.t / 40)
        self.draw_sphere(bx, by, bz, 55, "#e0533c")
        self.draw_box(bx - 14, by - 14, bz - 105, 28, 28, 22, "#8b5a2b")
        for dx, dy in ((-12, -12), (12, -12), (12, 12), (-12, 12)):
            self.draw_line3(bx + dx, by + dy, bz - 83, bx + dx * 2.6, by + dy * 2.6, bz - 40, "#5d4037", 1)
        # the route: the markers, the line between them, a ring at the next waypoint
        if self.show_route:
            loop = np.vstack([ROUTE, ROUTE[:1]])
            self.draw_path3(loop[:, 0], loop[:, 1], loop[:, 2], "#ffb300aa", 2)
            for k, (x, y, z) in enumerate(ROUTE):
                self.draw_point3(x, y, z, 26 if k == self.target else 16, "#43a047" if k == self.target else "#fb8c00")
            x, y, z = ROUTE[self.target]
            self.draw_circle(x, y, 70, None, "#43a047", z=z)
        if self.labels:
            self.draw_text(C, C, "Church", "#3e2723", 1, z=450)
            self.draw_text(1320, 1500, "Offices", "#263238", 1, z=580)
            self.draw_text(bx, by, "Balloon", "#b71c1c", 1, z=bz + 75)
            if self.show_route:
                x, y, z = ROUTE[self.target]
                self.draw_text(x, y, "Waypoint %d" % (self.target + 1), "#1b5e20", 1, z=z + 45)

    @action("Scatter the flock", group="Flight")
    def scatter(self):
        n = len(self.p)
        angle, climb = self.random_array(n, 0, 2 * np.pi), self.random_array(n, -0.6, 0.6)
        self.v = self.max_speed * np.column_stack([np.cos(angle) * np.cos(climb), np.sin(angle) * np.cos(climb), np.sin(climb)])


simulation(GliderTown)
