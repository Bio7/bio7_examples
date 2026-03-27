/*
 * 3D GAME OF LIFE - BIO7 OPENGL MODEL
 * 
 * --- HOW THE CODE WORKS ---
 * 1. Grid Initialization: 
 *    The setup() method creates a 3D space and "seeds" it with a random population 
 *    (approx. 15% density).
 * 
 * 2. Evolution Logic: 
 *    In calculateParallel(), the model evaluates every cell by counting its 26 
 *    direct neighbors in a 3x3x3 Moore neighborhood (minus the center cell).
 * 
 * 3. Rule Set (4-5/5):
 *    - Survival: A living cell stays alive if it has 4 or 5 neighbors.
 *    - Birth: A dead cell becomes alive if it has exactly 5 neighbors.
 *    - Death: Otherwise, the cell dies or remains dead.
 * 
 * 4. Rendering: 
 *    The renderFast() method iterates through the grid and draws a cube 
 *    at the corresponding coordinates for every "alive" cell.
 * 
 * --- APPLIED OPTIMIZATIONS ---
 * A. Parallel Processing (Multithreading):
 *    Uses 'IntStream.range(...).parallel()' to split logic across all available 
 *    CPU cores. This prevents the "logic-bottleneck" when increasing grid size.
 * 
 * B. OpenGL Display Lists:
 *    Instead of sending cube geometry (vertices/faces) to the GPU in every frame, 
 *    the geometry is recorded once (glGenLists) and stored in GPU memory. 
 *    Rendering is triggered by a single command (glCallList).
 * 
 * C. Array Flattening (1D instead of 3D):
 *    Replaced nested arrays 'grid[x][y][z]' with a flat 'byte[] grid'. 
 *    Indexing is done via 'x + (y * RES) + (z * RES_SQ)'. This is more 
 *    cache-friendly and reduces Java's object overhead.
 * 
 * D. Conditional Rendering:
 *    The engine only calls OpenGL drawing functions for cells that are 
 *    actually alive. This skips thousands of unnecessary draw calls in 
 *    sparse simulations.
 */

package modeling3d;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.spatial.SpatialUtil;
import java.util.stream.IntStream;

public class GameOfLife3D extends com.eco.bio7.compile.Model {

	// Grid Settings - Increase RES for larger simulations (e.g., 40 or 50)
	private final int RES = 50;
	private final int RES_SQ = RES * RES;
	private final int totalCells = RES * RES * RES;

	// Using byte arrays to save memory and improve CPU cache performance
	private byte[] grid = new byte[totalCells];
	private byte[] nextGrid = new byte[totalCells];

	// OpenGL Optimization: Display List ID
	private int cubeList = -1;
	private final float size = 30.0f;
	private final float gap = 35.0f;

	/* Start after compilation if run action is active! */
	public GameOfLife3D() {
		setup();
	}

	// Called when the class is compiled! Action in the main toolbar!
	public void setup() {
		setupModel();
	}

	private void setupModel() {
		/*Start the run method!*/
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

		// Initialize grid with a random seed using parallel streams for speed
		IntStream.range(0, totalCells).parallel().forEach(i -> {
			grid[i] = (byte) (Math.random() > 0.85 ? 1 : 0);
		});
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setupModel();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// Compile the cube geometry once onto the GPU memory
		if (cubeList == -1) {
			cubeList = gl.glGenLists(1);
			gl.glNewList(cubeList, GL_COMPILE);
			glut.glutSolidCube(size);
			gl.glEndList();
		}

		// Trigger logic update via Bio7 control panel
		if (SpatialUtil.canStep()) {
			calculateParallel();
		}

		renderFast(gl);
	}

	private void calculateParallel() {
		// Use all available CPU cores to process the 3D grid logic
		IntStream.range(0, totalCells).parallel().forEach(i -> {
			// De-flatten 1D index back to 3D coordinates
			int z = i / RES_SQ;
			int y = (i % RES_SQ) / RES;
			int x = i % RES;

			int neighbors = countNeighbors(x, y, z);

			// Standard 3D GOL Rules: 4-5/5 (Survival/Birth)
			if (grid[i] == 1) {
				// Survival
				nextGrid[i] = (byte) ((neighbors == 4 || neighbors == 5) ? 1 : 0);
			} else {
				// Birth
				nextGrid[i] = (byte) (neighbors == 5 ? 1 : 0);
			}
		});

		// Fast memory copy to update the main grid
		System.arraycopy(nextGrid, 0, grid, 0, totalCells);
	}

	private int countNeighbors(int x, int y, int z) {
		int count = 0;
		// Search all 26 direct neighbors in the 3x3x3 neighborhood
		for (int dz = -1; dz <= 1; dz++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -1; dx <= 1; dx++) {
					if (dx == 0 && dy == 0 && dz == 0)
						continue;

					int nx = x + dx, ny = y + dy, nz = z + dz;
					// Boundary check
					if (nx >= 0 && nx < RES && ny >= 0 && ny < RES && nz >= 0 && nz < RES) {
						count += grid[nx + ny * RES + nz * RES_SQ];
					}
				}
			}
		}
		return count;
	}

	private void renderFast(GL2 gl) {
		float offset = (RES * gap) / 2.0f;
		gl.glPushMatrix();
		// Center the entire grid in the view
		gl.glTranslatef(-offset, -offset, -offset);

		gl.glEnable(GL_COLOR_MATERIAL);

		for (int i = 0; i < totalCells; i++) {
			// Only draw live cells to save rendering time
			if (grid[i] == 1) {
				int z = i / RES_SQ;
				int y = (i % RES_SQ) / RES;
				int x = i % RES;

				gl.glPushMatrix();
				gl.glTranslatef(x * gap, y * gap, z * gap);

				// Dynamic color based on cell position
				gl.glColor3f(x / (float) RES, y / (float) RES, z / (float) RES);

				// Execute pre-compiled GPU drawing command
				gl.glCallList(cubeList);

				gl.glPopMatrix();
			}
		}
		gl.glPopMatrix();
	}
}
