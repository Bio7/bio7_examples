package geo.gdal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.eco.bio7.gdal.CoordinateTransform;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GdalDriver;
import com.eco.bio7.gdal.SpatialReference;

/*
 * The tools of GDAL with the options of the command line: gdalwarp
 * (reprojection), gdal_translate (formats, scaling), coordinate
 * transformations and the formats (drivers) of this GDAL.
 * Run Gdal01_RasterBasics first (it creates dem_utm.tif).
 *
 *   Gdal.warp(src, dest, "-t_srs", "EPSG:4326", ...)       gdalwarp
 *   Gdal.translate(src, dest, "-of", "PNG", ...)           gdal_translate
 *   Gdal.warp(dataset, "", "-of", "MEM", ...)              the result in memory
 */
public class Gdal02_ReprojectAndFormats {

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path dir = GdalSetup.folder();
		Path dem = dir.resolve("dem_utm.tif");
		if (!Files.exists(dem)) {
			System.err.println("Please run Gdal01_RasterBasics first.");
			return;
		}

		/* gdalwarp: to WGS84 (longitude/latitude), bilinear */
		Path wgs = dir.resolve("dem_wgs84.tif");
		Gdal.warp(dem.toString(), wgs.toString(), "-t_srs", "EPSG:4326", "-r", "bilinear", "-co", "COMPRESS=DEFLATE");
		try (GdalDataset ds = Gdal.open(wgs.toString())) {
			System.out.println("WGS84: " + ds.width() + " x " + ds.height() + ", extent " + Arrays.toString(ds.bounds()));
		}

		/* gdalwarp in memory: a resolution of 50 m (no file) */
		try (GdalDataset src = Gdal.open(dem.toString()); GdalDataset coarse = Gdal.warp(src, "", "-of", "MEM", "-tr", "50", "50", "-r", "average")) {
			System.out.println("50 m in memory: " + coarse.width() + " x " + coarse.height());
		}

		/* gdal_translate: an 8 bit PNG for the web (scaled from min/max to 0..255) */
		Path png = dir.resolve("dem.png");
		Gdal.translate(dem.toString(), png.toString(), "-of", "PNG", "-ot", "Byte", "-scale");
		System.out.println("PNG: " + png + " (" + Files.size(png) + " bytes)");

		/* Coordinate transformation: a UTM point to longitude/latitude */
		try (SpatialReference utm = SpatialReference.fromEpsg(32632);
				SpatialReference ll = SpatialReference.fromEpsg(4326);
				CoordinateTransform ct = utm.transformTo(ll)) {
			double[] p = ct.transform(502000, 5298500);
			System.out.printf("UTM 502000 / 5298500 -> lon %.5f, lat %.5f%n", p[0], p[1]);
			/* Many points at once */
			double[] x = { 500000, 501000, 502000 }, y = { 5300000, 5299000, 5298000 };
			ct.transform(x, y);
			System.out.println("lon " + Arrays.toString(x) + "\nlat " + Arrays.toString(y));
		}

		/* The formats of this GDAL */
		long raster = Gdal.drivers().stream().filter(GdalDriver::raster).count();
		long vector = Gdal.drivers().stream().filter(GdalDriver::vector).count();
		System.out.println("\nDrivers: " + raster + " raster, " + vector + " vector formats, e.g.:");
		for (GdalDriver d : Gdal.drivers()) {
			if (d.shortName().matches("GTiff|COG|netCDF|HDF5|JP2OpenJPEG|GPKG|ESRI Shapefile|GeoJSON|KML|FlatGeobuf")) {
				System.out.println("  " + d.shortName() + ": " + d.longName() + (d.extensions().isEmpty() ? "" : " (" + d.extensions() + ")"));
			}
		}
	}
}
