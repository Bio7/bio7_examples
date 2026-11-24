package geo.gdal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.FieldType;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.Geometry;
import com.eco.bio7.gdal.GeometryType;
import com.eco.bio7.gdal.VectorLayer;

/*
 * A stratified random sampling design: random points in each plot (polygon,
 * holes excluded), at least 50 m apart, with the elevation and slope at each
 * point. Saved as GeoPackage (UTM) and as CSV with longitude and latitude (for
 * a GPS device or a spreadsheet).
 * Run Gdal01_RasterBasics and Gdal03_Vectors first.
 *
 *   feature.geometry().parts()               the rings of a polygon (outer ring, holes)
 *   Gdal.vectorTranslate(src, dst, "-f", "CSV", "-t_srs", "EPSG:4326", "-lco", "GEOMETRY=AS_XY")
 */
public class Gdal13_SamplingDesign {

	static final int POINTS_PER_PLOT = 12;
	static final double MIN_DISTANCE = 50;

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
		Path out = dir.resolve("samples.gpkg");
		Files.deleteIfExists(out);
		Random random = new Random(42);
		try (GdalDataset ds = Gdal.open(dem.toString());
				GdalDataset slopeDs = Gdal.dem(ds, "", "slope", null, "-of", "MEM", "-compute_edges");
				GdalDataset vectors = Gdal.open(plots.toString());
				GdalDataset samples = Gdal.createVector(out.toString(), "GPKG")) {
			float[] z = ds.band(1).readFloats(), slope = slopeDs.band(1).readFloats();
			VectorLayer layer = samples.createLayer("samples", ds.spatialReference(), GeometryType.POINT);
			layer.createField("id", FieldType.STRING);
			layer.createField("plot", FieldType.STRING);
			layer.createField("elevation", FieldType.REAL);
			layer.createField("slope", FieldType.REAL);

			for (Feature f : vectors.layer(0).features()) {
				Geometry g = f.geometry();
				if (!"POLYGON".equals(g.type())) {
					continue;   // only areas (not the sampling point of Gdal03)
				}
				List<double[][]> rings = g.parts().get(0);
				double[] box = box(rings.get(0));
				List<double[]> chosen = new ArrayList<>();
				int tries = 0;
				while (chosen.size() < POINTS_PER_PLOT && tries++ < 10000) {
					double x = box[0] + random.nextDouble() * (box[2] - box[0]), y = box[1] + random.nextDouble() * (box[3] - box[1]);
					if (!inside(rings, x, y) || chosen.stream().anyMatch(p -> Math.hypot(p[0] - x, p[1] - y) < MIN_DISTANCE)) {
						continue;
					}
					chosen.add(new double[] { x, y });
				}
				String plot = String.valueOf(f.attributes().get("plot"));
				double sumZ = 0;
				for (int i = 0; i < chosen.size(); i++) {
					double[] p = chosen.get(i);
					double[] px = ds.worldToPixel(p[0], p[1]);
					int idx = Math.min(ds.height() - 1, (int) px[1]) * ds.width() + Math.min(ds.width() - 1, (int) px[0]);
					Map<String, Object> a = new LinkedHashMap<>();
					a.put("id", plot.substring(0, 1) + (i + 1));
					a.put("plot", plot);
					a.put("elevation", (double) z[idx]);
					a.put("slope", (double) slope[idx]);
					layer.addFeature(a, "POINT (" + p[0] + " " + p[1] + ")");
					sumZ += z[idx];
				}
				System.out.printf("%-8s %2d points (%d tries), mean elevation %.0f m%n", plot, chosen.size(), tries, sumZ / chosen.size());
			}
			layer.sync();

			/* The points as CSV with longitude and latitude (WGS84) */
			Path csv = dir.resolve("samples_wgs84.csv");
			Files.deleteIfExists(csv);
			try (GdalDataset c = Gdal.vectorTranslate(samples, csv.toString(), "-f", "CSV", "-t_srs", "EPSG:4326", "-lco", "GEOMETRY=AS_XY", "-lco",
					"SEPARATOR=SEMICOLON")) {
				c.flush();
			}
			System.out.println("\nWritten: " + out.getFileName() + " (UTM), " + csv.getFileName() + " (longitude, latitude) in " + dir);
			Files.readAllLines(csv).stream().limit(4).forEach(l -> System.out.println("  " + l));
		}
	}

	/** The bounding box of a ring: minX, minY, maxX, maxY. */
	private static double[] box(double[][] ring) {
		double[] b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
		for (double[] p : ring) {
			b[0] = Math.min(b[0], p[0]);
			b[1] = Math.min(b[1], p[1]);
			b[2] = Math.max(b[2], p[0]);
			b[3] = Math.max(b[3], p[1]);
		}
		return b;
	}

	/** Inside the outer ring and outside of all holes (even-odd rule). */
	private static boolean inside(List<double[][]> rings, double x, double y) {
		boolean in = false;
		for (double[][] ring : rings) {
			for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
				if ((ring[i][1] > y) != (ring[j][1] > y) && x < (ring[j][0] - ring[i][0]) * (y - ring[i][1]) / (ring[j][1] - ring[i][1]) + ring[i][0]) {
					in = !in;
				}
			}
		}
		return in;
	}
}
