package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoGridModel;
import com.eco.bio7.simulation.Param;

/**
 * A wildfire on a real landscape (the Harz mountains around the Brocken,
 * Germany): the fire spreads from a burning cell to its neighbors with a
 * probability which depends on the land cover of the neighbor (forest burns
 * easily, the fire can also cross grassland, fields and settlements with an own
 * probability, water and bare ground do not burn), grows uphill (the slope from
 * the elevation model) and with the wind. The elevation (data/geo/harz_dem.tif)
 * and the land cover (data/geo/harz_landcover.tif, ESA WorldCover) are read
 * with GDAL.
 * <p>
 * The model is shown in ImageJ and live on the WorldWind globe of Bio7.
 * Compile the class (GDAL is needed, see Preferences - GDAL), start it with
 * the Play/Pause button and export it with "Export Simulation as HTML...": the
 * page shows the fire on a map (online).
 */
public class Wildfire extends GeoGridModel {

	/* The states of the cells: the fuel (by the land cover), burning, burnt, no fuel */
	static final int FOREST = 0, BURNING = 1, BURNT = 2, NO_FUEL = 3, OPEN_LAND = 4, SETTLEMENT = 5;

	@Param(label = "Spread in the forest", min = 0, max = 1, step = 0.01, group = "Spread by the land cover")
	double forest = 0.35;

	@Param(label = "Spread over grassland and fields", min = 0, max = 1, step = 0.01, group = "Spread by the land cover")
	double openLand = 0.15;

	@Param(label = "Spread over settlements", min = 0, max = 1, step = 0.01, group = "Spread by the land cover")
	double settlement = 0.04;

	@Param(label = "Effect of the slope (uphill faster)", min = 0, max = 10, step = 0.1, group = "Terrain")
	double slopeEffect = 4;

	@Param(label = "Wind from", options = { "No wind", "West", "East", "North", "South" }, group = "Wind")
	String wind = "West";

	@Param(label = "Strength of the wind", min = 0, max = 1, step = 0.05, group = "Wind")
	double windStrength = 0.6;

	double[] elevation, landCover;
	/* The land cover class of each cell (the state of the unburnt cell) */
	int[] fuel;

	protected void load() {
		grid("data/geo/harz_dem.tif", 200);
		addLayer("elevation", raster("data/geo/harz_dem.tif"));
		/* ESA WorldCover classes (10 trees, 20 shrubs, 30 grassland, 40 cropland, 50 built-up, 60 bare, 80 water, ...) */
		addLayer("landcover", raster("data/geo/harz_landcover.tif", 1, "near"));
		/* Also live on the WorldWind globe of Bio7 */
		showOnGlobe();
		/* The sources of the data (shown on the map of the HTML export, required by their licenses) */
		attribution("Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data, ETOPO1); Land cover: \u00a9 ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium (CC BY 4.0)");
	}

	public void init() {
		palette("#2e7d3240", "#ff5a00", "#2b2420", "#00000000", "#f2d16b30", "#7f7f7f80");
		wrap = false;
		elevation = layer("elevation");
		landCover = layer("landcover");
		fuel = new int[cells.length];
		for (int i = 0; i < cells.length; i++) {
			int c = (int) landCover[i];
			if (c == 10 || c == 95) {
				fuel[i] = FOREST;
			} else if (c == 20 || c == 30 || c == 40 || c == 90) {
				fuel[i] = OPEN_LAND;
			} else if (c == 50) {
				fuel[i] = SETTLEMENT;
			} else {
				fuel[i] = NO_FUEL;
			}
			cells[i] = fuel[i];
		}
		ignite(10.668, 51.788);
	}

	/* The spread probability into a cell by its land cover */
	double spread(int state) {
		if (state == FOREST) {
			return forest;
		}
		if (state == OPEN_LAND) {
			return openLand;
		}
		if (state == SETTLEMENT) {
			return settlement;
		}
		return 0;
	}

	public void step() {
		/* The direction to which the wind blows */
		double wx = 0, wy = 0;
		if (wind.equals("West")) {
			wx = 1;
		} else if (wind.equals("East")) {
			wx = -1;
		} else if (wind.equals("North")) {
			wy = 1;
		} else if (wind.equals("South")) {
			wy = -1;
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = y * width + x;
				int s = cells[i];
				int n = s;
				if (s == BURNING) {
					n = BURNT;
				} else if (s != BURNT && s != NO_FUEL) {
					double p = spread(s);
					for (int dy = -1; dy <= 1 && n == s; dy++) {
						for (int dx = -1; dx <= 1 && n == s; dx++) {
							if ((dx != 0 || dy != 0) && get(x + dx, y + dy) == BURNING) {
								double distance = (dx != 0 && dy != 0 ? 1.4142 : 1) * cellSizeMeters;
								/* Uphill (from the burning neighbor to this cell) the fire is faster */
								double rise = (elevation[i] - value(elevation, x + dx, y + dy)) / distance;
								double slopeFactor = clamp(1 + slopeEffect * rise, 0.2, 3);
								/* The wind: the direction from the neighbor to this cell */
								double windFactor = 1 + windStrength * (-dx * wx - dy * wy) / (dx != 0 && dy != 0 ? 1.4142 : 1);
								if (chance(p * slopeFactor * windFactor)) {
									n = BURNING;
								}
							}
						}
					}
				}
				next[i] = n;
			}
		}
		swap();
		double ha = cellSizeMeters * cellSizeMeters / 10000;
		int burntSettlement = 0;
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] == BURNT && fuel[i] == SETTLEMENT) {
				burntSettlement++;
			}
		}
		chart("Burning area (ha)", count(BURNING) * ha);
		chart("Burnt area (ha)", count(BURNT) * ha);
		chart("Burnt settlements (ha)", burntSettlement * ha);
	}

	/* Ignites the burnable cells around the coordinates */
	void ignite(double lon, double lat) {
		int cx = colAt(lon, lat), cy = rowAt(lon, lat);
		if (cx < 0 || cy < 0) {
			return;
		}
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				int s = get(cx + dx, cy + dy);
				if (s == FOREST || s == OPEN_LAND || s == SETTLEMENT) {
					set(cx + dx, cy + dy, BURNING);
				}
			}
		}
	}

	@Action(label = "Ignite near the Brocken")
	public void igniteBrocken() {
		ignite(10.630, 51.795);
	}

	@Action(label = "Lightning at a random place")
	public void lightning() {
		ignite(lon(randomInt(width)), lat(randomInt(height)));
	}
}
