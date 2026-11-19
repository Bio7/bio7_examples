package modeling3d.artificial;

import static com.jogamp.opengl.GL2.*;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.*;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.Overlay;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.spatial.SpatialUtil;
import com.eco.bio7.collection.CustomView;
import com.eco.bio7.collection.Work;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ARTIFICIAL LIFE WITH NEURAL EVOLUTION
 * 
 * --- OVERVIEW ---
 * Advanced generative model featuring creatures with neural networks
 * that evolve through natural selection over multiple generations.
 * 
 * --- CREATURES ---
 * - Neural-network-controlled creatures (3-layer MLP)
 * - Inputs: energy level, proximity to food, proximity to obstacles
 * - Outputs: forward speed, turn left/right, eat
 * - Yellow: well-fed creatures
 * - Orange: low energy creatures
 * - Red: dying creatures
 * 
 * --- EVOLUTION ---
 * 1. Creatures consume food to gain energy
 * 2. Movement costs energy (movement = evolution pressure)
 * 3. Reproduction at energy threshold (copies + mutation)
 * 4. Natural selection: high energy = more offspring
 * 5. Mutation: small random changes to neural weights
 * 6. Generations: fitness tracking over time
 * 
 * --- VISUALIZATION ---
 * 3D creatures move around seeking food
 * Line chart tracks:
 * - Average fitness (energy level)
 * - Max fitness (best creature)
 * - Population size
 * - Generation count
 */

public class ArtificialLife3D extends com.eco.bio7.compile.Model {

	// --- WORLD PARAMETERS ---
	private final int GRID_SIZE = 120;
	private final float CELL_SPACING = 8.0f;
	private final int FOOD_SPAWN_RATE = 3; // Food items per step

	// --- CREATURE PARAMETERS ---
	public int INITIAL_POPULATION = 80;
	public double ENERGY_COST_PER_STEP = 0.15;
	public double ENERGY_FROM_FOOD = 80.0;
	public double REPRODUCE_THRESHOLD = 150.0;
	public double MUTATION_RATE = 0.1; // 10% mutation chance
	public double MUTATION_AMOUNT = 0.3; // Max weight change

	// --- NEURAL NETWORK PARAMETERS ---
	private final int INPUT_NEURONS = 3;
	private final int HIDDEN_NEURONS = 6;
	private final int OUTPUT_NEURONS = 3;

	// --- AGENT CLASSES ---
	private class Creature {
		float x, y, z;
		float vx, vy, vz;
		float energy;
		float fitness;
		int age;
		NeuralNetwork brain;
		int genomeId;

		Creature(float x, float y, float z, NeuralNetwork brain, int genomeId) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.vx = (float) (Math.random() - 0.5) * 0.5f;
			this.vy = (float) (Math.random() - 0.5) * 0.5f;
			this.vz = 0;
			this.energy = 100.0f;
			this.fitness = 100.0f;
			this.age = 0;
			this.brain = brain;
			this.genomeId = genomeId;
		}

		Creature reproduce(int newGenomeId) {
			Creature child = new Creature(
				this.x + (float) (Math.random() - 0.5) * 5,
				this.y + (float) (Math.random() - 0.5) * 5,
				this.z,
				this.brain.mutate(MUTATION_RATE, MUTATION_AMOUNT),
				newGenomeId
			);
			child.energy = this.energy / 2;
			this.energy /= 2;
			return child;
		}

