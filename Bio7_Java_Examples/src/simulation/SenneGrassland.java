package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoGridModel;
import com.eco.bio7.simulation.Param;

/**
 * The heaths and dry acidic grasslands of the Senne (training area Senne between
 * Hövelhof and Oerlinghausen, North Rhine-Westphalia, Germany) on real data: the
 * elevation and the land cover (ESA WorldCover) of an area of 7 x 8 km, a
 * satellite image (Sentinel-2 cloudless by EOX) or a drawn map below. One step
 * is one year, one cell about 57 x 57 m.
 * <p>
 * The sites: the habitats come from the land cover (open land - heath, sand
 * grassland -, forest, wet heath, fields, settlements, water). Their site
 * factors on the scales of Ellenberg (light L, moisture F, reaction R, nitrogen
 * N) vary with the terrain (wetter in hollows, after the elevation) and with the
 * neighborhood: the fields and settlements bring lime and nutrients to the
 * neighboring cells. Nitrogen from the air raises N everywhere (setting).
 * <p>
 * Twelve typical species of the Senne with their indicator values: the pioneers
 * of the open sand (grey hair-grass, sand sedge, sheep's sorrel), the heath
 * (heather, petty whin), the wet heath (cross-leaved heath, purple moor-grass),
 * the grasses which follow on richer or older sites (sheep's fescue, wavy
 * hair-grass, wood small-reed) and the trees which overgrow the open land (Scots
 * pine, silver birch). The suitability of a cell for a species (the product of
 * the fits of L, F, R and N) decides the establishment of seeds, the survival
 * and the competition (the height); adult trees shade their neighbors.
 * <p>
 * Without disturbance the open land becomes grassland and woodland. The military
 * training (tanks, fire) opens the sand again: the pioneers and the heath need
 * it; the young trees of the open land are removed regularly (as the
 * conservation management does; also an action). Set the management to 0 to
 * see the succession to woodland. The views show the species (each plant an oval
 * in the color of its species), the habitats or a site factor; the legend in the
 * map is an ImageJ overlay.
 * <p>
 * The indicator values after Ellenberg et al. (Zeigerwerte von Pflanzen in
 * Mitteleuropa, Scripta Geobotanica 18) as commonly cited; check them against
 * the original tables before a scientific use. The data: see data/geo/README.md
 * (Terrain Tiles, ESA WorldCover 2021, Sentinel-2 cloudless 2017 by EOX).
 */
public class SenneGrassland extends GeoGridModel {

	static final int X = -1, NS = 12;
	/* The habitats from the land cover (no plants on fields, water and settlements) */
	static final int OPEN = 0, FOREST = 1, WET = 2, FIELD = 3, WATER = 4, SETTLEMENT = 5, NH = 6;
	static final String[] HABITATS = { "Open land (heath, sand grassland)", "Forest", "Wet heath and fen", "Fields", "Water", "Settlements" };
	static final String[] HABITAT_COLORS = { "#f2d16b90", "#2e7d3290", "#4fc3f790", "#d7a86e90", "#1565c090", "#9e9e9e90" };
	/* The site factors of the habitats: L, F, R, N */
	static final double[][] SITE = { { 8, 3, 2, 1.3 }, { 5, 4.5, 2.5, 3.5 }, { 8, 8, 2.5, 2.5 }, { 8, 5, 6.5, 7 }, { 8, 12, 5, 4 }, { 6, 4, 7, 7 } };

