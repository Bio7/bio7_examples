package simulation;

import com.eco.bio7.simulation.Param;

/**
 * The sand grasslands southwest of Nuremberg in 3D on the WorldWind globe of
 * Bio7: the same model as NurembergGrassland (see there), the species on the
 * terrain of the globe (with its satellite images), the pines which grow into
 * the open land as 3D trees (a trunk and a crown, their size by their age) and
 * the flock of sheep (white spheres) which grazes the open land of the Hainberg
 * (it moves to the sand grassland with the most grass). Without management (grazing, trampling
 * and the removal of the young trees set to 0) the open land becomes a pine
 * forest on the globe. The ImageJ view shows the map as in NurembergGrassland.
 * <p>
 * Tilt the globe (the right mouse button or the WorldWind options) to see the
 * trees from the side. In the species view the plants are 3D shapes on the
 * terrain in the colors of the legend (no grid on the globe): fast points
 * (balls) or 3D shapes (grasses cones, dwarf shrubs domes, herbs cushions; the
 * setting "Plants on the globe"); a yellow fence marks the simulation area. For the HTML export use NurembergGrassland (the page
 * shows the grid on a map, not the 3D trees).
 */
public class NurembergGrassland3D extends NurembergGrassland {

	@Param(label = "Size of the trees on the globe (times)", min = 1, max = 6, step = 0.5, group = "Display")
	double treeScale = 2;

	@Param(label = "Size of the plants on the globe (times)", min = 0.5, max = 4, step = 0.5, group = "Display")
	double plantScale = 1.5;

	@Param(label = "Plants per cell on the globe", min = 1, max = 3, group = "Display")
	int plantsPerCell = 2;

	@Param(label = "Plants on the globe", options = { "Points (fast)", "3D shapes" }, group = "Display")
	String plantShape = "Points (fast)";

	@Param(label = "Sheep of the flock", min = 0, max = 60, group = "Display")
	int sheep = 25;

	/* At most so many trees on the globe (else every k-th) */
	static final int MAX_TREES = 2500;
	/* The growth form of each species on the globe: a cone (grasses, sedges), a dome (dwarf shrubs), a cushion (herbs), none (trees) */
	static final int TUFT = 0, DOME = 1, CUSHION = 2, TREE = 3;
	/* The growth forms of the species (the order of SPECIES) */
	static final int[] FORM = { TUFT, CUSHION, CUSHION, CUSHION, DOME, CUSHION, CUSHION, CUSHION, TUFT, DOME, TUFT, TREE };
	/* The Hainberg (WGS84): the flock grazes there */
	static final double HAINBERG_LON = 10.978, HAINBERG_LAT = 49.427;

	/* The flock: its center (cells) */
	double flockX, flockY;

	@Override
	protected void load() {
		super.load();
		/* Also on the WorldWind globe: the species on the terrain, the trees in 3D, the sheep */
		showOnGlobe();
		/* The area of the simulation: a yellow fence on the terrain */
		globeOutline("#ffd54f", 40);
	}

	@Override
	public void init() {
		flockX = colAt(HAINBERG_LON, HAINBERG_LAT) + 0.5;
		flockY = rowAt(HAINBERG_LON, HAINBERG_LAT) + 0.5;
		super.init();
		perspectiveLabel("Nuremberg - 3D (WorldWind)");
		/* The icon of the Custom perspective (a file in the project; without it: the simulation icon of Bio7) */
		perspectiveIcon("data/icons/grassland3d.svg");
	}

	@Override
	void show() {
		/* The species view: the plants as 3D shapes (no grid on the globe), the other views: the grid */
		boolean plants = view.equals("Species");
		globeGrid(!plants);
		super.show();
		if (plants) {
			drawPlants();
		}
		drawTrees();
		drawFlock();
	}

