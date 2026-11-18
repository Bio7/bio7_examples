package gdal.gui;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.gdal.CoordinateTransform;
import com.eco.bio7.gdal.SpatialReference;

import gdal.GdalSetup;

/*
 * A coordinate converter with GDAL (PROJ): a point or a list of points from
 * one coordinate system into another (EPSG codes, WKT, PROJ strings, e.g.
 * WGS84, UTM, ETRS89, Web Mercator, Gauss-Krueger, Swiss LV95), geographic
 * coordinates also in degrees, minutes and seconds, the UTM zone of a point.
 * The axis order is x = longitude / easting, y = latitude / northing.
 *
 * The GUI is in CoordinateConverterGui (WindowBuilder: Open With > WindowBuilder Editor).
 */
public class CoordinateConverter extends com.eco.bio7.compile.Model {

	private CoordinateConverterGui gui;

	public CoordinateConverter() {
		if (!GdalSetup.load()) {
			return;
		}
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Coordinates");
			parent.setLayout(new FillLayout());
			gui = new CoordinateConverterGui(parent, CoordinateConverter.this, SWT.NONE);
			parent.layout(true);
		});
	}

	/** The name and units of a coordinate system (for the GUI), or the error. */
	public String describe(String crs) {
		try (SpatialReference s = SpatialReference.fromUserInput(crs.trim())) {
			return s.name() + (s.isGeographic() ? " (degrees)" : " (meters)");
		} catch (RuntimeException e) {
			return "Unknown: " + e.getMessage();
		}
	}

	/** Converts one point: the result text (with degrees, minutes, seconds for geographic coordinates). */
	public String convert(String from, String to, String x, String y) {
		double[] p = { parse(x), parse(y) };
		try (SpatialReference s = SpatialReference.fromUserInput(from.trim());
				SpatialReference t = SpatialReference.fromUserInput(to.trim());
				CoordinateTransform ct = s.transformTo(t)) {
			double[] r = ct.transform(p[0], p[1]);
			if (t.isGeographic()) {
				return String.format(Locale.ROOT, "x (lon) = %.7f   y (lat) = %.7f%n%s %s", r[0], r[1], dms(r[0], "E", "W"), dms(r[1], "N", "S"));
			}
			return String.format(Locale.ROOT, "x = %.3f   y = %.3f", r[0], r[1]);
		}
	}

	/** Converts a list: one point per line ("x y", "x;y" or "x<tab>y"), the results as lines. */
	public String convertList(String from, String to, String text) {
		StringBuilder out = new StringBuilder();
		try (SpatialReference s = SpatialReference.fromUserInput(from.trim());
				SpatialReference t = SpatialReference.fromUserInput(to.trim());
				CoordinateTransform ct = s.transformTo(t)) {
			String format = t.isGeographic() ? "%.7f;%.7f%n" : "%.3f;%.3f%n";
			for (String line : text.split("\\R")) {
				String[] parts = line.trim().split("[\\s;]+");
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				if (parts.length < 2) {
					out.append("? ").append(line).append('\n');
					continue;
				}
				double[] r = ct.transform(parse(parts[0]), parse(parts[1]));
				out.append(String.format(Locale.ROOT, format, r[0], r[1]));
			}
		}
		return out.toString();
	}

	/** The EPSG code of the UTM zone (WGS84) of a point (converted to longitude/latitude first). */
	public String utmZone(String from, String x, String y) {
		try (SpatialReference s = SpatialReference.fromUserInput(from.trim());
				SpatialReference wgs84 = SpatialReference.fromEpsg(4326);
				CoordinateTransform ct = s.transformTo(wgs84)) {
			double[] ll = ct.transform(parse(x), parse(y));
			int zone = (int) Math.floor((ll[0] + 180) / 6) + 1;
			return "EPSG:" + ((ll[1] >= 0 ? 32600 : 32700) + zone);
		}
	}

	private static double parse(String v) {
		return Double.parseDouble(v.trim().replace(',', '.'));
	}

	/* Degrees, minutes and seconds */
	private static String dms(double v, String positive, String negative) {
		double a = Math.abs(v);
		int d = (int) a;
		int m = (int) ((a - d) * 60);
		double s = ((a - d) * 60 - m) * 60;
		return String.format(Locale.ROOT, "%d°%02d'%05.2f\"%s", d, m, s, v >= 0 ? positive : negative);
	}
}
