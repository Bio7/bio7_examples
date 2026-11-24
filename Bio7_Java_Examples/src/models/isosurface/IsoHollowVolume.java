/*
 * ISO SURFACE: OUTER SHAPE, MATERIAL AND INNER VOLUME OF A HOLLOW VESSEL
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the iso surface of a thick-walled, egg-shaped vessel is displayed. Inside
 * is a closed chamber of another shape (a smaller, tilted ellipsoid, shifted to
 * one side: the wall is thin on one side and thick on the other). It can't be
 * seen from outside.
 *
 * The vessel has three different volumes, all known exactly:
 *    the outer shape (filled)   = the volume inside of the outer surface
 *    the inner volume (chamber) = the volume inside of the inner wall
 *    the material (the wall)    = outer shape - chamber
 *
 * --- HOW THE CODE WORKS ---
 * 1. The solid is a field which is positive in the material:
 *       outer:   inside of the outer ellipsoid (semi-axes A, B, C)
 *       chamber: outside of the inner ellipsoid (semi-axes a, b, c), shifted
 *                and tilted about the y axis
 *       field = min(outer, chamber)
 * 2. The exact volumes are printed to the console (voxels^3):
 *       ellipsoid 4/3 * pi * a * b * c
 *
 * --- MEASURE IT (Spatial view, tab "Measure") ---
 * Before: no clipping (box or plane) active (it cuts the surface and closes
 * it with caps: only the inside of the clipping is measured) and the smoothing
 * of the tab "Iso Surface" left at 0 (the example sets it).
 * Settings: tab "Measure", "Measured Volumes (Volume Tool)":
 *    Object:  Material (without its cavities) | Filled (with its cavities)
 *    Cavity:  Whole space (objects in it included) | Free space (without the
 *             objects in it)
 *    Display: On the surface | Through the surface | Only the measured volumes
 * Then the Volume tool (toolbar of the tab) and a click. The results are in the
 * table "3D Areas" (column Volume).
 *
 *    What                     Settings                       Click on            Expected
 *    the material (the wall)  Object: Material               the vessel outside  about 364500
 *    the outer shape (filled) Object: Filled                 the vessel outside  about 412600
 *    the chamber              (Cavity: Whole space)          the inner wall      about 48150
 * For the chamber: cut the vessel open with the Surface Eraser (drag over it,
 * e.g., on the thin side of the wall: the +x side) and click on the blue
 * inner wall through the opening (not on the back of the outer wall: that is
 * the vessel again). Or Display: Through the surface, then the measured
 * chamber is visible inside of the closed vessel.
 * The measured values are about 0.05 % smaller than the exact ones printed to
 * the console (flat triangles inside of the curved surfaces).
 * Check: material + chamber = outer shape.
 */

package models.isosurface;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoHollowVolume extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N voxels)
	static final int N = 128;
	// The outer shape: an egg-shaped ellipsoid (semi-axes)
	static final double OUTER_A = 56, OUTER_B = 44, OUTER_C = 40;
	// The chamber: a smaller ellipsoid, shifted in x and tilted about the y axis (degrees)
	static final double INNER_X = 14, INNER_A = 30, INNER_B = 24, INNER_C = 16, INNER_TILT = 30;

	private boolean created;

	/* Start after compilation if run action is active! */
	public IsoHollowVolume() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		created = false;
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setup();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (created) {
			return;
		}
		// The 3D view is available now
		IsoSurface iso = SpatialUtil.getIsoSurface();
		if (iso == null) {
			return;
		}
		created = true;
		iso.resetColors();
		float[] field = createField(N);
		// No smoothing: the measured volumes stay close to the exact ones
		iso.setIsoValue(0);
		iso.setSmoothing(0f);
		iso.setMeshSmoothing(0);
		iso.setOpacity(1f);
		iso.setColorMode(IsoSurface.COLOR_DEPTH);
		iso.setData(field, N, N, N);
		printExpectedVolumes(field);
	}

	/* Approximately the distance inside of an ellipsoid (positive inside, exact 0 on the surface) */
	private static double insideEllipsoid(double x, double y, double z, double a, double b, double c) {
		double ex = x / a, ey = y / b, ez = z / c;
		return (1 - Math.sqrt(ex * ex + ey * ey + ez * ez)) * Math.min(a, Math.min(b, c));
	}

	/** The field: positive in the material (the wall), 0 on the surfaces. */
	public static float[] createField(int n) {
		float[] field = new float[n * n * n];
		double center = (n - 1) / 2.0;
		double tilt = Math.toRadians(INNER_TILT), cos = Math.cos(tilt), sin = Math.sin(tilt);
		for (int z = 0; z < n; z++) {
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					double px = x - center, py = y - center, pz = z - center;
					// The outer shape (positive inside)
					double outer = insideEllipsoid(px, py, pz, OUTER_A, OUTER_B, OUTER_C);
					// The chamber, shifted and tilted (positive outside of it: in the wall)
					double qx = px - INNER_X;
					double rx = cos * qx + sin * pz, rz = -sin * qx + cos * pz;
					double chamber = -insideEllipsoid(rx, py, rz, INNER_A, INNER_B, INNER_C);
					field[x + y * n + z * n * n] = (float) Math.min(outer, chamber);
				}
			}
		}
		return field;
	}

	/** The exact volume of the outer shape (filled, voxels^3). */
	public static double outerVolume() {
		return 4.0 / 3.0 * Math.PI * OUTER_A * OUTER_B * OUTER_C;
	}

	/** The exact volume of the chamber (voxels^3). */
	public static double chamberVolume() {
		return 4.0 / 3.0 * Math.PI * INNER_A * INNER_B * INNER_C;
	}

	/** Prints the expected results of the measurement. */
	public static void printExpectedVolumes(float[] field) {
		long material = 0;
		for (float v : field) {
			if (v > 0) {
				material++;
			}
		}
		double outer = outerVolume(), chamber = chamberVolume();
		System.out.println("Hollow vessel (all volumes in voxels^3):");
		System.out.printf("  Outer shape, filled (A = %.0f, B = %.0f, C = %.0f): %8.0f  (4/3 pi A B C)%n", OUTER_A, OUTER_B, OUTER_C, outer);
		System.out.printf("  Inner volume, chamber (a = %.0f, b = %.0f, c = %.0f): %8.0f  (4/3 pi a b c)%n", INNER_A, INNER_B, INNER_C, chamber);
		System.out.printf("  Material, the wall (outer - chamber):          %8.0f  (counted voxels: %d)%n", outer - chamber, material);
		System.out.println("The measured volumes are about 1 % smaller: the surface consists of flat triangles inside of the curved surfaces.");
		System.out.println("Measure (tab Measure, no clipping active, Volume tool):");
		System.out.println("  Object: Material -> click on the vessel: the material; Object: Filled -> click on the vessel: the outer shape.");
		System.out.println("  Cut the vessel open with the Surface Eraser and click on the inner wall: the chamber (the cut doesn't change");
		System.out.println("  the measured volumes). Display: Through the surface shows the chamber inside of the closed vessel.");
	}
}
