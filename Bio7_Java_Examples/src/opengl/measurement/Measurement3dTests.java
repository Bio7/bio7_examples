package opengl.measurement;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.Clipping;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.Measurement3d;
import com.eco.bio7.spatial.SpatialUtil;
import com.eco.bio7.spatial.ValueSelection;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.DoubleProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Regression tests of the measurements of the 3D perspective (Measure tab of
 * the Options Space view): the iso surface objects, the smoothing, the reduced
 * display, the resolution, the distances, Measure Region, the clipping mask,
 * the cut mesh, the picked points (ruler), imported models and RGB stacks.
 * <p>
 * The test stack (spheres, an ellipsoid, a torus and a hollow sphere with
 * analytically known values) is created here. The reference values were
 * computed with Python (trimesh, SciPy, scikit-image, FCL) on the meshes of
 * Bio7 (see the validation report "Validation of the Bio7 3D Measurements"); Bio7
 * and Python agree to 1e-13, the tests allow 1e-9. If the surface extraction is
 * changed on purpose, these references must be computed again.
 * <p>
 * The mouse tools (lines, angle, along the surface, spline, polygon, volume,
 * cavity, adding and deleting points) are tested with the positions of the
 * current view: the 3D view must be visible (not minimized).
 * <p>
 * Run it in Bio7 (the 3D perspective is opened). The report is printed to the
 * console: one line for each check and the number of passed and failed checks.
 */
public class Measurement3dTests {

	static final double GOLDEN = 1e-9;
	static int passed, failed;
	static final List<String> failures = new ArrayList<>();

	public static void main(String[] args) {
		/* In the background: the iso surface is computed in another thread */
		new Thread(Measurement3dTests::runAll, "Bio7 3D Measurement Tests").start();
	}

	static void runAll() {
		passed = failed = 0;
		failures.clear();
		long t0 = System.currentTimeMillis();
		Work.openPerspective("com.eco.bio7.perspective_3d");
		sleep(2000);
		IsoSurface iso = SpatialUtil.getIsoSurface();
		Measurement3d m = SpatialUtil.getMeasurement();
		Clipping clip = SpatialUtil.getClipping();
		if (iso == null || m == null || clip == null) {
			System.out.println("The 3D view is not available!");
			return;
		}
		System.out.println("Bio7 3D measurement tests");
		ImagePlus spheres = stack(1.0);
		reset(iso, m, clip);
		run("Iso surface objects (validated with Python)", () -> isoObjects(iso, m, spheres));
		run("Analytic values of the test objects", () -> analytic(iso, m, spheres));
		run("Mesh smoothing 0 (validated with Python)", () -> meshSmoothing0(iso, m, spheres));
		run("Anisotropic voxels (validated with Python)", () -> anisotropic(iso, m));
		run("Gaussian smoothing sigma = 1 (validated with SciPy)", () -> gaussian(iso, m, spheres));
		run("Reduced display (Max Size): the same measurements", () -> reduced(iso, m, spheres));
		run("Resolution 2x (cubic upsampling): small objects", () -> resolution(iso, m, spheres));
		run("Distances (validated with FCL)", () -> distances(iso, m, spheres));
		run("Measure Region (validated with NumPy)", () -> region(iso, m, clip, spheres));
		run("Clipping mask (a selection)", () -> mask(iso, m, clip, spheres));
		run("Cut iso surface mesh", () -> cutMesh(iso, m, clip, spheres));
		run("Picked points (ruler)", () -> picks(iso, m, clip, spheres));
		run("Imported model (OBJ)", () -> model(iso, m));
		run("RGB stack (luminance)", () -> rgb(iso, m));
		run("Settings of the measurement", () -> settings(iso, m, spheres));
		run("Decimals of the tables", () -> decimals(m));
		run("64-bit stacks (offsets and tiny contrasts)", () -> gray64(iso, m));
		run("Values: threshold of the image and labels (validated with Python)", () -> valueSelection(iso, m, spheres));
		run("Mouse tools: lines, angle, along the surface, spline", () -> mouseLines(iso, m, clip, spheres));
		run("Mouse tools: polygon, freehand, volume, cavity (validated with Python)", () -> mouseAreas(iso, m, clip, spheres));
		run("Volume tool: object filled / material, cavity whole / free space", () -> volumeOptions(iso, m, clip));
		reset(iso, m, clip);
		System.out.println(String.format(Locale.ROOT, "%n%d checks passed, %d failed (%.1f s)", passed, failed,
				(System.currentTimeMillis() - t0) / 1000.0));
		for (String f : failures) {
			System.out.println("  FAILED: " + f);
		}
	}

	/* ------------------------------------------------------------------ the tests */

	static void isoObjects(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		double[][] r = m.measureIsoSurface(false);
		equal("objects", r.length, 8);
		compareRows("objects", r, PYTHON_SPHERES, GOLDEN);
	}

	static void analytic(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		double[][] r = m.measureIsoSurface(false);
		/* {x, y, z, radius / semi-axes, exact volume, holes, max. volume difference} */
		Object[][] objects = { { new double[] { 28, 30, 24 }, "sphere r = 3", 4 / 3.0 * Math.PI * 27, 0, 0.09 },
				{ new double[] { 60, 28, 24 }, "sphere r = 5", 4 / 3.0 * Math.PI * 125, 0, 0.035 },
				{ new double[] { 100, 32, 30 }, "sphere r = 8", 4 / 3.0 * Math.PI * 512, 0, 0.012 },
				{ new double[] { 128, 120, 40 }, "sphere r = 12", 4 / 3.0 * Math.PI * 1728, 0, 0.006 },
				{ new double[] { 40, 110, 50 }, "sphere r = 18", 4 / 3.0 * Math.PI * 5832, 0, 0.003 },
				{ new double[] { 100, 80, 60 }, "ellipsoid 20 x 12 x 7", 4 / 3.0 * Math.PI * 20 * 12 * 7, 0, 0.007 },
				{ new double[] { 125, 45, 66 }, "torus R = 14, r = 5", 2 * Math.PI * Math.PI * 14 * 25, 1, 0.01 },
				{ new double[] { 62, 132, 72 }, "hollow sphere 10 / 5 (one object)", 4 / 3.0 * Math.PI * (1000 - 125), 0, 0.006 } };
		for (Object[] o : objects) {
			double[] c = (double[]) o[0];
			double[] row = nearest(r, c);
			String name = (String) o[1];
			check(name + ": centroid", dist(row, c) < 0.01, String.format(Locale.ROOT, "offset %.4f", dist(row, c)));
			double dv = row[0] / (double) o[2] - 1;
			check(name + ": volume", dv < 0 && -dv < (double) o[4], String.format(Locale.ROOT, "%+.2f %%", 100 * dv));
			equal(name + ": holes", row[12], ((Integer) o[3]).doubleValue());
		}
	}

