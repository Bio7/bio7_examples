/*
 * ISO SURFACE: OBJECTS FLOATING IN A CHAMBER (FREE SPACE)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the iso surface of a hollow sphere is displayed. In its chamber three
 * objects float, without touching the wall or each other:
 *    a ball, a cube and a hollow ball (with its own small cavity, in which a
 *    tiny ball floats).
 * They can't be seen from outside. Their exact volumes are known, so the
 * measurements of the Volume tool can be checked.
 *
 * --- HOW THE CODE WORKS ---
 * 1. The solid is a field which is positive in the material:
 *       shell:       inside of the outer sphere and outside of the chamber
 *       ball, cube:  positive inside (the cube: a signed box distance)
 *       hollow ball: inside of its sphere and outside of its cavity
 *       tiny ball:   positive inside (in the cavity of the hollow ball)
 *       field = max(shell, ball, cube, hollow ball, tiny ball)
 *    Each object is a separate closed surface (an object of the table).
 * 2. The expected volumes are printed to the console (voxels^3).
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
 *    What                          Settings              Click on                  Expected
 *    the shell (material)          Object: Material      the sphere outside        about 409500
 *    the chamber, whole space      Cavity: Whole space   the inner wall (cut open) about 407500
 *    the chamber, free space       Cavity: Free space    the inner wall (cut open) about 387100
 *    the ball, the cube            -                     the object                about 7180, 4050
 *    the hollow ball, material     Object: Material      the hollow ball           about 7740
 *    the hollow ball, filled       Object: Filled        the hollow ball           about 9140
 *    its cavity, whole/free space  Cavity: ...           its inner wall (cut open) about 1405 / 1304
 * Colors: an object orange, a cavity blue; the objects in a cavity blue
 * (included, whole space) or red (subtracted, free space).
 * For the chamber: cut the sphere open with the Surface Eraser (drag over it)
 * and click on the inner wall through the opening (not on the back of the
 * outer wall: that is the sphere again).
 * The cuts don't change the measured volumes (the complete surfaces are
 * measured).
 */

package modeling3d.isosurface;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoFloatingObjects extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N voxels)
	static final int N = 128;
	// The hollow sphere: the outer radius and the radius of the chamber
	static final double SHELL_R = 58, CHAMBER_R = 46;
	// The ball: its center and radius
	static final double[] BALL = { -22, 0, 0 };
	static final double BALL_R = 12;
	// The cube: its center and half of its side (axis aligned)
	static final double[] CUBE = { 16, -14, 0 };
	static final double CUBE_H = 8;
	// The hollow ball: its center, its radius, the radius of its cavity and of the tiny ball in it
	static final double[] HOLLOW = { 8, 18, 10 };
	static final double HOLLOW_R = 13, HOLLOW_CAVITY_R = 7, TINY_R = 3;

	private boolean created;

	/* Start after compilation if run action is active! */
	public IsoFloatingObjects() {
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
		printExpectedVolumes();
	}

	private static double distance(double x, double y, double z, double[] c) {
		double dx = x - c[0], dy = y - c[1], dz = z - c[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/* The signed distance inside of an axis aligned cube (positive inside) */
	private static double insideCube(double x, double y, double z, double[] c, double h) {
		double qx = Math.abs(x - c[0]) - h, qy = Math.abs(y - c[1]) - h, qz = Math.abs(z - c[2]) - h;
		double ox = Math.max(qx, 0), oy = Math.max(qy, 0), oz = Math.max(qz, 0);
		double outside = Math.sqrt(ox * ox + oy * oy + oz * oz) + Math.min(Math.max(qx, Math.max(qy, qz)), 0);
		return -outside;
	}

	/** The field: positive in the material, 0 on the surfaces. */
	public static float[] createField(int n) {
		float[] field = new float[n * n * n];
		double center = (n - 1) / 2.0;
		double[] origin = { 0, 0, 0 };
		for (int z = 0; z < n; z++) {
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					double px = x - center, py = y - center, pz = z - center;
					// The hollow sphere: inside of the outer sphere and outside of the chamber
					double r = distance(px, py, pz, origin);
					double shell = Math.min(SHELL_R - r, r - CHAMBER_R);
					// The floating objects (positive inside)
					double ball = BALL_R - distance(px, py, pz, BALL);
					double cube = insideCube(px, py, pz, CUBE, CUBE_H);
					double h = distance(px, py, pz, HOLLOW);
					double hollow = Math.min(HOLLOW_R - h, h - HOLLOW_CAVITY_R);
					double tiny = TINY_R - h;
					double value = Math.max(Math.max(shell, ball), Math.max(cube, Math.max(hollow, tiny)));
					field[x + y * n + z * n * n] = (float) value;
				}
			}
		}
		return field;
	}

	private static double sphere(double r) {
		return 4.0 / 3.0 * Math.PI * r * r * r;
	}

	/** Prints the expected results of the measurement. */
	public static void printExpectedVolumes() {
		double chamber = sphere(CHAMBER_R);
		double ball = sphere(BALL_R);
		double cube = Math.pow(2 * CUBE_H, 3);
		double hollowFilled = sphere(HOLLOW_R), hollowCavity = sphere(HOLLOW_CAVITY_R), tiny = sphere(TINY_R);
		double free = chamber - ball - cube - hollowFilled;
		System.out.println("Hollow sphere with floating objects (all volumes in voxels^3):");
		System.out.printf("  Shell, material (R = %.0f, chamber r = %.0f):      %8.0f%n", SHELL_R, CHAMBER_R, sphere(SHELL_R) - chamber);
		System.out.printf("  Chamber, whole space inside of the wall:       %8.0f%n", chamber);
		System.out.printf("  Chamber, free space (without the objects):     %8.0f  (chamber - ball - cube - hollow ball filled)%n", free);
		System.out.printf("  Ball (r = %.0f):                                %8.0f%n", BALL_R, ball);
		System.out.printf("  Cube (side %.0f):                               %8.0f  (measured a bit smaller: the edges are cut)%n", 2 * CUBE_H, cube);
		System.out.printf("  Hollow ball, material:                         %8.0f%n", hollowFilled - hollowCavity);
		System.out.printf("  Hollow ball, filled:                           %8.0f%n", hollowFilled);
		System.out.printf("  Cavity of the hollow ball:                     %8.0f  (free space %.0f, without the tiny ball)%n", hollowCavity, hollowCavity - tiny);
		System.out.printf("  Tiny ball (r = %.0f):                            %8.0f%n", TINY_R, tiny);
		System.out.println("The measured volumes are about 1-2 % smaller (flat triangles inside of the curved surfaces; the smaller an");
		System.out.println("object compared with a voxel, the larger the difference).");
		System.out.println("Measure: cut the sphere open with the Surface Eraser, Volume tool on the inner wall: the chamber. With the");
		System.out.println("setting Cavity: Free space (tab Measure): the free space. Click on the objects too (Object: Material or Filled).");
	}
}
