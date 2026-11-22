package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoGridModel;
import com.eco.bio7.simulation.Param;

/**
 * The dispersal of a species on a real terrain (the Harz mountains,
 * Germany): the species lives in an elevation band (a niche with an optimum
 * and a tolerance), spreads to neighbor cells with a probability which depends
 * on the suitability, makes rare long-distance jumps and dies in unsuitable
 * cells. It starts in a release area: a polygon (data/geo/release_area.geojson,
 * a fictitious example) rasterized with GDAL; the elevation is read from a
 * GeoTIFF.
 * <p>
 * Compile the class (GDAL is needed), start it with the Play/Pause button and
 * export it with "Export Simulation as HTML..." (a map, online).
 */
public class SpeciesDispersal extends GeoGridModel {

	static final int ABSENT = 0, PRESENT = 1, UNSUITABLE = 2;

	@Param(label = "Optimal elevation (m)", min = 200, max = 1100, step = 10, group = "Niche")
	double optimum = 450;

	@Param(label = "Tolerance (m)", min = 50, max = 600, step = 10, group = "Niche")
	double tolerance = 250;

	@Param(label = "Dispersal probability", min = 0, max = 0.5, step = 0.01, group = "Population")
	double dispersal = 0.12;

	@Param(label = "Long-distance jumps (per step)", min = 0, max = 0.5, step = 0.01, group = "Population")
	double jumps = 0.05;

	@Param(label = "Mortality in poor cells", min = 0, max = 0.5, step = 0.01, group = "Population")
	double mortality = 0.1;

	double[] elevation, release;

	protected void load() {
		grid("data/geo/harz_dem.tif", 200);
		addLayer("elevation", raster("data/geo/harz_dem.tif"));
		addLayer("release", vector("data/geo/release_area.geojson", 1));
		/* The sources of the data (shown on the map of the HTML export, required by their licenses) */
		attribution("Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data, ETOPO1)");
	}

	public void init() {
		palette("#00000000", "#8e24aa", "#45454540");
		elevation = layer("elevation");
		release = layer("release");
		for (int i = 0; i < cells.length; i++) {
			cells[i] = release[i] > 0 ? PRESENT : ABSENT;
		}
	}

	/* The suitability of a cell (1 at the optimum, 0 beyond the tolerance) */
	double suitability(int i) {
		double d = (elevation[i] - optimum) / tolerance;
		return Math.max(0, 1 - d * d);
	}

	public void step() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = y * width + x;
				double s = suitability(i);
				int n;
				if (cells[i] == PRESENT) {
					n = chance(mortality * (1 - s)) || s == 0 ? (s == 0 ? UNSUITABLE : ABSENT) : PRESENT;
				} else if (s == 0) {
					n = UNSUITABLE;
				} else {
					n = ABSENT;
					int k = countNeighbors(x, y, PRESENT);
					for (int j = 0; j < k && n == ABSENT; j++) {
						if (chance(dispersal * s)) {
							n = PRESENT;
						}
					}
				}
				next[i] = n;
			}
		}
		swap();
		/* Rare long-distance jumps to a random suitable cell */
		if (chance(jumps)) {
			int i = randomInt(cells.length);
			if (cells[i] == ABSENT && suitability(i) > 0.5) {
				cells[i] = PRESENT;
			}
		}
		chart("Occupied area (km²)", count(PRESENT) * cellSizeMeters * cellSizeMeters / 1e6);
	}

	@Action(label = "Release again in the release area")
	public void releaseAgain() {
		for (int i = 0; i < cells.length; i++) {
			if (release[i] > 0) {
				cells[i] = PRESENT;
			}
		}
	}
}