	static void meshSmoothing0(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 0);
		compareRows("objects", m.measureIsoSurface(false), PYTHON_SPHERES_MS0, GOLDEN);
	}

	static void anisotropic(IsoSurface iso, Measurement3d m) {
		ImagePlus imp = stack(2.0);
		load(iso, imp, 127.5, 0, 5);
		compareRows("objects", m.measureIsoSurface(false), PYTHON_SPHERES_ANISO, GOLDEN);
	}

	static void gaussian(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		load(iso, imp, 127.5, 1, 5);
		compareRows("objects", m.measureIsoSurface(false), PYTHON_GAUSS1, GOLDEN);
	}

	static void reduced(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		iso.setReduceDisplay(true);
		iso.setMaxSize(64);
		try {
			load(iso, imp, 127.5, 0, 5);
			check("display reduced", iso.isReduced(), "");
			compareRows("objects", m.measureIsoSurface(false), PYTHON_SPHERES, GOLDEN);
		} finally {
			iso.setReduceDisplay(false);
		}
	}

	static void resolution(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		double v1 = nearest(m.measureIsoSurface(false), new double[] { 28, 30, 24 })[0];
		iso.setMeasureResolution(2);
		try {
			double[][] r = m.measureIsoSurface(false);
			equal("objects", r.length, 8);
			double exact = 4 / 3.0 * Math.PI * 27;
			double v2 = nearest(r, new double[] { 28, 30, 24 })[0];
			check("sphere r = 3: volume closer to the exact value", Math.abs(v2 / exact - 1) < Math.abs(v1 / exact - 1),
					String.format(Locale.ROOT, "1x %+.2f %%, 2x %+.2f %%", 100 * (v1 / exact - 1), 100 * (v2 / exact - 1)));
			check("sphere r = 3: volume within 4 %", Math.abs(v2 / exact - 1) < 0.04, "");
			double[] big = nearest(r, new double[] { 40, 110, 50 });
			check("sphere r = 18: volume within 0.3 %", Math.abs(big[0] / (4 / 3.0 * Math.PI * 5832) - 1) < 0.003, "");
		} finally {
			iso.setMeasureResolution(1);
		}
	}

	static void distances(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		File folder = tempFolder();
		m.setMatrixFolder(folder);
		double[][][] d;
		try {
			d = m.measureDistances(false);
		} finally {
			m.setMatrixFolder(null);
		}
		check("measureDistances(boolean) returns a result", d != null, "");
		if (d == null) {
			return;
		}
		compareRows("nearest neighbors", d[0], FCL_NEAREST, GOLDEN);
		/* The matrices are symmetric, the nearest surface distance is the minimum of the matrix */
		double maxAsym = 0, maxNearest = 0;
		for (int a = 0; a < d[2].length; a++) {
			double min = Double.MAX_VALUE;
			for (int b = 0; b < d[2].length; b++) {
				maxAsym = Math.max(maxAsym, Math.abs(d[2][a][b] - d[2][b][a]));
				if (a != b) {
					min = Math.min(min, d[2][a][b]);
				}
			}
			maxNearest = Math.max(maxNearest, Math.abs(min - d[0][a][3]));
		}
		check("surface matrix symmetric", maxAsym == 0, "");
		check("nearest surface distance = minimum of the matrix", maxNearest == 0, "");
		File[] files = folder.listFiles();
		check("two CSV files of the matrices", files != null && files.length == 2, "");
		deleteFolder(folder);
	}

	static void region(IsoSurface iso, Measurement3d m, Clipping clip, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		double[][] boxes = { null, { 0.2, 0.7, 0.1, 0.6, 0.3, 0.9 }, null, null, { 0.1, 0.8, 0.25, 0.95, 0.0, 0.6 } };
		double[][] planes = { null, null, { 30, 20, 0.1 }, { 30, 20, 0.1 }, { -120, -35, -0.2 } };
		boolean[] flips = { false, false, false, true, false };
		String[] names = { "no clipping", "box", "plane", "plane, flipped", "box and plane" };
		try {
			for (int i = 0; i < names.length; i++) {
				clipOff(clip);
				clip.setFlip(flips[i]);
				if (boxes[i] != null) {
					double[] b = boxes[i];
					clip.setBox(b[0], b[1], b[2], b[3], b[4], b[5]);
				}
				if (planes[i] != null) {
					clip.setPlane(planes[i][0], planes[i][1], planes[i][2]);
				}
				compareRow(names[i], m.measureRegion(false), NUMPY_REGION[i], 1e-12);
			}
		} finally {
			clipOff(clip);
		}
	}

	static void mask(IsoSurface iso, Measurement3d m, Clipping clip, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		double v3 = nearest(m.measureIsoSurface(false), new double[] { 28, 30, 24 })[0];
		try {
			imp.setRoi(new Roi(49, 0, 111, 160));
			clip.cutRoi(imp, true, 1, Integer.MAX_VALUE);
			imp.deleteRoi();
			waitFor(iso);
			equal("removed voxels", clip.getMask().getRemovedCount(), 111 * 160 * 96);
			equal("region: voxels", m.measureRegion(false)[0], 49 * 160 * 96);
			double[][] r = m.measureIsoSurface(false);
			equal("objects", r.length, 2);
			double[] small = nearest(r, new double[] { 28, 30, 24 });
			check("the uncut sphere r = 3 is not changed by the mask", Math.abs(small[0] / v3 - 1) < GOLDEN,
					String.format(Locale.ROOT, "%.6f / %.6f", small[0], v3));
			double cut = 4 / 3.0 * Math.PI * 5832 - Math.PI * 81 * (54 - 9) / 3;
			double[] big = nearest(r, new double[] { 38.5, 110, 50 });
			check("sphere r = 18 cut at x = 49: volume within 0.6 %", Math.abs(big[0] / cut - 1) < 0.006,
					String.format(Locale.ROOT, "%+.2f %%", 100 * (big[0] / cut - 1)));
		} finally {
			clip.clearMask();
			waitFor(iso);
		}
	}

	static void cutMesh(IsoSurface iso, Measurement3d m, Clipping clip, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		try {
			clip.setCutMesh(true);
			clip.setPlane(0, 90, -0.1708);
			waitFor(iso);
			double[][] r = m.measureIsoSurface(false);
			equal("objects", r.length, 3);
			/* The sphere r = 18 without a cap of the height 9 (closed with the cut disk) */
			double v = 4 / 3.0 * Math.PI * 5832 - Math.PI * 81 * (54 - 9) / 3;
			double a = 4 * Math.PI * 324 - 2 * Math.PI * 18 * 9 + Math.PI * (324 - 81);
			double[] row = nearest(r, new double[] { 40, 111.5, 50 });
			check("cut sphere: volume within 0.5 %", Math.abs(row[0] / v - 1) < 0.005, String.format(Locale.ROOT, "%+.2f %%", 100 * (row[0] / v - 1)));
			check("cut sphere: area within 1 %", Math.abs(row[1] / a - 1) < 0.01, String.format(Locale.ROOT, "%+.2f %%", 100 * (row[1] / a - 1)));
		} finally {
			clipOff(clip);
			waitFor(iso);
		}
	}

	static void picks(IsoSurface iso, Measurement3d m, Clipping clip, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		iso.setShow(true);
		m.measureIsoSurface(false);
		m.clearPoints();
		try {
			/* Rays along z (relative positions): the first surfaces */
			check("hit: sphere r = 18", m.pick(rel(40, 110, 0), rel(40, 110, 96)), "");
			check("hit: sphere r = 12", m.pick(rel(128, 120, 0), rel(128, 120, 96)), "");
			check("no hit: the hole of the torus", !m.pick(rel(125, 45, 0), rel(125, 45, 96)), "");
			check("hit: ellipsoid from below", m.pick(rel(100, 80, 96), rel(100, 80, 0)), "");
			clip.setBox(0, 1, 0, 1, 0.4, 1);
			check("hit with a clipping box: the back of the sphere r = 18", m.pick(rel(40, 110, 0), rel(40, 110, 96)), "");
			List<double[]> p = m.getPoints();
			equal("points", p.size(), 4);
			double[] exact = { 32, 28, 67, 68 };
			for (int i = 0; i < p.size() && i < 4; i++) {
				/* The points are calibrated (x, y, z in micron, value, object, distance, length) */
				double z = p.get(i)[2];
				check("point " + (i + 1) + ": z within 0.05 micron", Math.abs(z - exact[i]) < 0.05, String.format(Locale.ROOT, "%.4f (exact %.0f)", z, exact[i]));
			}
		} finally {
			clipOff(clip);
			m.clearPoints();
		}
	}

	static void model(IsoSurface iso, Measurement3d m) {
		/* A box 20 x 10 x 5 (file units) at (75, 18, -6), scale 0.5 */
		File folder = tempFolder();
		File obj = new File(folder, "box.obj");
		double cx = 75, cy = -18, cz = -6, hx = 10, hy = 5, hz = 2.5;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			sb.append(String.format(Locale.ROOT, "v %s %s %s%n", cx + ((i & 1) == 0 ? -hx : hx), cy + ((i & 2) == 0 ? -hy : hy), cz + ((i & 4) == 0 ? -hz : hz)));
		}
		int[][] faces = { { 1, 3, 4, 2 }, { 5, 6, 8, 7 }, { 1, 2, 6, 5 }, { 3, 7, 8, 4 }, { 1, 5, 7, 3 }, { 2, 4, 8, 6 } };
		for (int[] f : faces) {
			sb.append(String.format("f %d %d %d%nf %d %d %d%n", f[0], f[1], f[2], f[0], f[2], f[3]));
		}
		try {
			Files.writeString(obj.toPath(), sb.toString());
			com.eco.bio7.loader3d.MeshData mesh = com.eco.bio7.loader3d.MeshReader.read(obj).weld();
			iso.setMesh(mesh, 0.5, "mm");
			double[][] r = m.measureIsoSurface(false);
			equal("objects", r.length, 1);
			double s = 0.5;
			compareValue("volume", r[0][0], 20 * 10 * 5 * s * s * s, 1e-6);
			compareValue("area", r[0][1], 2 * (20 * 10 + 20 * 5 + 10 * 5) * s * s, 1e-6);
			compareValue("centroid x (file)", r[0][3], cx * s, 1e-6);
			compareValue("centroid y (file)", r[0][4], cy * s, 1e-6);
			compareValue("centroid z (file)", r[0][5], cz * s, 1e-6);
			compareValue("axis 1", r[0][6], 2 * Math.sqrt(5 * 20 * 20 / 12.0) * s, 1e-6);
			compareValue("Feret diameter", r[0][11], Math.sqrt(20 * 20 + 10 * 10 + 5 * 5) * s, 1e-6);
			equal("holes", r[0][12], 0);
			check("Measure Region: no values for a model", m.measureRegion(false) == null, "");
		} catch (Exception e) {
			check("model loaded", false, e.toString());
		} finally {
			deleteFolder(folder);
		}
	}

	static void rgb(IsoSurface iso, Measurement3d m) {
		/* An RGB stack: a sphere of the color (250, 40, 10) */
		int n = 40;
		ImageStack st = new ImageStack(n, n);
		for (int z = 0; z < n; z++) {
			ColorProcessor cp = new ColorProcessor(n, n);
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					double d = Math.sqrt((x + 0.5 - 20) * (x + 0.5 - 20) + (y + 0.5 - 20) * (y + 0.5 - 20) + (z + 0.5 - 20) * (z + 0.5 - 20));
					double f = Math.max(0, Math.min(1, 0.5 + 10 - d));
					cp.set(x, y, ((int) Math.round(250 * f) << 16) | ((int) Math.round(40 * f) << 8) | (int) Math.round(10 * f));
				}
			}
			st.addSlice(cp);
		}
		ImagePlus imp = new ImagePlus("rgb", st);
		double lum = 0.299 * 250 + 0.587 * 40 + 0.114 * 10;
		load(iso, imp, lum / 2, 0, 5);
		double[] region = m.measureRegion(false);
		double sum = 0;
		for (int z = 0; z < n; z++) {
			int[] px = (int[]) st.getPixels(z + 1);
			for (int c : px) {
				sum += (float) (0.299 * ((c >> 16) & 0xff) + 0.587 * ((c >> 8) & 0xff) + 0.114 * (c & 0xff));
			}
		}
		compareValue("region mean = mean of the luminance", region[2], sum / (n * n * n), 1e-12);
		m.measureIsoSurface(false);
		m.clearPoints();
		m.pick(new double[] { 0.5, 0.5, 0 }, new double[] { 0.5, 0.5, 1 });
		List<double[]> p = m.getPoints();
		check("picked point", p.size() == 1, "");
		m.clearPoints();
	}

	static void settings(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		String s = m.getSettings();
		check("settings: iso value", s != null && s.contains("Iso value: 127.5"), s);
		check("settings: mesh smoothing", s != null && s.contains("Mesh smoothing: 5 iterations"), s);
		check("settings: resolution", s != null && s.contains("Resolution: 1x"), s);
	}

	static void decimals(Measurement3d m) {
		double[] values = { 17780394.551519748, 0.2700878175809153, 1.0 / 3, 1e-7 * Math.PI, -12.5, 2.4e-10 };
		try {
			m.setDecimals(-1);
			boolean same = true;
			for (double v : values) {
				same &= Double.parseDouble(Measurement3d.formatValue(v)) == v;
			}
			check("full precision: the same double values", same, Measurement3d.formatValue(values[0]));
			m.setDecimals(9);
			double max = 0;
			for (double v : values) {
				max = Math.max(max, Math.abs(Double.parseDouble(Measurement3d.formatValue(v)) - v) / Math.max(1, Math.abs(v)));
			}
			check("9 decimals: difference <= 5e-10", max <= 5e-10, String.format(Locale.ROOT, "%.1e", max));
			m.setDecimals(4);
			check("4 decimals (default)", Measurement3d.formatValue(0.2700878175809153).equals("0.2701"), Measurement3d.formatValue(0.2700878175809153));
		} finally {
			m.setDecimals(4);
		}
	}

	/*
	 * A 64-bit stack (SWTImageJ: GRAY64) is read in double: large offsets and tiny
	 * contrasts are measured like the same object with the values 0 - 255.
	 */
	static void gray64(IsoSurface iso, Measurement3d m) {
		double[][] cases = { { 0, 1 }, { 1e9, 1 }, { 1e12, 1 }, { 1, 1e-9 } };
		String[] names = { "values 0 - 255", "offset 1e9", "offset 1e12", "tiny contrast 1e-9" };
		/* The input of 1e12 + (0 - 255) has only about 4 decimals in double */
		double[] tolerance = { 0, 1e-12, 1e-6, 1e-12 };
		double[] ref = null;
		long refVoxels = 0;
		for (int k = 0; k < cases.length; k++) {
			ImagePlus imp = sphere64(cases[k][0], cases[k][1]);
			load(iso, imp, cases[k][0] + cases[k][1] * 127.5, 0, 5);
			double[][] r = m.measureIsoSurface(false);
			double[] region = m.measureRegion(false);
			if (k == 0) {
				check("64-bit stack: one object", r != null && r.length == 1, "");
				if (r == null || r.length == 0) {
					return;
				}
				ref = r[0];
				refVoxels = (long) region[8];
				continue;
			}
			if (r == null || r.length != 1) {
				check(names[k] + ": one object", false, (r == null ? "none" : r.length + " objects"));
				continue;
			}
			double max = 0;
			for (int c = 0; c < ref.length; c++) {
				/* The mean inside value in the units of the stack */
				double expected = c == 13 ? cases[k][0] + cases[k][1] * ref[c] : ref[c];
				max = Math.max(max, Math.abs(r[0][c] - expected) / Math.max(Math.abs(expected), 1e-300));
			}
			check(names[k] + ": all columns like the values 0 - 255", max <= tolerance[k], String.format(Locale.ROOT, "max. difference %.1e", max));
			equal(names[k] + ": region voxels >= iso", region[8], refVoxels);
		}
	}

	static ImagePlus sphere64(double offset, double scale) {
		int n = 40;
		ImageStack st = new ImageStack(n, n);
		for (int z = 0; z < n; z++) {
			double[] px = new double[n * n];
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					double d = 12 - Math.sqrt((x + 0.5 - 20) * (x + 0.5 - 20) + (y + 0.5 - 20) * (y + 0.5 - 20) + (z + 0.5 - 20) * (z + 0.5 - 20));
					px[y * n + x] = offset + scale * 255 * Math.max(0, Math.min(1, 0.5 + d));
				}
			}
			st.addSlice(new DoubleProcessor(n, n, px));
		}
		return new ImagePlus("gray64", st);
	}

	/* ------------------------------------------------------------------ the test stack */

	/*
	 * The test objects: the value is 255 * clamp(0.5 + s, 0, 1) with s the
	 * (approximate) signed distance to the surface in micron (rounded half to even
	 * like NumPy), so the iso surface at 127.5 is the surface of the object. The
	 * voxels are 1 x 1 x dz micron, 160 x 160 x 96 micron.
	 */
	/* The cavity of the hollow sphere and the labels 1, 3 of the label stack (trimesh on the meshes of Bio7) */
	static final double[] PYTHON_CAVITY = { 506.4727993264251, 308.3789994117511 };
	static final double[][] PYTHON_LABELS = { { 7169.848202435732, 1822.0659484179953 }, { 2149.6743546989997, 813.9980046448147 } };

	static void valueSelection(IsoSurface iso, Measurement3d m, ImagePlus imp) {
		try {
			/* The threshold 128 - 255 of the image: the same surface as the iso value 127.5 */
			imp.getProcessor().setThreshold(128, 255, ImageProcessor.NO_LUT_UPDATE);
			iso.setSelection(ValueSelection.IMAGE_THRESHOLD);
			load(iso, imp, 127.5, 0, 5);
			compareRows("threshold 128 - 255 = iso value 127.5", m.measureIsoSurface(false), PYTHON_SPHERES, GOLDEN);
			/* The labels 1 and 3 of a label stack */
			ImagePlus labels = labelStack();
			iso.setSelection(ValueSelection.labels("1, 3"));
			load(iso, labels, 0, 0, 5);
			double[][] r = m.measureIsoSurface(false);
			equal("labels 1, 3: objects", r.length, 2);
			for (int i = 0; i < r.length && i < 2; i++) {
				compareRow("labels 1, 3: object " + (i + 1) + " (volume, area)", r[i], PYTHON_LABELS[i], GOLDEN);
			}
			double[] region = m.measureRegion(false);
			equal("labels 1, 3: selected voxels (Measure Region)", region[8], 2176 + 7208);
			/* The threshold 1 - 2 of the labels: like the labels 1 - 2 */
			labels.getProcessor().setThreshold(1, 2, ImageProcessor.NO_LUT_UPDATE);
			iso.setSelection(ValueSelection.IMAGE_THRESHOLD);
			waitFor(iso);
			double[][] threshold = m.measureIsoSurface(false);
			iso.setSelection(ValueSelection.labels("1-2"));
			waitFor(iso);
			double[][] list = m.measureIsoSurface(false);
			equal("threshold 1 - 2 = labels 1-2: objects", threshold.length, list.length);
			for (int i = 0; i < Math.min(threshold.length, list.length); i++) {
				compareRow("threshold 1 - 2 = labels 1-2: object " + (i + 1), threshold[i], list[i], 0);
			}
		} finally {
			imp.getProcessor().resetThreshold();
			iso.setSelection(ValueSelection.ALL);
		}
	}

	/* Three balls (radius 8, 10, 12) with the labels 1, 2, 3 (16 bit) */
	static ImagePlus labelStack() {
		int w = 100, h = 60, d = 40;
		double[][] balls = { { 20, 30, 20, 8, 1 }, { 50, 30, 20, 10, 2 }, { 80, 30, 20, 12, 3 } };
		ImageStack st = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			ShortProcessor ip = new ShortProcessor(w, h);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					for (double[] b : balls) {
						double dx = x + 0.5 - b[0], dy = y + 0.5 - b[1], dz = z + 0.5 - b[2];
						if (dx * dx + dy * dy + dz * dz <= b[3] * b[3]) {
							ip.set(x, y, (int) b[4]);
						}
					}
				}
			}
			st.addSlice("z" + (z + 1), ip);
		}
		ImagePlus imp = new ImagePlus("labels", st);
		imp.getCalibration().setUnit("micron");
		return imp;
	}

	/* A click of the mouse tool at the window position (x, y; count 2 = a double-click) */
	static void click(Clipping clip, double[] w, int count) {
		clip.toolPressed(w[0], w[1], false, count);
		clip.toolReleased(w[0], w[1]);
	}

	/* A click without a tool (ends the selection of a measurement) */
	static void deselect(Clipping clip, int tool) {
		clip.setTool(Clipping.TOOL_NONE);
		click(clip, new double[] { 2, 2 }, 1);
		clip.setTool(tool);
	}

	/* The window position of a position of the spheres stack (micron) */
	static double[] window(Clipping clip, double x, double y, double z) {
		return clip.projectToWindow(rel(x, y, z));
	}

	static void mouseLines(IsoSurface iso, Measurement3d m, Clipping clip, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		iso.setShow(true);
		m.clearPoints();
		sleep(1000);
		try {
			double[] c = window(clip, 40, 110, 50), e = window(clip, 58, 110, 50);
			if (c == null || e == null) {
				check("the 3D view is displayed", false, "no view");
				return;
			}
			double r = Math.hypot(e[0] - c[0], e[1] - c[1]);
			double[] a = { c[0] - 0.7 * r, c[1] }, b = { c[0] + 0.7 * r, c[1] }, top = { c[0], c[1] - 0.7 * r };
			/* A straight line and an angle: from the coordinates of the points */
			clip.setTool(Clipping.TOOL_STRAIGHT);
			click(clip, a, 1);
			click(clip, b, 1);
			deselect(clip, Clipping.TOOL_ANGLE);
			click(clip, a, 1);
			click(clip, top, 1);
			click(clip, b, 1);
			deselect(clip, Clipping.TOOL_STRAIGHT);
			List<double[]> p = m.getPoints();
			equal("points (straight line, angle)", p.size(), 5);
			compareValue("straight line: length = distance of the points", p.get(1)[5], distance(p.get(0), p.get(1)), 1e-12);
			compareValue("angle = the angle of the points", p.get(4)[7], angle(p.get(2), p.get(3), p.get(4)), 1e-12);
			/* Along the surface: the arc of the plane of the view rays on the sphere r = 18 (the mesh: within 0.5 %) */
			m.setFollowSurface(true);
			click(clip, a, 1);
			click(clip, b, 1);
			deselect(clip, Clipping.TOOL_PICK);
			m.setFollowSurface(false);
			p = m.getPoints();
			double along = p.get(6)[5], straight = distance(p.get(5), p.get(6));
			double arc = arc(clip, p.get(5), p.get(6), new double[] { 40, 110, 50 }, 18);
			check("along the surface: longer than the straight line", along > straight, String.format(Locale.ROOT, "%.4f > %.4f", along, straight));
			compareValue("along the surface: the arc on the sphere", along, arc, 0.005);
			/* A spline through 7 points: the centripetal Catmull-Rom spline (own implementation) */
			m.setFitSpline(true);
			for (int i = 0; i <= 6; i++) {
				double t = 2 * Math.PI * i / 8;
				click(clip, new double[] { c[0] + 0.6 * r * Math.cos(t), c[1] + 0.6 * r * Math.sin(t) }, 1);
			}
			/* A double-click ends the line (it is selected: Fit Spline stays on, switching it off would change the line) */
			click(clip, new double[] { c[0] + 0.6 * r * Math.cos(1.5 * Math.PI), c[1] + 0.6 * r * Math.sin(1.5 * Math.PI) }, 2);
			p = m.getPoints();
			List<double[]> s = p.subList(7, 14);
			double expected = splineLength(s);
			compareValue("spline: length (7 points)", s.get(6)[6], expected, 1e-5);
			/* Editing: a point added between two points and deleted again (the same length) */
			double before = s.get(6)[6];
			double[] mid = { c[0] + 0.85 * r * Math.cos(Math.PI / 8), c[1] + 0.85 * r * Math.sin(Math.PI / 8) };
			clip.setTool(Clipping.TOOL_PICK);
			clip.toolPressed(mid[0], mid[1], true, 1);
			clip.toolReleased(mid[0], mid[1]);
			equal("spline: a point added (Shift + click)", m.getPoints().size(), 15);
			m.deleteSelected(false);
			p = m.getPoints();
			equal("spline: the point deleted", p.size(), 14);
			compareValue("spline: the length again", p.get(13)[6], before, 1e-12);
		} finally {
			clip.setTool(Clipping.TOOL_NONE);
			m.clearPoints();
			m.setFollowSurface(false);
			m.setFitSpline(false);
		}
	}

	static void mouseAreas(IsoSurface iso, Measurement3d m, Clipping clip, ImagePlus imp) {
		load(iso, imp, 127.5, 0, 5);
		iso.setShow(true);
		m.clearPoints();
		sleep(1000);
		try {
			double[] c = window(clip, 40, 110, 50), e = window(clip, 58, 110, 50);
			if (c == null || e == null) {
				check("the 3D view is displayed", false, "no view");
				return;
			}
			double r = Math.hypot(e[0] - c[0], e[1] - c[1]);
			/* A polygon (8 corners) and the same polygon as a spline: the spline encloses more (the octagon: 90 %) */
			clip.setTool(Clipping.TOOL_POLYGON);
			double[] corner = null;
			for (int i = 0; i < 8; i++) {
				double t = 2 * Math.PI * i / 8;
				corner = new double[] { c[0] + 0.5 * r * Math.cos(t), c[1] + 0.5 * r * Math.sin(t) };
				click(clip, corner, 1);
			}
			/* A double-click closes the polygon */
			click(clip, corner, 2);
			List<double[]> areas = m.getAreas();
			equal("polygon: measured", areas.size(), 1);
			double polygon = areas.get(0)[4];
			m.setFitSpline(true);
			double spline = m.getAreas().get(0)[4];
			m.setFitSpline(false);
			double ratio = polygon / spline;
			check("polygon / spline: about the octagon / circle (0.903)", Math.abs(ratio - 0.903) < 0.01, String.format(Locale.ROOT, "%.4f", ratio));
			check("spline back to the polygon: the same area", m.getAreas().get(0)[4] == polygon, "");
			/* A corner added and deleted again: the same area */
			double[] out = { c[0] + 0.65 * r * Math.cos(Math.PI / 8), c[1] + 0.65 * r * Math.sin(Math.PI / 8) };
			clip.toolPressed(out[0], out[1], true, 1);
			clip.toolReleased(out[0], out[1]);
			check("polygon: a corner added (more area)", m.getAreas().get(0)[4] > polygon && m.getAreas().get(0)[1] == 9, "");
			m.deleteSelected(false);
			compareValue("polygon: the corner deleted (the same area)", m.getAreas().get(0)[4], polygon, 1e-12);
			/* The volume inside of an outline around the sphere r = 12: all its voxels (NumPy: 7208) */
			deselect(clip, Clipping.TOOL_VOLUME);
			double[] c2 = window(clip, 128, 120, 40), e2 = window(clip, 140, 120, 40);
			double r2 = Math.hypot(e2[0] - c2[0], e2[1] - c2[1]);
			clip.toolPressed(c2[0] + 1.3 * r2, c2[1], false, 1);
			for (int i = 1; i < 48; i++) {
				double t = 2 * Math.PI * i / 48;
				clip.toolDragged(c2[0] + 1.3 * r2 * Math.cos(t), c2[1] + 1.3 * r2 * Math.sin(t));
			}
			clip.toolReleased(c2[0] + 1.3 * r2, c2[1]);
			areas = m.getAreas();
			equal("volume inside of an outline: the voxels of the sphere r = 12", areas.get(areas.size() - 1)[5], 7208);
			/* A click on the object: its volume (the table of the objects) */
			click(clip, c2, 1);
			areas = m.getAreas();
			double[] object = nearest(m.measureIsoSurface(false), new double[] { 128, 120, 40 });
			equal("volume of a clicked object = the table of the objects", areas.get(areas.size() - 1)[5], object[0]);
			/* The cavity of the hollow sphere: the front half clipped, a click on the inner wall */
			clip.setBox(0, 1, 0, 1, 0, 0.75);
			clip.setBoxEnabled(true);
			sleep(1500);
			click(clip, window(clip, 62, 132, 72), 1);
			areas = m.getAreas();
			double[] cavity = areas.get(areas.size() - 1);
			equal("cavity: the type", cavity[0], Measurement3d.AREA_CAVITY);
			compareRow("cavity: volume, surface area", new double[] { cavity[5], cavity[4] }, PYTHON_CAVITY, GOLDEN);
		} finally {
			clipOff(clip);
			clip.setTool(Clipping.TOOL_NONE);
			m.clearPoints();
			m.setFitSpline(false);
		}
	}

	/*
	 * The options of the volume tool (Measure tab): a click on an object measures
	 * its material (the cavities subtracted, the default) or, with "object filled",
	 * the volume inside of its outer surface; a click on the inner wall of a cavity
	 * measures the whole cavity (the default) or, with "cavity free space", the
	 * cavity without the objects which float in it. The stack: a hollow sphere
	 * (R = 30, cavity r = 20) with a ball (r = 6) floating in the cavity.
	 */
	static void volumeOptions(IsoSurface iso, Measurement3d m, Clipping clip) {
		ImagePlus imp = cavityStack();
		load(iso, imp, 127.5, 0, 5);
		iso.setShow(true);
		m.clearPoints();
		boolean filled = m.isObjectFilled(), free = m.isCavityFreeSpace();
		sleep(1000);
		try {
			check("defaults: material and whole cavity", !filled && !free, "filled " + filled + ", free space " + free);
			double[][] table = m.measureIsoSurface(false);
			equal("objects: the hollow sphere and the floating ball", table.length, 2);
			double[] shell = nearest(table, new double[] { 80, 80, 48 });
			double[] ball = table[0] == shell ? table[1] : table[0];
			double outer = 4 / 3.0 * Math.PI * 30 * 30 * 30, inner = 4 / 3.0 * Math.PI * 20 * 20 * 20, small = 4 / 3.0 * Math.PI * 6 * 6 * 6;
			check("table: the hollow sphere (material) within 1.5 %", Math.abs(shell[0] / (outer - inner) - 1) < 0.015,
					String.format(Locale.ROOT, "%+.2f %%", 100 * (shell[0] / (outer - inner) - 1)));
			check("table: the floating ball within 4 %", Math.abs(ball[0] / small - 1) < 0.04,
					String.format(Locale.ROOT, "%+.2f %%", 100 * (ball[0] / small - 1)));
			double[] centre = window(clip, 80, 80, 48);
			if (centre == null) {
				check("the 3D view is displayed", false, "no view");
				return;
			}
			/* A click on the outer surface: the material (the table) */
			clip.setTool(Clipping.TOOL_VOLUME);
			m.setObjectFilled(false);
			click(clip, centre, 1);
			double[] material = last(m);
			equal("object: the type", material[0], Measurement3d.AREA_OBJECT);
			compareValue("object (material) = the table of the objects", material[5], shell[0], GOLDEN);
			/* Object filled: the volume inside of the outer surface (with the cavity and the ball) */
			m.setObjectFilled(true);
			click(clip, centre, 1);
			double[] full = last(m);
			equal("object filled: the type", full[0], Measurement3d.AREA_OBJECT);
			check("object filled within 1 % of the sphere R = 30", Math.abs(full[5] / outer - 1) < 0.01,
					String.format(Locale.ROOT, "%+.2f %%", 100 * (full[5] / outer - 1)));
			check("object filled > material", full[5] > material[5], "");
			/* The filled object has only the outer surface (the material also the wall of the cavity) */
			check("object filled: the area of the outer surface within 1.5 % of the sphere R = 30", Math.abs(full[4] / (4 * Math.PI * 900) - 1) < 0.015,
					String.format(Locale.ROOT, "%+.2f %%", 100 * (full[4] / (4 * Math.PI * 900) - 1)));
			check("material: the area of the outer surface and the walls (more than filled)", material[4] > full[4], "");
			m.setObjectFilled(false);
			/* The cavity: the front half clipped (z <= 48), a click between the ball and the wall */
			clip.setBox(0, 1, 0, 1, 0, 0.5);
			clip.setBoxEnabled(true);
			sleep(1500);
			double[] gap = window(clip, 93, 80, 48);
			m.setCavityFreeSpace(false);
			click(clip, gap, 1);
			double[] whole = last(m);
			equal("cavity: the type", whole[0], Measurement3d.AREA_CAVITY);
			check("cavity (whole space) within 1.5 % of the sphere r = 20", Math.abs(whole[5] / inner - 1) < 0.015,
					String.format(Locale.ROOT, "%+.2f %%", 100 * (whole[5] / inner - 1)));
			compareValue("cavity (whole space) = object filled - material", whole[5], full[5] - material[5], GOLDEN);
			/* Cavity free space: without the floating ball (its volume of the table) */
			m.setCavityFreeSpace(true);
			click(clip, gap, 1);
			double[] space = last(m);
			equal("cavity free space: the type", space[0], Measurement3d.AREA_CAVITY);
			compareValue("cavity free space = whole cavity - the floating ball", space[5], whole[5] - ball[0], GOLDEN);
			check("cavity free space within 1.5 % of the exact free space", Math.abs(space[5] / (inner - small) - 1) < 0.015,
					String.format(Locale.ROOT, "%+.2f %%", 100 * (space[5] / (inner - small) - 1)));
			compareValue("cavity free space: the same surface area", space[4], whole[4], GOLDEN);
			/* The options don't change the table of the objects */
			compareRows("the table of the objects is unchanged", m.measureIsoSurface(false), table, GOLDEN);
		} finally {
			m.setObjectFilled(filled);
			m.setCavityFreeSpace(free);
			clipOff(clip);
			clip.setTool(Clipping.TOOL_NONE);
			m.clearPoints();
		}
	}

	/* The last measured area or volume */
	static double[] last(Measurement3d m) {
		List<double[]> areas = m.getAreas();
		return areas.get(areas.size() - 1);
	}

	/* The stack of volumeOptions (the size of the spheres stack, 1 µm voxels): a hollow sphere with a floating ball */
	static ImagePlus cavityStack() {
		int nx = 160, ny = 160, nz = 96;
		ImageStack st = new ImageStack(nx, ny);
		for (int k = 0; k < nz; k++) {
			byte[] px = new byte[nx * ny];
			double z = k + 0.5;
			for (int j = 0; j < ny; j++) {
				double y = j + 0.5;
				for (int i = 0; i < nx; i++) {
					double x = i + 0.5;
					double s = Math.max(shell(x - 80, y - 80, z - 48, 30, 20), sphere(x, y, z, 80, 80, 48, 6));
					px[j * nx + i] = (byte) (int) Math.rint(Math.max(0, Math.min(1, 0.5 + s)) * 255);
				}
			}
			st.addSlice("z" + (k + 1), new ByteProcessor(nx, ny, px));
		}
		ImagePlus imp = new ImagePlus("cavity", st);
		imp.getCalibration().pixelWidth = 1;
		imp.getCalibration().pixelHeight = 1;
		imp.getCalibration().pixelDepth = 1;
		imp.getCalibration().setUnit("micron");
		return imp;
	}

	static double distance(double[] a, double[] b) {
		return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
	}

	/* The angle (degrees) at the vertex b */
	static double angle(double[] a, double[] b, double[] c) {
		double[] u = { a[0] - b[0], a[1] - b[1], a[2] - b[2] }, v = { c[0] - b[0], c[1] - b[1], c[2] - b[2] };
		double[] x = { u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0] };
		return Math.toDegrees(Math.atan2(Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]), u[0] * v[0] + u[1] * v[1] + u[2] * v[2]));
	}

	/*
	 * The arc between two points on a sphere (center, radius in micron) in the
	 * plane of the view rays of the points (a circle of the sphere)
	 */
	static double arc(Clipping clip, double[] p, double[] q, double[] center, double radius) {
		double[][] ra = clip.rayAt(clip.projectToWindow(rel(p[0], p[1], p[2]))[0], clip.projectToWindow(rel(p[0], p[1], p[2]))[1]);
		double[][] rb = clip.rayAt(clip.projectToWindow(rel(q[0], q[1], q[2]))[0], clip.projectToWindow(rel(q[0], q[1], q[2]))[1]);
		double[] o = micron(ra[0]), u = sub(micron(ra[1]), o), w = sub(micron(rb[1]), o);
		double[] n = { u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0] };
		double len = Math.sqrt(dot(n, n));
		n = new double[] { n[0] / len, n[1] / len, n[2] / len };
		double d = dot(sub(center, o), n);
		double rho = Math.sqrt(radius * radius - d * d);
		double[] cc = { center[0] - d * n[0], center[1] - d * n[1], center[2] - d * n[2] };
		double[] a = sub(new double[] { p[0], p[1], p[2] }, cc), b = sub(new double[] { q[0], q[1], q[2] }, cc);
		return rho * Math.acos(Math.max(-1, Math.min(1, dot(a, b) / Math.sqrt(dot(a, a) * dot(b, b)))));
	}

	static double[] micron(double[] r) {
		return new double[] { r[0] * 160, r[1] * 160, r[2] * 96 };
	}

	static double[] sub(double[] a, double[] b) {
		return new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
	}

	static double dot(double[] a, double[] b) {
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	}

	/* The length of the centripetal Catmull-Rom spline through the points (20000 samples per segment) */
	static double splineLength(List<double[]> points) {
		int n = points.size();
		double length = 0;
		for (int k = 1; k < n; k++) {
			double[] p1 = points.get(k - 1), p2 = points.get(k);
			double[] p0 = k >= 2 ? points.get(k - 2) : mirror(p1, p2), p3 = k + 1 < n ? points.get(k + 1) : mirror(p2, p1);
			double[] last = p1;
			for (int i = 1; i <= 20000; i++) {
				double[] q = catmullRom(p0, p1, p2, p3, i / 20000.0);
				length += distance(last, q);
				last = q;
			}
		}
		return length;
	}

	static double[] mirror(double[] a, double[] b) {
		return new double[] { 2 * a[0] - b[0], 2 * a[1] - b[1], 2 * a[2] - b[2] };
	}

	static double[] catmullRom(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
		double t0 = 0, t1 = t0 + Math.sqrt(distance(p0, p1)), t2 = t1 + Math.sqrt(distance(p1, p2)), t3 = t2 + Math.sqrt(distance(p2, p3));
		double u = t1 + (t2 - t1) * t;
		double[] r = new double[3];
		for (int k = 0; k < 3; k++) {
			double a1 = lerp(p0[k], p1[k], t0, t1, u), a2 = lerp(p1[k], p2[k], t1, t2, u), a3 = lerp(p2[k], p3[k], t2, t3, u);
			double b1 = lerp(a1, a2, t0, t2, u), b2 = lerp(a2, a3, t1, t3, u);
			r[k] = lerp(b1, b2, t1, t2, u);
		}
		return r;
	}

	static double lerp(double a, double b, double ta, double tb, double u) {
		return (a * (tb - u) + b * (u - ta)) / (tb - ta);
	}

	static ImagePlus stack(double dz) {
		int nx = 160, ny = 160, nz = (int) Math.round(96 / dz);
		ImageStack st = new ImageStack(nx, ny);
		for (int k = 0; k < nz; k++) {
			byte[] px = new byte[nx * ny];
			double z = (k + 0.5) * dz;
			for (int j = 0; j < ny; j++) {
				double y = j + 0.5;
				for (int i = 0; i < nx; i++) {
					double x = i + 0.5;
					double s = -1e9;
					s = Math.max(s, sphere(x, y, z, 28, 30, 24, 3));
					s = Math.max(s, sphere(x, y, z, 60, 28, 24, 5));
					s = Math.max(s, sphere(x, y, z, 100, 32, 30, 8));
					s = Math.max(s, sphere(x, y, z, 128, 120, 40, 12));
					s = Math.max(s, sphere(x, y, z, 40, 110, 50, 18));
					s = Math.max(s, ellipsoid(x - 100, y - 80, z - 60, 20, 12, 7));
					s = Math.max(s, torus(x - 125, y - 45, z - 66, 14, 5));
					s = Math.max(s, shell(x - 62, y - 132, z - 72, 10, 5));
					px[j * nx + i] = (byte) (int) Math.rint(Math.max(0, Math.min(1, 0.5 + s)) * 255);
				}
			}
			st.addSlice("z" + (k + 1), new ByteProcessor(nx, ny, px));
		}
		ImagePlus imp = new ImagePlus("spheres", st);
		imp.getCalibration().pixelWidth = 1;
		imp.getCalibration().pixelHeight = 1;
		imp.getCalibration().pixelDepth = dz;
		imp.getCalibration().setUnit("micron");
		return imp;
	}

	static double sphere(double x, double y, double z, double cx, double cy, double cz, double r) {
		double X = x - cx, Y = y - cy, Z = z - cz;
		return r - Math.sqrt(X * X + Y * Y + Z * Z);
	}

	static double ellipsoid(double X, double Y, double Z, double a, double b, double c) {
		double q = Math.sqrt((X / a) * (X / a) + (Y / b) * (Y / b) + (Z / c) * (Z / c));
		double gx = X / (a * a), gy = Y / (b * b), gz = Z / (c * c);
		return (1 - q) / Math.max(Math.sqrt(gx * gx + gy * gy + gz * gz) / Math.max(q, 1e-9), 1e-9);
	}

	static double torus(double X, double Y, double Z, double R, double r) {
		double t = Math.sqrt(X * X + Y * Y) - R;
		return r - Math.sqrt(t * t + Z * Z);
	}

	static double shell(double X, double Y, double Z, double R, double r) {
		double dist = Math.sqrt(X * X + Y * Y + Z * Z);
		return Math.min(R - dist, dist - r);
	}

	/* ------------------------------------------------------------------ helpers */

	static void load(IsoSurface iso, ImagePlus imp, double isoValue, float smoothing, int meshSmoothing) {
		iso.setMeasureResolution(1);
		iso.setSmoothing(smoothing);
		iso.setMeshSmoothing(meshSmoothing);
		iso.load(imp);
		waitFor(iso);
		iso.setIsoValue(isoValue);
		waitFor(iso);
	}

	static void waitFor(IsoSurface iso) {
		try {
			if (!iso.waitForSurface(120000)) {
				check("iso surface computed in time", false, "timeout");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	static void reset(IsoSurface iso, Measurement3d m, Clipping clip) {
		clipOff(clip);
		clip.clearMask();
		iso.setReduceDisplay(false);
		iso.setMeasureResolution(1);
		m.setMinObjectSize(10);
		m.setMaxObjectSize(0);
		m.setMatrixFolder(null);
		m.setDecimals(4);
		m.clearPoints();
	}

	static void clipOff(Clipping clip) {
		clip.setBoxEnabled(false);
		clip.setPlaneEnabled(false);
		clip.setFlip(false);
		clip.setCutMesh(false);
	}

	static double[] rel(double x, double y, double z) {
		return new double[] { x / 160, y / 160, z / 96 };
	}

	static double[] nearest(double[][] rows, double[] c) {
		double[] best = null;
		double d = Double.MAX_VALUE;
		for (double[] r : rows) {
			if (dist(r, c) < d) {
				d = dist(r, c);
				best = r;
			}
		}
		return best;
	}

	static double dist(double[] row, double[] c) {
		double dx = row[3] - c[0], dy = row[4] - c[1], dz = row[5] - c[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	static void run(String name, Runnable test) {
		System.out.println("\n" + name);
		try {
			test.run();
		} catch (Throwable t) {
			check(name, false, t.toString());
		}
	}

	static void check(String name, boolean ok, String detail) {
		if (ok) {
			passed++;
		} else {
			failed++;
			failures.add(name + (detail.isEmpty() ? "" : " (" + detail + ")"));
		}
		System.out.println((ok ? "  ok      " : "  FAILED  ") + name + (detail.isEmpty() ? "" : "  " + detail));
	}

	static void equal(String name, double value, double expected) {
		check(name, value == expected, String.format(Locale.ROOT, "%s (expected %s)", fmt(value), fmt(expected)));
	}

	static void compareValue(String name, double value, double expected, double tolerance) {
		double d = relative(value, expected);
		check(name, d <= tolerance, String.format(Locale.ROOT, "%s (expected %s, difference %.1e)", fmt(value), fmt(expected), d));
	}

	static void compareRow(String name, double[] row, double[] expected, double tolerance) {
		if (row == null || row.length < expected.length) {
			check(name, false, "no result");
			return;
		}
		double max = 0;
		for (int k = 0; k < expected.length; k++) {
			max = Math.max(max, relative(row[k], expected[k]));
		}
		check(name, max <= tolerance, String.format(Locale.ROOT, "max. difference %.1e", max));
	}

	static void compareRows(String name, double[][] rows, double[][] expected, double tolerance) {
		if (rows == null || rows.length != expected.length) {
			check(name + ": number", false, (rows == null ? "none" : rows.length) + " (expected " + expected.length + ")");
			return;
		}
		double max = 0;
		for (int i = 0; i < expected.length; i++) {
			for (int k = 0; k < expected[i].length; k++) {
				max = Math.max(max, relative(rows[i][k], expected[i][k]));
			}
		}
		check(name + " (" + rows.length + " x " + expected[0].length + " values)", max <= tolerance, String.format(Locale.ROOT, "max. difference %.1e", max));
	}

	/* The relative difference (absolute for values near 0, NaN = NaN) */
	static double relative(double a, double b) {
		if (Double.isNaN(a) && Double.isNaN(b)) {
			return 0;
		}
		return Math.abs(a - b) / Math.max(1, Math.abs(b));
	}

	static String fmt(double v) {
		return v == Math.rint(v) && Math.abs(v) < 1e15 ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.6g", v);
	}

	static File tempFolder() {
		try {
			return Files.createTempDirectory("bio7_measure_tests").toFile();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				f.delete();
			}
		}
		folder.delete();
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/* ------------------------------------------------------------------ the reference values */

	/* Python (trimesh, SciPy) on the mesh of Bio7: the spheres stack, iso value 127.5, mesh smoothing 5 */
	static final double[][] PYTHON_SPHERES = {
			{ 24369.83304197153, 4066.601922820022, 0.9995875356337275, 39.99999969898729, 109.99999981827294, 49.99999931919257, 35.97097409237206, 35.970971608602966, 35.97096988019599, 1.0000000690492634, 1.0000000480500522, 36.10027242627425, 0.0, 248.72860835346972, 22.006988525390625, 92.00697326660156, 32.00698661804199, 57.99301528930664, 127.99303436279297, 67.99300384521484, 6122.0, 12240.0 },
			{ 7198.500034978885, 1804.1010405920354, 0.9993507472565987, 127.99999977071846, 119.99999879359177, 40.00000033929997, 23.95613285776293, 23.95613064459108, 23.956128204856654, 1.0000000923843622, 1.000000101841767, 24.072478808792344, 0.0, 248.94420368135647, 115.99794006347656, 107.99794006347656, 27.9979190826416, 140.00205993652344, 132.0020751953125, 52.00208282470703, 2690.0, 5376.0 },
			{ 6994.547219848801, 2034.5048915626749, 0.8693576039114992, 99.99999932028669, 80.00000008291494, 60.00000006310612, 39.866422219657004, 23.924134483098896, 14.006780240162753, 1.666368421721608, 1.7080395403434208, 39.866729736328125, 0.0, 248.38288497285455, 80.06663513183594, 68.00431823730469, 53.00416564941406, 119.93336486816406, 91.99568939208984, 66.99584197998047, 2922.0, 5840.0 },
			{ 6846.574779120364, 2754.4806281988613, 0.6330334212590182, 124.99999932397292, 44.999999387815265, 65.99999968732702, 46.28346808686055, 46.283464027799866, 11.158531595844392, 1.0000000877000192, 4.147809559909884, 38.04199611511309, 1.0, 248.0647105181954, 105.99529266357422, 25.995296478271484, 60.992042541503906, 144.00469970703125, 64.00470733642578, 71.00796508789062, 3976.0, 7952.0 },
			{ 3649.12219148268, 1559.5071335866605, 0.7350028608716259, 62.000000193242435, 132.00000021027557, 72.00000074489226, 20.965157696757945, 20.965153940272252, 20.965151762916328, 1.000000179177587, 1.000000103855958, 20.068041591409447, 0.0, 248.83957797843277, 51.98878479003906, 121.98878479003906, 61.98878479003906, 72.01119995117188, 142.01119995117188, 82.01119995117188, 2380.0, 4752.0 },
			{ 2121.556046700218, 799.4641720653608, 0.9987453992446574, 99.9999997205225, 31.999999440490548, 29.99999931437563, 15.942503782396336, 15.942501760616302, 15.94249693063884, 1.0000001268169867, 1.0000003029624207, 16.058305616845928, 0.0, 248.3049488625497, 91.98867797851562, 23.988677978515625, 21.988679885864258, 108.01129913330078, 40.011322021484375, 38.011322021484375, 1250.0, 2496.0 },
			{ 506.472531466623, 308.37886628404414, 0.9964070260713249, 60.0000006579016, 28.000000922920368, 24.00000042798531, 9.890304419689462, 9.890296712003229, 9.890294912119511, 1.000000779317998, 1.000000181984838, 10.028791047778045, 0.0, 247.68984378730414, 55.01609420776367, 23.016094207763672, 19.016094207763672, 64.98391723632812, 32.98390579223633, 28.983896255493164, 482.0, 960.0 },
			{ 103.60080479559672, 107.52831261814259, 0.9920571994871055, 28.00000060230147, 30.000001282455088, 24.000000152717192, 5.82882031097477, 5.828818163072784, 5.828816639495911, 1.0000003684969965, 1.0000002613869963, 6.0137409235105075, 0.0, 244.69025104259288, 25.139514923095703, 27.139514923095703, 21.139511108398438, 30.86048698425293, 32.86049270629883, 26.860488891601562, 194.0, 384.0 },
	};
	/* The same, mesh smoothing 0 */
	static final double[][] PYTHON_SPHERES_MS0 = {
			{ 24356.000175309397, 4066.745562206105, 0.9991739483471432, 39.999999943343944, 109.99999999520354, 50.00000007436457, 35.964190812947514, 35.9641904232446, 35.96419028284714, 1.0000000108358593, 1.0000000039038126, 36.11616908139166, 0.0, 248.23801501834177, 22.00796890258789, 92.00796508789062, 32.00796890258789, 57.99203109741211, 127.99203491210938, 67.99203491210938, 6122.0, 12240.0 },
			{ 7189.833105131926, 1803.3985819995228, 0.9989374005308287, 127.99999972881935, 119.99999965021067, 39.99999993117792, 23.946543678122005, 23.946543365140727, 23.94654324539353, 1.0000000130699982, 1.0000000050006046, 24.088486441147623, 0.0, 248.40013641760703, 116.01204681396484, 108.01204681396484, 28.012048721313477, 139.98794555664062, 131.98794555664062, 51.987953186035156, 2690.0, 5376.0 },
			{ 6985.526935703144, 2033.8623985173404, 0.8688844100034183, 100.0, 79.99999997508557, 59.99999994144424, 39.84558966838421, 23.91296577289118, 14.00269823221194, 1.6662755279629502, 1.7077398495871006, 39.914886474609375, 0.0, 248.09346708302112, 80.04255676269531, 68.0204086303711, 53.00395202636719, 119.95744323730469, 91.9795913696289, 66.99604797363281, 2922.0, 5840.0 },
			{ 6831.272777415578, 2752.9183013167176, 0.6324485769914008, 125.00000002081903, 44.99999998016473, 65.99999998034295, 46.27334910269356, 46.27334769408219, 11.14446158112073, 1.000000030441095, 4.152138473201032, 38.080969446836924, 1.0, 247.6724747860086, 106.01829528808594, 26.018293380737305, 61.01411437988281, 143.98170471191406, 63.98170852661133, 70.98588562011719, 3976.0, 7952.0 },
			{ 3645.0218964621536, 1557.8118667080694, 0.7352514298487943, 61.9999997883368, 132.00000072057145, 71.99999986227485, 20.94934071380545, 20.949339672979644, 20.949339601664594, 1.0000000496829888, 1.000000003404167, 20.073800144244906, 0.0, 248.38439017473186, 52.01411437988281, 122.01411437988281, 62.01411437988281, 71.98588562011719, 141.9858856201172, 81.98588562011719, 2380.0, 4752.0 },
			{ 2115.7936658695444, 798.5106290678565, 0.9981266041225307, 100.0, 31.999999796284946, 29.99999979161077, 15.928139916236027, 15.928139888385425, 15.928138967755519, 1.0000000017485156, 1.0000000577989625, 16.04646995336816, 0.0, 247.68719345720643, 92.01619720458984, 24.016193389892578, 22.016193389892578, 107.98380279541016, 39.98380661010742, 37.98380661010742, 1250.0, 2496.0 },
			{ 503.4564596370248, 307.4296429088766, 0.9955116089998879, 60.000000240209026, 28.000000060166663, 24.000000041560003, 9.870782137412565, 9.870780984195473, 9.870780899399646, 1.000000116831393, 1.0000000085905896, 10.005609385772237, 0.0, 247.54757387294444, 55.02904510498047, 23.02904510498047, 19.02904510498047, 64.97095489501953, 32.97095489501953, 28.97095489501953, 482.0, 960.0 },
			{ 102.48536854061906, 107.17223646182143, 0.9881959636436306, 28.000000049881123, 29.999999948462797, 24.000000049881123, 5.807867448594619, 5.807867448594587, 5.807866892651287, 1.0000000000000056, 1.000000095722459, 5.999563935693124, 0.0, 244.39362921457973, 25.05434799194336, 27.05434799194336, 21.05434799194336, 30.94565200805664, 32.94565200805664, 26.94565200805664, 194.0, 384.0 },
	};
	/* The same, anisotropic voxels (1 x 1 x 2 micron) */
	static final double[][] PYTHON_SPHERES_ANISO = {
			{ 24347.42914499526, 4080.2708421216685, 0.9956282399040873, 39.99999860193162, 110.00000016920248, 49.9999995364023, 36.07076641902464, 35.907385298871965, 35.907368493442966, 1.0045500700981926, 1.0000004680217378, 36.80168387651301, 0.0, 250.47970054278306, 21.995361328125, 91.99534606933594, 31.968509674072266, 58.00464630126953, 128.0046615600586, 68.03147888183594, 4090.0, 8176.0 },
			{ 7189.182161031577, 1809.7998496492846, 0.9953440745216334, 128.00000016354863, 119.99999916704535, 40.00000004221255, 24.114390061591134, 23.865477300251317, 23.865429671912487, 1.0104298254004414, 1.0000019957042252, 24.7020139720162, 0.0, 249.90744101083047, 116.02609252929688, 108.02606964111328, 27.9576416015625, 139.97390747070312, 131.97390747070312, 52.042362213134766, 1826.0, 3648.0 },
			{ 6862.7116896600355, 2038.104906903513, 0.8568828010183243, 100.0000000066409, 80.00000070558714, 60.00000038546003, 39.89918698484764, 23.916308259585293, 13.77447697317225, 1.6682836895973128, 1.7362770511116827, 39.671844482421875, 0.0, 250.45335194343468, 80.16407775878906, 68.11668395996094, 53.04383850097656, 119.83592224121094, 91.88330841064453, 66.95616149902344, 2154.0, 4304.0 },
			{ 6526.269104640295, 2710.3667262704284, 0.62311191016209, 125.00000009168565, 45.000000084701746, 66.00000014261933, 46.26126501820032, 46.261261554761575, 10.700371952424998, 1.0000000748669324, 4.323332101018928, 37.95417079057288, 1.0, 251.05634741005082, 106.09127807617188, 26.091293334960938, 61.11540985107422, 143.90872192382812, 63.90870666503906, 70.88458251953125, 2632.0, 5264.0 },
			{ 3659.9627096635395, 1554.038808716815, 0.7390492342415323, 62.00000093931262, 132.000000971641, 71.99999969959946, 21.13259695603531, 20.786043320342554, 20.785954326893275, 1.016672419582307, 1.000004281422343, 20.631310632783496, 0.0, 249.94009968117916, 52.05262756347656, 122.05259704589844, 61.954593658447266, 71.94738006591797, 141.94740295410156, 82.04541015625, 1572.0, 3136.0 },
			{ 2108.0179520725396, 799.2725584806376, 0.9947304794160765, 100.00000063432057, 32.00000000854233, 29.999999435822694, 16.16011261947384, 15.788552319134219, 15.788466656205065, 1.023533525609522, 1.000005425664887, 16.553668672073208, 0.0, 249.70617367351912, 92.09699249267578, 24.096988677978516, 21.939579010009766, 107.90300750732422, 39.90301513671875, 38.060428619384766, 818.0, 1632.0 },
			{ 481.4393443408492, 300.50240489766924, 0.98854677845837, 60.000000125350674, 27.9999999602938, 23.999998992472744, 9.821508652731538, 9.821504573538279, 9.561756591681839, 1.0000004153328268, 1.0271652995310931, 10.11185802875513, 0.0, 248.25270878438084, 55.1280632019043, 23.12806510925293, 19.27692222595215, 64.87193298339844, 32.87192916870117, 28.723081588745117, 298.0, 592.0 },
			{ 85.53281468566543, 96.0087720036865, 0.977826310811194, 27.99999986800255, 29.99999913114814, 23.99999983351902, 5.581753532670886, 5.581750230422253, 5.261671519811691, 1.0000005916152634, 1.0608321346943408, 5.957121683018378, 0.0, 244.6179769606394, 25.164583206176758, 27.164588928222656, 21.311277389526367, 30.835412979125977, 32.83541488647461, 26.688724517822266, 98.0, 192.0 },
	};
	/* The nearest neighbors of the spheres (validated with SciPy and FCL): centroid object, distance, surface object, distance, mean centroid distance */
	static final double[][] FCL_NEAREST = {
			{ 5.0, 38.10511910132457, 5.0, 10.12908083585728, 82.42273127058169 },
			{ 3.0, 52.763622989997316, 3.0, 27.301202278251758, 91.36277704512888 },
			{ 4.0, 43.42810201162307, 4.0, 10.542743562026857, 65.10403941496838 },
			{ 3.0, 43.42810201162307, 3.0, 10.542743562026857, 81.4788811355194 },
			{ 1.0, 38.10511910132457, 1.0, 10.12908083585728, 90.39374267335499 },
			{ 7.0, 40.64480163301232, 4.0, 25.69072132951615, 74.78209487406188 },
			{ 8.0, 32.06243916168181, 8.0, 24.2382623208952, 77.88115453315234 },
			{ 7.0, 32.06243916168181, 7.0, 24.2382623208952, 92.00919144137673 },
	};
	/* The objects with the Gaussian smoothing sigma = 1 (validated with SciPy's Gaussian filter and trimesh) */
	static final double[][] PYTHON_GAUSS1 = {
			{ 24127.19888023464, 4038.8974545949195, 0.9997526571643156, 39.99999993848828, 110.00000067813315, 50.00000003170363, 35.851148067882114, 35.8511473335842, 35.851145875178965, 1.0000000204818524, 1.0000000406794596, 35.90754531217452, 0.0, 250.42815719091882, 22.05882453918457, 92.05883026123047, 32.05882453918457, 57.94118118286133, 127.94116973876953, 67.94117736816406, 6122.0, 12240.0 },
			{ 7050.864951183716, 1779.1049322986535, 0.9994877871293273, 127.99999991409258, 120.00000060823966, 40.00000002263117, 23.791144945490217, 23.791141918013093, 23.791140707845287, 1.000000127252283, 1.0000000508663212, 23.8512475929217, 0.0, 251.18882911949757, 116.07659149169922, 108.07659912109375, 28.07657814025879, 139.9233856201172, 131.9233856201172, 51.92341613769531, 2690.0, 5376.0 },
			{ 6820.885695771867, 1994.9526777770304, 0.8718572313609417, 99.99999969780306, 80.00000024650795, 60.000000322664974, 39.43722262160138, 23.67394796246042, 13.953295662581388, 1.6658490034757467, 1.6966563695734584, 39.344970703125, 0.0, 250.96860712177877, 80.3275146484375, 68.1524887084961, 53.03451156616211, 119.6724853515625, 91.84750366210938, 66.96549987792969, 2858.0, 5712.0 },
			{ 6562.977786070522, 2693.1767621808704, 0.6294383823898064, 124.9999974912389, 45.00000014388884, 65.99999932823009, 46.10700514116742, 46.107002354364944, 10.913688286781738, 1.000000060442066, 4.224694818360175, 37.77101452474651, 1.0, 250.85082093387234, 106.14232635498047, 26.142316818237305, 61.1020622253418, 143.8577117919922, 63.85768127441406, 70.8979263305664, 3976.0, 7952.0 },
			{ 3583.084249647083, 1508.867205148649, 0.7504776880927996, 62.00000017779472, 132.00000132502257, 72.00000006563693, 20.667940776331854, 20.66793758527632, 20.667933685078903, 1.000000154396418, 1.0000001887076606, 19.82089536847874, 0.0, 249.54447428872484, 52.09735107421875, 122.09732818603516, 62.097347259521484, 71.90265655517578, 141.9026641845703, 81.90267181396484, 2308.0, 4608.0 },
			{ 2016.6112562537971, 772.7566773848304, 0.9989018466327654, 99.99999955454712, 32.00000006717964, 29.999999886057783, 15.675127835775204, 15.67512432174462, 15.675122978515306, 1.0000002241788015, 1.0000000856917879, 15.765498390306552, 0.0, 252.13760911119826, 92.13892364501953, 24.138925552368164, 22.138925552368164, 107.8610610961914, 39.861080169677734, 37.861080169677734, 1154.0, 2304.0 },
			{ 446.45699005628484, 283.35972368938485, 0.9969325028130112, 60.0000003711925, 28.000000474004864, 24.000000077238397, 9.483030756167365, 9.483029243732885, 9.48302748336941, 1.000000159488539, 1.000000185633067, 9.637424674203043, 0.0, 252.729178581786, 55.290836334228516, 23.290842056274414, 19.29083251953125, 64.70913696289062, 32.709163665771484, 28.709169387817383, 458.0, 912.0 },
			{ 64.89371880965717, 79.86402559195692, 0.9778405333384895, 28.00000041042766, 29.99999978779349, 23.99999931561297, 4.992907140619694, 4.9929065570245585, 4.992903503052461, 1.0000001168848505, 1.0000006116625517, 5.316100429760555, 0.0, 253.603629013893, 25.40903663635254, 27.409034729003906, 21.40903663635254, 30.590965270996094, 32.590965270996094, 26.59096336364746, 146.0, 288.0 },
	};
	/* Measure Region of the spheres (NumPy): no clipping, box, plane, flipped plane, box and plane */
	static final double[][] NUMPY_REGION = {
			{ 2457600.0, 2457600.0, 5.4113671875, 35.94458237310928, 0.0, 255.0, 13298976.0, 127.5, 52216.0, 52216.0, 0.021246744791666668 },
			{ 364800.0, 364800.0, 20.842554824561404, 68.5164647480465, 0.0, 255.0, 7603364.0, 127.5, 29844.0, 29844.0, 0.08180921052631579 },
			{ 1460763.0, 1460763.0, 5.214613869601023, 35.396572888769974, 0.0, 255.0, 7617315.0, 127.5, 29932.0, 29932.0, 0.020490661387233933 },
			{ 996837.0, 996837.0, 5.699689116676046, 36.73098308059864, 0.0, 255.0, 5681661.0, 127.5, 22284.0, 22284.0, 0.022354707941218074 },
			{ 82138.0, 82138.0, 2.355681901190679, 23.503250572118663, 0.0, 255.0, 193491.0, 127.5, 765.0, 765.0, 0.009313594195135017 },
	};

}
