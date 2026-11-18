/*
 * ISO SURFACE: HEAT DIFFUSION FROM MOVING SOURCES (SIMULATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. The heat equation dT/dt = D * Laplace(T) - COOLING * T is solved with an
 *    explicit scheme (7-point Laplacian). The walls of the box are kept at
 *    T = 0 (Dirichlet borders).
 *
 * 2. Three heat sources move on circles through the volume and heat their
 *    position in every iteration.
 *
 * 3. The iso surface T = ISO (an isotherm) is displayed. It is colored with
 *    the temperature inside of the surface (COLOR_INTENSITY, Viridis): the
 *    regions close to the sources are bright.
 */

package modeling3d.isosurface;

import java.util.stream.IntStream;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoHeatDiffusion3D extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 64;
	private static final int NN = N * N;
	// Diffusion (stable for D < 1/6), cooling and heating
	private static final float D = 0.15f;
	private static final float COOLING = 0.004f;
	private static final float HEATING = 6.0f;
	// Model iterations per time step of Bio7
	private static final int ITERATIONS_PER_STEP = 8;
	// The isotherm
	private static final double ISO = 0.15;

	private float[] t = new float[N * NN];
	private float[] tNext = new float[N * NN];
	private IsoSurface iso;
	private double time;

	/* Start after compilation if run action is active! */
	public IsoHeatDiffusion3D() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		java.util.Arrays.fill(t, 0f);
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
			iso.setSmoothing(0.5f);
			iso.setMeshSmoothing(2);
			iso.setOpacity(1f);
			iso.setColorMode(IsoSurface.COLOR_INTENSITY);
		}
		if (SpatialUtil.canStep()) {
			for (int k = 0; k < ITERATIONS_PER_STEP; k++) {
				heatSources();
				diffuse();
				time += 0.02;
			}
			iso.setData(t, N, N, N);
		}
	}

	/* Three sources on circles in different planes! */
	private void heatSources() {
		double c = N / 2.0, r = N / 4.0;
		heat(c + r * Math.cos(time), c + r * Math.sin(time), c);
		heat(c, c + r * Math.cos(1.3 * time + 2), c + r * Math.sin(1.3 * time + 2));
		heat(c + r * Math.sin(0.8 * time + 4), c, c + r * Math.cos(0.8 * time + 4));
	}

	private void heat(double x, double y, double z) {
		int ix = (int) x, iy = (int) y, iz = (int) z;
		for (int dz = -1; dz <= 1; dz++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -1; dx <= 1; dx++) {
					int px = ix + dx, py = iy + dy, pz = iz + dz;
					if (px > 0 && py > 0 && pz > 0 && px < N - 1 && py < N - 1 && pz < N - 1) {
						t[px + py * N + pz * NN] += HEATING / 27f;
					}
				}
			}
		}
	}

	/* One explicit iteration, the borders stay 0! */
	private void diffuse() {
		IntStream.range(1, N - 1).parallel().forEach(z -> {
			for (int y = 1; y < N - 1; y++) {
				for (int x = 1; x < N - 1; x++) {
					int i = x + y * N + z * NN;
					float lap = t[i - 1] + t[i + 1] + t[i - N] + t[i + N] + t[i - NN] + t[i + NN] - 6 * t[i];
					tNext[i] = t[i] + D * lap - COOLING * t[i];
				}
			}
		});
		float[] swap = t;
		t = tNext;
		tNext = swap;
	}
}
