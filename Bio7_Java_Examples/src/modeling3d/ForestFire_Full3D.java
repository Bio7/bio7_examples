
package modeling3d;

import static com.eco.bio7.spatial.SpatialEvents.getPressEvent;
import static com.eco.bio7.spatial.SpatialEvents.isMousePressed;
import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.spatial.SpatialPicking;
import com.eco.bio7.spatial.SpatialUtil;
import java.util.stream.IntStream;

/*
 * FULL 3D FOREST FIRE SIMULATION - BIO7
 * 
 * Features:
 * - Realistic 3D Terrain with Water levels.
 * - Slope Logic: Fire spreads faster uphill than downhill.
 * - Spatial Picking: Left-click to ignite individual trees.
 * - Graphical UI: Live statistics with dynamic bars.
 * - Performance: GPU Display Lists and Parallel Processing.
 *
 * --- SIMULATION RULES & LOGIC ---
 * 
 * 1. Neighborhood Logic:
 *    - The model uses a 2D Moore neighborhood (8 adjacent cells) for fire spread.
 * 
 * 2. Fire Propagation (Slope Effect):
 *    - Fire spreads dynamically based on terrain elevation.
 *    - Uphill Spread: Fire has a high probability (~70%) to ignite higher 
 *      neighboring trees (heat rises).
 *    - Downhill Spread: Fire has a much lower probability (~15%) to ignite 
 *      trees at a lower elevation.
 * 
 * 3. State Transitions:
 *    - Tree (1) -> Burning (2): Occurs if a neighbor is on fire or via 
 *      rare spontaneous ignition (lightning strike).
 *    - Burning (2) -> Empty (0): A burning tree is removed after one time 
 *      step, leaving behind ash/empty soil.
 *    - Empty (0) -> Tree (1): Empty land has a small chance to regrow a tree 
 *      over time, provided the location is above water level.
 * 
 * 4. Environmental Constraints:
 *    - Water Level: Areas below the defined 'waterLevel' are rendered as 
 *      lakes. No trees can grow here, and fire cannot pass through water.
 * 
 * 5. Interactive Picking:
 *    - Using SpatialPicking, the user can click any healthy tree in the 
 *      3D view to manually ignite it, overriding the natural simulation flow.
 */
public class ForestFire_Full3D extends com.eco.bio7.compile.Model {

	// Simulation Parameters
	public int RES = 60;
	private final float spacing = 45.0f;
	private final float waterLevel = -10.0f;

	private byte[] grid;
	private byte[] nextGrid;
	private float[] heightMap;

	private SpatialPicking pick = new SpatialPicking();
	private int treeDisplayList = -1;
	private int countTrees, countFire;

