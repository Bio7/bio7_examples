"""The mathematical functions of the simulation API in Python (the module bio7sim, numpy and math).

The grid shows a moving surface of the chosen function (noise, fractal_noise, sin_deg/cos_deg,
normal_pdf, distance). The charts show the statistics of the grid (mean, sd, median, quantile),
a histogram, a predator-prey system (rk4) and its phase plane, and the logistic growth (rk4
compared with the exact solution). init() and the buttons print the other functions to the console.

The functions of bio7sim have the same names as in Java (in snake_case) and work also with numpy
arrays (clamp, lerp, map_range, noise, fractal_noise, normal_pdf, normal_cdf). For the other
mathematics use numpy (np.median, np.quantile, np.linalg, ...) and the module math.

Run the script in Bio7, start it with Play/Pause and export it with "Export Simulation as HTML..."
(the page runs the script with Pyodide: Python and numpy in the browser).
"""
import math

import numpy as np

from bio7sim import (ValueGridModel, param, action, simulation, clamp, lerp, map_range, wrap_around, sq, distance,
                     distance3, logistic, logistic_growth, mean, variance, sd, rk4, noise, fractal_noise, cos_deg,
                     sin_deg, heading_of, linear_regression, normal_pdf, normal_cdf)

SIZE = 100
CAPACITY = 100.0
START = 2.0


def r4(v):
    """The value (or array) with 4 decimal places (for the console)."""
    return (np.round(v, 4) + 0.0).tolist() if isinstance(v, np.ndarray) else round(float(v), 4) + 0.0


