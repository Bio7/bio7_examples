/*
 * ISO SURFACE: MORPHING SUPERQUADRIC (ANIMATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. A superquadric is the surface |x|^p + |y|^p + |z|^p = 1. With p = 1 it is
 *    an octahedron, with p = 2 a sphere and with a large p a cube. The
 *    exponent p changes with the time, so the shape morphs from an octahedron
 *    over a sphere to a cube and back.
 *
 * 2. The shape is twisted around the vertical (y) axis: the rotation angle of a
 *    layer grows with its height. The twist also changes with the time.
 *
 * 3. The field 1 - (|x|^p + |y|^p + |z|^p) is positive inside, the iso value is
 *    0. The surface is colored by the depth (COLOR_DEPTH).
 */

package modeling3d.isosurface;

import java.util.stream.IntStream;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoMorphingShapes extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 90;
	// The extent of the volume [-SIZE, SIZE]^3
	private static final double SIZE = 1.5;

	private final float[] field = new float[N * N * N];
	private IsoSurface iso;
	private double time;

	/* Start after compilation if run action is active! */
	public IsoMorphingShapes() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		time = 0;
		iso = null;
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
			iso.setSmoothing(0f);
			iso.setMeshSmoothing(1);
			iso.setOpacity(1f);
			iso.setColorMode(IsoSurface.COLOR_DEPTH);
			computeField();
			iso.setData(field, N, N, N);
		}
		if (SpatialUtil.canStep()) {
			time += 0.05;
			computeField();
			iso.setData(field, N, N, N);
		}
	}

	private void computeField() {
		// The exponent: 1 (octahedron) - 2 (sphere) - 8 (cube)
		final double p = Math.pow(2, 1.5 + 1.5 * Math.sin(time));
		// The twist (radians per unit of height)
		final double twist = 1.2 * Math.sin(time * 0.7);
		final double scale = 2 * SIZE / (N - 1);
		IntStream.range(0, N).parallel().forEach(z -> {
			for (int y = 0; y < N; y++) {
				// The first row (y index 0) is displayed at the top
				double py = SIZE - y * scale;
				double angle = twist * py;
				double cos = Math.cos(angle), sin = Math.sin(angle);
				for (int x = 0; x < N; x++) {
					double px = -SIZE + x * scale, pz = -SIZE + z * scale;
					// Rotate the layer around the y axis
					double rx = cos * px - sin * pz;
					double rz = sin * px + cos * pz;
					double value = Math.pow(Math.abs(rx), p) + Math.pow(Math.abs(py), p) + Math.pow(Math.abs(rz), p);
					field[x + y * N + z * N * N] = (float) Math.max(-5, 1 - value);
				}
			}
		});
	}
}
