package modeling3d.predatorprey;


import static com.jogamp.opengl.GL2.*;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.*;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.Overlay;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.spatial.SpatialUtil;
import com.eco.bio7.collection.CustomView;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 3D PREDATOR-PREY ECOSYSTEM (Lotka-Volterra Dynamics)
 * 
 * --- OVERVIEW ---
 * This model simulates a realistic ecological system with three trophic levels:
 * 1. FOOD SOURCE (Grass) - Small blue spheres
 * 2. HERBIVORES (Prey) - Medium green spheres, consume grass, reproduce
 * 3. CARNIVORES (Predators) - Large red spheres, hunt herbivores, reproduce
 * 
 * --- RENDERING ---
 * Uses display lists for speed-optimized solid lighted spheres.
 * All three entities rendered as spheres with proper colors.
 * Lighting uses existing Bio7 light source.
 * 
 * --- STABLE EQUILIBRIUM ---
 * Carefully tuned parameters create stable oscillations
 * around an equilibrium point without extinction events.
 * Population oscillates around ~400 prey, ~65 predators, ~2500 food.
 */

public class PredatorPrey3D extends com.eco.bio7.compile.Model {

	// --- GRID & ENVIRONMENT ---
	private final int GRID_SIZE = 80;
	private final float CELL_SPACING = 12.0f;

	// =========================================================================
	// STABLE EQUILIBRIUM PARAMETERS
	// Tuned for smooth population cycles without crashes or extinctions
	// =========================================================================

	// --- HERBIVORE PARAMETERS (Prey/Herbivores) ---
	public double PREY_MOVE_COST = 0.25;              // Very low - herbivores survive long
	public double PREY_REPRODUCE_THRESHOLD = 90.0;    // Easy to breed
	public double PREY_REPRODUCE_COST = 50.0;         // Cheap to reproduce
	public double GRASS_ENERGY = 45.0;                // Good nutrition
	public double PREY_SENSE_RANGE = 13.0;            // Good predator avoidance
	public double PREDATOR_MOVE_SPEED = 0.75;         // Slow - easier prey escape

	// --- CARNIVORE PARAMETERS (Predators/Carnivores) ---
	public double PREDATOR_MOVE_COST = 0.75;          // Moderate cost
	public double PREDATOR_REPRODUCE_THRESHOLD = 140.0; // Moderate threshold
	public double PREDATOR_REPRODUCE_COST = 85.0;     // Reasonable cost
	public double PREDATOR_GAIN_FROM_PREY = 75.0;     // Balanced energy gain
	public double PREDATOR_SENSE_RANGE = 16.0;        // Good hunting range
	public double PREDATOR_HUNT_SPEED = 1.05;         // Moderate hunting speed

	// --- ENVIRONMENT PARAMETERS ---
	public double GRASS_REGEN_RATE = 0.22;            // Good grass regeneration
	public int MAX_PREY = 900;                        // Conservative limit
	public int MAX_PREDATORS = 110;                   // Balanced limit
	public int MAX_GRASS = 3200;                      // Stable food supply

	// --- INITIAL EQUILIBRIUM CONDITIONS ---
	private final int INITIAL_PREY = 400;
	private final int INITIAL_PREDATORS = 65;
	private final int INITIAL_GRASS = 2500;

	// --- AGENT CLASSES ---
	private class Herbivore {
		float x, y, z;
		float vx, vy, vz;
		float energy;

		Herbivore(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.vx = (float) (Math.random() - 0.5) * (float)PREDATOR_MOVE_SPEED;
			this.vy = (float) (Math.random() - 0.5) * (float)PREDATOR_MOVE_SPEED;
			this.vz = (float) (Math.random() - 0.5) * (float)PREDATOR_MOVE_SPEED;
			this.energy = 80.0f;
		}
	}

	private class Carnivore {
		float x, y, z;
		float vx, vy, vz;
		float energy;

