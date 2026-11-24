package geo.gdal.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.geo.GdalImageJ;
import com.eco.bio7.geo.GdalWorldWind;

import geo.gdal.GdalSetup;
import gui.swt.BackgroundJob;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.FloatPolygon;

/*
 * Georeferencing of an image (a scanned map, an aerial photo, a drawing) with
 * ground control points (GCPs): points of the image (selected in ImageJ with
 * the point tool) with their map coordinates. GDAL transforms the image into
 * the coordinate system (polynomial 1 = affine, polynomial 2, thin plate
 * spline) and writes a GeoTIFF, shown in ImageJ (with its georeference) and in
 * WorldWind. The residuals of an affine fit show wrong points.
 *
 * "Create test scan": a rotated, scaled "scan" of the elevation model of the
 * GDAL examples (UTM 32N) with 6 control points, georeferenced back.
 *
 * The GUI is in GeoreferenceImageGui (WindowBuilder: Open With > WindowBuilder Editor).
 */
public class GeoreferenceImage extends com.eco.bio7.compile.Model {

	/** A control point: the position in the image (pixels, the upper left corner is 0, 0) and on the map. */
	public record Gcp(double pixel, double line, double x, double y) {
	}

	private GeoreferenceImageGui gui;
	private final List<Gcp> points = new ArrayList<>();
	private Path result;

	public GeoreferenceImage() {
		CustomView view = new CustomView();
		Display.getDefault().syncExec(() -> {
			Composite parent = view.getComposite("Georeference");
			parent.setLayout(new FillLayout());
			gui = new GeoreferenceImageGui(parent, GeoreferenceImage.this, SWT.NONE);
			parent.layout(true);
		});
	}

	public List<Gcp> points() {
		return points;
	}

