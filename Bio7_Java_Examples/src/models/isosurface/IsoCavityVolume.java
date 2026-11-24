/*
 * ISO SURFACE: MEASURE THE INNER VOLUME OF A COMPLEX SHAPE
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the iso surface of a bumpy "pebble" with three sealed chambers is
 * displayed: a sphere, a tilted torus (a ring) and an ellipsoid. They can't be
 * seen from outside. Their exact volumes are known, so the measurements of the
 * Volume tool can be checked.
 *
 * --- HOW THE CODE WORKS ---
 * 1. The solid is a signed distance field (positive in the material):
 *       outer:  the radius of a sphere modulated with bumps, r(dir) - |p|
 *       chambers: the distance outside of a sphere, a tilted torus and an
 *                 ellipsoid
 *       field = min(outer, sphere, torus, ellipsoid)
 *    The chambers are completely inside (walls of at least about 5 voxels):
 *    they are closed inner surfaces.
 *
 * 2. The expected volumes are printed to the console (in voxels^3):
 *       sphere cavity  4/3 * pi * r^3
 *       torus cavity   2 * pi^2 * R * r^2
 *       ellipsoid      4/3 * pi * a * b * c
 *       the material   counted voxels (the shape has no simple formula)
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
 * a) Object: Material, click on the pebble: the volume of the material (without
 *    its chambers).
 * b) Find the chambers: cut the pebble open with the Surface Eraser (drag over
 *    it). Then click on the inner wall of a chamber (blue): its volume. Compare
 *    it with the printed value.
 * c) Display: Through the surface: the measured chambers are shown inside of
 *    the pebble; Display: Only the measured volumes: only the measured volumes
 *    remain visible, e.g., the three chambers alone.
 */

package models.isosurface;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoCavityVolume extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N voxels)
	static final int N = 128;
	// The mean radius of the pebble and the height of its bumps (fractions of the radius)
	static final double RADIUS = 48, BUMPS = 0.15, RIPPLES = 0.07;
	// The sphere cavity: its center (x) and radius
	static final double SPHERE_X = -18, SPHERE_R = 12;
	// The torus cavity: its center (x), the radius of the ring, the radius of the tube, the tilt (degrees)
	static final double TORUS_X = 16, TORUS_R = 12, TORUS_TUBE = 5, TORUS_TILT = 35;
	// The ellipsoid cavity: its center and its semi-axes
	static final double ELLIPSOID_X = -4, ELLIPSOID_Y = 22, ELLIPSOID_Z = -6, ELLIPSOID_A = 10, ELLIPSOID_B = 6, ELLIPSOID_C = 7;

	private boolean created;

	/* Start after compilation if run action is active! */
	public IsoCavityVolume() {
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

	/** The signed distance field: positive in the material, 0 on the surface. */
	public static float[] createField(int n) {
		float[] field = new float[n * n * n];
		double c = (n - 1) / 2.0;
		double tilt = Math.toRadians(TORUS_TILT), cos = Math.cos(tilt), sin = Math.sin(tilt);
		for (int z = 0; z < n; z++) {
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					double px = x - c, py = y - c, pz = z - c;
					double r = Math.sqrt(px * px + py * py + pz * pz);
					// The outer surface: a sphere with bumps and ripples (direction dependent radius)
					double theta = Math.acos(r > 0 ? pz / r : 1), phi = Math.atan2(py, px);
					double radius = RADIUS * (1 + BUMPS * Math.sin(5 * theta) * Math.sin(4 * phi)
							+ RIPPLES * Math.cos(11 * theta + 2 * phi));
					double outer = radius - r;
					// The sphere cavity (positive outside of the cavity)
					double dx = px - SPHERE_X;
					double sphere = Math.sqrt(dx * dx + py * py + pz * pz) - SPHERE_R;
					// The torus cavity, tilted about the y axis
					double tx = px - TORUS_X;
					double qx = cos * tx + sin * pz, qz = -sin * tx + cos * pz;
					double ring = Math.sqrt(qx * qx + py * py) - TORUS_R;
					double torus = Math.sqrt(ring * ring + qz * qz) - TORUS_TUBE;
					// The ellipsoid cavity (exact on the surface, approximately a distance elsewhere)
					double ex = (px - ELLIPSOID_X) / ELLIPSOID_A, ey = (py - ELLIPSOID_Y) / ELLIPSOID_B, ez = (pz - ELLIPSOID_Z) / ELLIPSOID_C;
					double ellipsoid = (Math.sqrt(ex * ex + ey * ey + ez * ez) - 1) * Math.min(ELLIPSOID_A, Math.min(ELLIPSOID_B, ELLIPSOID_C));
					field[x + y * n + z * n * n] = (float) Math.min(Math.min(outer, sphere), Math.min(torus, ellipsoid));
				}
			}
		}
		return field;
	}

	/** The exact volume of the sphere cavity (voxels^3). */
	public static double sphereVolume() {
		return 4.0 / 3.0 * Math.PI * Math.pow(SPHERE_R, 3);
	}

	/** The exact volume of the torus cavity (voxels^3). */
	public static double torusVolume() {
		return 2 * Math.PI * Math.PI * TORUS_R * TORUS_TUBE * TORUS_TUBE;
	}

	/** The exact volume of the ellipsoid cavity (voxels^3). */
	public static double ellipsoidVolume() {
		return 4.0 / 3.0 * Math.PI * ELLIPSOID_A * ELLIPSOID_B * ELLIPSOID_C;
	}

	/** Prints the expected results of the measurement. */
	public static void printExpectedVolumes(float[] field) {
		long material = 0;
		for (float v : field) {
			if (v > 0) {
				material++;
			}
		}
		System.out.println("Pebble with three sealed chambers (all volumes in voxels^3):");
		System.out.printf("  Sphere chamber    (r = %.0f):              %6.0f  (4/3 pi r^3)%n", SPHERE_R, sphereVolume());
		System.out.printf("  Torus chamber     (R = %.0f, r = %.0f):       %6.0f  (2 pi^2 R r^2)%n", TORUS_R, TORUS_TUBE, torusVolume());
		System.out.printf("  Ellipsoid chamber (a = %.0f, b = %.0f, c = %.0f): %6.0f  (4/3 pi a b c)%n", ELLIPSOID_A, ELLIPSOID_B, ELLIPSOID_C, ellipsoidVolume());
		System.out.printf("  Material of the pebble:                 about %d  (counted voxels, without the chambers)%n", material);
		System.out.println("The measured chambers are about 1-2 % smaller: the surface consists of flat triangles inside of the curved");
		System.out.println("walls (the smaller a chamber compared with a voxel, the larger the difference; a finer grid reduces it).");
		System.out.println("Measure: tab Measure, Volume tool on the pebble (the object), then cut it open with the Surface Eraser");
		System.out.println("and click on the inner wall of a chamber. Show them with Display: Through the surface or Only the measured volumes.");
	}
}