		Carnivore(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.vx = (float) (Math.random() - 0.5) * (float)PREDATOR_HUNT_SPEED;
			this.vy = (float) (Math.random() - 0.5) * (float)PREDATOR_HUNT_SPEED;
			this.vz = (float) (Math.random() - 0.5) * (float)PREDATOR_HUNT_SPEED;
			this.energy = 150.0f;
		}
	}

	private class FoodSource {
		float x, y, z;

		FoodSource(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	// --- AGENT LISTS ---
	private List<Herbivore> herbivoreList = new ArrayList<>();
	private List<Carnivore> carnivoreList = new ArrayList<>();
	private List<FoodSource> foodList = new ArrayList<>();

	// --- STATISTICS & HISTORY ---
	private int herbivoreCount = 0, carnivoreCount = 0, foodCount = 0;
	private int[] herbivoreHistory = new int[200];
	private int[] carnivoreHistory = new int[200];
	private int[] foodHistory = new int[200];
	private int historyIndex = 0;
	private int stepCounter = 0;

	// --- OVERLAY DIMENSIONS (WIDER) ---
	private final int OVERLAY_WIDTH = 1000;
	private final int OVERLAY_HEIGHT = 400;
	private final int CHART_X = 30;
	private final int CHART_Y = 65;
	private final int CHART_WIDTH = 940;
	private final int CHART_HEIGHT = 250;

	// --- DISPLAY LISTS FOR OPTIMIZED RENDERING ---
	private int herbivoreDisplayList = -1;
	private int carnivoreDisplayList = -1;
	private int foodDisplayList = -1;

	// --- OVERLAY ---
	public Overlay chartOverlay;
	private Font titleFont = new Font("SansSerif", Font.BOLD, 26);
	private Font legendFont = new Font("SansSerif", Font.BOLD, 16);
	private Font statsFont = new Font("SansSerif", Font.BOLD, 18);

	/* Start after compilation if run action is active! */
	public PredatorPrey3D() {
		setup();
	}

	public void setup() {
		setupModel();
	}

	private void setupModel() {
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

		// Initialize chart overlay
		chartOverlay = SpatialUtil.createOverlay();

		// Initialize agents with equilibrium-balanced populations
		initializeEquilibrium();

		// Create custom view GUI for options
		createGui();
	}

	private void createGui() {
		CustomView view = new CustomView();
		Display d = Display.getDefault();
		d.syncExec(() -> {
			// Create a tab in Bio7's Custom View
			Composite parent = view.getComposite("Predator-Prey Control Panel", null);
			parent.setLayout(new FillLayout());
			// Instantiate our settings GUI class
			new PredatorPreySettings(parent, PredatorPrey3D.this);
			parent.layout(true);
		});
	}

	private void initializeEquilibrium() {
		herbivoreList.clear();
		carnivoreList.clear();
		foodList.clear();

		// Initialize herbivores
		for (int i = 0; i < INITIAL_PREY; i++) {
			herbivoreList.add(new Herbivore(
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE
			));
		}

		// Initialize carnivores - balanced with herbivore population
		for (int i = 0; i < INITIAL_PREDATORS; i++) {
			carnivoreList.add(new Carnivore(
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE
			));
		}

		// Initialize food sources
		for (int i = 0; i < INITIAL_GRASS; i++) {
			foodList.add(new FoodSource(
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE
			));
		}

		// Initialize history arrays
		for (int i = 0; i < herbivoreHistory.length; i++) {
			herbivoreHistory[i] = INITIAL_PREY;
			carnivoreHistory[i] = INITIAL_PREDATORS;
			foodHistory[i] = INITIAL_GRASS;
		}
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setupModel();
		// Create display lists on first GL setup
		createDisplayLists(gl, glut);
	}

	/**
	 * Create display lists for all three sphere types for speed optimization
	 */
	private void createDisplayLists(GL2 gl, GLUT glut) {
		if (herbivoreDisplayList != -1)
			return; // Already created

		// ===== HERBIVORE SPHERE (Green) =====
		herbivoreDisplayList = gl.glGenLists(1);
		gl.glNewList(herbivoreDisplayList, GL_COMPILE);
		gl.glColor3f(0.2f, 1.0f, 0.2f); // Bright green
		glut.glutSolidSphere(2.0f, 12, 12);
		gl.glEndList();

		// ===== CARNIVORE SPHERE (Red) =====
		carnivoreDisplayList = gl.glGenLists(1);
		gl.glNewList(carnivoreDisplayList, GL_COMPILE);
		gl.glColor3f(1.0f, 0.1f, 0.1f); // Bright red
		glut.glutSolidSphere(3.0f, 14, 14);
		gl.glEndList();

		// ===== FOOD SOURCE SPHERE (Blue) =====
		foodDisplayList = gl.glGenLists(1);
		gl.glNewList(foodDisplayList, GL_COMPILE);
		gl.glColor3f(0.2f, 0.6f, 1.0f); // Bright blue
		glut.glutSolidSphere(0.8f, 8, 8); // Small sphere for food
		gl.glEndList();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// Create display lists on first run if needed
		if (herbivoreDisplayList == -1) {
			createDisplayLists(gl, glut);
		}

		if (SpatialUtil.canStep()) {
			updateHerbivores();
			updateCarnivores();
			updateFood();
			updateStatistics();
			stepCounter++;
		}

		render(gl, glut);
		drawChartOverlay(gl, glu, glut);
	}

	private void updateHerbivores() {
		List<Herbivore> newHerbivores = new ArrayList<>();

		for (Herbivore herb : herbivoreList) {
			herb.x += herb.vx;
			herb.y += herb.vy;
			herb.z += herb.vz;

			herb.x = (herb.x + GRID_SIZE) % GRID_SIZE;
			herb.y = (herb.y + GRID_SIZE) % GRID_SIZE;
			herb.z = (herb.z + GRID_SIZE) % GRID_SIZE;

			herb.energy -= (float)PREY_MOVE_COST;

			List<FoodSource> toRemove = new ArrayList<>();
			for (FoodSource food : foodList) {
				float dx = herb.x - food.x;
				float dy = herb.y - food.y;
				float dz = herb.z - food.z;
				float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

				if (dist < 3.0f) {
					herb.energy += (float)GRASS_ENERGY;
					toRemove.add(food);
				}
			}
			foodList.removeAll(toRemove);

			for (Carnivore carn : carnivoreList) {
				float dx = herb.x - carn.x;
				float dy = herb.y - carn.y;
				float dz = herb.z - carn.z;
				float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

				if (dist < PREY_SENSE_RANGE && dist > 0.1f) {
					herb.vx = (dx / dist) * (float)PREDATOR_MOVE_SPEED * 0.5f;
					herb.vy = (dy / dist) * (float)PREDATOR_MOVE_SPEED * 0.5f;
					herb.vz = (dz / dist) * (float)PREDATOR_MOVE_SPEED * 0.5f;
				}
			}

			if (Math.random() < 0.1) {
				herb.vx = (float) (Math.random() - 0.5) * (float)PREDATOR_MOVE_SPEED;
				herb.vy = (float) (Math.random() - 0.5) * (float)PREDATOR_MOVE_SPEED;
				herb.vz = (float) (Math.random() - 0.5) * (float)PREDATOR_MOVE_SPEED;
			}

			if (herb.energy >= PREY_REPRODUCE_THRESHOLD && herbivoreList.size() < MAX_PREY) {
				herb.energy -= (float)PREY_REPRODUCE_COST;
				newHerbivores.add(new Herbivore(herb.x + (float) (Math.random() - 0.5) * 5, 
									  herb.y + (float) (Math.random() - 0.5) * 5,
									  herb.z + (float) (Math.random() - 0.5) * 5));
			}
		}

		herbivoreList.removeIf(h -> h.energy <= 0);
		herbivoreList.addAll(newHerbivores);
	}

	private void updateCarnivores() {
		List<Carnivore> newCarnivores = new ArrayList<>();

		for (Carnivore carn : carnivoreList) {
			Herbivore nearestHerb = null;
			float minDist = (float)PREDATOR_SENSE_RANGE;

			for (Herbivore herb : herbivoreList) {
				float dx = carn.x - herb.x;
				float dy = carn.y - herb.y;
				float dz = carn.z - herb.z;
				float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

				if (dist < minDist) {
					minDist = dist;
					nearestHerb = herb;
				}
			}

			if (nearestHerb != null) {
				float dx = nearestHerb.x - carn.x;
				float dy = nearestHerb.y - carn.y;
				float dz = nearestHerb.z - carn.z;
				float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

				if (dist > 0.1f) {
					carn.vx = (dx / dist) * (float)PREDATOR_HUNT_SPEED;
					carn.vy = (dy / dist) * (float)PREDATOR_HUNT_SPEED;
					carn.vz = (dz / dist) * (float)PREDATOR_HUNT_SPEED;
				}

				if (dist < 4.0f) {
					carn.energy += (float)PREDATOR_GAIN_FROM_PREY;
					herbivoreList.remove(nearestHerb);
				}
			} else {
				if (Math.random() < 0.15) {
					carn.vx = (float) (Math.random() - 0.5) * (float)PREDATOR_HUNT_SPEED;
					carn.vy = (float) (Math.random() - 0.5) * (float)PREDATOR_HUNT_SPEED;
					carn.vz = (float) (Math.random() - 0.5) * (float)PREDATOR_HUNT_SPEED;
				}
			}

			carn.x += carn.vx;
			carn.y += carn.vy;
			carn.z += carn.vz;

			carn.x = (carn.x + GRID_SIZE) % GRID_SIZE;
			carn.y = (carn.y + GRID_SIZE) % GRID_SIZE;
			carn.z = (carn.z + GRID_SIZE) % GRID_SIZE;

			carn.energy -= (float)PREDATOR_MOVE_COST;

			if (carn.energy >= PREDATOR_REPRODUCE_THRESHOLD && carnivoreList.size() < MAX_PREDATORS) {
				carn.energy -= (float)PREDATOR_REPRODUCE_COST;
				newCarnivores.add(new Carnivore(carn.x + (float) (Math.random() - 0.5) * 5,
											  carn.y + (float) (Math.random() - 0.5) * 5,
											  carn.z + (float) (Math.random() - 0.5) * 5));
			}
		}

		carnivoreList.removeIf(c -> c.energy <= 0);
		carnivoreList.addAll(newCarnivores);
	}

	private void updateFood() {
		if (foodList.size() < MAX_GRASS && Math.random() < GRASS_REGEN_RATE) {
			foodList.add(new FoodSource(
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE
			));
		}
	}

	private void updateStatistics() {
		herbivoreCount = herbivoreList.size();
		carnivoreCount = carnivoreList.size();
		foodCount = foodList.size();

		herbivoreHistory[historyIndex] = herbivoreCount;
		carnivoreHistory[historyIndex] = carnivoreCount;
		foodHistory[historyIndex] = foodCount;
		historyIndex = (historyIndex + 1) % herbivoreHistory.length;
	}

	private void render(GL2 gl, GLUT glut) {
		gl.glPushMatrix();
		float offset = (GRID_SIZE * CELL_SPACING) / 2.0f;
		gl.glTranslatef(-offset, -offset, -offset);

		// Use existing Bio7 lighting - just enable color material
		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);

		// Render food sources (blue spheres)
		for (FoodSource food : foodList) {
			gl.glPushMatrix();
			gl.glTranslatef(food.x * CELL_SPACING, food.y * CELL_SPACING, food.z * CELL_SPACING);
			gl.glCallList(foodDisplayList);
			gl.glPopMatrix();
		}

		// Render herbivores (green spheres)
		for (Herbivore herb : herbivoreList) {
			gl.glPushMatrix();
			gl.glTranslatef(herb.x * CELL_SPACING, herb.y * CELL_SPACING, herb.z * CELL_SPACING);
			gl.glCallList(herbivoreDisplayList);
			gl.glPopMatrix();
		}

		// Render carnivores (red spheres)
		for (Carnivore carn : carnivoreList) {
			gl.glPushMatrix();
			gl.glTranslatef(carn.x * CELL_SPACING, carn.y * CELL_SPACING, carn.z * CELL_SPACING);
			gl.glCallList(carnivoreDisplayList);
			gl.glPopMatrix();
		}

		gl.glDisable(GL_COLOR_MATERIAL);
		gl.glPopMatrix();
	}

