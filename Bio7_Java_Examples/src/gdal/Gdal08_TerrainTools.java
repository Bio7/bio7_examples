package gdal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.FieldType;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataType;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GeometryType;
import com.eco.bio7.gdal.VectorLayer;

/*
 * The raster tools of GDAL: gdaldem (hillshade, slope, aspect, color relief),
 * contour lines, gdal_rasterize (the plots of Gdal03 as raster), polygonize
 * (elevation zones as polygons) and gdalbuildvrt (a mosaic of tiles).
 * Run Gdal01_RasterBasics and Gdal03_Vectors first.
 *
 *   Gdal.dem(src, dest, "hillshade", options...)   gdaldem (also slope, aspect, TRI, TPI, roughness)
 *   Gdal.colorRelief(src, dest, colorFile)         gdaldem color-relief
 *   band.contours(layer, interval, options...)     contour lines (or polygons: "POLYGONIZE=YES")
 *   band.polygonize(layer, field, useMask)         areas of equal values as polygons
 *   Gdal.rasterize(vectors, dest, options...)      gdal_rasterize
 *   Gdal.buildVrt(dest, files, options...)         gdalbuildvrt
 */
public class Gdal08_TerrainTools {

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path dir = GdalSetup.folder();
		Path dem = dir.resolve("dem_utm.tif"), plots = dir.resolve("plots_utm.gpkg");
		if (!Files.exists(dem) || !Files.exists(plots)) {
			System.err.println("Please run Gdal01_RasterBasics and Gdal03_Vectors first.");
			return;
		}

		/* gdaldem */
		Gdal.dem(dem.toString(), dir.resolve("dem_hillshade.tif").toString(), "hillshade", "-z", "2", "-multidirectional", "-compute_edges");
		Gdal.dem(dem.toString(), dir.resolve("dem_slope.tif").toString(), "slope", "-compute_edges");
		Gdal.dem(dem.toString(), dir.resolve("dem_aspect.tif").toString(), "aspect", "-zero_for_flat");
		Path colors = dir.resolve("colors.txt");
		Files.writeString(colors, "# elevation red green blue\n200 34 139 34\n450 240 230 140\n700 160 82 45\n1000 255 250 250\nnv 0 0 0 0\n");
		Gdal.colorRelief(dem.toString(), dir.resolve("dem_colors.tif").toString(), colors.toString(), "-alpha");
		try (GdalDataset s = Gdal.open(dir.resolve("dem_slope.tif").toString())) {
			System.out.printf("gdaldem: hillshade, aspect, color relief; slope up to %.1f degrees%n", s.band(1).minMax(false)[1]);
		}

		try (GdalDataset src = Gdal.open(dem.toString())) {
			/* Contour lines every 50 m as GeoPackage */
			Path contours = dir.resolve("contours.gpkg");
			Files.deleteIfExists(contours);
			try (GdalDataset out = Gdal.createVector(contours.toString(), "GPKG")) {
				VectorLayer lines = out.createLayer("contours", src.spatialReference(), GeometryType.LINESTRING);
				src.band(1).contours(lines, 50);
				Map<Object, Integer> perLevel = new TreeMap<>();
				for (Feature f : lines.features()) {
					perLevel.merge(f.attributes().get("ELEV"), 1, Integer::sum);
				}
				System.out.println("Contours: " + lines.featureCount() + " lines, per level " + perLevel);
			}

			/* Elevation zones (every 200 m) as polygons: classify, then polygonize */
			float[] z = src.band(1).readFloats();
			byte[] zone = new byte[z.length];
			for (int i = 0; i < z.length; i++) {
				zone[i] = (byte) (1 + (int) (z[i] / 200));
			}
			try (GdalDataset zones = Gdal.create("", "MEM", src.width(), src.height(), 1, GdalDataType.BYTE);
					GdalDataset mem = Gdal.createVector("", "MEM")) {
				zones.setGeoTransform(src.geoTransform());
				zones.setProjection(src.projection());
				zones.band(1).write(zone);
				VectorLayer poly = mem.createLayer("zones", src.spatialReference(), GeometryType.POLYGON);
				zones.band(1).polygonize(poly, "zone", false);
				System.out.println("Elevation zones: " + poly.featureCount() + " polygons");
				/* Saved with ogr2ogr (from the memory dataset) */
				Path zonesFile = dir.resolve("zones.geojson");
				Files.deleteIfExists(zonesFile);
				try (GdalDataset saved = Gdal.vectorTranslate(mem, zonesFile.toString(), "-f", "GeoJSON")) {
					saved.flush();
				}
			}
		}

		/*
		 * gdal_rasterize: the plots as raster with 10 m pixels, the value: the number
		 * of trees. gdal_rasterize doesn't reproject: the plots in UTM of Gdal03
		 * (plots_utm.gpkg), the pixel size in meters.
		 */
		Path plotsUtm = dir.resolve("plots_utm.gpkg");
		Path plotRaster = dir.resolve("plots_trees.tif");
		Gdal.rasterize(plotsUtm.toString(), plotRaster.toString(), "-a", "trees", "-tr", "10", "10", "-a_nodata", "-1", "-ot", "Int16");
		try (GdalDataset r = Gdal.open(plotRaster.toString())) {
			System.out.println("Rasterized plots: " + r.width() + " x " + r.height() + " pixels of 10 m, trees " + java.util.Arrays.toString(r.band(1).minMax(false)));
		}

		/* gdalbuildvrt: four tiles of the elevation model as one virtual raster */
		List<String> tiles = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			String tile = dir.resolve("tile" + i + ".tif").toString();
			Gdal.translate(dem.toString(), tile, "-srcwin", String.valueOf((i % 2) * 200), String.valueOf((i / 2) * 150), "200", "150");
			tiles.add(tile);
		}
		Path vrt = dir.resolve("mosaic.vrt");
		Gdal.buildVrt(vrt.toString(), tiles);
		try (GdalDataset m = Gdal.open(vrt.toString())) {
			System.out.println("Mosaic of 4 tiles: " + m.width() + " x " + m.height() + " (the VRT is a small XML file: " + Files.size(vrt) + " bytes)");
		}
		System.out.println("\nThe results are in " + dir + " (open them with File > GDAL > Open Raster in ImageJ, or in WorldWind).");
	}
}
