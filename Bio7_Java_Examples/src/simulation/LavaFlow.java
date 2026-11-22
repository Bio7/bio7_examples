package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoGridModel;
import com.eco.bio7.simulation.Param;

/**
 * A lava flow on Mount Etna (Sicily): lava erupts at a vent and flows to the
 * lower neighbors if it is thicker than its yield strength (a minimum
 * thickness), it cools and solidifies, and the solid lava raises the terrain
 * (later flows go around it). The elevation is read with GDAL from a GeoTIFF
 * (data/geo/etna_dem.tif).
 * <p>
 * The model is shown in ImageJ and live on the WorldWind globe of Bio7.
 * Compile the class (GDAL is needed), start it with the Play/Pause button and
 * export it with "Export Simulation as HTML..." (a map, online).
 */
public class LavaFlow extends GeoGridModel {

	@Param(label = "Vent", options = { "South-east crater", "South flank", "North flank" }, restart = true, group = "Eruption")
	String vent = "South-east crater";

	@Param(label = "Effusion (m of lava per step at the vent)", min = 0, max = 20, step = 0.5, group = "Eruption")
	double effusion = 6;

	@Param(label = "Yield thickness (m)", min = 0.1, max = 3, step = 0.1, group = "Lava")
	double yield = 0.5;

	@Param(label = "Flow (share per step)", min = 0.05, max = 0.5, step = 0.05, group = "Lava")
	double flow = 0.3;

	@Param(label = "Cooling (share per step)", min = 0, max = 0.02, step = 0.0005, group = "Lava")
	double cooling = 0.002;

	double[] elevation, lava, change;
	int ventCell;

	protected void load() {
		grid("data/geo/etna_dem.tif", 200);
		addLayer("elevation", raster("data/geo/etna_dem.tif"));
		/* Also live on the WorldWind globe of Bio7 */
		showOnGlobe();
		/* The sources of the data (shown on the map of the HTML export, required by their licenses) */
		attribution("Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data, ETOPO1)");
	}

	public void init() {
		elevation = layer("elevation");
		lava = new double[width * height];
		change = new double[width * height];
		double lon = 15.002, lat = 37.746;
		if (vent.equals("South flank")) {
			lon = 15.005;
			lat = 37.727;
		} else if (vent.equals("North flank")) {
			lon = 15.010;
			lat = 37.775;
		}
		ventCell = rowAt(lon, lat) * width + colAt(lon, lat);
		showValues(lava, 0, 10, "#ff220000", "#ff2200cc", "#ff8c00", "#ffe066", "#fffbe6");
	}

	public void step() {
		lava[ventCell] += effusion;
		for (int i = 0; i < change.length; i++) {
			change[i] = 0;
		}
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				int i = y * width + x;
				double mobile = lava[i] - yield;
				if (mobile <= 0) {
					continue;
				}
				double h = elevation[i] + lava[i];
				/* The lower neighbors get lava in proportion to the difference of the surface */
				double total = 0;
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						int j = (y + dy) * width + x + dx;
						double d = h - (elevation[j] + lava[j]);
						if (d > 0) {
							total += d;
						}
					}
				}
				if (total <= 0) {
					continue;
				}
				double out = Math.min(mobile, total / 2) * flow;
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						int j = (y + dy) * width + x + dx;
						double d = h - (elevation[j] + lava[j]);
						if (d > 0) {
							change[j] += out * d / total;
						}
					}
				}
				change[i] -= out;
			}
		}
		int covered = 0;
		for (int i = 0; i < lava.length; i++) {
			lava[i] += change[i];
			/* The solid lava raises the terrain */
			double solid = lava[i] * cooling;
			lava[i] -= solid;
			elevation[i] += solid;
			if (lava[i] > 0.1) {
				covered++;
			}
		}
		chart("Area of the lava (km²)", covered * cellSizeMeters * cellSizeMeters / 1e6);
	}

	@Action(label = "Eruption pulse (50 m at the vent)")
	public void pulse() {
		lava[ventCell] += 50;
	}
}
