package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoGridModel;
import com.eco.bio7.simulation.Param;

/**
 * The sand grasslands southwest of Nuremberg (the Hainberg near Oberasbach, the
 * Rednitz valley between Zirndorf, Oberasbach and Stein, Bavaria, Germany) on
 * real data: the elevation and the land cover (ESA WorldCover) of an area of 7 x
 * 7 km, a satellite image (Sentinel-2 cloudless by EOX) or a drawn map below.
 * One step is one year, one cell about 60 x 60 m.
 * <p>
 * The sand areas of Franconia ("Sandachse Franken", on Keuper sands) are dry,
 * poor and weakly acid to neutral: the habitats come from the land cover (open land, forest, wet meadows,
 * fields, settlements, water). Their site factors on the scales of Ellenberg
 * (light L, moisture F, reaction R, nitrogen N) vary with the terrain (wetter in
 * hollows and near the water) and with the neighborhood: the towns and the fields
 * bring lime and nutrients, the open land near them is no sand grassland. Nitrogen
 * from the air raises N everywhere (setting).
 * <p>
 * Twelve typical species with their indicator values: grey hair-grass, the sand
 * thrift (Armeria maritima ssp. elongata) and the yellow everlasting
 * (Helichrysum arenarium) of the Franconian sands, maiden pink, Breckland thyme,
 * sheep's-bit, sheep's sorrel, mouse-ear hawkweed, common bent, heather, wood
 * small-reed and Scots pine. The suitability of a cell for a species (the
 * product of the fits of L, F, R and N) decides the establishment of seeds, the
 * survival and the competition (the height); adult pines shade their neighbors.
 * <p>
 * Without management the sand grassland becomes grassland (common bent, wood
 * small-reed) and pine woodland. The management of the Hainberg: grazing by sheep
 * (removes the young trees and the tall grasses, takes nutrients away), trampling
 * (opens the sand), the removal of the young trees (regularly and as an action)
 * and the removal of the topsoil (an action: raw sand for the pioneers). The
 * views show the species (each plant an oval in the color of its species), the
 * habitats or a site factor; the legend in the map is an ImageJ overlay.
 * <p>
 * The indicator values after Ellenberg et al. (Zeigerwerte von Pflanzen in
 * Mitteleuropa, Scripta Geobotanica 18) as commonly cited; check them against
 * the original tables before a scientific use. The data: see data/geo/README.md
 * (Terrain Tiles, ESA WorldCover 2021, Sentinel-2 cloudless 2017 by EOX).
 */
public class NurembergGrassland extends GeoGridModel {

	static final int X = -1, NS = 12;
	/* The habitats from the land cover (no plants on fields, water and settlements) */
	static final int OPEN = 0, FOREST = 1, WET = 2, FIELD = 3, WATER = 4, SETTLEMENT = 5, NH = 6;
	static final String[] HABITATS = { "Open land (sand grassland, meadows)", "Forest", "Wet meadows", "Fields", "Water", "Settlements" };
	static final String[] HABITAT_COLORS = { "#f2d16b90", "#2e7d3290", "#4fc3f790", "#d7a86e90", "#1565c090", "#9e9e9e90" };
	/* The site factors of the habitats: L, F, R, N */
	static final double[][] SITE = { { 8, 3, 4, 1.8 }, { 5, 4.5, 3, 3.5 }, { 7, 8, 5.5, 5 }, { 8, 5, 6.5, 7 }, { 8, 12, 6, 5 }, { 6, 4, 7, 7 } };

