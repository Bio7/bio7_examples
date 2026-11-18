package gdal;

import java.nio.file.Files;
import java.nio.file.Path;

import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataType;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GeometryType;
import com.eco.bio7.gdal.VectorLayer;

/*
 * A habitat model as raster calculation: elevation, slope and aspect (gdaldem)
 * are combined into a suitability score (e.g., a warmth-loving species: south
 * facing, moderately steep, 350 - 700 m), the score is classified, the classes
 * are polygonized and summarized with SQL (the number of patches and their
 * area per class).
 * Run Gdal01_RasterBasics first.
 *
 *   Gdal.dem(ds, "", "aspect", null, "-of", "MEM")    the aspect in memory
 *   band.polygonize(layer, field, useMask)            the classes as polygons
 *   ds.executeSql(sql, "SQLITE")                      SQL with spatial functions
 */
public class Gdal12_HabitatModel {

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
		try (GdalDataset ds = Gdal.open(dem.toString());
				GdalDataset slopeDs = Gdal.dem(ds, "", "slope", null, "-of", "MEM", "-compute_edges");
				GdalDataset aspectDs = Gdal.dem(ds, "", "aspect", null, "-of", "MEM", "-compute_edges", "-zero_for_flat")) {
			float[] z = ds.band(1).readFloats(), slope = slopeDs.band(1).readFloats(), aspect = aspectDs.band(1).readFloats();
			int n = z.length;

			/* The suitability (0..1): the product of three partial scores */
			float[] score = new float[n];
			byte[] cls = new byte[n];
			int[] count = new int[4];
			for (int i = 0; i < n; i++) {
				double south = (1 - Math.cos(Math.toRadians(aspect[i]))) / 2;                 // 0 north, 1 south
				double steep = Math.exp(-Math.pow((slope[i] - 15) / 10, 2));                   // best at 15 degrees
				double elev = z[i] < 350 || z[i] > 700 ? 0.1 : 1 - Math.abs(z[i] - 525) / 350;  // best at 525 m
				score[i] = (float) (south * steep * elev);
				cls[i] = (byte) (score[i] > 0.6 ? 3 : score[i] > 0.35 ? 2 : score[i] > 0.15 ? 1 : 0);
				count[cls[i]]++;
			}
			String[] names = { "unsuitable", "low", "medium", "high" };
			for (int c = 0; c < 4; c++) {
				System.out.printf("Class %d (%s): %.1f %% of the area%n", c, names[c], 100.0 * count[c] / n);
			}

			/* The score and the classes as GeoTIFFs */
			Path scoreFile = dir.resolve("habitat_score.tif"), classFile = dir.resolve("habitat_classes.tif");
			try (GdalDataset s = Gdal.create(scoreFile.toString(), "GTiff", ds.width(), ds.height(), 1, GdalDataType.FLOAT32, "COMPRESS=DEFLATE");
					GdalDataset c = Gdal.create(classFile.toString(), "GTiff", ds.width(), ds.height(), 1, GdalDataType.BYTE, "COMPRESS=DEFLATE")) {
				for (GdalDataset out : new GdalDataset[] { s, c }) {
					out.setGeoTransform(ds.geoTransform());
					out.setProjection(ds.projection());
				}
				s.band(1).write(score);
				c.band(1).write(cls);
				c.band(1).setNoData(0);

				/* The classes 1 - 3 as polygons (0 is no data: the mask) */
				Path patches = dir.resolve("habitat_patches.gpkg");
				Files.deleteIfExists(patches);
				try (GdalDataset v = Gdal.createVector(patches.toString(), "GPKG")) {
					VectorLayer layer = v.createLayer("patches", ds.spatialReference(), GeometryType.POLYGON);
					c.band(1).polygonize(layer, "class", true);
					layer.sync();
					System.out.println("\nPatches: " + layer.featureCount() + " polygons in " + patches.getFileName());

					/* SQL: patches and area per class (SQLite with SpatiaLite; without it the area is computed here) */
					try (VectorLayer r = v.executeSql("SELECT class, COUNT(*) AS patches, SUM(ST_Area(geom)) / 10000 AS area_ha, MAX(ST_Area(geom)) / 10000 AS largest_ha "
							+ "FROM patches GROUP BY class ORDER BY class", "SQLITE")) {
						for (Feature f : r.features()) {
							System.out.printf("  %-7s %3d patches, %7.1f ha, the largest %6.1f ha%n", names[((Number) f.attributes().get("class")).intValue()],
									f.attributes().get("patches"), f.attributes().get("area_ha"), f.attributes().get("largest_ha"));
						}
					} catch (RuntimeException noSpatialite) {
						System.out.println("  (SQL without spatial functions: " + noSpatialite.getMessage() + ")");
						double[] area = new double[4];
						for (Feature f : layer.features()) {
							for (double[][] ring : f.geometry().parts().get(0)) {
								area[((Number) f.attributes().get("class")).intValue()] += ring == f.geometry().parts().get(0).get(0) ? Math.abs(area(ring)) : -Math.abs(area(ring));
							}
						}
						for (int k = 1; k < 4; k++) {
							System.out.printf("  %-7s %7.1f ha%n", names[k], area[k] / 10000);
						}
					}
				}
			}
			System.out.println("\nWritten: " + scoreFile.getFileName() + ", " + classFile.getFileName() + " in " + dir);
		}
	}

	/** The area of a ring (shoelace formula). */
	private static double area(double[][] ring) {
		double a = 0;
		for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
			a += (ring[j][0] + ring[i][0]) * (ring[j][1] - ring[i][1]);
		}
		return a / 2;
	}
}