		void think(List<Food> foodList, List<Creature> creatureList) {
			// Find nearest food
			float foodDist = Float.MAX_VALUE;
			for (Food food : foodList) {
				float dx = this.x - food.x;
				float dy = this.y - food.y;
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				if (dist < foodDist) {
					foodDist = Math.min(dist, 50.0f);
				}
			}

			// Find nearest creature (obstacle avoidance)
			float creatureDist = Float.MAX_VALUE;
			for (Creature c : creatureList) {
				if (c == this) continue;
				float dx = this.x - c.x;
				float dy = this.y - c.y;
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				if (dist > 0.1f && dist < creatureDist) {
					creatureDist = Math.min(dist, 30.0f);
				}
			}

			// Normalize inputs
			float energyInput = this.energy / 200.0f;
			float foodInput = 1.0f - (foodDist / 50.0f);
			float obstacleInput = 1.0f - (creatureDist / 30.0f);

			// Network decision
			float[] inputs = { energyInput, foodInput, obstacleInput };
			float[] outputs = brain.feedforward(inputs);

			// Outputs: [forward speed, turn, eat]
			float speed = outputs[0]; // 0-1
			float turn = (outputs[1] - 0.5f) * 0.3f; // -0.15 to 0.15
			float eatSignal = outputs[2]; // 0-1

			// Apply movement
			float moveSpeed = speed * 0.8f;
			vx = (float) Math.cos(Math.atan2(vy, vx) + turn) * moveSpeed;
			vy = (float) Math.sin(Math.atan2(vy, vx) + turn) * moveSpeed;
		}

		void update() {
			x += vx;
			y += vy;
			z += vz;

			// Toroidal wrapping
			x = (x + GRID_SIZE) % GRID_SIZE;
			y = (y + GRID_SIZE) % GRID_SIZE;

			energy -= ENERGY_COST_PER_STEP;
			age++;
			fitness = energy; // Fitness = current energy
		}