	/** The point selection of the current ImageJ image with the map coordinates as control point. */
	public void addPoint(String x, String y) {
		ImagePlus imp = WindowManager.getCurrentImage();
		Roi roi = imp == null ? null : imp.getRoi();
		if (roi == null || roi.getType() != Roi.POINT) {
			gui.setStatus("Please click a point in the image with the point tool of ImageJ");
			return;
		}
		double mx, my;
		try {
			mx = Double.parseDouble(x.trim().replace(',', '.'));
			my = Double.parseDouble(y.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			gui.setStatus("Please enter the map coordinates X and Y of the point");
			return;
		}
		FloatPolygon p = roi.getFloatPolygon();
		/* ImageJ: the point at the pixel (its upper left corner); GDAL: the center of the pixel is + 0.5 */
		double offset = roi.subPixelResolution() ? 0 : 0.5;
		points.add(new Gcp(p.xpoints[0] + offset, p.ypoints[0] + offset, mx, my));
		imp.deleteRoi();
		update();
	}

	public void removePoint(int index) {
		if (index >= 0 && index < points.size()) {
			points.remove(index);
			update();
		}
	}

	public void clearPoints() {
		points.clear();
		update();
	}

	private void update() {
		double[] residuals = residuals();
		gui.setPoints(points, residuals);
		if (residuals != null) {
			double rms = 0;
			for (double r : residuals) {
				rms += r * r;
			}
			gui.setStatus(String.format(Locale.ROOT, "%d points, RMS error of an affine fit: %.2f map units", points.size(), Math.sqrt(rms / residuals.length)));
		} else {
			gui.setStatus(points.size() + " points (at least 3 for an affine fit)");
		}
	}

	/* The distances of the points to an affine fit (least squares), null with less than 3 points */
	private double[] residuals() {
		int n = points.size();
		if (n < 3) {
			return null;
		}
		double[][] a = new double[3][3];
		double[] bx = new double[3], by = new double[3];
		for (Gcp g : points) {
			double[] v = { 1, g.pixel(), g.line() };
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					a[i][j] += v[i] * v[j];
				}
				bx[i] += v[i] * g.x();
				by[i] += v[i] * g.y();
			}
		}
		double[] cx = solve(a, bx), cy = solve(a, by);
		if (cx == null || cy == null) {
			return null;
		}
		double[] r = new double[n];
		for (int k = 0; k < n; k++) {
			Gcp g = points.get(k);
			r[k] = Math.hypot(cx[0] + cx[1] * g.pixel() + cx[2] * g.line() - g.x(), cy[0] + cy[1] * g.pixel() + cy[2] * g.line() - g.y());
		}
		return r;
	}

	/* A 3 x 3 system (Cramer's rule), null if singular (the points on a line) */
	private static double[] solve(double[][] m, double[] b) {
		double d = det(m);
		if (Math.abs(d) < 1e-9) {
			return null;
		}
		double[] x = new double[3];
		for (int c = 0; c < 3; c++) {
			double[][] t = { m[0].clone(), m[1].clone(), m[2].clone() };
			for (int r = 0; r < 3; r++) {
				t[r][c] = b[r];
			}
			x[c] = det(t) / d;
		}
		return x;
	}

	private static double det(double[][] m) {
		return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
	}

	/**
	 * A test "scan": the shaded elevation model of the examples (400 x 300 pixels
	 * of 10 m, upper left corner 500000 / 5300000, UTM 32N) rotated by 7 degrees,
	 * scaled and shifted on a sheet of paper, with 6 control points (as if
	 * clicked, with an error of about a third of a pixel).
	 */
	public void createTestScan() {
		gui.setBusy(true);
		BackgroundJob.run("Test scan", () -> {
			List<Gcp> test = new ArrayList<>();
			BufferedImage scan = testScan(test);
			ImagePlus imp = new ImagePlus("Scanned map (test)", scan);
			Display.getDefault().syncExec(imp::show);
			Display.getDefault().syncExec(() -> {
				points.clear();
				points.addAll(test);
				gui.setCrs("EPSG:32632");
				update();
			});
			return "Test scan with 6 control points (UTM 32N): press Georeference";
		}, this::done);
	}

	/** The test scan (an image) and its control points (added to gcps). */
	public static BufferedImage testScan(List<Gcp> gcps) {
		int w = 400, h = 300;
		float[] z = GdalSetup.terrain(w, h);
		BufferedImage map = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				/* A simple shading (light from the northwest) */
				double dzdx = (z[y * w + x + 1] - z[y * w + x - 1]) / 20, dzdy = (z[(y + 1) * w + x] - z[(y - 1) * w + x]) / 20;
				int v = (int) Math.max(0, Math.min(255, 170 + 120 * (dzdx + dzdy)));
				map.getRaster().setSample(x, y, 0, v);
			}
		}
		AffineTransform t = new AffineTransform();
		t.translate(90, 30);
		t.rotate(Math.toRadians(7));
		t.scale(1.15, 1.15);
		BufferedImage scan = new BufferedImage(600, 470, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = scan.createGraphics();
		g.setColor(new Color(245, 245, 245));
		g.fillRect(0, 0, scan.getWidth(), scan.getHeight());
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(map, t, null);
		g.setColor(Color.DARK_GRAY);
		g.drawString("Scanned map (test) - UTM 32N, the control points are entered", 12, scan.getHeight() - 12);
		g.dispose();
		/* 6 control points: map pixels -> scan pixels (with a small error), map pixels -> UTM */
		double[][] at = { { 30, 25 }, { 370, 30 }, { 25, 280 }, { 380, 275 }, { 200, 150 }, { 120, 210 } };
		Random random = new Random(7);
		for (double[] p : at) {
			Point2D s = t.transform(new Point2D.Double(p[0], p[1]), null);
			gcps.add(new Gcp(round(s.getX() + random.nextGaussian() * 0.3), round(s.getY() + random.nextGaussian() * 0.3), 500000 + p[0] * 10, 5300000 - p[1] * 10));
		}
		return scan;
	}

	private static double round(double v) {
		return Math.round(v * 10) / 10.0;
	}

	/** Writes the georeferenced GeoTIFF of the current ImageJ image and opens it in ImageJ. */
	public void georeference(String crs, int method) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			gui.setStatus("Please open the image in ImageJ");
			return;
		}
		int needed = method == 1 ? 6 : 3;
		if (points.size() < needed) {
			gui.setStatus("At least " + needed + " control points are needed for this method");
			return;
		}
		List<Gcp> gcps = List.copyOf(points);
		ImagePlus copy = imp.duplicate();
		copy.setTitle(imp.getTitle());
		gui.setBusy(true);
		BackgroundJob.run("Georeference", () -> {
			if (!GdalSetup.load()) {
				return "Error: GDAL could not be loaded (Preferences GDAL)";
			}
			Path dir = GdalSetup.folder();
			Path plain = dir.resolve("georef_source.tif"), withGcps = dir.resolve("georef_gcps.vrt");
			String name = imp.getTitle().replaceAll("\\.[A-Za-z0-9]+$", "").replaceAll("[^A-Za-z0-9_-]+", "_");
			Path out = dir.resolve(name + "_georeferenced.tif");
			if (copy.getType() == ImagePlus.COLOR_256) {
				copy.setProcessor(copy.getProcessor().convertToRGB());
			}
			new FileSaver(copy).saveAsTiff(plain.toString());

			write(plain, withGcps, out, gcps, crs, method);
			result = out;
			String summary;
			try (GdalDataset ds = Gdal.open(out.toString())) {
				double[] gt = ds.geoTransform(), b = ds.bounds();
				summary = String.format(Locale.ROOT, "%s: %d x %d, pixel %.2f, extent %.0f / %.0f - %.0f / %.0f", out.getFileName(), ds.width(), ds.height(), gt[1], b[0], b[1], b[2], b[3]);
			}
			ImagePlus georef = GdalImageJ.open(out);
			Display.getDefault().syncExec(georef::show);
			System.out.println("Georeferenced: " + out + "\n" + summary);
			return summary;
		}, this::done);
	}

	/**
	 * Georeferences an image file with control points: a VRT with the control
	 * points (gdal_translate), transformed with gdalwarp into a GeoTIFF. method: 0
	 * polynomial 1, 1 polynomial 2, 2 thin plate spline.
	 */
	public static void write(Path image, Path withGcps, Path out, List<Gcp> gcps, String crs, int method) throws Exception {
		/* gdal_translate: the image with the control points and the coordinate system */
		List<String> opts = new ArrayList<>(List.of("-of", "VRT", "-a_srs", crs));
		for (Gcp g : gcps) {
			opts.addAll(List.of("-gcp", fmt(g.pixel()), fmt(g.line()), fmt(g.x()), fmt(g.y())));
		}
		Gdal.translate(image.toString(), withGcps.toString(), opts.toArray(String[]::new));

		/* gdalwarp: the image transformed onto the map grid */
		List<String> warp = new ArrayList<>(List.of("-overwrite", "-r", "bilinear", "-dstalpha", "-co", "COMPRESS=DEFLATE"));
		warp.addAll(switch (method) {
		case 1 -> List.of("-order", "2");
		case 2 -> List.of("-tps");
		default -> List.of("-order", "1");
		});
		Files.deleteIfExists(out);
		Gdal.warp(withGcps.toString(), out.toString(), warp.toArray(String[]::new));
	}

	/** The last result as layer in WorldWind (the WorldWind perspective opens). */
	public void showInWorldWind() {
		if (result == null) {
			gui.setStatus("Please georeference first");
			return;
		}
		Path file = result;
		IJ.showStatus("WorldWind ...");
		BackgroundJob.run("WorldWind", () -> {
			GdalWorldWind.addRaster(file);
			return "Shown in WorldWind: " + file.getFileName();
		}, this::done);
	}

	private void done(String status) {
		if (!gui.isDisposed()) {
			gui.setStatus(status);
			gui.setBusy(false);
		}
	}

	private static String fmt(double v) {
		return String.format(Locale.ROOT, "%.4f", v);
	}
}
