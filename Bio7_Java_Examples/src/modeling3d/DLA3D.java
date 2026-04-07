package modeling3d;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.spatial.SpatialUtil;

/**
 * 3D DIFFUSION-LIMITED AGGREGATION (DLA)
 * 
 * Logic: 1. A central seed is placed in the 3D grid. 2. Particles move randomly
 * (Brownian motion) through the space. 3. When a particle touches a fixed
 * (frozen) cell, it freezes too. 4. This creates fractal, coral-like growth
 * structures. In the 3D "Options Space" view increase the step rate in the "Time" tab
 * and start the model to see the aggregating cells.
 */
public class DLA3D extends com.eco.bio7.compile.Model {

	private final int RES = 50;
	private final int RES_SQ = RES * RES;
	private final int totalCells = RES * RES * RES;
	private byte[] grid = new byte[totalCells];

	private int cubeList = -1;
	private final float spacing = 15.0f;

	public void setup() {
		setupModel();
	}

	private void setupModel() {
		

		// Place a seed in the center
		grid[RES / 2 + (RES / 2) * RES + (RES / 2) * RES_SQ] = 1;
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {

		setupModel();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (cubeList == -1) {
			cubeList = gl.glGenLists(1);
			gl.glNewList(cubeList, GL_COMPILE);
			glut.glutSolidCube(spacing * 0.9f);
			gl.glEndList();
		}

		if (SpatialUtil.canStep()) {
			simulateGrowth();
		}

		render(gl);
	}

	private void simulateGrowth() {
		// We simulate several random walkers per step to speed up growth
		for (int p = 0; p < 100; p++) {
			int x = (int) (Math.random() * RES);
			int y = (int) (Math.random() * RES);
			int z = (int) (Math.random() * RES);

			// Random walker logic
			for (int step = 0; step < 200; step++) {
				int nx = x + (int) (Math.random() * 3) - 1;
				int ny = y + (int) (Math.random() * 3) - 1;
				int nz = z + (int) (Math.random() * 3) - 1;

				if (nx >= 0 && nx < RES && ny >= 0 && ny < RES && nz >= 0 && nz < RES) {
					if (grid[nx + ny * RES + nz * RES_SQ] == 1) {
						grid[x + y * RES + z * RES_SQ] = 1; // Freeze current position
						break;
					}
					x = nx;
					y = ny;
					z = nz;
				}
			}
		}
	}

	private void render(GL2 gl) {
		gl.glPushMatrix();
		float offset = (RES * spacing) / 2.0f;
		gl.glTranslatef(-offset, -offset, -offset);
		gl.glEnable(GL_COLOR_MATERIAL);

		for (int i = 0; i < totalCells; i++) {
			if (grid[i] == 1) {
				int z = i / RES_SQ;
				int y = (i % RES_SQ) / RES;
				int x = i % RES;
				gl.glPushMatrix();
				gl.glTranslatef(x * spacing, y * spacing, z * spacing);
				gl.glColor3f(0.5f, 0.8f, 0.4f); // Greenish crystal color
				gl.glCallList(cubeList);
				gl.glPopMatrix();
			}
		}
		gl.glPopMatrix();
	}
}