	private void drawChartOverlay(GL2 gl, GLU glu, GLUT glut) {
		if (chartOverlay == null)
			return;

		if (SpatialUtil.isSplitView()) {
			if (SpatialUtil.isSplitPanelDrawing())
				return;
		}

		Graphics2D g2d = chartOverlay.createGraphics();

		// Semi-transparent dark background
		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f);
		g2d.setComposite(ac);
		g2d.setColor(new Color(15, 15, 25));
		g2d.fillRect(10, 10, OVERLAY_WIDTH, OVERLAY_HEIGHT);

		// Border
		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(3.0f));
		g2d.drawRect(10, 10, OVERLAY_WIDTH, OVERLAY_HEIGHT);

		// Title
		g2d.setFont(titleFont);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Lotka-Volterra Population Dynamics", 30, 42);

		// Draw chart background
		g2d.setColor(new Color(35, 35, 50));
		g2d.fillRect(CHART_X, CHART_Y, CHART_WIDTH, CHART_HEIGHT);

		// Draw grid lines
		g2d.setColor(new Color(80, 80, 110));
		g2d.setStroke(new BasicStroke(1.0f));
		for (int i = 0; i <= 5; i++) {
			int y = CHART_Y + (i * (CHART_HEIGHT / 5));
			g2d.drawLine(CHART_X, y, CHART_X + CHART_WIDTH, y);
		}

