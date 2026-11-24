package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GridModel;
import com.eco.bio7.simulation.Param;

/**
 * A neobiota: the invasion of the Himalayan balsam (Impatiens glandulifera), an
 * annual plant from the Himalaya which escaped from gardens and invades river
 * banks in Central Europe. One step is one year, one cell 5 x 5 m.
 * <p>
 * The landscape (generated from the seed): a river which flows from the top to
 * the bottom, its banks (moist, rich in nutrients), floodplain forest, meadows,
 * a road and a village at the bridge. The balsam starts in a garden of the
 * village. Each year:
 * <ul>
 * <li>the plants throw their seeds a few meters (explosive capsules: the cell
 * and its neighbors),</li>
 * <li>seeds which fall into the river drift downstream (an exponential distance)
 * and are washed onto a bank: the invasion runs fast downstream and slowly
 * upstream,</li>
 * <li>seeds at the road are carried far by vehicles and mowing (rare jumps),</li>
 * <li>the seeds rest in a short-lived seed bank and germinate in the next
 * spring; the cover of a cell grows with the germinating seeds up to the
 * suitability of its habitat (the highest cover: banks 1, floodplain forest 0.6,
 * road verges 0.3, the village 0.15, meadows 0.04: the dense grass).</li>
 * </ul>
 * Control: a click (or a drag) pulls the plants and their seeds in a 5 x 5 cells
 * square, the button "Control campaign" pulls half of the invaded banks. Charts:
 * the invaded area (ha), the invaded share of the river banks, the seed bank
 * and the invaded cells of each habitat.
 */
public class InvasivePlant extends GridModel {

	static final int WATER = 0, BANK = 1, FOREST = 2, MEADOW = 3, ROAD = 4, VILLAGE = 5, SPARSE = 6, DENSE = 7;
	static final String[] HABITATS = { "Water", "Bank", "Floodplain forest", "Meadow", "Road verge", "Village" };
	/* The highest cover of the balsam in a habitat (moisture, light and the competition of the vegetation) */
	static final double[] SUITABILITY = { 0, 1.0, 0.6, 0.04, 0.3, 0.15 };
	static final int W = 160, H = 100;

	@Param(label = "Seeds per cell (full cover)", min = 50, max = 2000, group = "Balsam")
	double fecundity = 400;

	@Param(label = "Seed bank survival per year", min = 0, max = 0.9, step = 0.05, group = "Balsam")
	double bankSurvival = 0.3;

	@Param(label = "Germination", min = 0.1, max = 1, step = 0.05, group = "Balsam")
	double germination = 0.6;

	@Param(label = "Drift of the seeds in the river", group = "Dispersal")
	boolean riverDrift = true;

	@Param(label = "Mean drift distance (cells)", min = 5, max = 100, group = "Dispersal")
	double driftDistance = 30;

	@Param(label = "Road jumps (probability per year)", min = 0, max = 1, step = 0.05, group = "Dispersal")
	double roadJumps = 0.3;

	int[] habitat;
	double[] cover, bank, seeds;
	double[] riverX;
	int[] roadY;

