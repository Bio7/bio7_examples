package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * Plant species settle in the habitats which fit their Ellenberg indicator
 * values. One step is one year, one cell 10 x 10 m.
 * <p>
 * The landscape is a mosaic of six habitats, each with its site factors on the
 * scales of Ellenberg (light L 1 - 9, moisture F 1 - 12, reaction R 1 - 9,
 * nitrogen N 1 - 9; small differences within a patch): beech forest, calcareous
 * grassland, wet meadow, raised bog, ruderal field margin and river bank with
 * reed. Twelve species with their indicator values (x: indifferent) and their
 * growth: clonal species (rhizomes: nettle, reed, wood sorrel) grow in compact
 * patches, species with light seeds (sundew, reed, heather) found new patches
 * far away, the beech grows slowly, reproduces after 15 years and shades its
 * neighbors (their light falls by 3: a forest creates its own shade).
 * <p>
 * The suitability of a cell for a species is the product of the fits of the four
 * factors (1 / (1 + (d / tolerance)^4), d the difference of the site and the
 * indicator value). It decides the establishment of a seed, the survival of a
 * plant and the displacement of a weaker neighbor (the competition: the height
 * of the species). Without disturbance the strongest competitor of a habitat
 * takes over (competitive exclusion); disturbance (gaps by windthrow, game or
 * floods) keeps weaker species in the habitat. Each plant is a filled oval in
 * the color of its species on the (pale) color of its habitat. The panel beside the map (an
 * ImageJ overlay: sharp at each zoom) summarizes each habitat: the species which
 * fit it by their indicator values
 * (the mean suitability at least 0.5: they can settle) and the species which
 * grow there now (the share of its cells, also in the bar charts). The setting
 * View shows the habitats.
 * <p>
 * The indicator values after Ellenberg et al. (Zeigerwerte von Pflanzen in
 * Mitteleuropa, Scripta Geobotanica 18) as commonly cited; check them against
 * the original tables before a scientific use.
 */
public class PlantHabitats extends GridModel {

	/*
	 * The landscape W x H, the panel of the summary beside it (PW cells)
	 */
	static final int W = 120, H = 80, PW = 79, NH = 6, NS = 12, X = -1;

	/*
	 * The display: each cell of the landscape is K x K cells of the image - the
	 * color of the habitat with a filled oval of the species (diameter 5 of 7, a
	 * border of the habitat between neighbors)
	 */
	static final int K = 7;
	static final boolean[] DOT = new boolean[K * K];
	static {
		double c = (K - 1) / 2.0;
		for (int dy = 0; dy < K; dy++) {
			for (int dx = 0; dx < K; dx++) {
				DOT[dy * K + dx] = (dx - c) * (dx - c) + (dy - c) * (dy - c) <= 2.5 * 2.5;
			}
		}
	}
	static final String[] HABITATS = { "Beech forest", "Calcareous grassland", "Wet meadow", "Raised bog", "Ruderal margin", "River bank" };
	/* The site factors of the habitats: L, F, R, N */
	static final double[][] SITE = { { 3, 5, 6, 5 }, { 8, 3, 8, 3 }, { 7, 8, 6, 5 }, { 8, 9, 1, 1 }, { 7, 5, 7, 8 }, { 7, 10, 7, 7 } };
	static final String[] SPECIES = { "Fagus sylvatica", "Oxalis acetosella", "Galium odoratum", "Bromus erectus", "Salvia pratensis", "Caltha palustris",
			"Filipendula ulmaria", "Drosera rotundifolia", "Calluna vulgaris", "Urtica dioica", "Phragmites australis", "Arrhenatherum elatius" };
	static final String[] SHORT = { "Beech", "Wood sorrel", "Woodruff", "Upright brome", "Meadow clary", "Marsh marigold", "Meadowsweet", "Sundew", "Heather",
			"Nettle", "Reed", "Oat-grass" };
	/* The indicator values L, F, R, N (X: indifferent) */
	static final int[][] VALUES = { { 3, 5, X, X }, { 1, 5, 4, 6 }, { 2, 5, 6, 5 }, { 8, 3, 8, 3 }, { 8, 3, 8, 4 }, { 7, 9, X, 6 }, { 7, 8, X, 5 }, { 8, 9, 1, 1 },
			{ 8, X, 1, 1 }, { X, 6, 7, 8 }, { 7, 10, 7, 7 }, { 8, 5, 7, 7 } };
	/* The growth: clonal spread (probability per neighbor and year), seeds per year, the mean dispersal distance (cells), the age of the first seeds, the mortality per year, the height (m) */
	static final double[] CLONAL = { 0, 0.4, 0.35, 0.2, 0, 0.15, 0.3, 0, 0.1, 0.45, 0.5, 0.15 };
	static final int[] SEEDS = { 6, 3, 2, 10, 15, 10, 20, 30, 40, 25, 30, 25 };
	static final double[] DISTANCE = { 1.5, 0.8, 1.2, 2, 1, 3, 2.5, 4, 3, 2, 8, 3 };
	static final int[] MATURITY = { 15, 1, 1, 1, 2, 2, 2, 2, 3, 1, 2, 1 };
	static final double[] MORTALITY = { 0.01, 0.06, 0.06, 0.05, 0.05, 0.05, 0.05, 0.08, 0.04, 0.04, 0.03, 0.05 };
	/* The additional mortality of a plant on an unsuitable site (times 1 - suitability) */
	static final double MISFIT = 0.25;
	static final double[] HEIGHT = { 30, 0.1, 0.2, 0.8, 0.5, 0.3, 1.5, 0.05, 0.5, 1.2, 3, 1.2 };

