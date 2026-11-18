package gdal;

import java.nio.file.Files;
import java.nio.file.Path;

import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;

/*
 * gdalwarp and gdal_translate for the daily raster work: clip with a polygon
 * (the forest plot), resample to a coarser grid (the mean of the pixels), cut a
 * window in map coordinates, and save as Cloud Optimized GeoTIFF (COG: tiled,
 * with overviews, readable in parts over the web).
 * Run Gdal01_RasterBasics and Gdal03_Vectors first.
 *
 *   Gdal.warp(src, dst, "-cutline", file, "-cwhere", "...", "-crop_to_cutline")
 *   Gdal.warp(src, dst, "-tr", "30", "30", "-r", "average")
 *   Gdal.translate(src, dst, "-projwin", ulx, uly, lrx, lry)
 *   Gdal.translate(src, dst, "-of", "COG", "-co", "COMPRESS=DEFLATE")
 */
public class Gdal10_ClipResampleCog {

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
		String src = dem.toString();

		/* Clipped with the forest plot (outside: no data) */
		Path forest = dir.resolve("dem_forest.tif");
		Gdal.warp(src, forest.toString(), "-cutline", plots.toString(), "-cwhere", "plot = 'Forest'", "-crop_to_cutline", "-dstnodata", "-9999", "-overwrite");
		print("Clipped to the forest", forest);

		/* Resampled: 30 m pixels, each the mean of the 10 m pixels */
		Path coarse = dir.resolve("dem_30m.tif");
		Gdal.warp(src, coarse.toString(), "-tr", "30", "30", "-r", "average", "-overwrite");
		print("Resampled to 30 m (average)", coarse);

		/* A window in map coordinates (upper left x, y, lower right x, y) */
		Path window;
		try (GdalDataset ds = Gdal.open(src)) {
			double[] b = ds.bounds();   // minX, minY, maxX, maxY
			double cx = (b[0] + b[2]) / 2, cy = (b[1] + b[3]) / 2;
			window = dir.resolve("dem_window.tif");
			Gdal.translate(src, window.toString(), "-projwin", fmt(cx - 1000), fmt(cy + 750), fmt(cx + 1000), fmt(cy - 750));
		}
		print("A window of 2 x 1.5 km", window);

		/* Cloud Optimized GeoTIFF */
		Path cog = dir.resolve("dem_cog.tif");
		Gdal.translate(src, cog.toString(), "-of", "COG", "-co", "COMPRESS=DEFLATE", "-co", "PREDICTOR=FLOATING_POINT", "-co", "OVERVIEWS=AUTO",
				"-co", "BLOCKSIZE=128");
		print("Cloud Optimized GeoTIFF", cog);
		String info = Gdal.info(cog.toString());
		System.out.println("  layout: " + (info.contains("LAYOUT=COG") ? "COG" : "?") + ", overviews: "
				+ info.lines().filter(l -> l.contains("Overviews:")).findFirst().orElse("none").trim());
		System.out.printf("  size: %d KB (the uncompressed GeoTIFF: %d KB)%n", Files.size(cog) / 1024, Files.size(dem) / 1024);
	}

	private static void print(String what, Path file) {
		try (GdalDataset ds = Gdal.open(file.toString())) {
			double[] gt = ds.geoTransform();
			double[] mm = ds.band(1).minMax(false);
			System.out.printf("%s: %d x %d pixels of %.0f m, elevation %.0f - %.0f m (%s)%n", what, ds.width(), ds.height(), gt[1], mm[0], mm[1], file.getFileName());
		}
	}

	private static String fmt(double v) {
		return String.valueOf(v);
	}
}