	static final String[] SPECIES = { "Corynephorus canescens", "Armeria maritima ssp. elongata", "Helichrysum arenarium", "Dianthus deltoides",
			"Thymus serpyllum", "Jasione montana", "Rumex acetosella", "Pilosella officinarum", "Agrostis capillaris", "Calluna vulgaris",
			"Calamagrostis epigejos", "Pinus sylvestris" };
	static final String[] SHORT = { "Grey hair-grass", "Sand thrift", "Yellow everlasting", "Maiden pink", "Breckland thyme", "Sheep's-bit", "Sheep's sorrel",
			"Mouse-ear hawkweed", "Common bent", "Heather", "Wood small-reed", "Scots pine" };
	/* The indicator values L, F, R, N (X: indifferent) */
	static final int[][] VALUES = { { 8, 2, 3, 2 }, { 9, 3, 3, 1 }, { 9, 2, 5, 1 }, { 8, 3, 3, 2 }, { 7, 2, X, 1 }, { 7, 3, 3, 2 }, { 8, 3, 2, 2 },
			{ 7, 4, X, 2 }, { 7, X, 4, 4 }, { 8, X, 1, 1 }, { 7, X, X, 6 }, { 7, X, X, X } };
	/*
	 * The growth: clonal spread (probability per neighbor and year), seeds per
	 * year, the mean dispersal distance (cells of 60 m), the age of the first
	 * seeds, the mortality per year, the height (m)
	 */
	static final double[] CLONAL = { 0.2, 0.1, 0.15, 0.1, 0.3, 0, 0.3, 0.15, 0.35, 0.15, 0.5, 0 };
	static final int[] SEEDS = { 30, 15, 25, 15, 10, 30, 20, 10, 15, 40, 15, 6 };
	static final double[] DISTANCE = { 0.6, 0.5, 0.8, 0.5, 0.5, 0.6, 0.5, 0.7, 0.6, 0.8, 1, 2 };
	static final int[] MATURITY = { 1, 2, 2, 2, 2, 1, 1, 1, 1, 3, 1, 10 };
	static final double[] MORTALITY = { 0.15, 0.04, 0.05, 0.06, 0.03, 0.2, 0.08, 0.06, 0.04, 0.04, 0.04, 0.01 };
	static final double[] HEIGHT = { 0.2, 0.3, 0.3, 0.25, 0.1, 0.3, 0.2, 0.15, 0.5, 0.5, 1.2, 20 };
	static final String[] SPECIES_COLORS = { "#eceff1", "#f06292", "#ffd600", "#ad1457", "#9575cd", "#42a5f5", "#bf360c", "#fff59d", "#8bc34a", "#7b1fa2",
			"#ffa726", "#1b5e20" };
	/* The tree (it shades its neighbors, it is a 3D tree on the globe) */
	static final int PINE = 11;
	/* The groups of the chart: open sand, sand grassland herbs, heath, grasses, trees */
	static final int[] GROUP = { 0, 1, 1, 1, 1, 1, 0, 1, 3, 2, 3, 4 };
	static final String[] GROUPS = { "Open sand (pioneers)", "Sand grassland herbs", "Heath", "Grasses", "Trees" };
	/* The colors of the groups in the chart (as their main species; open sand: the sand) */
	static final String[] GROUP_COLORS = { "#c9a86a", "#f06292", "#7b1fa2", "#8bc34a", "#1b5e20" };
	/* The additional mortality of a plant on an unsuitable site (times 1 - suitability) */
	static final double MISFIT = 0.2;
	/* Pine seedlings establish rarely in a closed vegetation (mainly in gaps): the factor of their chance */
	static final double SEEDLING_IN_SWARD = 0.03;

	@Param(label = "Map", options = { "Satellite", "Relief map", "No map" }, group = "Display")
	String map = "Satellite";

	@Param(label = "View", options = { "Species", "Habitats", "Light L", "Moisture F", "Reaction R", "Nitrogen N" }, group = "Display")
	String view = "Species";

	@Param(label = "Legend", group = "Display")
	boolean legend = true;

	@Param(label = "Sheep grazing (share of the open land grazed per year)", min = 0, max = 1, step = 0.05, group = "Management")
	double grazing = 0.4;

	@Param(label = "Trampling (share of the open land opened per year)", min = 0, max = 0.1, step = 0.005, group = "Management")
	double trampling = 0.02;

	@Param(label = "Removal of the young trees (every n years, 0: never)", min = 0, max = 30, group = "Management")
	int clearing = 15;

