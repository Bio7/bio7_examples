package simulation;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoGridModel;
import com.eco.bio7.simulation.Param;

/**
 * Rain on a real terrain (the Harz mountains, Germany): the rain water flows
 * to the lowest neighbor (of the surface of the terrain and the water), a part
 * infiltrates and the water leaves the area at the borders. Streams form in
 * the valleys and ponds in the depressions. The elevation is read with GDAL
 * from a GeoTIFF (data/geo/harz_dem.tif).
 * <p>
 * Compile the class (GDAL is needed), start it with the Play/Pause button and
 * export it with "Export Simulation as HTML..." (a map, online).
 */
public class RainRunoff extends GeoGridModel {

	@Param(label = "Rain (mm per step)", min = 0, max = 20, step = 0.5, group = "Water")
	double rain = 4;

	@Param(label = "Infiltration (share per step)", min = 0, max = 0.2, step = 0.005, group = "Water")
	double infiltration = 0.02;

	@Param(label = "Flow (share per step)", min = 0.05, max = 0.5, step = 0.05, group = "Water")
	double flow = 0.4;

	@Param(label = "Show", options = { "Water depth", "Elevation" }, group = "View")
	String show = "Water depth";

	double[] elevation, water, change;

	protected void load() {
		grid("data/geo/harz_dem.tif", 200);
		addLayer("elevation", raster("data/geo/harz_dem.tif"));
		/* The sources of the data (shown on the map of the HTML export, required by their licenses) */
		attribution("Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data, ETOPO1)");
	}

	public void init() {
		elevation = layer("elevation");
		water = new double[width * height];
		change = new double[width * height];
		applyView();
	}

	void applyView() {
		if (show.equals("Elevation")) {
			showValues(elevation, 150, 1150, "#1a9850", "#a6d96a", "#fee08b", "#d9a066", "#ffffff");
		} else {
			showValues(water, 0, 0.3, "#ffffff00", "#9ecae188", "#4292c6", "#08306b");
		}
	}

	public void step() {
		applyView();
		for (int i = 0; i < water.length; i++) {
			water[i] += rain / 1000;
			change[i] = 0;
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = y * width + x;
				if (water[i] <= 0) {
					continue;
				}
				/* At the border the water leaves the area */
				if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
					change[i] -= water[i] * flow;
					continue;
				}
				double h = elevation[i] + water[i];
				int lowest = -1;
				double lowestH = h;
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						int j = (y + dy) * width + x + dx;
						double hj = elevation[j] + water[j];
						if (hj < lowestH) {
							lowestH = hj;
							lowest = j;
						}
					}
				}
				if (lowest >= 0) {
					double move = Math.min(water[i], (h - lowestH) / 2) * flow;
					change[i] -= move;
					change[lowest] += move;
				}
			}
		}
		double volume = 0;
		for (int i = 0; i < water.length; i++) {
			water[i] = Math.max(0, (water[i] + change[i]) * (1 - infiltration));
			volume += water[i];
		}
		chart("Water volume (1000 m³)", volume * cellSizeMeters * cellSizeMeters / 1000);
	}

	@Action(label = "Heavy rain (50 mm)")
	public void heavyRain() {
		for (int i = 0; i < water.length; i++) {
			water[i] += 0.05;
		}
	}
}
