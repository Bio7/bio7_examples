package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.Param;
import com.eco.bio7.simulation.ValueGridModel;

/**
 * The zone of influence model of plant competition (after Schwinning and Weiner
 * 1998 and Weiner et al. 2001): each plant takes up resources in a circle around
 * it, its zone of influence, which grows with its mass m (the area A = c *
 * m^(2/3)). Where zones overlap, the plants share the resource of a cell:
 * <ul>
 * <li>complete symmetry: equal shares (the size doesn't matter),</li>
 * <li>size-symmetric: shares in proportion to the mass,</li>
 * <li>size-asymmetric: in proportion to m^4 (larger plants get much more, e.g.,
 * light),</li>
 * <li>complete asymmetry: the largest plant gets all.</li>
 * </ul>
 * The growth per step: dm = r * (A_eff - c * m^2 / M^(4/3)); A_eff is the
 * uptake area after the sharing, M the size of a plant without neighbors. A
 * plant which gets less than 5 % of the resources of its zone five steps in a
 * row starves and dies (or when its mass falls below 0.5): self-thinning. The competition mode shapes the population: asymmetric
 * competition makes few large and many small plants (a high Gini coefficient of
 * the sizes), symmetric competition plants of similar sizes.
 * <p>
 * The grid shows the crowding (the number of zones over a cell), the circles
 * the zones. A click plants a seedling. Charts: the plants alive, the mean mass,
 * the Gini coefficient and the size classes.
 */
public class ZoneOfInfluence extends ValueGridModel {

	static final int W = 100, H = 100;

	@Param(label = "Plants at the start", min = 20, max = 800, restart = true, group = "Plants")
	int plants = 250;

	@Param(label = "Competition", options = { "Complete symmetry", "Size-symmetric", "Size-asymmetric", "Complete asymmetry" }, group = "Plants")
	String competition = "Size-asymmetric";

	@Param(label = "Growth rate r", min = 0.1, max = 2, step = 0.05, group = "Plants")
	double rate = 0.6;

	@Param(label = "Size without neighbors M", min = 100, max = 3000, step = 50, group = "Plants")
	double maxMass = 1000;

	@Param(label = "Radius of the zone at M (cells)", min = 3, max = 15, step = 0.5, restart = true, group = "Plants")
	double maxRadius = 8;

	double[] px, py, mass;
	boolean[] alive;
	/* The steps in a row with less than 5 % of the resources of the zone */
	int[] starving;
	int planted;
	double c;

	public void init() {
		size(W, H);
		wrap = false;
		range(0, 6);
		colorMap("#f7f4ea", "#e6dfb8", "#c9b97a", "#a3884a", "#6e5a2c");
		px = new double[2000];
		py = new double[2000];
		mass = new double[2000];
		alive = new boolean[2000];
		starving = new int[2000];
		planted = 0;
		/* c: the area of the zone of a plant of the size M is pi * radius^2 */
		double cm = cbrt(maxMass);
		c = Math.PI * maxRadius * maxRadius / (cm * cm);
		for (int k = 0; k < plants; k++) {
			plant(random(0, W), random(0, H), random(0.8, 1.2));
		}
		show();
	}

	void plant(double x, double y, double m) {
		if (planted < px.length) {
			px[planted] = x;
			py[planted] = y;
			mass[planted] = m;
			alive[planted] = true;
			starving[planted] = 0;
			planted++;
		}
	}