	@Param(label = "Nitrogen from the air (Ellenberg N per decade)", min = 0, max = 1, step = 0.05, group = "Site")
	double deposition = 0.15;

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
		grid("data/geo/nuernberg_dem.tif", 120);
		addLayer("elevation", raster("data/geo/nuernberg_dem.tif"));
		addLayer("landcover", raster("data/geo/nuernberg_landcover.tif", 1, "mode"));
		background("Satellite", "data/geo/nuernberg_satellite.tif");
		background("Relief map", "data/geo/nuernberg_relief.tif");
		attribution("Satellite image: Sentinel-2 cloudless 2017 - https://s2maps.eu by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2017, CC BY 4.0); "
				+ "Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data); "
				+ "Land cover: © ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium (CC BY 4.0)");
	}

	public void init() {
		perspectiveLabel("Nuremberg - sand grasslands");
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
			/* The forest: pines */
			if (habitat[i] == FOREST) {
				species[i] = PINE;
				age[i] = MATURITY[PINE] + randomInt(40);
			}
		}
		shadeLight();
		/* The open land and the wet meadows at the start: a plant in each cell, chosen by the suitability of the site (a few young pines) */
		double[] weight = new double[NS];
		for (int i = 0; i < n; i++) {
			if (habitat[i] != OPEN && habitat[i] != WET) {
				continue;
			}
			double total = 0;
			for (int s = 0; s < NS; s++) {
				double f = suitability(s, i);
				weight[s] = s == PINE ? 0.03 * f : f * f;
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
				double wet = Math.max(-1, Math.min(3, 0.4 * hollow)) + 1.5 * Math.exp(-nearWet[i]) + 2.5 * Math.exp(-nearWater[i]);
				/* The towns bring more lime and nutrients than in the Senne (gardens, dogs, building rubble) */
				double lime = 1.5 * Math.exp(-nearField[i] / 2) + 2.5 * Math.exp(-nearSettlement[i] / 2);
				double nutrients = 3 * Math.exp(-nearField[i] / 1.5) + 3 * Math.exp(-nearSettlement[i] / 2);
				/* Ridges (above the surroundings) are sunnier and drier: the niche of the sand specialists */
				double ridge = Math.max(0, Math.min(1, -hollow / 4));
				double[] v = { SITE[h][0] + 0.5 * ridge, SITE[h][1] + wet, SITE[h][2] + lime, SITE[h][3] + nutrients };
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
		/* Sheep grazing: the young pines and the grasses (the sheep prefer them) of the grazed open land, nutrients are taken away */
		for (int i = 0; i < n; i++) {
			if (habitat[i] == OPEN && chance(grazing)) {
				int s = species[i];
				if (s == PINE && age[i] < MATURITY[PINE]) {
					species[i] = -1;
				} else if ((s == 10 || s == 8) && chance(0.6)) {
					species[i] = -1;
				}
				site[3 * n + i] = Math.max(1, site[3 * n + i] - 0.04);
			}
		}
		/* Trampling (paths, sheep): small patches of open sand */
		int patches = (int) Math.round(trampling * countHabitat(OPEN) / 3);
		for (int k = 0; k < patches; k++) {
			int i = randomInt(n);
			if (habitat[i] == OPEN) {
				clear(i % width, i / width, 1, 0.1);
			}
		}
		/* The conservation management: the young pines of the open land are removed */
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

	/* Clears the plants of a round patch of the open land (and lowers the nitrogen) */
	void clear(int cx, int cy, int radius, double lessNitrogen) {
		int n = width * height;
		for (int dy = -radius; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				int x = cx + dx, y = cy + dy;
				if (x < 0 || y < 0 || x >= width || y >= height || dx * dx + dy * dy > radius * radius) {
					continue;
				}
				int i = y * width + x;
				if (habitat[i] != OPEN) {
					continue;
				}
				species[i] = -1;
				site[3 * n + i] = Math.max(1, site[3 * n + i] - lessNitrogen);
			}
		}
	}

	@Action(label = "Remove the young pines in the open land", group = "Management")
	public void removeYoungTrees() {
		for (int i = 0; i < species.length; i++) {
			if (habitat[i] != FOREST && species[i] == PINE && age[i] < MATURITY[PINE] + 15) {
				species[i] = -1;
			}
		}
		paint();
	}

	@Action(label = "Remove the topsoil of a grass patch (raw sand)", group = "Management")
	public void removeTopsoil() {
		/* A patch of grasses (common bent, wood small-reed) becomes raw, poor sand for the pioneers */
		for (int k = 0; k < 200; k++) {
			int i = randomInt(width * height);
			if (habitat[i] == OPEN && species[i] >= 0 && GROUP[species[i]] == 3) {
				clear(i % width, i / width, 4, 3);
				break;
			}
		}
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
		boolean tree = s == PINE;
		/* The forest: only the pines (its floor is not simulated), they regenerate freely */
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
			/* Only a much stronger plant (e.g., much taller) displaces an established one; pine seedlings rarely */
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

	/* The light: the site, less at the edge of the adult pines (the 8 neighbors: a cell of 60 m is shaded only at its edge) */
	void shadeLight() {
		for (int i = 0; i < light.length; i++) {
			light[i] = site[i];
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				/* The pines which grow in the open land (a grove); the edge of the forest (a few meters of a cell of 60 m) not */
				if (species[y * width + x] == PINE && age[y * width + x] >= MATURITY[PINE] && habitat[y * width + x] != FOREST) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							int xx = x + dx, yy = y + dy;
							if (xx >= 0 && yy >= 0 && xx < width && yy < height && (dx != 0 || dy != 0)) {
								light[yy * width + xx] = Math.min(light[yy * width + xx], Math.max(1, site[yy * width + xx] - 1));
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
				/* The species view: the plants of the open land and the wet meadows (the forest: the map) */
				cells[i] = view.equals("Habitats") ? 1 + NS + habitat[i] : habitat[i] == FOREST ? 0 : species[i] + 1;
			}
			showStates();
		}
	}

	/* The shares of the species and of the groups in the open land: the charts and the legend */
	void chartsAndLegend() {
		int n = width * height;
		double open = 0;
		double[] share = new double[NS], groups = new double[GROUPS.length];
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
		for (int g = 0; g < GROUPS.length; g++) {
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
		double x = 1.5, y = 1.5, w = 33, h = 6.6 + NS * 2.3;
		drawRectOverlay(x, y, w, h, "#ffffffd8", "#00000040");
		drawTextOverlay(x + 1, y + 2.6, "Nuremberg - year " + year, "#111111", 2.2);
		drawTextOverlay(x + 1, y + 4.8, "% of the open land", "#555555", 1.6);
		for (int s = 0; s < NS; s++) {
			double ly = y + 7.4 + s * 2.3;
			drawRectOverlay(x + 1, ly - 1.6, 1.7, 1.7, SPECIES_COLORS[s], "#00000060");
			drawTextOverlay(x + 3.4, ly, SHORT[s] + "  " + Math.round(share[s]) + "%", "#222222", 1.75);
		}
	}
}