class MathFunctions(ValueGridModel):
    """The mathematical functions: basic functions, statistics, distributions, linear algebra, differential equations (rk4), angles and noise."""

    surface = param("Fractal noise", options=["Fractal noise", "Noise", "Waves (sin_deg, cos_deg)", "Normal density", "Distance"],
                    label="Function of the grid", group="Grid")
    scale = param(0.06, min=0.02, max=0.3, step=0.01, label="Scale", group="Grid")
    octaves = param(4, min=1, max=8, label="Octaves (fractal noise)", group="Grid")
    speed = param(0.3, min=0, max=2, step=0.05, label="Speed", group="Grid")
    prey_growth = param(0.6, min=0.1, max=1.5, step=0.05, label="Growth rate of the prey", group="Differential equations")
    growth_rate = param(0.3, min=0.05, max=1, step=0.05, label="Rate of the logistic growth", group="Differential equations")

    def init(self):
        self.size(SIZE, SIZE)
        self.range(0, 1)
        self.color_map(*self.VIRIDIS)
        # the coordinates of all cells (arrays of the shape of the grid: values[y, x])
        self.ys, self.xs = np.mgrid[0:SIZE, 0:SIZE].astype(float)
        # the state of the predator-prey system and of the logistic growth (rk4)
        self.predator_prey = np.array([10.0, 5.0])
        self.population = np.array([START])
        self.fill()
        self.print_basics()
        self.print_python_math()
        self.print_statistics()
        self.print_linear_algebra()

    def step(self):
        self.fill()
        v = self.values
        # statistics of the grid: bio7sim (mean, sd) and numpy (median, quantile)
        self.chart("Mean", mean(v))
        self.chart("Standard deviation", sd(v))
        self.chart("Median", float(np.median(v)))
        q10, q90 = np.quantile(v, [0.1, 0.9])
        self.chart("10 % quantile", float(q10))
        self.chart("90 % quantile", float(q90))
        # a histogram of the grid (a bar chart, replaced in each step)
        counts = np.bincount(np.clip(np.floor(v * 10).astype(int), 0, 9).ravel(), minlength=10)
        self.chart_bars("Histogram of the grid", ["0.%d" % k for k in range(10)], counts.tolist())
        # rk4: one Runge-Kutta step of a system of differential equations (Lotka-Volterra), 10 steps per time step
        g = self.prey_growth
        for k in range(10):
            self.predator_prey = rk4(lambda time, y: [g * y[0] - 0.05 * y[0] * y[1], 0.02 * y[0] * y[1] - 0.4 * y[1]],
                                     self.t + k * 0.1, self.predator_prey, 0.1)
        prey, predators = float(self.predator_prey[0]), float(self.predator_prey[1])
        self.chart("Prey", prey, chart="Predator-prey (rk4)")
        self.chart("Predators", predators, chart="Predator-prey (rk4)")
        # chart_xy: the phase plane (prey, predators)
        self.chart_xy("Phase plane", "Predators over prey", prey, predators)
        # the logistic growth: rk4 of logistic_growth(n, r, k) and the exact solution with logistic(x)
        r = self.growth_rate
        self.population = rk4(lambda time, y: [logistic_growth(y[0], r, CAPACITY)], self.t, self.population, 1)
        exact = CAPACITY * logistic(r * (self.t + 1) - math.log((CAPACITY - START) / START))
        self.chart("rk4", float(self.population[0]), chart="Logistic growth")
        self.chart("Exact (logistic)", exact, chart="Logistic growth")

    def fill(self):
        """The values of the grid: a surface which moves with the time step t (all cells at once with numpy)."""
        t, s, x, y = self.t, self.scale, self.xs, self.ys
        shift = t * self.speed
        # the center moves on a circle (cos_deg, sin_deg: angles in degrees) and wraps around the grid (wrap_around)
        cx = wrap_around(SIZE / 2 + SIZE / 4 * cos_deg(t * self.speed * 10) + shift, SIZE)
        cy = SIZE / 2 + SIZE / 4 * sin_deg(t * self.speed * 10)
        sigma = 1 / s
        if self.surface == "Fractal noise":
            # fractal_noise(x, y, octaves): 0 - 1, finer details with more octaves (arrays: all cells)
            v = fractal_noise(x * s + shift * s, y * s, self.octaves)
        elif self.surface == "Noise":
            # noise(x, y): a smooth value noise from 0 to 1
            v = noise(x * s + shift * s, y * s)
        elif self.surface.startswith("Waves"):
            # sin_deg and cos_deg take one number: one value per column and per row (numpy broadcasting)
            columns = np.array([sin_deg(c * s * 60 + t * self.speed * 20) for c in range(SIZE)])
            rows = np.array([cos_deg(c * s * 60) for c in range(SIZE)])
            v = 0.5 + 0.25 * columns[np.newaxis, :] + 0.25 * rows[:, np.newaxis]
        elif self.surface == "Normal density":
            # normal_pdf(x, mean, sd): a bell around the center (scaled to a maximum of 1)
            v = normal_pdf(x, cx, sigma) * normal_pdf(y, cy, sigma) / sq(normal_pdf(0, 0, sigma))
        else:
            # the distance to the center (np.hypot for arrays, distance for two points), map_range: 0 - half of the grid to 1 - 0
            v = map_range(np.hypot(x - cx, y - cy), 0, SIZE / 2, 1, 0)
        # clamp(value, min, max): the values in the range of the colors
        self.values = clamp(np.asarray(v, dtype=float), 0, 1)

    @action("Print the basic functions")
    def print_basics(self):
        print("--- Basic functions ---")
        print("clamp(1.5, 0, 1) =", clamp(1.5, 0, 1), ", lerp(0, 10, 0.25) =", lerp(0, 10, 0.25),
              ", map_range(5, 0, 10, 100, 200) =", map_range(5, 0, 10, 100, 200))
        print("wrap_around(-1, 100) =", wrap_around(-1, 100), ", sq(3) =", sq(3), ", distance(0, 0, 3, 4) =", distance(0, 0, 3, 4),
              ", distance3(0, 0, 0, 1, 2, 2) =", distance3(0, 0, 0, 1, 2, 2))
        print("logistic(0) =", logistic(0), ", logistic_growth(50, 0.1, 100) =", logistic_growth(50, 0.1, 100))
        print("cos_deg(60) =", cos_deg(60), ", sin_deg(30) =", sin_deg(30), ", heading_of(1, 1) =", heading_of(1, 1),
              ", heading_of(-1, 0) =", heading_of(-1, 0))
        print("noise(1.5, 2.5) =", noise(1.5, 2.5), ", fractal_noise(1.5, 2.5, 4) =", r4(fractal_noise(1.5, 2.5, 4)))
        print("numpy (arrays): np.sqrt([1, 4, 9]) =", np.sqrt([1, 4, 9]).tolist(), ", lerp(0, 10, np.array([0, 0.5, 1])) =",
              lerp(0, 10, np.array([0, 0.5, 1])).tolist())

    @action("Print the math of Python")
    def print_python_math(self):
        """The built-in functions and the module math of Python, and the same functions of numpy for arrays."""
        print("--- Math of Python ---")
        print("Constants: math.pi =", math.pi, ", math.e =", math.e, ", math.tau =", math.tau, ", math.inf =", math.inf)
        print("abs(-3.5) =", abs(-3.5), ", math.copysign(1, -3.5) =", math.copysign(1, -3.5), ", min(2, 7) =", min(2, 7),
              ", max(2, 7) =", max(2, 7), ", sum([1, 2, 3]) =", sum([1, 2, 3]))
        # rounding: round rounds .5 to the even number (as Math.rint of Java), int() cuts off
        print("math.floor(2.7) =", math.floor(2.7), ", math.ceil(2.1) =", math.ceil(2.1), ", round(2.5) =", round(2.5),
              ", round(3.5) =", round(3.5), ", round(2.567, 2) =", round(2.567, 2), ", int(2.9) =", int(2.9), ", math.trunc(-2.9) =", math.trunc(-2.9))
        # powers and roots (** is the power operator)
        print("2 ** 10 =", 2 ** 10, ", math.pow(2, 10) =", math.pow(2, 10), ", math.sqrt(16) =", math.sqrt(16),
              ", 27 ** (1 / 3) =", 27 ** (1 / 3), ", math.hypot(3, 4) =", math.hypot(3, 4), ", math.isqrt(17) =", math.isqrt(17))
        # exponential function and logarithms (math.log: the natural logarithm, with a second argument: the base)
        print("math.exp(1) =", math.exp(1), ", math.log(math.e) =", math.log(math.e), ", math.log10(1000) =", math.log10(1000),
              ", math.log2(8) =", math.log2(8), ", math.log(81, 3) =", math.log(81, 3))
        print("math.expm1(1e-10) =", math.expm1(1e-10), ", math.log1p(1e-10) =", math.log1p(1e-10), "(exact for small x)")
        # trigonometry in radians (math.radians, math.degrees)
        print("math.sin(math.pi / 6) =", math.sin(math.pi / 6), ", math.cos(math.radians(60)) =", math.cos(math.radians(60)),
              ", math.tan(math.pi / 4) =", math.tan(math.pi / 4))
        print("math.asin(1) =", math.asin(1), ", math.acos(0) =", math.acos(0), ", math.atan(1) =", math.atan(1),
              ", math.degrees(math.atan2(1, -1)) =", math.degrees(math.atan2(1, -1)))
        print("math.sinh(1) =", math.sinh(1), ", math.cosh(1) =", math.cosh(1), ", math.tanh(1) =", math.tanh(1))
        # integer arithmetic: // (floor division) and % (the sign of the divisor, as floorMod of Java)
        print("7 // 2 =", 7 // 2, ", 7 / 2 =", 7 / 2, ", 7 % 3 =", 7 % 3, ", -7 % 3 =", -7 % 3, ", -7 // 2 =", -7 // 2,
              ", divmod(17, 5) =", divmod(17, 5), ", math.fmod(-7, 3) =", math.fmod(-7, 3))
        # combinatorics and number theory (exact integers of any size)
        print("math.factorial(10) =", math.factorial(10), ", math.comb(10, 3) =", math.comb(10, 3), ", math.perm(10, 3) =", math.perm(10, 3),
              ", math.gcd(12, 18) =", math.gcd(12, 18), ", 2 ** 100 =", 2 ** 100)
        print("math.gamma(5) =", math.gamma(5), ", math.erf(1) =", math.erf(1), ", math.fsum([0.1] * 10) =", math.fsum([0.1] * 10),
              ", sum([0.1] * 10) =", sum([0.1] * 10))
        # special values
        print("math.sqrt(-1): ValueError (numpy: np.sqrt(-1.0) = nan with a warning), math.isnan(math.nan) =", math.isnan(math.nan),
              ", math.isclose(0.1 + 0.2, 0.3) =", math.isclose(0.1 + 0.2, 0.3))
        # numpy: the same functions for arrays (all values at once)
        a = np.array([0.0, 30.0, 60.0, 90.0])
        print("numpy: np.sin(np.radians(a)) =", r4(np.sin(np.radians(a))), ", np.round([0.5, 1.5, 2.5]) =", np.round([0.5, 1.5, 2.5]).tolist(),
              ", np.cumsum([1, 2, 3, 4]) =", np.cumsum([1, 2, 3, 4]).tolist(), ", np.prod([1, 2, 3, 4]) =", int(np.prod([1, 2, 3, 4])))

    @action("Print the statistics")
    def print_statistics(self):
        print("--- Statistics ---")
        sample = np.array([2, 4, 4, 4, 5, 5, 7, 9], dtype=float)
        print("sample =", sample.tolist())
        print("sum =", float(sample.sum()), ", mean =", mean(sample), ", variance =", variance(sample), ", sd =", sd(sample),
              ", min =", float(sample.min()), ", max =", float(sample.max()))
        print("median =", float(np.median(sample)), ", quantile(0.25) =", float(np.quantile(sample, 0.25)),
              ", quantile(0.75) =", float(np.quantile(sample, 0.75)))
        print("normal_pdf(0, 0, 1) =", float(normal_pdf(0, 0, 1)), ", normal_cdf(1.96, 0, 1) =", float(normal_cdf(1.96, 0, 1)))
        # linear_regression(x, y): intercept, slope and r squared of y = a + b x
        x = np.arange(1, 9, dtype=float)
        y = np.array([2.1, 3.9, 6.2, 7.8, 10.1, 12.2, 13.8, 16.1])
        a, b, r2 = linear_regression(x, y)
        print("linear_regression: intercept =", r4(a), ", slope =", r4(b), ", r2 =", r4(r2),
              ", correlation (np.corrcoef) =", r4(np.corrcoef(x, y)[0, 1]))

    @action("Print the linear algebra")
    def print_linear_algebra(self):
        # linear algebra with numpy: np.linalg.solve, inv, det, @ (matrix product), .T (transpose)
        print("--- Linear algebra (numpy) ---")
        a = np.array([[4, -2, 1], [3, 6, -4], [2, 1, 8]], dtype=float)
        b = np.array([11, -21, 24], dtype=float)
        print("A =", a.tolist(), ", b =", b.tolist())
        x = np.linalg.solve(a, b)
        print("np.linalg.solve(A, b) =", r4(x), ", A @ x =", r4(a @ x))
        print("np.linalg.det(A) =", r4(np.linalg.det(a)), ", A.T =", a.T.tolist())
        inverse = np.linalg.inv(a)
        print("np.linalg.inv(A) =", r4(inverse))
        print("A @ inv(A) =", r4(a @ inverse))
        print("np.dot(b, b) =", float(np.dot(b, b)), ", np.linalg.norm(b) =", r4(np.linalg.norm(b)))


simulation(MathFunctions)
