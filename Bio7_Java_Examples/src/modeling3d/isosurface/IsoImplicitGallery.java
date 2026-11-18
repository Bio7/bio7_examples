/*
 * ISO SURFACE: A GALLERY OF IMPLICIT SURFACES (GEOMETRY)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 * Every SHOW_STEPS time steps the next surface is displayed.
 *
 * --- HOW THE CODE WORKS ---
 * 1. An implicit surface is the set of points with f(x, y, z) = 0. The field
 *    -f(x, y, z) is positive inside, so the iso value 0 is the surface:
 *    - Heart:       (x^2 + 9/4 z^2 + y^2 - 1)^3 - x^2 y^3 - 9/80 z^2 y^3 = 0
 *    - Torus:       (R - sqrt(x^2 + z^2))^2 + y^2 - r^2 = 0
 *    - Tangle cube: x^4 - 5x^2 + y^4 - 5y^2 + z^4 - 5z^2 + 11.8 = 0
 *    - Genus 2:     2z(z^2 - 3x^2)(1 - y^2) + (x^2 + z^2)^2 - (9y^2 - 1)(1 - y^2) = 0
 *    (y is the vertical axis in the 3D view)
 *
 * 2. The field of the surface is sampled in a cube [-size, size]^3 and handed
 *    to the iso surface of the 3D view (setData). Like an image in ImageJ the
 *    first row (y index 0) is displayed at the top, so y is sampled from
 *    +size (top) to -size (bottom).
 */

package modeling3d.isosurface;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoImplicitGallery extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 100;
	// The number of time steps a surface is shown
	private static final int SHOW_STEPS = 20;

	/* An implicit function f(x, y, z) (negative inside) with a sampling size! */
	private interface Implicit {
		double f(double x, double y, double z);
	}

	private static final String[] NAMES = { "Heart", "Torus", "Tangle Cube", "Genus 2" };
	private static final double[] SIZES = { 1.4, 1.6, 3.0, 1.4 };
	private static final Implicit[] SURFACES = {
			/* Heart (the y axis is the vertical axis) */
			(x, y, z) -> Math.pow(x * x + 2.25 * z * z + y * y - 1, 3) - x * x * y * y * y - 0.1125 * z * z * y * y * y,
			/* Torus with the radii R = 1 and r = 0.4 */
			(x, y, z) -> Math.pow(1.0 - Math.sqrt(x * x + z * z), 2) + y * y - 0.16,
			/* Tangle cube */
			(x, y, z) -> x * x * x * x - 5 * x * x + y * y * y * y - 5 * y * y + z * z * z * z - 5 * z * z + 11.8,
			/* Genus 2 surface */
			(x, y, z) -> 2 * z * (z * z - 3 * x * x) * (1 - y * y) + Math.pow(x * x + z * z, 2)
					- (9 * y * y - 1) * (1 - y * y) };

	private IsoSurface iso;
	private int step;
	private int shown = -1;

	/* Start after compilation if run action is active! */
	public IsoImplicitGallery() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		iso = null;
		step = 0;
		shown = -1;
		/* Start the run method! */
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setup();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (iso == null) {
			iso = SpatialUtil.getIsoSurface();
			if (iso == null) {
				return;
			}
			// Start with the default colors (the iso surface keeps the settings of a former model)
			iso.resetColors();
			iso.setIsoValue(0);
			iso.setSmoothing(0.6f);
			iso.setMeshSmoothing(3);
			iso.setOpacity(1f);
			iso.setColorMode(IsoSurface.COLOR_DEPTH);
		}
		if (SpatialUtil.canStep()) {
			step++;
		}
		int index = (step / SHOW_STEPS) % SURFACES.length;
		if (index != shown) {
			shown = index;
			show(index);
		}
	}

	/* Samples the surface and displays it! */
	private void show(int index) {
		float[] field = new float[N * N * N];
		double size = SIZES[index];
		double scale = 2 * size / (N - 1);
		for (int z = 0; z < N; z++) {
			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					double f = SURFACES[index].f(-size + x * scale, size - y * scale, -size + z * scale);
					// Positive inside, limited (the steep functions grow fast)
					field[x + y * N + z * N * N] = (float) Math.max(-10, Math.min(10, -f));
				}
			}
		}
		iso.setData(field, N, N, N);
		System.out.println("Iso surface: " + NAMES[index]);
	}
}
