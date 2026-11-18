/*
 * ISO SURFACE: DENSITY OF THE LORENZ ATTRACTOR (SIMULATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. The Lorenz system (a simple model of atmospheric convection) is chaotic:
 *       dx/dt = SIGMA * (y - x)
 *       dy/dt = x * (RHO - z) - y
 *       dz/dt = x * y - BETA * z
 *    It is integrated with the Runge-Kutta method (4th order).
 *
 * 2. The visited points are counted in a 3D grid (a histogram of the
 *    trajectory). The density converges to the shape of the "butterfly"
 *    attractor. The z axis of the Lorenz system is the vertical axis of the 3D
 *    view (the first row of the grid is displayed at the top).
 *
 * 3. The logarithm of the density (normalized to 0 - 1) is handed to the iso
 *    surface. The surface of ISO encloses the frequently visited regions and is
 *    colored with the density inside (COLOR_INTENSITY).
 */

package modeling3d.isosurface;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoLorenzDensity extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 72;
	// The parameters of the Lorenz system
	private static final double SIGMA = 10, RHO = 28, BETA = 8.0 / 3.0;
	// Integration steps per time step of Bio7 and the step size
	private static final int ITERATIONS_PER_STEP = 20000;
	private static final double DT = 0.005;
	// The region of the attractor (x, y, z)
	private static final double[] MIN = { -25, -30, 0 };
	private static final double[] MAX = { 25, 30, 55 };
	// The iso value (normalized logarithmic density)
	private static final double ISO = 0.25;

	private final int[] counts = new int[N * N * N];
	private final float[] density = new float[N * N * N];
	private final double[] state = new double[3];
	private IsoSurface iso;
	private int maxCount;

	/* Start after compilation if run action is active! */
	public IsoLorenzDensity() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		java.util.Arrays.fill(counts, 0);
		maxCount = 0;
		state[0] = 1;
		state[1] = 1;
		state[2] = 20;
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
			iso.setSmoothing(1.5f);
			iso.setMeshSmoothing(3);
			iso.setOpacity(1f);
			iso.setColorMode(IsoSurface.COLOR_INTENSITY);
		}
		if (SpatialUtil.canStep()) {
			for (int k = 0; k < ITERATIONS_PER_STEP; k++) {
				rungeKutta();
				count();
			}
			updateDensity();
			iso.setData(density, N, N, N);
		}
	}

	private void derivative(double[] s, double[] d) {
		d[0] = SIGMA * (s[1] - s[0]);
		d[1] = s[0] * (RHO - s[2]) - s[1];
		d[2] = s[0] * s[1] - BETA * s[2];
	}

	private final double[] k1 = new double[3], k2 = new double[3], k3 = new double[3], k4 = new double[3],
			tmp = new double[3];

	private void rungeKutta() {
		derivative(state, k1);
		for (int i = 0; i < 3; i++) {
			tmp[i] = state[i] + DT / 2 * k1[i];
		}
		derivative(tmp, k2);
		for (int i = 0; i < 3; i++) {
			tmp[i] = state[i] + DT / 2 * k2[i];
		}
		derivative(tmp, k3);
		for (int i = 0; i < 3; i++) {
			tmp[i] = state[i] + DT * k3[i];
		}
		derivative(tmp, k4);
		for (int i = 0; i < 3; i++) {
			state[i] += DT / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
		}
	}

	/* Counts the current point in the grid (x, z from the top, y as the depth)! */
	private void count() {
		int[] g = new int[3];
		for (int i = 0; i < 3; i++) {
			g[i] = (int) ((state[i] - MIN[i]) / (MAX[i] - MIN[i]) * (N - 1));
			if (g[i] < 0 || g[i] >= N) {
				return;
			}
		}
		int index = g[0] + (N - 1 - g[2]) * N + g[1] * N * N;
		counts[index]++;
		maxCount = Math.max(maxCount, counts[index]);
	}

	/* The normalized logarithm of the counts! */
	private void updateDensity() {
		double norm = Math.log(1 + maxCount);
		for (int i = 0; i < counts.length; i++) {
			density[i] = (float) (Math.log(1 + counts[i]) / norm);
		}
	}
}
