/*
 * ISO SURFACE: METABALLS (ANIMATION)
 *
 * Compile the class with the Bio7 Java compiler. The 3D perspective is opened
 * and the model starts (see the "Play/Pause" action in the main toolbar).
 *
 * --- HOW THE CODE WORKS ---
 * 1. Metaballs: Every ball creates a field r^2 / d^2 (d = the distance to the
 *    center of the ball). The fields are added and the iso surface of the sum
 *    (iso value 1) is displayed. Close balls merge smoothly into one surface.
 *
 * 2. The balls move with a constant velocity and bounce at the walls of the
 *    box. In every time step the field is computed again (parallel) and handed
 *    to the iso surface (computed in the background).
 *
 * 3. The surface is colored with the field one voxel inside of the surface
 *    (COLOR_INTENSITY): the centers of the balls are bright.
 */

package models.isosurface;

import java.util.Random;
import java.util.stream.IntStream;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.IsoSurface;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

public class IsoMetaballs extends com.eco.bio7.compile.Model {

	// The resolution of the volume (N x N x N)
	private static final int N = 80;
	// The number of balls
	private static final int BALLS = 8;

	private final double[][] position = new double[BALLS][3];
	private final double[][] velocity = new double[BALLS][3];
	private final double[] radius = new double[BALLS];
	private final float[] field = new float[N * N * N];
	private IsoSurface iso;

	/* Start after compilation if run action is active! */
	public IsoMetaballs() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		Work.openPerspective("com.eco.bio7.perspective_3d");
		Random random = new Random();
		for (int i = 0; i < BALLS; i++) {
			radius[i] = 6 + 6 * random.nextDouble();
			for (int k = 0; k < 3; k++) {
				position[i][k] = radius[i] + random.nextDouble() * (N - 2 * radius[i]);
				velocity[i][k] = (random.nextDouble() - 0.5) * 1.6;
			}
		}
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
			iso.setIsoValue(1.0);
			iso.setSmoothing(0f);
			iso.setMeshSmoothing(2);
			iso.setOpacity(1f);
			iso.setColorMode(IsoSurface.COLOR_INTENSITY);
			computeField();
			iso.setData(field, N, N, N);
		}
		if (SpatialUtil.canStep()) {
			move();
			computeField();
			iso.setData(field, N, N, N);
		}
	}

	/* Moves the balls and bounces at the walls! */
	private void move() {
		for (int i = 0; i < BALLS; i++) {
			for (int k = 0; k < 3; k++) {
				position[i][k] += velocity[i][k];
				if (position[i][k] < radius[i] || position[i][k] > N - 1 - radius[i]) {
					velocity[i][k] = -velocity[i][k];
					position[i][k] += 2 * velocity[i][k];
				}
			}
		}
	}

	/* The sum of the ball fields (limited for the colors)! */
	private void computeField() {
		IntStream.range(0, N).parallel().forEach(z -> {
			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					double sum = 0;
					for (int i = 0; i < BALLS; i++) {
						double dx = x - position[i][0], dy = y - position[i][1], dz = z - position[i][2];
						sum += radius[i] * radius[i] / (dx * dx + dy * dy + dz * dz + 1e-6);
					}
					field[x + y * N + z * N * N] = (float) Math.min(4, sum);
				}
			}
		});
	}
}