	/* The pines of the open land as 3D trees: a trunk (pillar) and a crown (sphere), in cells */
	void drawTrees() {
		int n = width * height, trees = 0;
		for (int i = 0; i < n; i++) {
			if (species[i] == PINE && habitat[i] != FOREST) {
				trees++;
			}
		}
		int every = Math.max(1, (trees + MAX_TREES - 1) / MAX_TREES), k = 0;
		double m = cellSizeMeters;
		for (int i = 0; i < n; i++) {
			if (species[i] != PINE || habitat[i] == FOREST || k++ % every != 0) {
				continue;
			}
			double height = treeScale * (3 + 17 * Math.min(1, age[i] / 40.0));
			double x = i % width + 0.5 + 0.5 * (((i * 7919) % 100) / 100.0 - 0.5), y = i / width + 0.5 + 0.5 * (((i * 104729) % 100) / 100.0 - 0.5);
			if (age[i] < 12) {
				/* A young pine: a cone */
				drawCone(x, y, 0.3 * height / m, height / m, "#2e7d32");
				continue;
			}
			drawPillar(x, y, 0.03 * height / m, 0.55 * height / m, "#6d4c41");
			drawSphere(x, y, 0.72 * height / m, 0.2 * height / m, "#1b5e20");
		}
	}

	/* The flock: it moves to the nearby open land with the most grass (within 8 cells of the Hainberg) and grazes there */
	void drawFlock() {
		if (sheep <= 0 || grazing <= 0) {
			return;
		}
		int hx = colAt(HAINBERG_LON, HAINBERG_LAT), hy = rowAt(HAINBERG_LON, HAINBERG_LAT);
		int bestX = (int) flockX, bestY = (int) flockY;
		double best = -1;
		for (int dy = -8; dy <= 8; dy++) {
			for (int dx = -8; dx <= 8; dx++) {
				int x = hx + dx, y = hy + dy;
				if (x < 0 || y < 0 || x >= width || y >= height || habitat[y * width + x] != OPEN) {
					continue;
				}
				int s = species[y * width + x];
				double grass = (s == 8 || s == 10 ? 2 : s >= 0 ? 1 : 0) + random(0, 1.5);
				if (grass > best) {
					best = grass;
					bestX = x;
					bestY = y;
				}
			}
		}
		/* A few cells per year towards the target */
		double ddx = bestX + 0.5 - flockX, ddy = bestY + 0.5 - flockY, d = Math.sqrt(ddx * ddx + ddy * ddy);
		if (d > 0) {
			double step = Math.min(d, 3);
			flockX += ddx / d * step;
			flockY += ddy / d * step;
		}
		/* The sheep: white 3D spheres (woolly), as large as the trees by the size setting */
		double radius = treeScale * 0.7 / cellSizeMeters;
		for (int k = 0; k < sheep; k++) {
			double a = random(0, 360), r = random(0, 1.6);
			drawSphere(flockX + r * cosDeg(a), flockY + r * sinDeg(a), radius, radius, "#f5f5f0");
		}
	}


	/* The plants: in each cell of the open land a 3D plant of its species (or several), the size by the height of the species */
	void drawPlants() {
		int n = width * height;
		double m = cellSizeMeters;
		boolean points = plantShape.startsWith("Points");
		for (int i = 0; i < n; i++) {
			int s = species[i];
			if (s < 0 || FORM[s] == TREE || habitat[i] == FOREST) {
				continue;
			}
			for (int k = 0; k < plantsPerCell; k++) {
				/* A place and a size in the cell which differ from plant to plant (but not from year to year) */
				int h = (i * 7919 + k * 104729) * 31;
				double jx = ((h >>> 3) % 100) / 100.0, jy = ((h >>> 11) % 100) / 100.0, js = 0.8 + 0.4 * (((h >>> 17) % 100) / 100.0);
				double x = i % width + (plantsPerCell == 1 ? 0.3 + 0.4 * jx : (k + 0.2 + 0.6 * jx) / plantsPerCell), y = i / width + 0.15 + 0.7 * jy;
				/* The height on the globe (m): exaggerated, a grass about 9 m, a small reed about 23 m */
				double size = plantScale * js * (6 + 14 * HEIGHT[s]) / m;
				String c = SPECIES_COLORS[s];
				if (points) {
					/* A ball on the ground (one OpenGL point): a dome wider, a tuft narrower */
					double d = (FORM[s] == DOME ? 1.4 : FORM[s] == TUFT ? 0.8 : 1) * size;
					drawPoint3(x, y, 0.4 * d, d, c);
					continue;
				}
				switch (FORM[s]) {
				case TUFT -> drawCone(x, y, 0.45 * size, size, c);
				case DOME -> drawSphere(x, y, -0.35 * size, 0.8 * size, c);
				default -> drawSphere(x, y, 0.15 * size, 0.5 * size, c);
				}
			}
		}
	}
}