	static final String[] SPECIES = { "Corynephorus canescens", "Calluna vulgaris", "Rumex acetosella", "Carex arenaria", "Festuca ovina", "Genista anglica",
			"Erica tetralix", "Molinia caerulea", "Deschampsia flexuosa", "Calamagrostis epigejos", "Pinus sylvestris", "Betula pendula" };
	static final String[] SHORT = { "Grey hair-grass", "Heather", "Sheep's sorrel", "Sand sedge", "Sheep's fescue", "Petty whin", "Cross-leaved heath",
			"Purple moor-grass", "Wavy hair-grass", "Wood small-reed", "Scots pine", "Silver birch" };
	/* The indicator values L, F, R, N (X: indifferent) */
	static final int[][] VALUES = { { 8, 2, 3, 2 }, { 8, X, 1, 1 }, { 8, 3, 2, 2 }, { 9, 3, 2, 2 }, { 7, X, 3, 1 }, { 8, 5, 2, 2 }, { 8, 8, 1, 2 },
			{ 7, 7, X, 1 }, { 6, X, 2, 3 }, { 7, X, X, 6 }, { 7, X, X, X }, { 7, X, X, X } };
	/*
	 * The growth: clonal spread (probability per neighbor and year), seeds per
	 * year, the mean dispersal distance (cells of 57 m), the age of the first
	 * seeds, the mortality per year, the height (m)
	 */
	static final double[] CLONAL = { 0.2, 0.15, 0.3, 0.45, 0.15, 0.05, 0.15, 0.4, 0.35, 0.5, 0, 0 };
	static final int[] SEEDS = { 30, 40, 20, 5, 15, 8, 30, 15, 10, 15, 6, 12 };
	static final double[] DISTANCE = { 0.6, 0.8, 0.5, 0.6, 0.5, 0.4, 0.6, 0.8, 0.6, 1, 2, 4 };
	static final int[] MATURITY = { 1, 3, 1, 2, 1, 3, 3, 2, 2, 1, 10, 8 };
	static final double[] MORTALITY = { 0.15, 0.04, 0.08, 0.05, 0.05, 0.05, 0.04, 0.03, 0.04, 0.04, 0.01, 0.02 };
	static final double[] HEIGHT = { 0.2, 0.5, 0.2, 0.25, 0.3, 0.5, 0.4, 1, 0.5, 1.2, 20, 18 };
	static final String[] SPECIES_COLORS = { "#eceff1", "#b03a8a", "#c62828", "#c6a664", "#689f38", "#fdd835", "#f48fb1", "#26a69a", "#c5e1a5", "#ffa726",
			"#1b5e20", "#d4e157" };
	/* The trees (they shade their neighbors, they are 3D trees on the globe) */
	static final int PINE = 10, BIRCH = 11;
	/* The groups of the chart: open sand, heath, grasses, trees */
	static final int[] GROUP = { 0, 1, 0, 0, 2, 1, 1, 2, 2, 2, 3, 3 };
	static final String[] GROUPS = { "Open sand (pioneers)", "Heath", "Grasses", "Trees" };
	/* The colors of the groups in the chart (as their main species; open sand: the sand) */
	static final String[] GROUP_COLORS = { "#c9a86a", "#b03a8a", "#689f38", "#1b5e20" };
	/* The additional mortality of a plant on an unsuitable site (times 1 - suitability) */
	static final double MISFIT = 0.25;
	/* Tree seedlings establish rarely in a closed vegetation (mainly in gaps): the factor of their chance */
	static final double SEEDLING_IN_SWARD = 0.03;

	@Param(label = "Map", options = { "Satellite", "Relief map", "No map" }, group = "Display")
	String map = "Satellite";

	@Param(label = "View", options = { "Species", "Habitats", "Light L", "Moisture F", "Reaction R", "Nitrogen N" }, group = "Display")
	String view = "Species";

	@Param(label = "Legend", group = "Display")
	boolean legend = true;

	@Param(label = "Military training (share of the open land disturbed per year)", min = 0, max = 0.1, step = 0.005, group = "Management")
	double training = 0.04;

	@Param(label = "Heath fires (probability per year)", min = 0, max = 1, step = 0.05, group = "Management")
	double fires = 0.5;

	@Param(label = "Removal of the young trees (every n years, 0: never)", min = 0, max = 30, group = "Management")
	int clearing = 10;

	@Param(label = "Nitrogen from the air (Ellenberg N per decade)", min = 0, max = 1, step = 0.05, group = "Site")
	double deposition = 0.1;

