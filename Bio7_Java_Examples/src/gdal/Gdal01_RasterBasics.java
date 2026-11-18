package gdal;

import java.nio.file.Path;
import java.util.Arrays;

import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalBand;
import com.eco.bio7.gdal.GdalDataType;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.SpatialReference;

/*
 * Rasters with GDAL: a GeoTIFF (an elevation model in UTM zone 32N) is
 * created, read again, its georeferencing, statistics and the description of
 * gdalinfo are printed.
 *
 *   Gdal.create(file, "GTiff", w, h, bands, type, options)   a new raster
 *   Gdal.open(file)                                          any format of GDAL
 *   ds.band(1).readFloats() / write(...)                     the pixels as Java arrays
 *   ds.geoTransform(), ds.spatialReference()                 the georeferencing
 */
public class Gdal01_RasterBasics {

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path file = GdalSetup.folder().resolve("dem_utm.tif");
		int w = 400, h = 300;

		/* Create: 10 m pixels, the upper left corner at 500000 / 5300000 (UTM 32N) */
		try (GdalDataset ds = Gdal.create(file.toString(), "GTiff", w, h, 1, GdalDataType.FLOAT32, "COMPRESS=DEFLATE", "TILED=YES")) {
			ds.setGeoTransform(new double[] { 500000, 10, 0, 5300000, 0, -10 });
			ds.setProjection("EPSG:32632");
			GdalBand band = ds.band(1);
			band.write(GdalSetup.terrain(w, h));
			band.setNoData(-9999);
		}
		System.out.println("Written: " + file);

		/* Read */
		try (GdalDataset ds = Gdal.open(file.toString())) {
			System.out.println(ds);
			GdalBand band = ds.band(1);
			float[] z = band.readFloats();
			double sum = 0;
			for (float v : z) {
				sum += v;
			}
			System.out.printf("Elevation: mean %.1f m, min/max %s%n", sum / z.length, Arrays.toString(band.minMax(false)));
			System.out.println("Geotransform: " + Arrays.toString(ds.geoTransform()));
			System.out.println("Extent (x/y min, x/y max): " + Arrays.toString(ds.bounds()));
			try (SpatialReference srs = ds.spatialReference()) {
				System.out.println("Coordinate system: " + srs + " (projected: " + srs.isProjected() + ")");
			}
			/* The center of the pixel in row 100, column 200 and back */
			double[] xy = ds.pixelToWorld(200.5, 100.5);
			System.out.println("Pixel (200, 100) -> " + Arrays.toString(xy) + " -> " + Arrays.toString(ds.worldToPixel(xy[0], xy[1])));
			/* A window of 3 x 3 pixels */
			System.out.println("Window: " + Arrays.toString(band.readFloats(200, 100, 3, 3)));
			System.out.println("\n" + ds.info("-stats"));
		}
	}
}
