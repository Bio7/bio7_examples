package geo.gdal;

import java.nio.file.Files;
import java.nio.file.Path;

import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalBand;
import com.eco.bio7.gdal.GdalDataType;
import com.eco.bio7.gdal.GdalDataset;

/*
 * Raster analysis in Java: slope (degrees) and hillshade of the elevation
 * model, written as GeoTIFFs with the georeferencing of the source (they fit
 * exactly on top of each other in a GIS, ImageJ or WorldWind).
 * Run Gdal01_RasterBasics first (it creates dem_utm.tif).
 */
public class Gdal04_TerrainAnalysis {

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
		try (GdalDataset ds = Gdal.open(dem.toString())) {
			int w = ds.width(), h = ds.height();
			double[] gt = ds.geoTransform();
			double dx = gt[1], dy = Math.abs(gt[5]);
			float[] z = ds.band(1).readFloats();
			float[] slope = new float[w * h], shade = new float[w * h];
			/*
			 * The sun from the northwest (compass azimuth 315 degrees), 45 degrees above
			 * the horizon. The aspect below is a math angle (counterclockwise from east):
			 * the azimuth is converted to it (360 - azimuth + 90).
			 */
			double azimuth = Math.toRadians(360 - 315 + 90), zenith = Math.toRadians(90 - 45);
			for (int y = 1; y < h - 1; y++) {
				for (int x = 1; x < w - 1; x++) {
					/* The gradient (Horn's method, 3 x 3 neighbours) */
					double a = z[(y - 1) * w + x - 1], b = z[(y - 1) * w + x], c = z[(y - 1) * w + x + 1];
					double d = z[y * w + x - 1], f = z[y * w + x + 1];
					double g = z[(y + 1) * w + x - 1], hh = z[(y + 1) * w + x], i = z[(y + 1) * w + x + 1];
					double gx = ((c + 2 * f + i) - (a + 2 * d + g)) / (8 * dx);
					double gy = ((g + 2 * hh + i) - (a + 2 * b + c)) / (8 * dy);
					double s = Math.atan(Math.hypot(gx, gy));
					double aspect = Math.atan2(gy, -gx);
					slope[y * w + x] = (float) Math.toDegrees(s);
					/* The hillshade (as gdaldem): 255 * (cos(zenith) cos(slope) + sin(zenith) sin(slope) cos(azimuth - aspect)) */
					shade[y * w + x] = (float) Math.max(0, 255 * (Math.cos(zenith) * Math.cos(s) + Math.sin(zenith) * Math.sin(s) * Math.cos(azimuth - aspect)));
				}
			}
			write(dir.resolve("slope.tif"), ds, slope, GdalDataType.FLOAT32);
			write(dir.resolve("hillshade.tif"), ds, shade, GdalDataType.BYTE);
			float max = 0;
			for (float s : slope) {
				max = Math.max(max, s);
			}
			System.out.printf("Slope: maximum %.1f degrees%nWritten: %s, %s%n", max, dir.resolve("slope.tif"), dir.resolve("hillshade.tif"));

			/* Check: the hillshade of GDAL (gdaldem, Horn's method, the same sun) */
			try (GdalDataset gdalShade = Gdal.dem(ds, "", "hillshade", null, "-of", "MEM", "-az", "315", "-alt", "45")) {
				float[] ref = gdalShade.band(1).readFloats();
				double diff = 0;
				int n = 0;
				for (int y = 1; y < h - 1; y++) {
					for (int x = 1; x < w - 1; x++) {
						diff += Math.abs(shade[y * w + x] - ref[y * w + x]);
						n++;
					}
				}
				System.out.printf("Mean difference to gdaldem hillshade: %.2f (of 255)%n", diff / n);
			}
		}
	}

	/* A GeoTIFF with the georeferencing of the source */
	private static void write(Path file, GdalDataset like, float[] values, GdalDataType type) {
		try (GdalDataset out = Gdal.create(file.toString(), "GTiff", like.width(), like.height(), 1, type, "COMPRESS=DEFLATE")) {
			out.setGeoTransform(like.geoTransform());
			out.setProjection(like.projection());
			GdalBand band = out.band(1);
			if (type == GdalDataType.BYTE) {
				byte[] b = new byte[values.length];
				for (int i = 0; i < b.length; i++) {
					b[i] = (byte) Math.round(values[i]);
				}
				band.write(b);
			} else {
				band.write(values);
			}
		}
	}
}