	@Param(label = "Tolerance (Ellenberg units)", min = 0.5, max = 4, step = 0.1, group = "Species")
	double tolerance = 1.6;

	@Param(label = "Seed rain from outside (per species and year)", min = 0, max = 1, step = 0.05, group = "Species")
	double seedRain = 0.2;

	double[] elevation, landCover;
	int[] habitat, species, age;
	/* The site factors L, F, R, N (index f * cells + i), the light with the shade of the trees */
	double[] site, light;
	int year;

	protected void load() {
		grid("data/geo/senne_dem.tif", 120);
		addLayer("elevation", raster("data/geo/senne_dem.tif"));
		addLayer("landcover", raster("data/geo/senne_landcover.tif", 1, "mode"));
		background("Satellite", "data/geo/senne_satellite.tif");
		background("Relief map", "data/geo/senne_relief.tif");
		attribution("Satellite image: Sentinel-2 cloudless 2017 - https://s2maps.eu by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2017, CC BY 4.0); "
				+ "Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data); "
				+ "Land cover: © ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium (CC BY 4.0)");
	}

	public void init() {
		perspectiveLabel("Senne - dry grasslands");
		/* The icon of the Custom perspective (a file in the project; without it: the simulation icon of Bio7) */
		perspectiveIcon("data/icons/grassland.svg");
		wrap = false;
		year = 0;
		int n = width * height;
		elevation = layer("elevation");
		landCover = layer("landcover");
		/* The colors: no plant (transparent), the species, the habitats */
		String[] colors = new String[1 + NS + NH];
		colors[0] = "#00000000";
		for (int s = 0; s < NS; s++) {
			colors[1 + s] = SPECIES_COLORS[s] + "f0";
		}
		for (int h = 0; h < NH; h++) {
			colors[1 + NS + h] = HABITAT_COLORS[h];
		}
		palette(colors);
		habitat = new int[n];
		for (int i = 0; i < n; i++) {
			habitat[i] = habitatOf((int) Math.round(landCover[i]));
		}
		makeSites();
		species = new int[n];
		age = new int[n];
		light = new double[n];
		for (int i = 0; i < n; i++) {
			species[i] = -1;
			/* The forest: pines and birches */
			if (habitat[i] == FOREST) {
				species[i] = chance(0.8) ? PINE : BIRCH;
				age[i] = MATURITY[species[i]] + randomInt(40);
			}
		}
		shadeLight();
		/* The open land and the wet heath at the start: a plant in each cell, chosen by the suitability of the site (a few young trees) */
		double[] weight = new double[NS];
		for (int i = 0; i < n; i++) {
			if (habitat[i] != OPEN && habitat[i] != WET) {
				continue;
			}
			double total = 0;
			for (int s = 0; s < NS; s++) {
				double f = suitability(s, i);
				weight[s] = s == PINE || s == BIRCH ? 0.03 * f : f * f;
				total += weight[s];
			}
			double pick = random(0, total);
			for (int s = 0; s < NS; s++) {
				pick -= weight[s];
				if (pick <= 0) {
					species[i] = s;
					age[i] = randomInt(MATURITY[s] + 3);
					break;
				}
			}
		}
		show();
	}

	/* The habitat of an ESA WorldCover class */
	int habitatOf(int lc) {
		if (lc == 10 || lc == 95) {
			return FOREST;
		}
		if (lc == 90) {
			return WET;
		}
		if (lc == 40) {
			return FIELD;
		}
		if (lc == 80) {
			return WATER;
		}
		if (lc == 50 || lc == 70) {
			return SETTLEMENT;
		}
		return OPEN;
	}