		// Draw data lines
		drawChart(g2d, CHART_X, CHART_Y, CHART_WIDTH, CHART_HEIGHT);

		// Draw legend - NICELY FORMATTED
		g2d.setFont(legendFont);
		int legendY = 330;
		int legendBoxSize = 20;
		int legendSpacing = 330; // Space between legend items
		
		// Herbivores legend
		g2d.setColor(new Color(0.2f, 1.0f, 0.2f, 1.0f));
		g2d.fillRect(35, legendY, legendBoxSize, legendBoxSize);
		g2d.setColor(new Color(200, 200, 200));
		g2d.setStroke(new BasicStroke(1.5f));
		g2d.drawRect(35, legendY, legendBoxSize, legendBoxSize);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Herbivores", 65, legendY + 15);

		// Carnivores legend
		g2d.setColor(new Color(1.0f, 0.1f, 0.1f, 1.0f));
		g2d.fillRect(35 + legendSpacing, legendY, legendBoxSize, legendBoxSize);
		g2d.setColor(new Color(200, 200, 200));
		g2d.drawRect(35 + legendSpacing, legendY, legendBoxSize, legendBoxSize);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Carnivores", 65 + legendSpacing, legendY + 15);

		// Food Source legend
		g2d.setColor(new Color(0.2f, 0.6f, 1.0f, 1.0f));
		g2d.fillRect(35 + (legendSpacing * 2), legendY, legendBoxSize, legendBoxSize);
		g2d.setColor(new Color(200, 200, 200));
		g2d.drawRect(35 + (legendSpacing * 2), legendY, legendBoxSize, legendBoxSize);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Food Sources", 65 + (legendSpacing * 2), legendY + 15);