		float getColor() {
			if (energy > 120) return 0.0f; // Yellow (well-fed)
			if (energy > 60) return 0.3f; // Orange (moderate)
			return 0.6f; // Red (dying)
		}
	}

	private class NeuralNetwork {
		float[][] w1; // input to hidden
		float[][] w2; // hidden to output
		float[] b1;   // hidden bias
		float[] b2;   // output bias

		NeuralNetwork() {
			w1 = new float[INPUT_NEURONS][HIDDEN_NEURONS];
			w2 = new float[HIDDEN_NEURONS][OUTPUT_NEURONS];
			b1 = new float[HIDDEN_NEURONS];
			b2 = new float[OUTPUT_NEURONS];

			// Random initialization
			for (int i = 0; i < INPUT_NEURONS; i++) {
				for (int j = 0; j < HIDDEN_NEURONS; j++) {
					w1[i][j] = (float) (Math.random() - 0.5) * 2.0f;
				}
			}
			for (int i = 0; i < HIDDEN_NEURONS; i++) {
				for (int j = 0; j < OUTPUT_NEURONS; j++) {
					w2[i][j] = (float) (Math.random() - 0.5) * 2.0f;
				}
				b1[i] = (float) (Math.random() - 0.5) * 0.5f;
			}
			for (int i = 0; i < OUTPUT_NEURONS; i++) {
				b2[i] = (float) (Math.random() - 0.5) * 0.5f;
			}
		}

		float[] feedforward(float[] inputs) {
			// Hidden layer
			float[] hidden = new float[HIDDEN_NEURONS];
			for (int i = 0; i < HIDDEN_NEURONS; i++) {
				hidden[i] = b1[i];
				for (int j = 0; j < INPUT_NEURONS; j++) {
					hidden[i] += inputs[j] * w1[j][i];
				}
				hidden[i] = Math.max(0, hidden[i]); // ReLU
			}

			// Output layer
			float[] output = new float[OUTPUT_NEURONS];
			for (int i = 0; i < OUTPUT_NEURONS; i++) {
				output[i] = b2[i];
				for (int j = 0; j < HIDDEN_NEURONS; j++) {
					output[i] += hidden[j] * w2[j][i];
				}
				output[i] = 1.0f / (1.0f + (float) Math.exp(-output[i])); // Sigmoid
			}

			return output;
		}

		NeuralNetwork mutate(double mutationRate, double mutationAmount) {
			NeuralNetwork child = new NeuralNetwork();
			
			// Copy weights
			for (int i = 0; i < INPUT_NEURONS; i++) {
				for (int j = 0; j < HIDDEN_NEURONS; j++) {
					child.w1[i][j] = this.w1[i][j];
					if (Math.random() < mutationRate) {
						child.w1[i][j] += (float) (Math.random() - 0.5) * 2.0f * (float)mutationAmount;
					}
				}
			}
			for (int i = 0; i < HIDDEN_NEURONS; i++) {
				for (int j = 0; j < OUTPUT_NEURONS; j++) {
					child.w2[i][j] = this.w2[i][j];
					if (Math.random() < mutationRate) {
						child.w2[i][j] += (float) (Math.random() - 0.5) * 2.0f * (float)mutationAmount;
					}
				}
				child.b1[i] = this.b1[i];
				if (Math.random() < mutationRate) {
					child.b1[i] += (float) (Math.random() - 0.5) * (float)mutationAmount;
				}
			}
			for (int i = 0; i < OUTPUT_NEURONS; i++) {
				child.b2[i] = this.b2[i];
				if (Math.random() < mutationRate) {
					child.b2[i] += (float) (Math.random() - 0.5) * (float)mutationAmount;
				}
			}

			return child;
		}
	}

	private class Food {
		float x, y, z;

		Food(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	// --- AGENT LISTS ---
	private List<Creature> creatureList = new ArrayList<>();
	private List<Food> foodList = new ArrayList<>();

	// --- STATISTICS ---
	private int generation = 0;
	private int genomeCounter = 0;
	private float avgFitness = 0;
	private float maxFitness = 0;
	private int[] populationHistory = new int[200];
	private float[] avgFitnessHistory = new float[200];
	private float[] maxFitnessHistory = new float[200];
	private int historyIndex = 0;

	// --- DISPLAY LISTS ---
	private int creatureDisplayList = -1;
	private int foodDisplayList = -1;

	// --- OVERLAY ---
	public Overlay chartOverlay;
	private Font titleFont = new Font("SansSerif", Font.BOLD, 26);
	private Font legendFont = new Font("SansSerif", Font.BOLD, 16);
	private Font statsFont = new Font("SansSerif", Font.BOLD, 18);

	/* Start after compilation if run action is active! */
	public ArtificialLife3D() {
		setup();
	}

	public void setup() {
		setupModel();
	}

	private void setupModel() {
		Work.closePerspective("Custom");
		Work.openPerspective("com.eco.bio7.perspective_3d");
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

		chartOverlay = SpatialUtil.createOverlay();
		initializePopulation();
		createGui();
	}

	private void createGui() {
		CustomView view = new CustomView();
		Display d = Display.getDefault();
		d.syncExec(() -> {
			Composite parent = view.getComposite("Artificial Life Control Panel", null);
			parent.setLayout(new FillLayout());
			new ArtificialLifeSettings(parent, ArtificialLife3D.this);
			parent.layout(true);
		});
	}

	private void initializePopulation() {
		creatureList.clear();
		foodList.clear();
		generation = 0;
		genomeCounter = 0;

		for (int i = 0; i < INITIAL_POPULATION; i++) {
			NeuralNetwork brain = new NeuralNetwork();
			creatureList.add(new Creature(
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE,
				0,
				brain,
				genomeCounter++
			));
		}

		// Initialize history
		for (int i = 0; i < populationHistory.length; i++) {
			populationHistory[i] = INITIAL_POPULATION;
			avgFitnessHistory[i] = 100;
			maxFitnessHistory[i] = 100;
		}
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setupModel();
		createDisplayLists(gl, glut);
	}

	private void createDisplayLists(GL2 gl, GLUT glut) {
		if (creatureDisplayList != -1) return;

		// Creature sphere
		creatureDisplayList = gl.glGenLists(1);
		gl.glNewList(creatureDisplayList, GL_COMPILE);
		glut.glutSolidSphere(1.5f, 8, 8);
		gl.glEndList();

		// Food sphere
		foodDisplayList = gl.glGenLists(1);
		gl.glNewList(foodDisplayList, GL_COMPILE);
		glut.glutSolidSphere(0.5f, 6, 6);
		gl.glEndList();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		if (creatureDisplayList == -1) {
			createDisplayLists(gl, glut);
		}

		if (SpatialUtil.canStep()) {
			spawnFood();
			updateCreatures();
			handleEating();
			handleReproduction();
			updateStatistics();
		}

		render(gl, glut);
		drawChartOverlay(gl, glu, glut);
	}

	private void spawnFood() {
		for (int i = 0; i < FOOD_SPAWN_RATE; i++) {
			foodList.add(new Food(
				(float) Math.random() * GRID_SIZE,
				(float) Math.random() * GRID_SIZE,
				0
			));
		}
	}

	private void updateCreatures() {
		for (Creature creature : creatureList) {
			creature.think(foodList, creatureList);
			creature.update();
		}

		// Remove dead creatures
		creatureList.removeIf(c -> c.energy <= 0);

		// Generation tracking
		if (creatureList.isEmpty()) {
			generation++;
		}
	}

	private void handleEating() {
		List<Food> toRemove = new ArrayList<>();
		for (Creature creature : creatureList) {
			for (Food food : foodList) {
				float dx = creature.x - food.x;
				float dy = creature.y - food.y;
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				if (dist < 2.0f) {
					creature.energy += (float)ENERGY_FROM_FOOD;
					toRemove.add(food);
				}
			}
		}
		foodList.removeAll(toRemove);
	}

	private void handleReproduction() {
		List<Creature> newCreatures = new ArrayList<>();
		for (Creature creature : creatureList) {
			if (creature.energy >= REPRODUCE_THRESHOLD) {
				newCreatures.add(creature.reproduce(genomeCounter++));
			}
		}
		creatureList.addAll(newCreatures);
	}

	private void updateStatistics() {
		if (creatureList.isEmpty()) {
			avgFitness = 0;
			maxFitness = 0;
		} else {
			avgFitness = (float) creatureList.stream().mapToDouble(c -> c.fitness).average().orElse(0);
			maxFitness = (float) creatureList.stream().mapToDouble(c -> c.fitness).max().orElse(0);
		}

		populationHistory[historyIndex] = creatureList.size();
		avgFitnessHistory[historyIndex] = avgFitness;
		maxFitnessHistory[historyIndex] = maxFitness;
		historyIndex = (historyIndex + 1) % populationHistory.length;
	}

	private void render(GL2 gl, GLUT glut) {
		gl.glPushMatrix();
		float offset = (GRID_SIZE * CELL_SPACING) / 2.0f;
		gl.glTranslatef(-offset, -offset, 0);

		gl.glEnable(GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);

		// Render food (green)
		for (Food food : foodList) {
			gl.glColor3f(0.0f, 0.8f, 0.0f);
			gl.glPushMatrix();
			gl.glTranslatef(food.x * CELL_SPACING, food.y * CELL_SPACING, food.z);
			gl.glCallList(foodDisplayList);
			gl.glPopMatrix();
		}

		// Render creatures with color based on fitness
		for (Creature creature : creatureList) {
			float hue = creature.getColor();
			if (hue < 0.1f) { // Yellow
				gl.glColor3f(1.0f, 1.0f, 0.0f);
			} else if (hue < 0.4f) { // Orange
				gl.glColor3f(1.0f, 0.7f, 0.0f);
			} else { // Red
				gl.glColor3f(1.0f, 0.2f, 0.0f);
			}
			gl.glPushMatrix();
			gl.glTranslatef(creature.x * CELL_SPACING, creature.y * CELL_SPACING, creature.z);
			gl.glCallList(creatureDisplayList);
			gl.glPopMatrix();
		}

		gl.glDisable(GL_COLOR_MATERIAL);
		gl.glPopMatrix();
	}

	private void drawChartOverlay(GL2 gl, GLU glu, GLUT glut) {
		if (chartOverlay == null) return;

		/* Not in the small view of the split view */
		if (SpatialUtil.isSplitPanelDrawing()) return;

		Graphics2D g2d = chartOverlay.createGraphics();

		// Background
		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f);
		g2d.setComposite(ac);
		g2d.setColor(new Color(15, 15, 25));
		g2d.fillRect(10, 10, 1000, 400);

		// Border
		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(3.0f));
		g2d.drawRect(10, 10, 1000, 400);

		// Title
		g2d.setFont(titleFont);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Neural Evolution - Generation " + generation, 30, 42);

		// Chart background
		g2d.setColor(new Color(35, 35, 50));
		g2d.fillRect(30, 65, 940, 250);

		// Grid lines
		g2d.setColor(new Color(80, 80, 110));
		g2d.setStroke(new BasicStroke(1.0f));
		for (int i = 0; i <= 5; i++) {
			int y = 65 + (i * 50);
			g2d.drawLine(30, y, 970, y);
		}

		// Draw data
		drawChart(g2d, 30, 65, 940, 250);

		// Legend
		g2d.setFont(legendFont);
		int legendY = 330;

		g2d.setColor(new Color(1.0f, 1.0f, 0.0f, 1.0f));
		g2d.fillRect(35, legendY, 20, 20);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Max Fitness", 65, legendY + 15);

		g2d.setColor(new Color(1.0f, 0.7f, 0.0f, 1.0f));
		g2d.fillRect(280, legendY, 20, 20);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Avg Fitness", 310, legendY + 15);

		g2d.setColor(new Color(0.0f, 0.8f, 1.0f, 1.0f));
		g2d.fillRect(550, legendY, 20, 20);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Population", 580, legendY + 15);

		// Statistics
		g2d.setFont(statsFont);
		int statsY = 378;
		g2d.setColor(Color.WHITE);
		g2d.drawString("Population: " + creatureList.size() + " | Avg Energy: " + 
			String.format("%.1f", avgFitness) + " | Max Energy: " + String.format("%.1f", maxFitness), 30, statsY);

		chartOverlay.markDirty(10, 10, 1000, 400);
		chartOverlay.drawAll();
		g2d.dispose();
	}

	private void drawChart(Graphics2D g2d, int x, int y, int width, int height) {
		int maxVal = 200;

		// Max fitness line (yellow)
		g2d.setColor(new Color(1.0f, 1.0f, 0.0f, 0.9f));
		g2d.setStroke(new BasicStroke(3.5f));
		for (int i = 1; i < maxFitnessHistory.length; i++) {
			int px1 = x + (int) ((i - 1) / (float) maxFitnessHistory.length * width);
			int py1 = y + height - (int) (maxFitnessHistory[(historyIndex + i - 1) % maxFitnessHistory.length] / maxVal * height);
			int px2 = x + (int) ((i) / (float) maxFitnessHistory.length * width);
			int py2 = y + height - (int) (maxFitnessHistory[(historyIndex + i) % maxFitnessHistory.length] / maxVal * height);
			g2d.drawLine(px1, py1, px2, py2);
		}

		// Avg fitness line (orange)
		g2d.setColor(new Color(1.0f, 0.7f, 0.0f, 0.9f));
		g2d.setStroke(new BasicStroke(3.5f));
		for (int i = 1; i < avgFitnessHistory.length; i++) {
			int px1 = x + (int) ((i - 1) / (float) avgFitnessHistory.length * width);
			int py1 = y + height - (int) (avgFitnessHistory[(historyIndex + i - 1) % avgFitnessHistory.length] / maxVal * height);
			int px2 = x + (int) ((i) / (float) avgFitnessHistory.length * width);
			int py2 = y + height - (int) (avgFitnessHistory[(historyIndex + i) % avgFitnessHistory.length] / maxVal * height);
			g2d.drawLine(px1, py1, px2, py2);
		}

		// Population line (cyan, dashed)
		g2d.setColor(new Color(0.0f, 0.8f, 1.0f, 0.8f));
		g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
									   10.0f, new float[]{5}, 0.0f));
		for (int i = 1; i < populationHistory.length; i++) {
			int px1 = x + (int) ((i - 1) / (float) populationHistory.length * width);
			int py1 = y + height - (int) (populationHistory[(historyIndex + i - 1) % populationHistory.length] / (float)INITIAL_POPULATION * height);
			int px2 = x + (int) ((i) / (float) populationHistory.length * width);
			int py2 = y + height - (int) (populationHistory[(historyIndex + i) % populationHistory.length] / (float)INITIAL_POPULATION * height);
			g2d.drawLine(px1, py1, px2, py2);
		}
	}
}