/*
 * ISO SURFACE: INTERFERENCE OF SPHERICAL WAVES (SIMULATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. Three point sources emit spherical waves with the same wave length:
 *       u(p, t) = sum_i sin(k * r_i - omega * t) / (1 + r_i / DAMPING)
 *    (r_i = the distance of the point p to the source i)
 *
 * 2. Where the waves meet in phase they add up (constructive interference).
 *    The iso surface of u = ISO shows these regions. They move with the time.
 *
 * 3. The surface is colored by the depth (COLOR_DEPTH) and is semi
 *    transparent (setOpacity), so the inner surfaces are visible.
 */

package modeling3d.isosurface;

import java.util.stream.IntStream;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoWaveInterference extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 80;
	// Wave number (2 pi / wave length in voxels), angular frequency, damping
	private static final double K = 2 * Math.PI / 16.0;
	private static final double OMEGA = 0.3;
	private static final double DAMPING = 40;
	// The iso value (constructive interference)
	private static final double ISO = 1.2;
	// The positions of the sources
	private static final double[][] SOURCES = { { 20, 40, 40 }, { 60, 40, 40 }, { 40, 40, 70 } };

	private final float[] field = new float[N * N * N];
	private IsoSurface iso;
	private double time;

	/* Start after compilation if run action is active! */
	public IsoWaveInterference() {
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
			iso.setIsoValue(ISO);
			iso.setSmoothing(0f);
			iso.setMeshSmoothing(2);
			iso.setOpacity(0.7f);
			iso.setColorMode(IsoSurface.COLOR_DEPTH);
			computeField();
			iso.setData(field, N, N, N);
		}
		if (SpatialUtil.canStep()) {
			time += 1;
			computeField();
			iso.setData(field, N, N, N);
		}
	}

	private void computeField() {
		IntStream.range(0, N).parallel().forEach(z -> {
			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					double u = 0;
					for (double[] s : SOURCES) {
						double dx = x - s[0], dy = y - s[1], dz = z - s[2];
						double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
						u += Math.sin(K * r - OMEGA * time) / (1 + r / DAMPING);
					}
					field[x + y * N + z * N * N] = (float) u;
				}
			}
		});
	}
}