	/* The site factors L, F, R, N of the cells: the habitat, the terrain, the neighborhood and some variation */
	void makeSites() {
		int n = width * height;
		site = new double[4 * n];
		double[] nearField = distanceTo(FIELD, 6), nearSettlement = distanceTo(SETTLEMENT, 6), nearWet = distanceTo(WET, 4), nearWater = distanceTo(WATER, 4);
		double ox = random(0, 500), oy = random(0, 500);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = y * width + x, h = habitat[i];
				/* Hollows are wetter: the mean elevation around the cell minus its elevation (m) */
				double sum = 0;
				int count = 0;
				for (int dy = -5; dy <= 5; dy++) {
					for (int dx = -5; dx <= 5; dx++) {
						int xx = x + dx, yy = y + dy;
						if (xx >= 0 && yy >= 0 && xx < width && yy < height && !Double.isNaN(elevation[yy * width + xx])) {
							sum += elevation[yy * width + xx];
							count++;
						}
					}
				}
				double hollow = count > 0 && !Double.isNaN(elevation[i]) ? sum / count - elevation[i] : 0;
				double wet = Math.max(-1, Math.min(3, 0.6 * hollow)) + 1.5 * Math.exp(-nearWet[i]) + 2 * Math.exp(-nearWater[i]);
				double lime = 1.5 * Math.exp(-nearField[i] / 2) + 1.5 * Math.exp(-nearSettlement[i] / 2);
				double nutrients = 3 * Math.exp(-nearField[i] / 1.5) + 2 * Math.exp(-nearSettlement[i] / 2);
				double[] v = { SITE[h][0], SITE[h][1] + wet, SITE[h][2] + lime, SITE[h][3] + nutrients };
				for (int f = 0; f < 4; f++) {
					double noise = 1.2 * (fractalNoise(ox + 100 * f + x * 0.12, oy + y * 0.12, 3) - 0.5);
					site[f * n + i] = Math.max(1, Math.min(f == 1 ? 12 : 9, v[f] + noise));
				}
			}
		}
	}

	/* The distance (cells, up to max) of each cell to the nearest cell of a habitat */
	double[] distanceTo(int h, int max) {
		int n = width * height;
		double[] d = new double[n];
		for (int i = 0; i < n; i++) {
			d[i] = habitat[i] == h ? 0 : max;
		}
		/* Two passes of a chamfer distance (1 for a side, 1.4 for a corner) */
		for (int pass = 0; pass < 2; pass++) {
			for (int k = 0; k < n; k++) {
				int i = pass == 0 ? k : n - 1 - k;
				int x = i % width, y = i / width, s = pass == 0 ? -1 : 1;
				int[][] near = { { s, 0 }, { 0, s }, { s, s }, { -s, s } };
				for (int[] o : near) {
					int xx = x + o[0], yy = y + o[1];
					if (xx >= 0 && yy >= 0 && xx < width && yy < height) {
						double step = o[0] != 0 && o[1] != 0 ? 1.4 : 1;
						d[i] = Math.min(d[i], d[yy * width + xx] + step);
					}
				}
			}
		}
		return d;
	}

	/* A habitat with plants (not fields, water, settlements) */
	boolean vegetated(int i) {
		return habitat[i] == OPEN || habitat[i] == FOREST || habitat[i] == WET;
	}

	public void step() {
		year++;
		int n = width * height;
		/* Mortality: the base rate and the misfit of the site */
		for (int i = 0; i < n; i++) {
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
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = y * width + x, s = species[i];
				if (s < 0 || age[i] < MATURITY[s]) {
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
		/* Seeds from outside the area */
		for (int s = 0; s < NS; s++) {
			if (chance(seedRain)) {
				for (int k = 0; k < 3; k++) {
					colonize(s, randomInt(width), randomInt(height));
				}
			}
		}
		/* Military training: tanks open the sand (patches of 1 - 2 cells in the open land) */
		int patches = (int) Math.round(training * countHabitat(OPEN) / 5);
		for (int k = 0; k < patches; k++) {
			int i = randomInt(n);
			if (habitat[i] == OPEN) {
				clear(i % width, i / width, 1 + randomInt(2), false);
			}
		}
		/* A heath fire: the heath, the grasses and the young trees of a larger patch burn */
		if (chance(fires)) {
			burn(4 + randomInt(4));
		}
		/* The conservation management: the young trees of the open land are removed */
		if (clearing > 0 && year % clearing == 0) {
			removeYoungTrees();
		}
		/* Nitrogen from the air */
		for (int i = 0; i < n; i++) {
			site[3 * n + i] = Math.min(9, site[3 * n + i] + deposition / 10);
		}
		show();
	}

	/* The cells of a habitat */
	int countHabitat(int h) {
		int c = 0;
		for (int v : habitat) {
			if (v == h) {
				c++;
			}
		}
		return c;
	}

	/* Clears the plants of a round patch (fire: not the adult trees; less nitrogen afterwards) */
	void clear(int cx, int cy, int radius, boolean fire) {
		int n = width * height;
		for (int dy = -radius; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				int x = cx + dx, y = cy + dy;
				if (x < 0 || y < 0 || x >= width || y >= height || dx * dx + dy * dy > radius * radius) {
					continue;
				}
				int i = y * width + x, s = species[i];
				if (!vegetated(i) || habitat[i] == FOREST) {
					continue;
				}
				if (s >= 0 && !(fire && (s == PINE || s == BIRCH) && age[i] >= MATURITY[s] + 10)) {
					species[i] = -1;
				}
				site[3 * n + i] = Math.max(1, site[3 * n + i] - (fire ? 0.3 : 0.15));
			}
		}
	}

	/* A fire in the open land: a random patch with heath */
	void burn(int radius) {
		for (int k = 0; k < 50; k++) {
			int i = randomInt(width * height);
			if (habitat[i] == OPEN && species[i] >= 0 && GROUP[species[i]] == 1) {
				clear(i % width, i / width, radius, true);
				return;
			}
		}
	}

	@Action(label = "Remove the young trees in the open land", group = "Management")
	public void removeYoungTrees() {
		for (int i = 0; i < species.length; i++) {
			if (habitat[i] != FOREST && (species[i] == PINE || species[i] == BIRCH) && age[i] < MATURITY[species[i]] + 15) {
				species[i] = -1;
			}
		}
		paint();
	}

	@Action(label = "Controlled burning of a part of the heath", group = "Management")
	public void controlledBurning() {
		burn(8);
		paint();
	}

	/* A seed of the species s at x, y: it establishes on an empty cell or displaces a weaker plant */
	void colonize(int s, int x, int y) {
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return;
		}
		int i = y * width + x, o = species[i];
		if (o == s || !vegetated(i)) {
			return;
		}
		boolean tree = s == PINE || s == BIRCH;
		/* The forest: only the trees (its floor is not simulated), they regenerate freely */
		if (habitat[i] == FOREST && !tree) {
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
			/* Only a much stronger plant (e.g., much taller) displaces an established one; tree seedlings rarely */
			double factor = tree && habitat[i] != FOREST ? SEEDLING_IN_SWARD : 1;
			if (strength > 2 * other && chance(0.2 * factor * (strength - other) / strength)) {
				species[i] = s;
				age[i] = 0;
			}
		}
	}

	/* The suitability (0 - 1): the product of the fits of light, moisture, reaction and nitrogen */
	double suitability(int s, int i) {
		int n = width * height;
		double fit = 1;
		for (int f = 0; f < 4; f++) {
			int v = VALUES[s][f];
			if (v != X) {
				double e = f == 0 ? light[i] : site[f * n + i];
				double d = (e - v) / tolerance;
				fit *= 1 / (1 + d * d * d * d);
			}
		}
		return fit;
	}

	/* The light: the site, less at the edge of the adult trees (the 8 neighbors: a cell of 57 m is shaded only at its edge) */
	void shadeLight() {
		for (int i = 0; i < light.length; i++) {
			light[i] = site[i];
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int s = species[y * width + x];
				/* The trees which grow in the open land (a grove); the edge of the forest (a few meters of a cell of 57 m) not */
				if ((s == PINE || s == BIRCH) && age[y * width + x] >= MATURITY[s] && habitat[y * width + x] != FOREST) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							int xx = x + dx, yy = y + dy;
							if (xx >= 0 && yy >= 0 && xx < width && yy < height && (dx != 0 || dy != 0)) {
								light[yy * width + xx] = Math.min(light[yy * width + xx], Math.max(1, site[yy * width + xx] - (s == PINE ? 1 : 0.8)));
							}
						}
					}
				}
			}
		}
	}

	/* The map, the cells of the view, the legend and the charts */
	void show() {
		paint();
		chartsAndLegend();
	}

	/* The map and the cells of the view (also after an action) */
	void paint() {
		int n = width * height;
		showBackground(map.equals("Satellite") ? "Satellite" : map.equals("Relief map") ? "Relief map" : null);
		cellShape(view.equals("Species") ? "oval" : "square");
		if (view.startsWith("Light") || view.startsWith("Moisture") || view.startsWith("Reaction") || view.startsWith("Nitrogen")) {
			int f = view.startsWith("Light") ? 0 : view.startsWith("Moisture") ? 1 : view.startsWith("Reaction") ? 2 : 3;
			double[] v = new double[n];
			for (int i = 0; i < n; i++) {
				v[i] = f == 0 ? light[i] : site[f * n + i];
			}
			showValues(v, 1, f == 1 ? 12 : 9, "#d73027b0", "#fc8d59b0", "#fee090b0", "#e0f3f8b0", "#91bfdbb0", "#4575b4b0");
		} else {
			for (int i = 0; i < n; i++) {
				/* The species view: the plants of the open land and the wet heath (the forest: the map) */
				cells[i] = view.equals("Habitats") ? 1 + NS + habitat[i] : habitat[i] == FOREST ? 0 : species[i] + 1;
			}
			showStates();
		}
	}

	/* The shares of the species and of the groups in the open land: the charts and the legend */
	void chartsAndLegend() {
		int n = width * height;
		double open = 0;
		double[] share = new double[NS], groups = new double[4];
		for (int i = 0; i < n; i++) {
			if (habitat[i] == OPEN) {
				open++;
				if (species[i] >= 0) {
					share[species[i]]++;
					groups[GROUP[species[i]]]++;
				}
			}
		}
		for (int s = 0; s < NS; s++) {
			share[s] = open > 0 ? 100 * share[s] / open : 0;
		}
		for (int g = 0; g < 4; g++) {
			chart("The open land (% of its cells)", GROUPS[g], open > 0 ? 100 * groups[g] / open : 0);
		}
		chartBars("Species in the open land (%)", SHORT, share);
		/* The colors of the map and of the legend (the light grey hair-grass darker, visible on a light chart) */
		String[] barColors = SPECIES_COLORS.clone();
		barColors[0] = "#90a4ae";
		chartColors("The open land (% of its cells)", GROUP_COLORS);
		chartColors("Species in the open land (%)", barColors);
		if (legend) {
			drawLegend(share);
		}
	}

	/* The legend in the map (an ImageJ overlay): the year, the species and their share of the open land */
	void drawLegend(double[] share) {
		double x = 1.5, y = 1.5, w = 31, h = 6.6 + NS * 2.3;
		drawRectOverlay(x, y, w, h, "#ffffffd8", "#00000040");
		drawTextOverlay(x + 1, y + 2.6, "Senne - year " + year, "#111111", 2.2);
		drawTextOverlay(x + 1, y + 4.8, "% of the open land", "#555555", 1.6);
		for (int s = 0; s < NS; s++) {
			double ly = y + 7.4 + s * 2.3;
			drawRectOverlay(x + 1, ly - 1.6, 1.7, 1.7, SPECIES_COLORS[s], "#00000060");
			drawTextOverlay(x + 3.4, ly, SHORT[s] + "  " + Math.round(share[s]) + "%", "#222222", 1.75);
		}
	}
}
