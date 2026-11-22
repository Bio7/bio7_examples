package simulation.api;

import com.eco.bio7.simulation.Action;
import com.eco.bio7.simulation.GeoGridModel;
import com.eco.bio7.simulation.Param;

/**
 * All methods of a GeoGridModel (a georeferenced grid: the data is read with
 * GDAL in Bio7, the HTML page shows the grid on a map): the layers of the Harz
 * mountains (elevation, land cover, a vector polygon) shown as values or as
 * states, coordinates of cells and the slope.
 * <p>
 * load() (only in Bio7): grid, raster, vector, addLayer, attribution,
 * showOnGlobe. In init() and step(): layer, showValues, showStates, colAt,
 * rowAt, lon, lat, gradient, slopeDegrees, value and the fields originX,
 * originY, cellWidth, cellHeight, cellSizeMeters, south, west, north, east,
 * crs. The states and methods of a GridModel (see ApiGrid) are also available.
 * <p>
 * GDAL is needed (Preferences - GDAL). Compile the class and export it with
 * "Export Simulation as HTML..." (the page gets the loaded layers).
 */
public class ApiGeoGrid extends GeoGridModel {

	@Param(label = "Show", options = { "Elevation", "Gradient (slope)", "Land cover", "Release area", "States" })
	String show = "Elevation";

	double[] elevation, landCover, release, steepness;

	/* load(): reads the data with GDAL (once, before the first init; not in the HTML page) */
	protected void load() {
		/* grid(file, columns): the extent and the coordinate system of a raster, the number of the columns */
		grid("data/geo/harz_dem.tif", 160);
		/* raster(file): the first band on the cells (bilinear); raster(file, band, resampling) */
		addLayer("elevation", raster("data/geo/harz_dem.tif"));
		addLayer("landcover", raster("data/geo/harz_landcover.tif", 1, "near"));
		/* vector(file, value): the polygons (lines, points) with the value, 0 outside; vector(file, attribute) */
		addLayer("release", vector("data/geo/release_area.geojson", 1));
		/* attribution(text): the sources of the data on the map of the HTML page (required by their licenses) */
		attribution("Elevation: Terrain Tiles (AWS Open Data; sources incl. SRTM, EU-DEM produced using Copernicus data, ETOPO1); "
				+ "Land cover: © ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium (CC BY 4.0)");
		/* showOnGlobe(): also live on the WorldWind globe of Bio7 (opens the WorldWind perspective) */
		// showOnGlobe();
	}

	public void init() {
		/* layer(name): a copy of the loaded layer for this setup (index y * width + x) */
		elevation = layer("elevation");
		landCover = layer("landcover");
		release = layer("release");
		steepness = new double[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				/* gradient(layer, x, y): rise / run (0.1 = 10 %), with cellSizeMeters; slopeDegrees: in degrees */
				steepness[y * width + x] = gradient(elevation, x, y);
			}
		}
		/* The states (palette, cells) of a GridModel: 1 for forest, 2 for the release area */
		palette("#00000000", "#2e7d3290", "#8e24aaB0");
		for (int i = 0; i < cells.length; i++) {
			cells[i] = release[i] > 0 ? 2 : landCover[i] == 10 ? 1 : 0;
		}
		/* colAt(lon, lat), rowAt(lon, lat): the cell at WGS84 coordinates (-1 outside); lon(x), lat(y): the center of a cell */
		int bx = colAt(10.6158, 51.7991), by = rowAt(10.6158, 51.7991);
		System.out.println("Brocken: cell " + bx + ", " + by + " (center " + lon(bx) + ", " + lat(by) + "), elevation " + value(elevation, bx, by) + " m");
		/* The georeference: originX, originY, cellWidth, cellHeight (in the coordinate system crs) and the bounds in degrees */
		System.out.println("Cell size " + cellSizeMeters + " m, bounds " + south + ", " + west + " to " + north + ", " + east);
		applyView();
	}

	void applyView() {
		/* showValues(values, min, max, colors...): a layer with a color map; showStates(): the states with the palette */
		if (show.equals("Elevation")) {
			showValues(elevation, 150, 1150, "#1a985080", "#a6d96a80", "#fee08b80", "#d9a066a0", "#ffffffc0");
		} else if (show.equals("Gradient (slope)")) {
			showValues(steepness, 0, 0.5, "#ffffff00", "#fdae61a0", "#d7191cc0");
		} else if (show.equals("Land cover")) {
			showValues(landCover, 10, 90, "#2e7d32a0", "#f2d16ba0", "#9e9e9ea0", "#4fc3f7a0");
		} else if (show.equals("Release area")) {
			showValues(release, 0, 1, "#00000000", "#8e24aac0");
		} else {
			showStates();
		}
	}

	public void step() {
		applyView();
		double sum = 0;
		for (int i = 0; i < steepness.length; i++) {
			sum += steepness[i];
		}
		chart("Mean gradient", sum / steepness.length);
		chart("Forest cells", count(1));
	}

	@Action(label = "Mark the cells above 900 m")
	public void markHigh() {
		for (int i = 0; i < cells.length; i++) {
			if (elevation[i] > 900) {
				cells[i] = 2;
			}
		}
		showStates();
	}
}
