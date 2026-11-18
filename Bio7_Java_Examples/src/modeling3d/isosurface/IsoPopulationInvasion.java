/*
 * ISO SURFACE: INVASION OF A POPULATION IN A HETEROGENEOUS HABITAT (SIMULATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. The population density u follows the Fisher-KPP equation (logistic growth
 *    and diffusion = random dispersal):
 *       du/dt = D * Laplace(u) + R * u * (1 - u / K(x, y, z))
 *
 * 2. The habitat is heterogeneous: two walls with a low carrying capacity K
 *    (barriers, e.g., a river or a road) have a few openings (corridors). The
 *    population starts in a corner, spreads as a travelling wave and has to
 *    pass the corridors.
 *
 * 3. The iso surface u = 0.5 (the invasion front) is displayed. The surface is
 *    colored with the density inside of the front (COLOR_INTENSITY, Viridis)
 *    and is semi transparent, so the barriers inside are visible.
 */

package modeling3d.isosurface;

import java.util.stream.IntStream;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoPopulationInvasion extends com.eco.bio7.compile.Model {

	// The resolution of the habitat (N x N x N)
	private static final int N = 72;
	private static final int NN = N * N;
	// Dispersal (stable for D < 1/6) and growth rate
	private static final float D = 0.12f;
	private static final float R = 0.08f;
	// Carrying capacity of the habitat and of the barriers
	private static final float K_HABITAT = 1.0f;
	private static final float K_BARRIER = 0.05f;
	// Model iterations per time step of Bio7
	private static final int ITERATIONS_PER_STEP = 15;

	private float[] u = new float[N * NN];
	private float[] uNext = new float[N * NN];
	private final float[] capacity = new float[N * NN];
	private IsoSurface iso;

	/* Start after compilation if run action is active! */
	public IsoPopulationInvasion() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		createHabitat();
		java.util.Arrays.fill(u, 0f);
		// The initial population in a corner
		for (int z = 2; z < 10; z++) {
			for (int y = 2; y < 10; y++) {
				for (int x = 2; x < 10; x++) {
					u[x + y * N + z * NN] = 1f;
				}
			}
		}
		iso = null;
		/* Start the run method! */
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	/* Two barrier walls (x = N/3 and x = 2N/3) with openings! */
	private void createHabitat() {
		for (int z = 0; z < N; z++) {
			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					float k = K_HABITAT;
					boolean wall1 = Math.abs(x - N / 3) <= 1;
					boolean wall2 = Math.abs(x - 2 * N / 3) <= 1;
					// The openings: one corridor in the first wall, two in the second wall
					boolean open1 = Math.abs(y - 3 * N / 4) < 6 && Math.abs(z - N / 4) < 6;
					boolean open2 = (Math.abs(y - N / 4) < 5 && Math.abs(z - 3 * N / 4) < 5)
							|| (Math.abs(y - N / 2) < 5 && Math.abs(z - N / 2) < 5);
					if ((wall1 && !open1) || (wall2 && !open2)) {
						k = K_BARRIER;
					}
					capacity[x + y * N + z * NN] = k;
				}
			}
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
			iso.setIsoValue(0.5);
			iso.setSmoothing(0.5f);
			iso.setMeshSmoothing(2);
			iso.setOpacity(0.8f);
			iso.setColorMode(IsoSurface.COLOR_INTENSITY);
			iso.setData(u, N, N, N);
		}
		if (SpatialUtil.canStep()) {
			for (int k = 0; k < ITERATIONS_PER_STEP; k++) {
				grow();
			}
			iso.setData(u, N, N, N);
		}
	}

	/* One explicit iteration (no flux at the borders)! */
	private void grow() {
		IntStream.range(0, N).parallel().forEach(z -> {
			int zm = Math.max(0, z - 1) * NN, zp = Math.min(N - 1, z + 1) * NN, zc = z * NN;
			for (int y = 0; y < N; y++) {
				int ym = Math.max(0, y - 1) * N, yp = Math.min(N - 1, y + 1) * N, yc = y * N;
				for (int x = 0; x < N; x++) {
					int xm = Math.max(0, x - 1), xp = Math.min(N - 1, x + 1);
					int i = x + yc + zc;
					float lap = u[xm + yc + zc] + u[xp + yc + zc] + u[x + ym + zc] + u[x + yp + zc] + u[x + yc + zm]
							+ u[x + yc + zp] - 6 * u[i];
					float value = u[i] + D * lap + R * u[i] * (1 - u[i] / capacity[i]);
					uNext[i] = Math.max(0, value);
				}
			}
		});
		float[] swap = u;
		u = uNext;
		uNext = swap;
	}
}
