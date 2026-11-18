package gdal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.eco.bio7.gdal.FieldType;
import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GeometryType;
import com.eco.bio7.gdal.SpatialReference;
import com.eco.bio7.gdal.VectorLayer;

/*
 * Writing vectors, filters and SQL: sampling points of a field survey (random
 * positions and counts) are written to a GeoPackage, selected with an
 * attribute filter and a spatial filter, summarized with SQL and converted to
 * a shapefile.
 *
 *   Gdal.createVector(file, "GPKG")                 a new vector file (or "MEM" in memory)
 *   ds.createLayer(name, srs, GeometryType.POINT)   a layer
 *   layer.createField(name, FieldType.REAL)         the attributes
 *   layer.addFeature(attributes, wkt)               a feature (geometry as WKT)
 *   layer.setAttributeFilter("count > 10")          filters (also setSpatialFilter)
 *   ds.executeSql("SELECT ...")                     SQL (close the result)
 */
public class Gdal07_WriteVectorsAndSQL {

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path dir = GdalSetup.folder();
		Path gpkg = dir.resolve("survey.gpkg");
		Files.deleteIfExists(gpkg);

		/* Write: 60 points in UTM 32N with species, count and a date */
		String[] species = { "Carabus", "Pterostichus", "Abax" };
		Random random = new Random(7);
		try (GdalDataset ds = Gdal.createVector(gpkg.toString(), "GPKG"); SpatialReference utm = SpatialReference.fromEpsg(32632)) {
			VectorLayer points = ds.createLayer("traps", utm, GeometryType.POINT);
			points.createField("species", FieldType.STRING);
			points.createField("count", FieldType.INTEGER);
			points.createField("moisture", FieldType.REAL);
			points.createField("date", FieldType.DATE);
			for (int i = 0; i < 60; i++) {
				double x = 500000 + random.nextDouble() * 4000, y = 5297000 + random.nextDouble() * 3000;
				Map<String, Object> a = new HashMap<>();
				a.put("species", species[random.nextInt(species.length)]);
				a.put("count", random.nextInt(25));
				/* Some traps without a measurement (null) */
				a.put("moisture", i % 10 == 0 ? null : Math.round(random.nextDouble() * 1000) / 10.0);
				a.put("date", "2026-06-" + (10 + random.nextInt(10)));
				points.addFeature(a, "POINT (" + x + " " + y + ")");
			}
			System.out.println("Written: " + points.featureCount() + " points to " + gpkg);
		}

		try (GdalDataset ds = Gdal.open(gpkg.toString())) {
			VectorLayer traps = ds.layer(0);
			/* An attribute filter (SQL WHERE) */
			traps.setAttributeFilter("count >= 20 AND species = 'Carabus'");
			System.out.println("\nCarabus with count >= 20:");
			for (Feature f : traps.features()) {
				System.out.println("  " + f.attributes() + " at " + f.wkt());
			}
			traps.setAttributeFilter(null);
			/* A spatial filter: the western kilometer */
			traps.setSpatialFilter(500000, 5297000, 501000, 5300000);
			System.out.println("\nIn the western kilometer: " + traps.featureCount() + " traps");
			traps.clearSpatialFilter();

			/* SQL: a summary per species */
			try (VectorLayer result = ds.executeSql("SELECT species, COUNT(*) AS traps, SUM(count) AS total, AVG(moisture) AS moisture "
					+ "FROM traps GROUP BY species ORDER BY total DESC")) {
				System.out.println("\nSummary per species (SQL):");
				for (Feature f : result.features()) {
					System.out.println("  " + f.attributes());
				}
			}
		}

		/* ogr2ogr with a filter: the traps with many beetles as shapefile in WGS84 */
		Path shp = dir.resolve("many_beetles.shp");
		Files.deleteIfExists(shp);
		Gdal.vectorTranslate(gpkg.toString(), shp.toString(), "-f", "ESRI Shapefile", "-where", "count >= 15", "-t_srs", "EPSG:4326");
		try (GdalDataset ds = Gdal.open(shp.toString())) {
			System.out.println("\nShapefile: " + ds.layer(0) + ", files " + ds.files().size());
		}
	}
}