		// Draw statistics panel - NICELY FORMATTED
		g2d.setFont(statsFont);
		int statsY = 378;
		g2d.setColor(new Color(0.2f, 1.0f, 0.2f, 1.0f));
		g2d.drawString("●", 30, statsY);
		g2d.setColor(Color.WHITE);
		g2d.drawString(" Herbivores: " + herbivoreCount, 50, statsY);

		g2d.setColor(new Color(1.0f, 0.1f, 0.1f, 1.0f));
		g2d.drawString("●", 350, statsY);
		g2d.setColor(Color.WHITE);
		g2d.drawString(" Carnivores: " + carnivoreCount, 370, statsY);

		g2d.setColor(new Color(0.2f, 0.6f, 1.0f, 1.0f));
		g2d.drawString("●", 750, statsY);
		g2d.setColor(Color.WHITE);
		g2d.drawString(" Food: " + foodCount, 770, statsY);

		chartOverlay.markDirty(10, 10, OVERLAY_WIDTH, OVERLAY_HEIGHT);
		chartOverlay.drawAll();
		g2d.dispose();
	}

	private void drawChart(Graphics2D g2d, int x, int y, int width, int height) {
		int maxVal = Math.max(Math.max(MAX_PREY, MAX_PREDATORS), MAX_GRASS);

		// Draw herbivore line (green) - thicker
		g2d.setColor(new Color(0.2f, 1.0f, 0.2f, 0.9f));
		g2d.setStroke(new BasicStroke(3.5f));
		for (int i = 1; i < herbivoreHistory.length; i++) {
			int px1 = x + (int) ((i - 1) / (float) herbivoreHistory.length * width);
			int py1 = y + height - (int) (herbivoreHistory[(historyIndex + i - 1) % herbivoreHistory.length] / (float) maxVal * height);
			int px2 = x + (int) ((i) / (float) herbivoreHistory.length * width);
			int py2 = y + height - (int) (herbivoreHistory[(historyIndex + i) % herbivoreHistory.length] / (float) maxVal * height);
			g2d.drawLine(px1, py1, px2, py2);
		}

		// Draw carnivore line (red) - thicker
		g2d.setColor(new Color(1.0f, 0.1f, 0.1f, 0.9f));
		g2d.setStroke(new BasicStroke(3.5f));
		for (int i = 1; i < carnivoreHistory.length; i++) {
			int px1 = x + (int) ((i - 1) / (float) carnivoreHistory.length * width);
			int py1 = y + height - (int) (carnivoreHistory[(historyIndex + i - 1) % carnivoreHistory.length] / (float) maxVal * height);
			int px2 = x + (int) ((i) / (float) carnivoreHistory.length * width);
			int py2 = y + height - (int) (carnivoreHistory[(historyIndex + i) % carnivoreHistory.length] / (float) maxVal * height);
			g2d.drawLine(px1, py1, px2, py2);
		}

		// Draw food line (blue) - thinner, dashed
		g2d.setColor(new Color(0.2f, 0.6f, 1.0f, 0.8f));
		g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
									   10.0f, new float[]{5}, 0.0f));
		for (int i = 1; i < foodHistory.length; i++) {
			int px1 = x + (int) ((i - 1) / (float) foodHistory.length * width);
			int py1 = y + height - (int) (foodHistory[(historyIndex + i - 1) % foodHistory.length] / (float) maxVal * height);
			int px2 = x + (int) ((i) / (float) foodHistory.length * width);
			int py2 = y + height - (int) (foodHistory[(historyIndex + i) % foodHistory.length] / (float) maxVal * height);
			g2d.drawLine(px1, py1, px2, py2);
		}
	}
}