/*
 * ISO SURFACE: 3D REACTION-DIFFUSION (GRAY-SCOTT, SIMULATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. The model: Two substances U and V react and diffuse in a 3D grid
 *    (periodic borders):
 *       U + 2V -> 3V   (V grows by consuming U)
 *    U is fed with the rate F, V is removed with the rate F + K. Starting from a
 *    few seeds, V forms growing and dividing 3D structures.
 *
 * 2. The iso surface: In every time step the concentration of V is handed to
 *    the iso surface of the 3D view (IsoSurface.setData). The surface of V = ISO
 *    is computed in the background (Surface Nets, smoothed) and displayed with
 *    the next frame, colored by the depth (z).
 *    Call iso.computeNow() after setData to compute the surface in the step
 *    (slower but the surface always fits to the step).
 *
 * 3. Options: The iso value, the smoothing, the colors and the opacity can also
 *    be changed in the "Iso Surface" tab of the "Options Space" view. The
 *    surface can be exported there (OBJ, STL, PLY, glTF, VTK) or with
 *    iso.export(new File("surface.glb")).
 *
 * Stop the "Play/Pause" action to recompile.
 */

package models.isosurface;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoReactionDiffusion3D extends com.eco.bio7.compile.Model {

	// The size of the grid (N x N x N)
	private static final int N = 64;
	private static final int NN = N * N;
	// Model iterations per time step of Bio7
	private static final int ITERATIONS_PER_STEP = 10;
	// Diffusion rates, feed rate and kill rate
	private static final float DU = 0.16f;
	private static final float DV = 0.08f;
	private static final float F = 0.035f;
	private static final float K = 0.065f;
	// The iso value of the surface (concentration of V)
	private static final double ISO = 0.2;

	private float[] u = new float[N * NN];
	private float[] v = new float[N * NN];
	private float[] uNext = new float[N * NN];
	private float[] vNext = new float[N * NN];
	private IsoSurface iso;

	/* Start after compilation if run action is active! */
	public IsoReactionDiffusion3D() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		setupModel();
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setupModel();
	}

	private void setupModel() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		// Start with U = 1 and V = 0, a few random seeds of V
		Arrays.fill(u, 1f);
		Arrays.fill(v, 0f);
		Random random = new Random();
		for (int s = 0; s < 6; s++) {
			int cx = 16 + random.nextInt(32), cy = 16 + random.nextInt(32), cz = 16 + random.nextInt(32);
			for (int z = cz - 3; z <= cz + 3; z++) {
				for (int y = cy - 3; y <= cy + 3; y++) {
					for (int x = cx - 3; x <= cx + 3; x++) {
						int i = x + y * N + z * NN;
						u[i] = 0.5f;
						v[i] = 0.25f + 0.05f * random.nextFloat();
					}
				}
			}
		}
		iso = null;
		/* Start the run method! */
		if (!SpatialUtil.isStarted()) {
			SpatialUtil.startStop();
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// The 3D view is available now: configure the iso surface once
		if (iso == null) {
			iso = SpatialUtil.getIsoSurface();
			if (iso == null) {
				return;
			}
			// Start with the default colors (the iso surface keeps the settings of a former model)
			iso.resetColors();
			iso.setIsoValue(ISO);
			iso.setSmoothing(0.8f);
			iso.setMeshSmoothing(3);
			iso.setColorMode(IsoSurface.COLOR_DEPTH);
			iso.setData(v, N, N, N);
		}
		// Trigger logic update via Bio7 control panel
		if (SpatialUtil.canStep()) {
			for (int k = 0; k < ITERATIONS_PER_STEP; k++) {
				calculateParallel();
			}
			// The surface is computed in the background (the data is copied)
			iso.setData(v, N, N, N);
		}
		// The iso surface is drawn by the 3D view (no OpenGL code necessary)
	}

	/* One Gray-Scott iteration (explicit, 7-point Laplacian, periodic borders)! */
	private void calculateParallel() {
		IntStream.range(0, N).parallel().forEach(z -> {
			int zm = ((z - 1 + N) % N) * NN, zp = ((z + 1) % N) * NN, zc = z * NN;
			for (int y = 0; y < N; y++) {
				int ym = ((y - 1 + N) % N) * N, yp = ((y + 1) % N) * N, yc = y * N;
				for (int x = 0; x < N; x++) {
					int xm = (x - 1 + N) % N, xp = (x + 1) % N;
					int i = x + yc + zc;
					float lapU = u[xm + yc + zc] + u[xp + yc + zc] + u[x + ym + zc] + u[x + yp + zc] + u[x + yc + zm]
							+ u[x + yc + zp] - 6 * u[i];
					float lapV = v[xm + yc + zc] + v[xp + yc + zc] + v[x + ym + zc] + v[x + yp + zc] + v[x + yc + zm]
							+ v[x + yc + zp] - 6 * v[i];
					float uvv = u[i] * v[i] * v[i];
					uNext[i] = u[i] + DU * lapU - uvv + F * (1 - u[i]);
					vNext[i] = v[i] + DV * lapV + uvv - (F + K) * v[i];
				}
			}
		});
		float[] t = u;
		u = uNext;
		uNext = t;
		t = v;
		v = vNext;
		vNext = t;
	}
}