	public void step() {
		double p = competition.equals("Complete symmetry") ? 0 : competition.equals("Size-symmetric") ? 1 : 4;
		boolean complete = competition.equals("Complete asymmetry");
		double[] weights = new double[W * H], largest = new double[W * H];
		int[] owner = new int[W * H];
		for (int i = 0; i < values.length; i++) {
			values[i] = 0;
			owner[i] = -1;
		}
		/* The zones over the cells: the crowding, the sum of the weights, the largest plant */
		for (int i = 0; i < planted; i++) {
			if (!alive[i]) {
				continue;
			}
			double w = p == 0 ? 1 : p == 1 ? mass[i] : mass[i] * mass[i] * mass[i] * mass[i];
			double r = radius(mass[i]);
			for (int y = (int) Math.max(0, Math.floor(py[i] - r)); y <= Math.min(H - 1, Math.floor(py[i] + r)); y++) {
				for (int x = (int) Math.max(0, Math.floor(px[i] - r)); x <= Math.min(W - 1, Math.floor(px[i] + r)); x++) {
					if (covers(i, x, y, r)) {
						int k = y * W + x;
						values[k]++;
						weights[k] += w;
						if (mass[i] > largest[k]) {
							largest[k] = mass[i];
							owner[k] = i;
						}
					}
				}
			}
		}
		/* The uptake area after the sharing and the growth */
		double m43 = cbrt(maxMass);
		m43 = m43 * m43 * m43 * m43;
		for (int i = 0; i < planted; i++) {
			if (!alive[i]) {
				continue;
			}
			double w = p == 0 ? 1 : p == 1 ? mass[i] : mass[i] * mass[i] * mass[i] * mass[i];
			double r = radius(mass[i]), share = 0;
			int n = 0;
			for (int y = (int) Math.max(0, Math.floor(py[i] - r)); y <= Math.min(H - 1, Math.floor(py[i] + r)); y++) {
				for (int x = (int) Math.max(0, Math.floor(px[i] - r)); x <= Math.min(W - 1, Math.floor(px[i] + r)); x++) {
					if (covers(i, x, y, r)) {
						int k = y * W + x;
						share += complete ? (owner[k] == i ? 1 : 0) : w / weights[k];
						n++;
					}
				}
			}
			double area = area(mass[i]);
			double effective = n > 0 ? area * share / n : 0;
			double growth = rate * (effective - c * mass[i] * mass[i] / m43);
			mass[i] += growth;
			starving[i] = effective < 0.05 * area ? starving[i] + 1 : 0;
			if (starving[i] >= 5 || mass[i] < 0.5) {
				alive[i] = false;
			}
		}
		show();
	}

	/* The cell x, y in the zone: its center within the radius, or the cell of the plant */
	boolean covers(int i, int x, int y, double r) {
		double dx = x + 0.5 - px[i], dy = y + 0.5 - py[i];
		return dx * dx + dy * dy <= r * r || (x == (int) Math.floor(px[i]) && y == (int) Math.floor(py[i]));
	}

	/* The area of the zone c * m^(2/3) and its radius */
	double area(double m) {
		double cm = cbrt(m);
		return c * cm * cm;
	}

	double radius(double m) {
		return Math.sqrt(area(m) / Math.PI);
	}

	/* The cube root with the same digits in Java and in the browser (Newton's method) */
	static double cbrt(double v) {
		if (v <= 0) {
			return 0;
		}
		double r = v < 1 ? 1 : v / 3 + 1;
		for (int k = 0; k < 60; k++) {
			r = (2 * r + v / (r * r)) / 3;
		}
		return r;
	}

	void show() {
		int n = 0;
		double sum = 0;
		double[] classes = new double[10];
		String[] labels = new String[10];
		for (int k = 0; k < 10; k++) {
			labels[k] = "< " + (int) Math.round((k + 1) * maxMass / 10);
		}
		for (int i = 0; i < planted; i++) {
			if (alive[i]) {
				n++;
				sum += mass[i];
				classes[(int) Math.min(9, Math.floor(mass[i] / maxMass * 10))]++;
				double f = Math.min(1, mass[i] / maxMass);
				drawCircle(px[i], py[i], radius(mass[i]), f > 0.5 ? "#2e7d3240" : "#8bc34a33", f > 0.5 ? "#1b5e20" : "#689f38");
			}
		}
		double mean = n > 0 ? sum / n : 0, gini = 0;
		if (n > 1 && mean > 0) {
			double d = 0;
			for (int i = 0; i < planted; i++) {
				if (alive[i]) {
					for (int j = 0; j < planted; j++) {
						if (alive[j]) {
							d += Math.abs(mass[i] - mass[j]);
						}
					}
				}
			}
			gini = d / (2.0 * n * n * mean);
		}
		chart("Plants alive", n);
		chart("Size", "Mean mass", mean);
		chart("Inequality", "Gini coefficient", gini);
		chartBars("Size classes (mass)", labels, classes);
		record("Gini coefficient", gini);
		record("Plants alive", n);
	}

	/** A click: a seedling at the position. */
	public void click(double x, double y) {
		plant(x, y, 1);
		show();
	}

	@Action(label = "Sow 50 seedlings", group = "Plants")
	public void sow() {
		for (int k = 0; k < 50; k++) {
			plant(random(0, W), random(0, H), 1);
		}
		show();
	}
}