	@Param(label = "Tolerance (Ellenberg units)", min = 0.5, max = 4, step = 0.1, group = "Species")
	double tolerance = 1.6;

	@Param(label = "Seed rain from outside (per species and year)", min = 0, max = 1, step = 0.05, group = "Species")
	double seedRain = 0.3;

	@Param(label = "Disturbance (share of the cells cleared per year)", min = 0, max = 0.1, step = 0.005, group = "Landscape")
	double disturbance = 0.05;

	@Param(label = "View", options = { "Species", "Habitats" }, group = "Landscape")
	String view = "Species";

	@Param(label = "Shade of the beech", group = "Species")
	boolean shade = true;

	@Param(label = "Patches of the landscape", min = 6, max = 40, restart = true, group = "Landscape")
	int patches = 16;

	int[] habitat, species, age;
	double[][] site;
	double[] light;
	double[] patchX, patchY;
	int[] patchHabitat;
	/* The species which fit a habitat (the mean suitability of its cells at least 0.5) */
	String[] fits;

	public void init() {
		size((W + PW) * K, H * K);
		wrap = false;
		/* The colors: the habitats under the plants (pale), the species, the panel (white), the habitats (view Habitats) */
		palette("#e3eadf", "#f3efcf", "#dceee8", "#eadfe6", "#efe5d6", "#d8e6ef", "#2f5d34", "#9fd36a", "#5fae5a", "#c9b458", "#7a5cc0", "#f2c230", "#b9a3d9",
				"#d1435b", "#b03a8a", "#3d6e3a", "#8a9a5b", "#cfa46b", "#ffffff", "#2e6b3a", "#d9c46a", "#7fc6a4", "#b6789a", "#c98a4b", "#5b8fc9");
		/* The habitats: patches around random centers (each habitat at least twice), the borders with noise */
		patchX = new double[patches];
		patchY = new double[patches];
		patchHabitat = new int[patches];
		for (int p = 0; p < patches; p++) {
			patchX[p] = random(0, W);
			patchY[p] = random(0, H);
			patchHabitat[p] = p % NH;
		}
		double ox = random(0, 500), oy = random(0, 500);
		habitat = new int[W * H];
		site = new double[W * H][4];
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				double jx = x + 12 * (noise(ox + x * 0.08, oy + y * 0.08) - 0.5), jy = y + 12 * (noise(ox + 40 + x * 0.08, oy + y * 0.08) - 0.5);
				int best = 0;
				double bd = 1e18;
				for (int p = 0; p < patches; p++) {
					double d = (jx - patchX[p]) * (jx - patchX[p]) + (jy - patchY[p]) * (jy - patchY[p]);
					if (d < bd) {
						bd = d;
						best = p;
					}
				}
				int h = patchHabitat[best];
				habitat[y * W + x] = h;
				for (int f = 0; f < 4; f++) {
					site[y * W + x][f] = SITE[h][f] + 1.2 * (fractalNoise(ox + 100 * f + x * 0.1, oy + y * 0.1, 3) - 0.5);
				}
			}
		}
		species = new int[W * H];
		age = new int[W * H];
		light = new double[W * H];
		for (int i = 0; i < species.length; i++) {
			species[i] = -1;
		}
		shadeLight();
		/* The species which can settle in each habitat (by the indicator values, without the shade of the beeches) */
		fits = new String[NH];
		double[][] meanFit = new double[NH][NS];
		double[] cellsOf = new double[NH];
		for (int i = 0; i < habitat.length; i++) {
			cellsOf[habitat[i]]++;
			for (int s = 0; s < NS; s++) {
				meanFit[habitat[i]][s] += suitability(s, i);
			}
		}
		for (int h = 0; h < NH; h++) {
			double[] f = new double[NS];
			for (int s = 0; s < NS; s++) {
				f[s] = cellsOf[h] > 0 ? 100 * meanFit[h][s] / cellsOf[h] : 0;
			}
			fits[h] = names(f, 50, 4, false);
		}
		/* The seeds of all species everywhere: they establish where the site fits (adult plants at the start) */
		for (int s = 0; s < NS; s++) {
			for (int k = 0; k < 300; k++) {
				colonize(s, randomInt(W), randomInt(H));
			}
		}
		for (int i = 0; i < species.length; i++) {
			if (species[i] >= 0) {
				age[i] = MATURITY[species[i]];
			}
		}
		show();
	}

	public void step() {
		/* Mortality: the base rate and the misfit of the site */
		for (int i = 0; i < species.length; i++) {
			int s = species[i];
			if (s >= 0) {
				if (chance(MORTALITY[s] + MISFIT * (1 - suitability(s, i)))) {
					species[i] = -1;
				} else {
					age[i]++;
				}
			}
		}
		shadeLight();
		/* Reproduction: clonal growth into the neighbors and seeds */
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int s = species[y * W + x];
				if (s < 0 || age[y * W + x] < MATURITY[s]) {
					continue;
				}
				if (CLONAL[s] > 0) {
					if (chance(CLONAL[s])) {
						colonize(s, x + 1, y);
					}
					if (chance(CLONAL[s])) {
						colonize(s, x - 1, y);
					}
					if (chance(CLONAL[s])) {
						colonize(s, x, y + 1);
					}
					if (chance(CLONAL[s])) {
						colonize(s, x, y - 1);
					}
				}
				for (int k = 0; k < SEEDS[s]; k++) {
					double d = randomExponential(1 / DISTANCE[s]), a = random(0, 360);
					colonize(s, (int) Math.round(x + d * cosDeg(a)), (int) Math.round(y + d * sinDeg(a)));
				}
			}
		}
		/* Seeds from outside the landscape */
		for (int s = 0; s < NS; s++) {
			if (chance(seedRain)) {
				for (int k = 0; k < 3; k++) {
					colonize(s, randomInt(W), randomInt(H));
				}
			}
		}
		/* Disturbance (e.g., windthrow, game, floods): gaps for new plants */
		for (int i = 0; i < species.length; i++) {
			if (species[i] >= 0 && chance(disturbance)) {
				species[i] = -1;
			}
		}
		show();
	}

	/* A seed of the species s at x, y: it establishes on an empty cell or displaces a weaker plant */
	void colonize(int s, int x, int y) {
		if (x < 0 || y < 0 || x >= W || y >= H) {
			return;
		}
		int i = y * W + x, o = species[i];
		if (o == s) {
			return;
		}
		double fit = suitability(s, i);
		if (o < 0) {
			if (chance(fit)) {
				species[i] = s;
				age[i] = 0;
			}
		} else {
			double strength = fit * HEIGHT[s] / (HEIGHT[s] + 1), other = suitability(o, i) * HEIGHT[o] / (HEIGHT[o] + 1);
			/* Only a much stronger plant (e.g., much taller) displaces an established one; similar plants coexist */
			if (strength > 2 * other && chance(0.2 * (strength - other) / strength)) {
				species[i] = s;
				age[i] = 0;
			}
		}
	}

	/* The suitability (0 - 1): the product of the fits of light, moisture, reaction and nitrogen */
	double suitability(int s, int i) {
		double fit = 1;
		for (int f = 0; f < 4; f++) {
			int v = VALUES[s][f];
			if (v != X) {
				double e = f == 0 ? light[i] : site[i][f];
				double d = (e - v) / tolerance;
				fit *= 1 / (1 + d * d * d * d);
			}
		}
		return fit;
	}

	/* The light of the cells: the site, minus 3 under the crowns of the adult beeches (3 x 3 cells) */
	void shadeLight() {
		for (int i = 0; i < light.length; i++) {
			light[i] = site[i][0];
		}
		if (!shade) {
			return;
		}
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				if (species[y * W + x] == 0 && age[y * W + x] >= MATURITY[0]) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							int xx = x + dx, yy = y + dy;
							if (xx >= 0 && yy >= 0 && xx < W && yy < H && (dx != 0 || dy != 0)) {
								light[yy * W + xx] = Math.max(1, site[yy * W + xx][0] - 3);
							}
						}
					}
				}
			}
		}
	}

	/* The map, the panel of the summary and the charts: the share of the cells of each habitat with each species */
	void show() {
		double[][] cover = new double[NH][NS];
		double[] cellsOf = new double[NH];
		boolean habitats = view.equals("Habitats");
		/* The map: the habitat (pale) of each cell with an oval of its species, or the habitats (view Habitats) */
		int dw = (W + PW) * K;
		java.util.Arrays.fill(cells, NH + NS);
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int i = y * W + x, h = habitat[i], s = species[i];
				int ground = habitats ? NH + NS + 1 + h : h;
				for (int dy = 0; dy < K; dy++) {
					int row = (y * K + dy) * dw + x * K;
					for (int dx = 0; dx < K; dx++) {
						cells[row + dx] = !habitats && s >= 0 && DOT[dy * K + dx] ? NH + s : ground;
					}
				}
				cellsOf[h]++;
				if (s >= 0) {
					cover[h][s]++;
				}
			}
		}
		/* The panel as an ImageJ overlay (sharp at each zoom, Image > Overlay > Hide Overlay hides it) */
		double ty = 3.6;
		text(W + 2, ty, "Settled species (% of the cells)", "#111111", 3.0);
		ty += 4.6;
		for (int h = 0; h < NH; h++) {
			double[] share = new double[NS];
			int settled = 0;
			for (int s = 0; s < NS; s++) {
				share[s] = cellsOf[h] > 0 ? 100 * cover[h][s] / cellsOf[h] : 0;
				if (share[s] >= 1) {
					settled++;
				}
			}
			chartBars(HABITATS[h] + " (% of the cells)", SHORT, share);
			chart("Species in the habitats", HABITATS[h], settled);
			/* The panel: the habitat and its three most common species */
			square(W + 2, ty - 2.2, 2.4, HABITAT_COLORS[h]);
			text(W + 5.6, ty, HABITATS[h], "#111111", 2.8);
			text(W + 5.6, ty + 2.8, "Fits: " + fits[h], "#2e6b3a", 2.2);
			text(W + 5.6, ty + 5.4, "Now: " + names(share, 1, 3, true), "#444444", 2.2);
			ty += 9.2;
		}
		ty += 0.8;
		text(W + 2, ty, "Species", "#111111", 3.0);
		ty += 3.6;
		for (int s = 0; s < NS; s++) {
			double lx = W + 2 + (s % 3) * 25.5, ly = ty + (s / 3) * 3.2;
			square(lx, ly - 2, 2.2, SPECIES_COLORS[s]);
			text(lx + 3, ly, SHORT[s], "#222222", 2.5);
		}
	}

	/* A text and a square of the panel (in the cells of the landscape) as an ImageJ overlay */
	void text(double x, double y, String text, String color, double size) {
		drawTextOverlay(K * x, K * y, text, color, K * size);
	}

	void square(double x, double y, double size, String color) {
		drawRectOverlay(K * x, K * y, K * size, K * size, color, null);
	}

	static final String[] HABITAT_COLORS = { "#2e6b3a", "#d9c46a", "#7fc6a4", "#b6789a", "#c98a4b", "#5b8fc9" };
	static final String[] SPECIES_COLORS = { "#2f5d34", "#9fd36a", "#5fae5a", "#c9b458", "#7a5cc0", "#f2c230", "#b9a3d9", "#d1435b", "#b03a8a", "#3d6e3a",
			"#8a9a5b", "#cfa46b" };

	/* The species with the largest values (at least min, at most max species): "Reed 80 %, Nettle 5 %" or "none" */
	String names(double[] value, double min, int max, boolean percent) {
		String text = "";
		boolean[] used = new boolean[NS];
		for (int k = 0; k < max; k++) {
			int best = -1;
			for (int s = 0; s < NS; s++) {
				if (!used[s] && value[s] >= min && (best < 0 || value[s] > value[best])) {
					best = s;
				}
			}
			if (best < 0) {
				break;
			}
			used[best] = true;
			text += (text.equals("") ? "" : ", ") + SHORT[best] + (percent ? " " + (int) Math.round(value[best]) + "%" : "");
		}
		return text.equals("") ? "none" : text;
	}

	@Action(label = "Sow all species everywhere", group = "Species")
	public void sowAll() {
		for (int s = 0; s < NS; s++) {
			for (int k = 0; k < 40; k++) {
				colonize(s, randomInt(W), randomInt(H));
			}
		}
		show();
	}
}
