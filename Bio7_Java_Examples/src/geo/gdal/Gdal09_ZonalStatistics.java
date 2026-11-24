package geo.gdal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.FieldType;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataType;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GeometryType;
import com.eco.bio7.gdal.VectorLayer;

/*
 * Zonal statistics: the elevation and the slope of each plot. The plots are
 * rasterized onto the grid of the elevation model (the value: the feature id),
 * then the pixels of each plot are summarized. The statistics are joined to
 * the plots and saved as a new GeoPackage (plots_stats.gpkg).
 * Run Gdal01_RasterBasics and Gdal03_Vectors first.
 *
 *   Gdal.dem(ds, "", "slope", null, "-of", "MEM")     the slope in memory
 *   Gdal.rasterize(vectors, target, "-sql", ...)       burned into an existing raster
 *   layer.createField / addFeature                     a new layer with the results
 */
public class Gdal09_ZonalStatistics {

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
		try (GdalDataset ds = Gdal.open(dem.toString());
				GdalDataset slopeDs = Gdal.dem(ds, "", "slope", null, "-of", "MEM", "-compute_edges");
				GdalDataset zones = Gdal.create("", "MEM", ds.width(), ds.height(), 1, GdalDataType.INT32);
				GdalDataset vectors = Gdal.open(plots.toString())) {
			float[] z = ds.band(1).readFloats();
			float[] slope = slopeDs.band(1).readFloats();

			/* The zones: the same grid as the elevation model, 0 outside of the plots */
			zones.setGeoTransform(ds.geoTransform());
			zones.setProjection(ds.projection());
			Gdal.rasterize(vectors, zones, "-sql", "SELECT FID AS zone FROM plots", "-dialect", "OGRSQL", "-a", "zone", "-at");
			int[] zone = zones.band(1).readInts();

			/* The statistics of each zone */
			double[] gt = ds.geoTransform();
			double pixelArea = Math.abs(gt[1] * gt[5]);
			Map<Integer, double[]> stats = new LinkedHashMap<>();   // count, sum z, min z, max z, sum slope
			for (int i = 0; i < z.length; i++) {
				if (zone[i] == 0) {
					continue;
				}
				double[] s = stats.computeIfAbsent(zone[i], k -> new double[] { 0, 0, Double.MAX_VALUE, -Double.MAX_VALUE, 0 });
				s[0]++;
				s[1] += z[i];
				s[2] = Math.min(s[2], z[i]);
				s[3] = Math.max(s[3], z[i]);
				s[4] += slope[i];
			}

			/* The plots with their statistics as new GeoPackage */
			Path out = dir.resolve("plots_stats.gpkg");
			Files.deleteIfExists(out);
			try (GdalDataset result = Gdal.createVector(out.toString(), "GPKG")) {
				VectorLayer source = vectors.layer(0);
				VectorLayer target = result.createLayer("plots_stats", source.spatialReference(), GeometryType.UNKNOWN);
				target.createField("plot", FieldType.STRING);
				target.createField("pixels", FieldType.INTEGER);
				target.createField("area_ha", FieldType.REAL);
				target.createField("elev_mean", FieldType.REAL);
				target.createField("elev_min", FieldType.REAL);
				target.createField("elev_max", FieldType.REAL);
				target.createField("slope_mean", FieldType.REAL);
				System.out.printf("%-10s %7s %8s %9s %9s %9s %10s%n", "Plot", "Pixels", "Area ha", "Mean m", "Min m", "Max m", "Slope deg");
				for (Feature f : source.features()) {
					double[] s = stats.get((int) f.fid());
					Map<String, Object> a = new LinkedHashMap<>();
					a.put("plot", f.attributes().get("plot"));
					if (s != null) {
						a.put("pixels", (long) s[0]);
						a.put("area_ha", s[0] * pixelArea / 10000);
						a.put("elev_mean", s[1] / s[0]);
						a.put("elev_min", s[2]);
						a.put("elev_max", s[3]);
						a.put("slope_mean", s[4] / s[0]);
						System.out.printf("%-10s %7d %8.2f %9.1f %9.1f %9.1f %10.1f%n", a.get("plot"), (long) s[0], s[0] * pixelArea / 10000, s[1] / s[0], s[2], s[3],
								s[4] / s[0]);
					} else {
						System.out.printf("%-10s (outside of the elevation model)%n", a.get("plot"));
					}
					target.addFeature(a, f.wkt());
				}
				System.out.println("\nWritten: " + out + " (open it with File > GDAL > Open Vector, or in QGIS)");
			}
		}
	}
}
