package geo.gdal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.eco.bio7.gdal.CoordinateTransform;
import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.Geometry;
import com.eco.bio7.gdal.SpatialReference;
import com.eco.bio7.gdal.VectorLayer;

/*
 * Vectors with GDAL (OGR): the features of a GeoJSON file (study plots and
 * sampling points), their attributes and geometries, the areas of the plots
 * (in UTM), and ogr2ogr to a GeoPackage in UTM.
 *
 *   Gdal.open(file).layers()                 the layers (any vector format)
 *   layer.features() / forEach(...)          attributes, WKT, GeoJSON, coordinates
 *   Gdal.vectorTranslate(src, dest, ...)     ogr2ogr
 */
public class Gdal03_Vectors {

	public static void main(String[] args) throws Exception {
		if (!GdalSetup.load()) {
			return;
		}
		Path dir = GdalSetup.folder();
		Path json = dir.resolve("plots.geojson");
		Files.writeString(json, """
				{"type": "FeatureCollection", "features": [
				 {"type": "Feature", "properties": {"plot": "Forest", "trees": 42, "cover": 0.8},
				  "geometry": {"type": "Polygon", "coordinates": [[[9.000, 47.840], [9.012, 47.840], [9.012, 47.848], [9.000, 47.848], [9.000, 47.840]]]}},
				 {"type": "Feature", "properties": {"plot": "Meadow", "trees": 3, "cover": 0.2},
				  "geometry": {"type": "Polygon", "coordinates": [[[9.015, 47.838], [9.025, 47.838], [9.025, 47.845], [9.015, 47.845], [9.015, 47.838]],
				                                                   [[9.018, 47.840], [9.020, 47.840], [9.020, 47.842], [9.018, 47.842], [9.018, 47.840]]]}},
				 {"type": "Feature", "properties": {"plot": "Sample 1", "trees": null, "cover": null},
				  "geometry": {"type": "Point", "coordinates": [9.005, 47.844]}}
				]}
				""");

		try (GdalDataset ds = Gdal.open(json.toString()); SpatialReference utm = SpatialReference.fromEpsg(32632)) {
			for (VectorLayer layer : ds.layers()) {
				System.out.println("Layer " + layer.name() + ": " + layer.featureCount() + " features, fields " + layer.fieldNames());
				try (SpatialReference srs = layer.spatialReference(); CoordinateTransform toUtm = srs.transformTo(utm)) {
					for (Feature f : layer.features()) {
						Geometry g = f.geometry();
						String size = "";
						if ("POLYGON".equals(g.type())) {
							/* The area in m² (in UTM; minus the holes) */
							double area = 0;
							List<double[][]> rings = g.parts().get(0);
							for (int r = 0; r < rings.size(); r++) {
								area += (r == 0 ? 1 : -1) * Math.abs(area(rings.get(r), toUtm));
							}
							size = String.format(", area %.0f m² (%d holes)", area, rings.size() - 1);
						}
						System.out.println("  " + f.fid() + " " + f.attributes() + " " + g.type() + size);
					}
				}
			}
		}

		/* ogr2ogr: to a GeoPackage in UTM */
		Path gpkg = dir.resolve("plots_utm.gpkg");
		Files.deleteIfExists(gpkg);
		Gdal.vectorTranslate(json.toString(), gpkg.toString(), "-f", "GPKG", "-t_srs", "EPSG:32632", "-nln", "plots");
		try (GdalDataset ds = Gdal.open(gpkg.toString())) {
			VectorLayer layer = ds.layer(0);
			try (SpatialReference srs = layer.spatialReference()) {
				System.out.println("\nGeoPackage: " + layer + " in " + srs);
			}
			System.out.println("First geometry: " + layer.features().get(0).wkt());
			System.out.println("As GeoJSON: " + layer.features().get(2).geoJson());
		}
	}

	/* The area of a ring (shoelace formula) after the transformation into meters */
	private static double area(double[][] ring, CoordinateTransform ct) {
		double[] x = new double[ring.length], y = new double[ring.length];
		for (int i = 0; i < ring.length; i++) {
			x[i] = ring[i][0];
			y[i] = ring[i][1];
		}
		ct.transform(x, y);
		double a = 0;
		for (int i = 0; i < x.length - 1; i++) {
			a += x[i] * y[i + 1] - x[i + 1] * y[i];
		}
		return a / 2;
	}
}
