package gdal;

import java.nio.file.Files;
import java.nio.file.Path;

import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.ui.GdalActivation;

/*
 * Helpers of the GDAL examples: GDAL is loaded as in Bio7 (a new library is
 * tested first in a separate process) and the folder of the example data.
 *
 * GDAL must be installed (Preferences > Preferences GDAL > Install GDAL...),
 * e.g. brew install gdal, conda install -c conda-forge libgdal-core,
 * apt install gdal-bin, OSGeo4W.
 */
public class GdalSetup {

	/** Loads GDAL if necessary: true if it is loaded (else the reason is printed). */
	public static boolean load() {
		if (Gdal.isLoaded()) {
			return true;
		}
		String error = GdalActivation.loadFirstWorking(GdalActivation.libraries());
		if (error != null) {
			System.err.println("GDAL could not be loaded:\n" + error + "\n\nPlease install GDAL (Preferences > Preferences GDAL > Install GDAL...).");
			return false;
		}
		System.out.println("GDAL " + Gdal.version() + " (" + Gdal.library() + ")");
		return true;
	}

	/** The folder of the example data (in the temporary folder). */
	public static Path folder() throws Exception {
		Path dir = Path.of(System.getProperty("java.io.tmpdir"), "bio7_gdal_examples");
		Files.createDirectories(dir);
		return dir;
	}

	/**
	 * An elevation model as array, row by row (in meters): two hills, a ridge from
	 * the southwest to the northeast, a valley with a stream and a little
	 * roughness (as a landscape of the foothills of the Alps).
	 */
	public static float[] terrain(int w, int h) {
		float[] z = new float[w * h];
		java.util.Random random = new java.util.Random(3);
		/* Smooth roughness: random bumps (no regular pattern) */
		double[][] bumps = new double[40][];
		for (int i = 0; i < bumps.length; i++) {
			bumps[i] = new double[] { random.nextDouble() * w, random.nextDouble() * h, 8 + random.nextDouble() * 25, (random.nextDouble() - 0.5) * 30 };
		}
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double v = 450;
				/* Two hills */
				v += 380 * gauss(x - w * 0.30, y - h * 0.35, w * 0.12);
				v += 250 * gauss(x - w * 0.62, y - h * 0.70, w * 0.09);
				/* A ridge from the southwest to the northeast */
				double along = (x - w * 0.5) * 0.8 + (h * 0.5 - y) * 0.6, across = (x - w * 0.5) * 0.6 - (h * 0.5 - y) * 0.8;
				v += 160 * Math.exp(-across * across / (2 * Math.pow(w * 0.05, 2))) * Math.exp(-along * along / (2 * Math.pow(w * 0.35, 2)));
				/* A valley from north to south (meandering) */
				double cx = w * 0.82 + 15 * Math.sin(y / 25.0);
				v -= 120 * Math.exp(-Math.pow(x - cx, 2) / (2 * Math.pow(w * 0.04, 2)));
				/* The general slope down to the south */
				v -= 0.25 * y;
				for (double[] b : bumps) {
					v += b[3] * gauss(x - b[0], y - b[1], b[2]);
				}
				z[y * w + x] = (float) v;
			}
		}
		return z;
	}

	private static double gauss(double dx, double dy, double sigma) {
		return Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
	}
}
