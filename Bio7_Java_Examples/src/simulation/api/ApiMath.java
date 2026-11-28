package simulation.api;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * The mathematical functions of the simulation API (static methods of the
 * class Simulation, inherited by all models): basic functions, statistics,
 * distributions, linear algebra, differential equations (rk4), angles and
 * noise, and the class Math of Java.
 * <p>
 * The grid shows a moving surface of the chosen function (noise,
 * fractalNoise, sinDeg/cosDeg, normalPdf, distance). The charts show the
 * statistics of the grid (mean, sd, median, quantile), a histogram, a
 * predator-prey system (rk4) and its phase plane, and the logistic growth
 * (rk4 compared with the exact solution). init() and the buttons print the
 * other functions to the console.
 * <p>
 * All functions give the same results in the HTML export ("Export Simulation
 * as HTML..."). Math of Java can also be used (Math.sin, Math.exp, ...); in
 * the browser the last digits of these functions can differ.
 */
public class ApiMath extends ValueGridModel {

	@Param(label = "Function of the grid", options = { "Fractal noise", "Noise", "Waves (sinDeg, cosDeg)", "Normal density", "Distance" }, group = "Grid")
	String surface = "Fractal noise";

	@Param(label = "Scale", min = 0.02, max = 0.3, step = 0.01, group = "Grid")
	double scale = 0.06;

	@Param(label = "Octaves (fractal noise)", min = 1, max = 8, group = "Grid")
	int octaves = 4;

	@Param(label = "Speed", min = 0, max = 2, step = 0.05, group = "Grid")
	double speed = 0.3;

	@Param(label = "Growth rate of the prey", min = 0.1, max = 1.5, step = 0.05, group = "Differential equations")
	double preyGrowth = 0.6;

	@Param(label = "Rate of the logistic growth", min = 0.05, max = 1, step = 0.05, group = "Differential equations")
	double growthRate = 0.3;

	static final int SIZE = 100;
	static final double CAPACITY = 100;
	static final double START = 2;

	/* The state of the predator-prey system and of the logistic growth (rk4) */
	double[] predatorPrey;
	double[] population;

	public void init() {
		size(SIZE, SIZE);
		range(0, 1);
		colorMap(VIRIDIS);
		predatorPrey = new double[] { 10, 5 };
		population = new double[] { START };
		fill();
		printBasics();
		printJavaMath();
		printStatistics();
		printLinearAlgebra();
	}

	public void step() {
		fill();
		/* Statistics of the grid: mean, sd, median, quantile (p from 0 to 1) */
		chart("Mean", mean(values));
		chart("Standard deviation", sd(values));
		chart("Median", median(values));
		chart("10 % quantile", quantile(values, 0.1));
		chart("90 % quantile", quantile(values, 0.9));
		/* A histogram of the grid (a bar chart, replaced in each step) */
		double[] counts = new double[10];
		String[] labels = new String[10];
		for (int k = 0; k < 10; k++) {
			labels[k] = "0." + k;
		}
		for (int i = 0; i < values.length; i++) {
			counts[(int) clamp(Math.floor(values[i] * 10), 0, 9)]++;
		}
		chartBars("Histogram of the grid", labels, counts);
		/* rk4: one Runge-Kutta step of a system of differential equations (Lotka-Volterra), 10 steps per time step */
		for (int k = 0; k < 10; k++) {
			predatorPrey = rk4((time, v) -> new double[] { preyGrowth * v[0] - 0.05 * v[0] * v[1], 0.02 * v[0] * v[1] - 0.4 * v[1] }, t + k * 0.1, predatorPrey, 0.1);
		}
		chart("Predator-prey (rk4)", "Prey", predatorPrey[0]);
		chart("Predator-prey (rk4)", "Predators", predatorPrey[1]);
		/* chartXY: the phase plane (prey, predators) */
		chartXY("Phase plane", "Predators over prey", predatorPrey[0], predatorPrey[1]);
		/* The logistic growth: rk4 of logisticGrowth(n, r, k) and the exact solution with logistic(x) */
		population = rk4((time, v) -> new double[] { logisticGrowth(v[0], growthRate, CAPACITY) }, t, population, 1);
		double exact = CAPACITY * logistic(growthRate * (t + 1) - Math.log((CAPACITY - START) / START));
		chart("Logistic growth", "rk4", population[0]);
		chart("Logistic growth", "Exact (logistic)", exact);
	}

	/* The values of the grid: a surface which moves with the time step t */
	void fill() {
		double shift = t * speed;
		/* The center moves on a circle (cosDeg, sinDeg: angles in degrees) and wraps around the grid (wrapAround) */
		double cx = wrapAround(SIZE / 2 + SIZE / 4 * cosDeg(t * speed * 10) + shift, SIZE);
		double cy = SIZE / 2 + SIZE / 4 * sinDeg(t * speed * 10);
		double sigma = 1 / scale;
		double peak = sq(normalPdf(0, 0, sigma));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double v;
				if (surface.equals("Fractal noise")) {
					/* fractalNoise(x, y, octaves): 0 - 1, finer details with more octaves */
					v = fractalNoise(x * scale + shift * scale, y * scale, octaves);
				} else if (surface.equals("Noise")) {
					/* noise(x, y): a smooth value noise from 0 to 1 */
					v = noise(x * scale + shift * scale, y * scale);
				} else if (surface.equals("Waves (sinDeg, cosDeg)")) {
					v = 0.5 + 0.25 * sinDeg(x * scale * 60 + t * speed * 20) + 0.25 * cosDeg(y * scale * 60);
				} else if (surface.equals("Normal density")) {
					/* normalPdf(x, mean, sd): a bell around the center (scaled to a maximum of 1) */
					v = normalPdf(x, cx, sigma) * normalPdf(y, cy, sigma) / peak;
				} else {
					/* distance(x1, y1, x2, y2) to the center, mapRange: 0 - half of the grid to 1 - 0 */
					v = mapRange(distance(x, y, cx, cy), 0, SIZE / 2, 1, 0);
				}
				/* clamp(value, min, max): the value in the range of the colors */
				values[y * width + x] = clamp(v, 0, 1);
			}
		}
	}

	@Action(label = "Print the basic functions")
	public void printBasics() {
		System.out.println("--- Basic functions ---");
		System.out.println("clamp(1.5, 0, 1) = " + clamp(1.5, 0, 1) + ", lerp(0, 10, 0.25) = " + lerp(0, 10, 0.25) + ", mapRange(5, 0, 10, 100, 200) = " + mapRange(5, 0, 10, 100, 200));
		System.out.println("wrapAround(-1, 100) = " + wrapAround(-1, 100) + ", sq(3) = " + sq(3) + ", distance(0, 0, 3, 4) = " + distance(0, 0, 3, 4) + ", distance3(0, 0, 0, 1, 2, 2) = "
				+ distance3(0, 0, 0, 1, 2, 2));
		System.out.println("logistic(0) = " + logistic(0) + ", logisticGrowth(50, 0.1, 100) = " + logisticGrowth(50, 0.1, 100));
		System.out.println("cosDeg(60) = " + cosDeg(60) + ", sinDeg(30) = " + sinDeg(30) + ", headingOf(1, 1) = " + headingOf(1, 1) + ", headingOf(-1, 0) = " + headingOf(-1, 0));
		System.out.println("noise(1.5, 2.5) = " + noise(1.5, 2.5) + ", fractalNoise(1.5, 2.5, 4) = " + fractalNoise(1.5, 2.5, 4));
	}

	/*
	 * The functions of the class Math of Java (all of them are translated for the
	 * HTML export, except Math.random(): use random() of the model). In the
	 * browser the last digits of sin, cos, exp, log, ... can differ.
	 */
	@Action(label = "Print the math of Java")
	public void printJavaMath() {
		System.out.println("--- Math of Java ---");
		System.out.println("Constants: Math.PI = " + Math.PI + ", Math.E = " + Math.E);
		System.out.println("Math.abs(-3.5) = " + Math.abs(-3.5) + ", Math.signum(-3.5) = " + Math.signum(-3.5) + ", Math.min(2, 7) = " + Math.min(2, 7) + ", Math.max(2, 7) = " + Math.max(2, 7));
		/* Rounding: floor (down), ceil (up), round (to the nearest, .5 up), rint (.5 to the even number) */
		System.out.println("Math.floor(2.7) = " + Math.floor(2.7) + ", Math.ceil(2.1) = " + Math.ceil(2.1) + ", Math.round(2.5) = " + Math.round(2.5) + ", Math.round(-2.5) = " + Math.round(-2.5)
				+ ", Math.rint(2.5) = " + Math.rint(2.5) + ", (int) 2.9 = " + (int) 2.9);
		/* Powers and roots */
		System.out.println("Math.pow(2, 10) = " + Math.pow(2, 10) + ", Math.sqrt(16) = " + Math.sqrt(16) + ", Math.cbrt(27) = " + Math.cbrt(27) + ", Math.hypot(3, 4) = " + Math.hypot(3, 4));
		/* Exponential function and logarithms (log: the natural logarithm) */
		System.out.println("Math.exp(1) = " + Math.exp(1) + ", Math.log(Math.E) = " + Math.log(Math.E) + ", Math.log10(1000) = " + Math.log10(1000) + ", log2(8) = Math.log(8) / Math.log(2) = "
				+ Math.log(8) / Math.log(2));
		System.out.println("Math.expm1(1e-10) = " + Math.expm1(1e-10) + ", Math.log1p(1e-10) = " + Math.log1p(1e-10) + " (exact for small x)");
		/* Trigonometry in radians (Math.toRadians, Math.toDegrees) */
		System.out.println("Math.sin(Math.PI / 6) = " + Math.sin(Math.PI / 6) + ", Math.cos(Math.toRadians(60)) = " + Math.cos(Math.toRadians(60)) + ", Math.tan(Math.PI / 4) = " + Math.tan(Math.PI / 4));
		System.out.println("Math.asin(1) = " + Math.asin(1) + ", Math.acos(0) = " + Math.acos(0) + ", Math.atan(1) = " + Math.atan(1) + ", Math.toDegrees(Math.atan2(1, -1)) = "
				+ Math.toDegrees(Math.atan2(1, -1)));
		System.out.println("Math.sinh(1) = " + Math.sinh(1) + ", Math.cosh(1) = " + Math.cosh(1) + ", Math.tanh(1) = " + Math.tanh(1));
		/* Integer arithmetic: division and remainder (% keeps the sign, floorMod is always positive for a positive divisor) */
		System.out.println("7 / 2 = " + 7 / 2 + ", 7 / 2.0 = " + 7 / 2.0 + ", 7 % 3 = " + 7 % 3 + ", -7 % 3 = " + -7 % 3 + ", Math.floorMod(-7, 3) = " + Math.floorMod(-7, 3)
				+ ", Math.floorDiv(-7, 2) = " + Math.floorDiv(-7, 2));
		/* Special values of double */
		System.out.println("1.0 / 0 = " + 1.0 / 0 + ", Math.sqrt(-1) = " + Math.sqrt(-1) + ", Double.isNaN(Math.sqrt(-1)) = " + Double.isNaN(Math.sqrt(-1)));
	}

	@Action(label = "Print the statistics")
	public void printStatistics() {
		System.out.println("--- Statistics ---");
		double[] sample = { 2, 4, 4, 4, 5, 5, 7, 9 };
		System.out.println("sample = " + vector(sample));
		System.out.println("sum = " + sum(sample) + ", mean = " + mean(sample) + ", variance = " + variance(sample) + ", sd = " + sd(sample) + ", min = " + min(sample) + ", max = " + max(sample));
		System.out.println("median = " + median(sample) + ", quantile(0.25) = " + quantile(sample, 0.25) + ", quantile(0.75) = " + quantile(sample, 0.75));
		System.out.println("normalPdf(0, 0, 1) = " + normalPdf(0, 0, 1) + ", normalCdf(1.96, 0, 1) = " + normalCdf(1.96, 0, 1));
		/* linearRegression(x, y): intercept, slope and r² of y = a + b x */
		double[] x = { 1, 2, 3, 4, 5, 6, 7, 8 };
		double[] y = { 2.1, 3.9, 6.2, 7.8, 10.1, 12.2, 13.8, 16.1 };
		double[] fit = linearRegression(x, y);
		System.out.println("linearRegression: intercept = " + round(fit[0]) + ", slope = " + round(fit[1]) + ", r² = " + round(fit[2]) + ", correlation = " + round(correlation(x, y)));
	}

	@Action(label = "Print the linear algebra")
	public void printLinearAlgebra() {
		System.out.println("--- Linear algebra ---");
		double[][] a = { { 4, -2, 1 }, { 3, 6, -4 }, { 2, 1, 8 } };
		double[] b = { 11, -21, 24 };
		System.out.println("A = " + matrix(a) + ", b = " + vector(b));
		/* solve(a, b): the solution x of a x = b */
		double[] x = solve(a, b);
		System.out.println("solve(A, b) = " + vector(x) + ", matVec(A, x) = " + vector(matVec(a, x)));
		System.out.println("determinant(A) = " + round(determinant(a)) + ", transpose(A) = " + matrix(transpose(a)));
		System.out.println("inverse(A) = " + matrix(inverse(a)));
		System.out.println("matMul(A, inverse(A)) = " + matrix(matMul(a, inverse(a))));
		System.out.println("dot(b, b) = " + dot(b, b) + ", norm(b) = " + round(norm(b)));
	}

	/* The value with 4 decimal places */
	static double round(double v) {
		return Math.round(v * 10000) / 10000.0;
	}

	static String vector(double[] v) {
		String s = "[";
		for (int i = 0; i < v.length; i++) {
			s += (i > 0 ? ", " : "") + round(v[i]);
		}
		return s + "]";
	}

	static String matrix(double[][] m) {
		String s = "[";
		for (int i = 0; i < m.length; i++) {
			s += (i > 0 ? ", " : "") + vector(m[i]);
		}
		return s + "]";
	}
}
