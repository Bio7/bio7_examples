package geo.gdal;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.eco.bio7.gdal.FieldType;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GeometryType;
import com.eco.bio7.gdal.SpatialReference;
import com.eco.bio7.gdal.VectorLayer;

/*
 * An elevation profile along a line (e.g., a transect): the elevation is
 * interpolated (bilinear) every 10 m between the points, the ascent, the
 * descent and the steepest part are computed. The profile is plotted with
 * ImageJ (in Bio7), the transect is saved as GeoJSON with its values.
 * Run Gdal01_RasterBasics first.
 *
 *   ds.worldToPixel(x, y)          map coordinates to the pixel position
 *   band.readFloats()              the elevations (row by row)
 */
public class Gdal11_ElevationProfile {

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
			float[] z = ds.band(1).readFloats();
			double[] b = ds.bounds();
			/* The transect: from the southwest to the northeast over the hill and the ridge (UTM, m) */
			double[][] points = { { b[0] + 300, b[1] + 300 }, { b[0] + 1300, b[1] + 2000 }, { b[2] - 300, b[3] - 400 } };

			double step = 10;
			java.util.List<double[]> profile = new java.util.ArrayList<>();   // distance, elevation, x, y
			double distance = 0;
			for (int s = 0; s < points.length - 1; s++) {
				double[] p = points[s], q = points[s + 1];
				double len = Math.hypot(q[0] - p[0], q[1] - p[1]);
				for (double d = 0; d < len || (s == points.length - 2 && d <= len); d += step) {
					double x = p[0] + (q[0] - p[0]) * d / len, y = p[1] + (q[1] - p[1]) * d / len;
					double[] px = ds.worldToPixel(x, y);
					profile.add(new double[] { distance + d, bilinear(z, w, h, px[0] - 0.5, px[1] - 0.5), x, y });
				}
				distance += len;
			}

			double ascent = 0, descent = 0, steepest = 0, steepestAt = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (int i = 0; i < profile.size(); i++) {
				double e = profile.get(i)[1];
				min = Math.min(min, e);
				max = Math.max(max, e);
				if (i > 0) {
					double dz = e - profile.get(i - 1)[1], dd = profile.get(i)[0] - profile.get(i - 1)[0];
					if (dz > 0) {
						ascent += dz;
					} else {
						descent -= dz;
					}
					double slope = Math.toDegrees(Math.atan(Math.abs(dz) / dd));
					if (slope > steepest) {
						steepest = slope;
						steepestAt = profile.get(i)[0];
					}
				}
			}
			System.out.printf("Transect: %.0f m, %d points every %.0f m%n", distance, profile.size(), step);
			System.out.printf("Elevation %.0f - %.0f m, ascent %.0f m, descent %.0f m, steepest %.1f degrees at %.0f m%n", min, max, ascent, descent, steepest, steepestAt);

			/* The transect as GeoJSON line with its values */
			Path out = dir.resolve("transect.geojson");
			Files.deleteIfExists(out);
			try (GdalDataset v = Gdal.createVector(out.toString(), "GeoJSON"); SpatialReference srs = ds.spatialReference()) {
				VectorLayer layer = v.createLayer("transect", srs, GeometryType.LINESTRING);
				layer.createField("length_m", FieldType.REAL);
				layer.createField("ascent_m", FieldType.REAL);
				layer.createField("descent_m", FieldType.REAL);
				StringBuilder wkt = new StringBuilder("LINESTRING (");
				for (int i = 0; i < points.length; i++) {
					wkt.append(i > 0 ? ", " : "").append(points[i][0]).append(' ').append(points[i][1]);
				}
				layer.addFeature(Map.of("length_m", distance, "ascent_m", ascent, "descent_m", descent), wkt.append(')').toString());
			}
			System.out.println("Written: " + out);

			/* The profile plotted with ImageJ (in Bio7) */
			if (!GraphicsEnvironment.isHeadless()) {
				double[] xs = profile.stream().mapToDouble(p -> p[0]).toArray(), ys = profile.stream().mapToDouble(p -> p[1]).toArray();
				ij.gui.Plot plot = new ij.gui.Plot("Elevation profile", "Distance (m)", "Elevation (m)");
				plot.setColor(new java.awt.Color(76, 114, 176));
				plot.setLineWidth(2);
				plot.add("line", xs, ys);
				plot.setLimits(0, distance, min - 20, max + 20);
				plot.show();
			}
		}
	}

	/** The bilinear interpolation at a pixel position (the pixel centers at whole numbers). */
	private static double bilinear(float[] z, int w, int h, double x, double y) {
		x = Math.max(0, Math.min(w - 1.001, x));
		y = Math.max(0, Math.min(h - 1.001, y));
		int x0 = (int) x, y0 = (int) y;
		double fx = x - x0, fy = y - y0;
		double top = z[y0 * w + x0] * (1 - fx) + z[y0 * w + x0 + 1] * fx;
		double bottom = z[(y0 + 1) * w + x0] * (1 - fx) + z[(y0 + 1) * w + x0 + 1] * fx;
		return top * (1 - fy) + bottom * fy;
	}
}