	public void init() {
		size(W, H);
		wrap = false;
		palette("#4a90d9", "#a8d5a2", "#3e7b47", "#dfe9b0", "#9e9e9e", "#d2c9be", "#ee9ad4", "#a2238f");
		habitat = new int[W * H];
		cover = new double[W * H];
		bank = new double[W * H];
		seeds = new double[W * H];
		/* The river (meanders), the road with the bridge, the forest of the floodplain (noise) */
		double phase = random(0, 360), ox = random(0, 500), oy = random(0, 500);
		riverX = new double[H];
		for (int y = 0; y < H; y++) {
			riverX[y] = W * 0.45 + 20 * sinDeg(phase + y * 3.6) + 10 * (fractalNoise(ox, oy + y * 0.04, 3) - 0.5);
		}
		roadY = new int[W];
		for (int x = 0; x < W; x++) {
			roadY[x] = (int) Math.round(H * 0.68 + 3 * sinDeg(x * 2.5));
		}
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				double d = Math.abs(x - riverX[y]);
				int h;
				if (d <= 1.5) {
					h = WATER;
				} else if (d <= 4.5) {
					h = BANK;
				} else if (Math.abs(y - roadY[x]) <= 0) {
					h = ROAD;
				} else if (Math.abs(y - roadY[x]) < 12 && d < 28) {
					h = VILLAGE;
				} else if (d < 30 && fractalNoise(ox + x * 0.07, oy + y * 0.07, 4) > 0.5) {
					h = FOREST;
				} else {
					h = MEADOW;
				}
				habitat[y * W + x] = h;
			}
		}
		/* The garden: in the village, near the river, upstream of the bridge */
		int gy = roadY[(int) riverX[0]] - 9, gx = (int) Math.round(riverX[gy] + 9);
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				cover[(gy + dy) * W + gx + dx] = 1;
			}
		}
		show();
	}

	public void step() {
		for (int i = 0; i < seeds.length; i++) {
			seeds[i] = 0;
		}
		/* The seeds of the year */
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int i = y * W + x;
				if (cover[i] <= 0) {
					continue;
				}
				double n = cover[i] * fecundity;
				/* Seeds which fall into the river (a cell beside the water) drift downstream */
				if (riverDrift && nearWater(x, y)) {
					double drift = n * 0.3;
					n -= drift;
					for (int k = 0; k < 3; k++) {
						int y2 = y + (int) Math.round(randomExponential(1 / driftDistance));
						if (y2 < H) {
							int side = chance(0.5) ? 1 : -1;
							int x2 = (int) Math.round(riverX[y2] + side * (2 + randomInt(3)));
							add(x2, y2, drift / 3);
						}
					}
				}
				/* At the road: a rare jump with vehicles or mowing */
				if (habitat[i] == ROAD && chance(roadJumps)) {
					int x2 = x + randomInt(-60, 61);
					if (x2 >= 0 && x2 < W) {
						add(x2, roadY[x2], 20);
					}
				}
				/* The explosive capsules: the cell and its eight neighbors */
				add(x, y, n * 0.4);
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						if (dx != 0 || dy != 0) {
							add(x + dx, y + dy, n * 0.6 / 8);
						}
					}
				}
			}
		}
		/* The seed bank, the germination in spring and the cover of the year */
		for (int i = 0; i < cover.length; i++) {
			bank[i] = bank[i] * bankSurvival + seeds[i];
			double germinating = bank[i] * germination;
			bank[i] -= germinating;
			cover[i] = SUITABILITY[habitat[i]] * germinating / (germinating + 50);
		}
		show();
	}

	/* Seeds to a cell (none into the water) */
	void add(int x, int y, double n) {
		if (x >= 0 && y >= 0 && x < W && y < H && habitat[y * W + x] != WATER) {
			seeds[y * W + x] += n;
		}
	}

	boolean nearWater(int x, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				int xx = x + dx, yy = y + dy;
				if (xx >= 0 && yy >= 0 && xx < W && yy < H && habitat[yy * W + xx] == WATER) {
					return true;
				}
			}
		}
		return false;
	}

	/* The map (the cover over the habitats) and the charts */
	void show() {
		double area = 0, invadedBank = 0, banks = 0, total = 0;
		double[] perHabitat = new double[HABITATS.length];
		for (int i = 0; i < cells.length; i++) {
			int h = habitat[i];
			cells[i] = cover[i] > 0.5 ? DENSE : cover[i] > 0.1 ? SPARSE : h;
			if (cover[i] > 0.1) {
				area += 25;
				perHabitat[h]++;
			}
			if (h == BANK) {
				banks++;
				if (cover[i] > 0.1) {
					invadedBank++;
				}
			}
			total += bank[i];
		}
		chart("Invaded area (ha)", area / 10000);
		chart("Banks", "Invaded banks (%)", 100 * invadedBank / banks);
		chart("Seed bank", "Seeds (thousands)", total / 1000);
		chartBars("Invaded cells per habitat", HABITATS, perHabitat);
		record("Invaded area (ha)", area / 10000);
	}

	/** A click: pulls the plants and the seeds in a 5 x 5 cells square. */
	public void click(double x, double y) {
		pull((int) x, (int) y);
		show();
	}

	/** A drag: pulls the plants along the way. */
	public void drag(double x, double y) {
		pull((int) x, (int) y);
		show();
	}

	void pull(int cx, int cy) {
		for (int y = cy - 2; y <= cy + 2; y++) {
			for (int x = cx - 2; x <= cx + 2; x++) {
				if (x >= 0 && y >= 0 && x < W && y < H) {
					cover[y * W + x] = 0;
					bank[y * W + x] = 0;
				}
			}
		}
	}

	@Action(label = "Control campaign (half of the invaded banks)", group = "Control")
	public void campaign() {
		for (int i = 0; i < cover.length; i++) {
			if (habitat[i] == BANK && cover[i] > 0.1 && chance(0.5)) {
				cover[i] = 0;
				bank[i] *= 0.3;
			}
		}
		show();
	}
}