	public ForestFire_Full3D() {
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
		/*Start the run method!*/
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

		grid = new byte[RES * RES];
		nextGrid = new byte[RES * RES];
		heightMap = new float[RES * RES];

		// Generate Terrain (Wavy Landscape) and Forest
		for (int i = 0; i < grid.length; i++) {
			int x = i % RES;
			int y = i / RES;

			// Simple landscape generation using sine/cosine waves
			heightMap[i] = (float) (Math.sin(x * 0.15) * 35.0 + Math.cos(y * 0.15) * 35.0);

			// Only plant trees if above water level
			if (heightMap[i] > waterLevel) {
				grid[i] = (byte) (Math.random() > 0.4 ? 1 : 0);
			} else {
				grid[i] = 0; // Water / Empty
			}
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// Initialize GPU List for trees
		if (treeDisplayList == -1)
			createTreeList(gl, glut);

		// 1. Interaction: Picking
		if (isMousePressed()) {
			handlePicking(gl, glu, glut);
		}

		// 2. Logic: Simulation Step
		if (SpatialUtil.canStep()) {
			updateLogic();
		}

		// 3. Drawing
		renderTerrain(gl);
		renderForest(gl, glut);
		drawGraphicalUI(gl, glu, glut);
	}

	private void handlePicking(GL2 gl, GLU glu, GLUT glut) {
		pick.startPicking(gl, glu, getPressEvent());
		float offset = (RES * spacing) / 2.0f;

		gl.glPushMatrix();
		gl.glTranslated(-offset, -offset, 0);
		for (int i = 0; i < grid.length; i++) {
			if (grid[i] == 1) {
				gl.glPushName(i);
				gl.glPushMatrix();
				gl.glTranslated((i % RES) * spacing, (i / RES) * spacing, heightMap[i]);
				// Picking volume (cylinder)
				glut.glutSolidCylinder(10, 40, 6, 1);
				gl.glPopMatrix();
				gl.glPopName();
			}
		}
		gl.glPopMatrix();

		pick.endPicking(gl);
		int id = pick.getSelection();
		if (id >= 0 && id < grid.length)
			grid[id] = 2; // Ignite selected tree
	}

	private void updateLogic() {
		IntStream.range(0, grid.length).parallel().forEach(i -> {
			int x = i % RES, y = i / RES;
			byte state = grid[i];

			if (state == 2) {
				nextGrid[i] = 0; // Burned out
			} else if (state == 1) {
				if (checkSpread(x, y, i) || Math.random() < 0.00001)
					nextGrid[i] = 2;
				else
					nextGrid[i] = 1;
			} else {
				// Regrow only on land
				nextGrid[i] = (heightMap[i] > waterLevel && Math.random() < 0.0005) ? (byte) 1 : (byte) 0;
			}
		});
		System.arraycopy(nextGrid, 0, grid, 0, grid.length);

		// Update Statistics
		countTrees = 0;
		countFire = 0;
		for (byte s : grid) {
			if (s == 1)
				countTrees++;
			if (s == 2)
				countFire++;
		}
	}

	private boolean checkSpread(int x, int y, int i) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (dx == 0 && dy == 0)
					continue;
				int nx = x + dx, ny = y + dy;
				if (nx >= 0 && nx < RES && ny >= 0 && ny < RES) {
					int nIdx = nx + ny * RES;
					if (grid[nIdx] == 2) {
						// Uphill spread is faster than downhill spread
						float slopeDiff = heightMap[nIdx] - heightMap[i];
						double prob = (slopeDiff < 0) ? 0.7 : 0.15;
						if (Math.random() < prob)
							return true;
					}
				}
			}
		}
		return false;
	}

	private void createTreeList(GL2 gl, GLUT glut) {
		treeDisplayList = gl.glGenLists(1);
		gl.glNewList(treeDisplayList, GL_COMPILE);
		// Trunk
		gl.glColor3f(0.4f, 0.2f, 0.1f);
		glut.glutSolidCylinder(4.0, 15.0, 8, 1);
		// Canopy
		gl.glColor3f(0.0f, 0.5f, 0.1f);
		gl.glPushMatrix();
		gl.glTranslatef(0, 0, 10);
		glut.glutSolidCone(18.0, 45.0, 10, 1);
		gl.glPopMatrix();
		gl.glEndList();
	}

	private void renderTerrain(GL2 gl) {
		float offset = (RES * spacing) / 2.0f;
		gl.glPushMatrix();
		gl.glTranslated(-offset, -offset, 0);
		gl.glBegin(GL_QUADS);
		for (int x = 0; x < RES - 1; x++) {
			for (int y = 0; y < RES - 1; y++) {
				int i = x + y * RES;
				float h = heightMap[i];

				if (h <= waterLevel)
					gl.glColor3f(0.1f, 0.3f, 0.7f); // Water
				else
					gl.glColor3f(0.2f, 0.35f + (h / 150f), 0.15f); // Land

				gl.glVertex3f(x * spacing, y * spacing, h);
				gl.glVertex3f((x + 1) * spacing, y * spacing, heightMap[i + 1]);
				gl.glVertex3f((x + 1) * spacing, (y + 1) * spacing, heightMap[i + 1 + RES]);
				gl.glVertex3f(x * spacing, (y + 1) * spacing, heightMap[i + RES]);
			}
		}
		gl.glEnd();
		gl.glPopMatrix();
	}

	private void renderForest(GL2 gl, GLUT glut) {
		float offset = (RES * spacing) / 2.0f;
		gl.glPushMatrix();
		gl.glTranslated(-offset, -offset, 0);
		for (int i = 0; i < grid.length; i++) {
			if (grid[i] == 0)
				continue;
			gl.glPushMatrix();
			gl.glTranslated((i % RES) * spacing, (i / RES) * spacing, heightMap[i]);
			if (grid[i] == 1) {
				gl.glCallList(treeDisplayList);
			} else {
				// Flickering fire
				float flicker = 0.8f + (float) Math.random() * 0.5f;
				gl.glColor3f(1.0f, 0.1f, 0.0f);
				gl.glTranslated(0, 0, 25);
				gl.glScalef(flicker, flicker, flicker);
				glut.glutSolidSphere(20, 10, 10);
			}
			gl.glPopMatrix();
		}
		gl.glPopMatrix();
	}

	private void drawGraphicalUI(GL2 gl, GLU glu, GLUT glut) {
		gl.glDisable(GL_LIGHTING);
		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluOrtho2D(0, 800, 0, 600);
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		// Background Panel
		gl.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
		gl.glBegin(GL_QUADS);
		gl.glVertex2i(10, 470);
		gl.glVertex2i(320, 470);
		gl.glVertex2i(320, 590);
		gl.glVertex2i(10, 590);
		gl.glEnd();

		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glRasterPos2i(20, 570);
		glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_24, "TERRAIN FIRE CONTROL");
		gl.glRasterPos2i(20, 545);
		glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_24, "Interaction: CLICK TREE TO IGNITE");

		renderStatBar(gl, glut, 510, "Trees: " + countTrees, countTrees, 0.0f, 0.8f, 0.2f);
		renderStatBar(gl, glut, 480, "Fire:  " + countFire, countFire, 1.0f, 0.2f, 0.0f);

		gl.glPopMatrix();
		gl.glMatrixMode(GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glEnable(GL_LIGHTING);
	}

	private void renderStatBar(GL2 gl, GLUT glut, int y, String label, int val, float r, float g, float b) {
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glRasterPos2i(20, y + 5);
		glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_24, label);
		gl.glColor3f(0.2f, 0.2f, 0.2f);
		gl.glBegin(GL_QUADS);
		gl.glVertex2i(120, y);
		gl.glVertex2i(300, y);
		gl.glVertex2i(300, y + 10);
		gl.glVertex2i(120, y + 10);
		gl.glEnd();
		float barWidth = Math.min(180.0f, (val / (float) (RES * RES)) * 180.0f * 2.0f);
		gl.glColor3f(r, g, b);
		gl.glBegin(GL_QUADS);
		gl.glVertex2i(120, y);
		gl.glVertex2i(120 + (int) barWidth, y);
		gl.glVertex2i(120 + (int) barWidth, y + 10);
		gl.glVertex2i(120, y + 10);
		gl.glEnd();
	}
}
