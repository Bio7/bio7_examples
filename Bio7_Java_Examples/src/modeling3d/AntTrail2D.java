package modeling3d;

import static com.jogamp.opengl.GL2.*;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_COLOR_MATERIAL;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.spatial.SpatialUtil;
import java.util.stream.IntStream;

/**
 * 2D ANT TRAIL SIMULATION (Stigmergy)
 * 
 * Rules: 1. Ants move forward and deposit pheromones. 2. Ants "smell" three
 * areas ahead (Left, Center, Right). 3. They turn towards the highest pheromone
 * concentration. 4. Pheromones evaporate and diffuse over time.
 */
public class AntTrail2D extends com.eco.bio7.compile.Model {

	// Simulation Constants
	private final int RES = 100; // Grid resolution
	private final int ANT_COUNT = 500;
	private final float sensorAngle = 0.5f; // Radians
	private final float sensorDist = 3.0f;
	private final float turnSpeed = 0.2f;
	private final float moveSpeed = 1.0f;
	private final float evaporationRate = 0.96f;

	private float[] pheromones = new float[RES * RES];
	private float[] nextPheromones = new float[RES * RES];

	// Ant properties
	private float[] antX = new float[ANT_COUNT];
	private float[] antY = new float[ANT_COUNT];
	private float[] antAngle = new float[ANT_COUNT];

	/* Start after compilation if run action is active! */
	public AntTrail2D() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		setupModel();
	}

	private void setupModel() {
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

		// Start ants in the center with random directions
		for (int i = 0; i < ANT_COUNT; i++) {
			antX[i] = RES / 2.0f;
			antY[i] = RES / 2.0f;
			antAngle[i] = (float) (Math.random() * Math.PI * 2);
		}
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setupModel();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (SpatialUtil.canStep()) {
			updateAnts();
			updatePheromones();
		}
		render(gl, glut);
	}

	private void updateAnts() {
		for (int i = 0; i < ANT_COUNT; i++) {
			// 1. Sensing logic: Sample 3 points ahead
			float vCenter = sample(antX[i], antY[i], antAngle[i]);
			float vLeft = sample(antX[i], antY[i], antAngle[i] - sensorAngle);
			float vRight = sample(antX[i], antY[i], antAngle[i] + sensorAngle);

			// 2. Steering logic
			if (vCenter > vLeft && vCenter > vRight) {
				// Stay on path
			} else if (vCenter < vLeft && vCenter < vRight) {
				antAngle[i] += (Math.random() - 0.5) * 2 * turnSpeed; // Random choice
			} else if (vLeft > vRight) {
				antAngle[i] -= turnSpeed;
			} else if (vRight > vLeft) {
				antAngle[i] += turnSpeed;
			}

			// 3. Movement
			antX[i] += Math.cos(antAngle[i]) * moveSpeed;
			antY[i] += Math.sin(antAngle[i]) * moveSpeed;

			// 4. Boundary check (Bounce)
			if (antX[i] < 0 || antX[i] >= RES) {
				antAngle[i] = (float) Math.PI - antAngle[i];
			}
			if (antY[i] < 0 || antY[i] >= RES) {
				antAngle[i] = -antAngle[i];
			}

			// 5. Deposit pheromone
			int gx = (int) antX[i];
			int gy = (int) antY[i];
			if (gx >= 0 && gx < RES && gy >= 0 && gy < RES) {
				pheromones[gx + gy * RES] = 1.0f;
			}
		}
	}

	private float sample(float x, float y, float angle) {
		float sx = x + (float) Math.cos(angle) * sensorDist;
		float sy = y + (float) Math.sin(angle) * sensorDist;
		int gx = (int) sx, gy = (int) sy;
		if (gx >= 0 && gx < RES && gy >= 0 && gy < RES) {
			return pheromones[gx + gy * RES];
		}
		return -1.0f; // Wall or out of bounds
	}

	private void updatePheromones() {
		// Parallel processing for evaporation and diffusion
		IntStream.range(0, pheromones.length).parallel().forEach(i -> {
			// Evaporation
			nextPheromones[i] = pheromones[i] * evaporationRate;

			// Simple Diffusion (Blur)
			int x = i % RES, y = i / RES;
			if (x > 0 && x < RES - 1 && y > 0 && y < RES - 1) {
				float avg = (pheromones[i - 1] + pheromones[i + 1] + pheromones[i - RES] + pheromones[i + RES]) / 4.0f;
				nextPheromones[i] = nextPheromones[i] * 0.9f + avg * 0.1f;
			}
		});
		System.arraycopy(nextPheromones, 0, pheromones, 0, pheromones.length);
	}

	private void render(GL2 gl, GLUT glut) {

		gl.glEnable(GL_COLOR_MATERIAL);
		float scale = 20.0f;
		float offset = (RES * scale) / 2.0f;
		gl.glPushMatrix();
		gl.glTranslatef(-offset, -offset, 0);

		// 1. Draw Pheromone Trail as colored grid
		gl.glBegin(GL_QUADS);
		for (int i = 0; i < pheromones.length; i++) {
			if (pheromones[i] < 0.05f)
				continue;
			int x = i % RES, y = i / RES;
			gl.glColor4f(0.0f, 0.8f, 1.0f, Math.min(1.0f, pheromones[i])); // Cyan glow
			gl.glVertex2f(x * scale, y * scale);
			gl.glVertex2f((x + 1) * scale, y * scale);
			gl.glVertex2f((x + 1) * scale, (y + 1) * scale);
			gl.glVertex2f(x * scale, (y + 1) * scale);
		}
		gl.glEnd();

		// 2. Draw Ants
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		for (int i = 0; i < ANT_COUNT; i++) {
			gl.glPushMatrix();
			gl.glTranslatef(antX[i] * scale, antY[i] * scale, 1.0f);
			glut.glutSolidCube(scale * 0.8f);
			gl.glPopMatrix();
		}
		gl.glPopMatrix();
	}
}
