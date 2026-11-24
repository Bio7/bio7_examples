/*
 * ISO SURFACE: DIFFUSION LIMITED AGGREGATION (SIMULATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. A cluster grows from a seed in the center of the volume. Particles start
 *    on a sphere around the cluster and move randomly (random walk). A
 *    particle which touches the cluster sticks to it. Particles which move too
 *    far away start again.
 *
 * 2. The cluster becomes a branched (fractal) structure, like mineral
 *    dendrites, corals or bacterial colonies under nutrient limitation.
 *
 * 3. The occupied voxels (1) are smoothed by the iso surface (setSmoothing)
 *    and the surface of 0.25 is displayed. The surface is colored by the
 *    depth (COLOR_DEPTH).
 */

package models.isosurface;

import java.util.Random;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoDLA3D extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 96;
	private static final int NN = N * N;
	// Attached particles per time step of Bio7
	private static final int PARTICLES_PER_STEP = 120;
	// The maximum number of particles
	private static final int MAX_PARTICLES = 12000;

	private final float[] grid = new float[N * NN];
	private final Random random = new Random();
	private IsoSurface iso;
	private double clusterRadius;
	private int particles;

	/* Start after compilation if run action is active! */
	public IsoDLA3D() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		java.util.Arrays.fill(grid, 0f);
		int c = N / 2;
		grid[c + c * N + c * NN] = 1;
		clusterRadius = 1;
		particles = 1;
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
			iso.setIsoValue(0.25);
			iso.setSmoothing(0.8f);
			iso.setMeshSmoothing(2);
			iso.setOpacity(1f);
			iso.setColorMode(IsoSurface.COLOR_DEPTH);
			iso.setData(grid, N, N, N);
		}
		if (SpatialUtil.canStep() && particles < MAX_PARTICLES && clusterRadius < N / 2.0 - 6) {
			for (int k = 0; k < PARTICLES_PER_STEP; k++) {
				addParticle();
			}
			iso.setData(grid, N, N, N);
		}
	}

	/* A random walk from a sphere around the cluster until it sticks! */
	private void addParticle() {
		int c = N / 2;
		double start = clusterRadius + 4;
		double kill = clusterRadius + 12;
		while (true) {
			// A random point on the start sphere
			double theta = 2 * Math.PI * random.nextDouble();
			double phi = Math.acos(2 * random.nextDouble() - 1);
			int x = (int) Math.round(c + start * Math.sin(phi) * Math.cos(theta));
			int y = (int) Math.round(c + start * Math.sin(phi) * Math.sin(theta));
			int z = (int) Math.round(c + start * Math.cos(phi));
			while (true) {
				switch (random.nextInt(6)) {
				case 0 -> x++;
				case 1 -> x--;
				case 2 -> y++;
				case 3 -> y--;
				case 4 -> z++;
				default -> z--;
				}
				double dx = x - c, dy = y - c, dz = z - c;
				double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (r > kill || x < 1 || y < 1 || z < 1 || x > N - 2 || y > N - 2 || z > N - 2) {
					break; // too far: start again
				}
				if (touches(x, y, z)) {
					grid[x + y * N + z * NN] = 1;
					clusterRadius = Math.max(clusterRadius, r);
					particles++;
					return;
				}
			}
		}
	}

	private boolean touches(int x, int y, int z) {
		int i = x + y * N + z * NN;
		return grid[i - 1] > 0 || grid[i + 1] > 0 || grid[i - N] > 0 || grid[i + N] > 0 || grid[i - NN] > 0
				|| grid[i + NN] > 0;
	}
}
